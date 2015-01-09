package org.esa.beam.dataio.bigtiff;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;

public class BigGeoTiffProductReader extends AbstractProductReader {

    private ImageInputStream inputStream;

    protected BigGeoTiffProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected synchronized Product readProductNodesImpl() throws IOException {
        File inputFile = null;
        Object input = getInput();
        if (input instanceof String) {
            input = new File((String) input);
        }
        if (input instanceof File) {
            inputFile = (File) input;
        }
        inputStream = ImageIO.createImageInputStream(input);
        //return readGeoTIFFProduct(inputStream, inputFile);
        return null;
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {

    }
}
