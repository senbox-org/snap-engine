package eu.esa.snap.core.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;

import java.io.IOException;

public class BandFlipY extends Band {

    private final Band wrappedBand;

    public BandFlipY(Band original) {
        super(original.getName(),original.getDataType(), original.getRasterWidth(), original.getRasterHeight());
        this.wrappedBand = original;
    }

    @Override
    public void readRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData, ProgressMonitor pm) throws IOException {
        final int rasterHeight = getRasterHeight();

        final int sourceMax = offsetY + height - 1;
        final int targetMin = (rasterHeight - 1) - sourceMax;

        final ProductData lineBuffer = ProductData.createInstance(rasterData.getType(), width);
        super.readRasterData(offsetX, targetMin, width, height, rasterData, pm);

        final Object rasterDataRawBuffer = rasterData.getElems();
        final Object lineBufferRawData = lineBuffer.getElems();
        int srcOffset = 0;
        int targetOffset = (height - 1) * width;
        final int halfHeight = height / 2;
        for (int y = 0; y <= halfHeight; y++) {
            System.arraycopy(rasterDataRawBuffer, srcOffset, lineBufferRawData, 0, width);
            System.arraycopy(rasterDataRawBuffer, targetOffset, rasterDataRawBuffer, srcOffset, width);
            System.arraycopy(lineBufferRawData, 0, rasterDataRawBuffer, targetOffset, width);
            srcOffset += width;
            targetOffset -= width;
        }
    }

    @Override
    public boolean isProductReaderDirectlyUsable() {
        return wrappedBand.isProductReaderDirectlyUsable();
    }
}
