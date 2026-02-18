package eu.esa.snap.core.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;

import java.io.IOException;

public class BandFlipY extends Band {

    private Band wrappedBand;

    public BandFlipY(Band original) {
        super(original.getName(),original.getDataType(), original.getRasterWidth(), original.getRasterHeight());
        this.wrappedBand = original;
    }

    @Override
    public void readRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData, ProgressMonitor pm) throws IOException {
        final int rasterHeight = getRasterHeight();
        final int mappedMax = rasterHeight - offsetY - 1;
        final int mappedMin = rasterHeight - (offsetY + height);
        final ProductData lineBuffer = ProductData.createInstance(rasterData.getType(), width);

        super.readRasterData(offsetX, mappedMin, width, height, rasterData, pm);

        final int halfHeight = height / 2;
        final Object rasterDataRawBuffer = rasterData.getElems();
        final Object  lineBufferRawData = lineBuffer.getElems();
        for (int y = 0; y < halfHeight; y++) {
            final int srcOffset = offsetY + y * width;
            System.arraycopy(rasterDataRawBuffer, srcOffset, lineBufferRawData, 0, width);

            final int dstOffset = (mappedMax - y) * width;
            System.arraycopy(rasterDataRawBuffer, dstOffset, rasterDataRawBuffer, srcOffset, width);

            System.arraycopy(lineBufferRawData, 0, rasterDataRawBuffer, dstOffset, width);
        }
    }

    @Override
    public boolean isProductReaderDirectlyUsable() {
        return wrappedBand.isProductReaderDirectlyUsable();
    }
}
