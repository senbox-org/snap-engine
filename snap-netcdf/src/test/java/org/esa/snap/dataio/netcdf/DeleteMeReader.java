package org.esa.snap.dataio.netcdf;

import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.dataio.cache.*;
import eu.esa.snap.core.datamodel.band.BandUsingReaderDirectly;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;

import static org.esa.snap.dataio.netcdf.util.DataTypeUtils.getRasterDataType;

public class DeleteMeReader extends AbstractProductReader implements CacheDataProvider {

    private NetcdfFile netcdfFile;
    private ProductCache productCache;

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

        productCache = new ProductCache(this);
        CacheManager.getInstance().register(productCache);

        // get Dimensions "ccd_pixels"=width and "number_of_scans"=height
        final int width = getDimensionLength("ccd_pixels");
        final int height = getDimensionLength("number_of_scans");

        final Product product = new Product("dit", "dat", width, height, this);

        final Band heightBand = new BandUsingReaderDirectly("height", ProductData.TYPE_INT16, width, height);
        product.addBand(heightBand);

        return product;
    }

    private int getDimensionLength(String dimensionName) {
        final Dimension dimension = netcdfFile.findDimension(dimensionName);
        if (dimension == null) {
            return -1;
        }
        return dimension.getLength();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        final int[] offsets = {sourceOffsetY, sourceOffsetX};
        final int[] shapes = {sourceHeight, sourceWidth};

        ProductData read = productCache.read(destBand, offsets, shapes);

        // @todo 1 tb/tb copy to appropriate location in target buffer
        // @todo 2 take subsampling into account
    }

    @Override
    public void readTiePointGridRasterData(TiePointGrid tpg, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        super.readTiePointGridRasterData(tpg, destOffsetX, destOffsetY, destWidth, destHeight, destBuffer, pm);
    }

    @Override
    public void close() throws IOException {
        super.close();

        if (productCache != null) {
            CacheManager.getInstance().remove(productCache);
            productCache = null;
        }

        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
    }

    @Override
    public VariableDescriptor getVariableDescriptor(String variableName) {
        final Variable heightVar = netcdfFile.findVariable("geolocation_data/height");

        final VariableDescriptor variableDescriptor = new VariableDescriptor();
        variableDescriptor.dataType = getRasterDataType(heightVar.getDataType(), false);

        int[] shape = heightVar.getShape();

        final Array chunkSizesValues;
        final Attribute chunkSizes = heightVar.findAttribute("_ChunkSizes");
        if (chunkSizes != null) {
            chunkSizesValues = chunkSizes.getValues();
        } else {
            chunkSizesValues = Array.factory(heightVar.getDataType(), shape);
        }

        if (shape.length == 2) {
            variableDescriptor.width = shape[1];
            variableDescriptor.height = shape[0];
            variableDescriptor.layer = -1;

            variableDescriptor.tileWidth = chunkSizesValues.getInt(1);
            variableDescriptor.tileHeight = chunkSizesValues.getInt(0);
            variableDescriptor.tileLayer = -1;
        } else if (shape.length == 3) {
            variableDescriptor.width = shape[2];
            variableDescriptor.height = shape[1];
            variableDescriptor.layer = shape[0];

            variableDescriptor.tileWidth = chunkSizesValues.getInt(2);
            variableDescriptor.tileHeight = chunkSizesValues.getInt(1);
            variableDescriptor.tileLayer = chunkSizesValues.getInt(0);
        }

        return variableDescriptor;
    }
}
