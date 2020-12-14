package org.esa.snap.dem.dataio.copernicus;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


public class CopernicusGeoTIFFReader extends AbstractProductReader {
    /**
     * Constructs a new abstract product reader. Handles the .TAR archive format that Copernicus GTED DEM products are
     * delivered in. Can also take in just a geotiff as well.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be {@code null} for internal reader
     *                     implementations
     */
    private ImageInputStream imageInputStream = null;
    private GeoTiffProductReaderPlugIn geotiffReaderPlugin = new GeoTiffProductReaderPlugIn();
    private File tarFile;
    protected CopernicusGeoTIFFReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final Path inputPath = ReaderUtils.getPathFromInput(getInput());
        try {
            if (inputPath == null) {
                throw new IOException("Unable to read Copernicus DTED file " + getInput().toString());
            }
            final String inputFileName = inputPath.getFileName().toString();
            final String ext = FileUtils.getExtension(inputFileName);
            ProductReader geotiffPR = geotiffReaderPlugin.createReaderInstance();
            Product geotiffProduct = geotiffPR.readProductNodes(inputPath.toFile(), null);
            geotiffProduct.getBandAt(0).setName("elevation");
            return geotiffProduct;

        }catch (IOException e ){
            try {
                close();
                return null;
            } catch (IOException ignored) {
            }
            throw e;
        }
    }


    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {


    }
    @Override
    public void close() throws IOException {
        super.close();
        if (imageInputStream != null) {
            imageInputStream.close();
            imageInputStream = null;
        }
        if (tarFile != null) {
            tarFile = null;
        }
    }

}
