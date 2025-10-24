package org.esa.snap.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.dataio.cache.CacheDataProvider;
import eu.esa.snap.core.dataio.cache.CacheManager;
import eu.esa.snap.core.dataio.cache.ProductCache;
import eu.esa.snap.core.dataio.cache.VariableDescriptor;
import org.esa.snap.core.dataio.*;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;

public class DeleteMeReader extends AbstractProductReader implements CacheDataProvider {

    private NetcdfFile netcdfFile;
    private ProductCache cache;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be {@code null} for internal reader
     *                     implementations
     */
    protected DeleteMeReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File fileLocation = new File(getInput().toString());

        netcdfFile = NetcdfFileOpener.open(fileLocation.getPath());
        if (netcdfFile == null) {
            throw new IOException("Failed to open file " + fileLocation.getPath());
        }

        cache = new ProductCache(this);
        CacheManager.getInstance().register(cache);

        // get Dimensions "ccd_pixels"=width and "number_of_scans"=height
        final int width = getDimensionLength("ccd_pixels");
        final int height = getDimensionLength("number_of_scans");

        return new Product("dit", "dat", width, height, this);
    }

    private int getDimensionLength(String ccdPixels) {
        final Dimension widthDim = netcdfFile.findDimension(ccdPixels);
        if (widthDim == null) {
            return -1;
        }
        return widthDim.getLength();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void readTiePointGridRasterData(TiePointGrid tpg, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        super.readTiePointGridRasterData(tpg, destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, pm);
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (cache != null) {
            CacheManager.getInstance().remove(cache);
            cache = null;
        }

        if(netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
    }

    @Override
    public VariableDescriptor getVariableDescriptor(String variableName) {
        throw new RuntimeException("not implemented");
    }
}
