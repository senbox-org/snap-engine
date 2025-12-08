package eu.esa.snap.dataio.cached;

import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.dataio.cache.CacheDataProvider;
import eu.esa.snap.core.dataio.cache.CacheManager;
import eu.esa.snap.core.dataio.cache.ProductCache;
import eu.esa.snap.core.dataio.cache.VariableDescriptor;
import eu.esa.snap.core.datamodel.band.BandUsingReaderDirectly;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.esa.snap.dataio.netcdf.util.DataTypeUtils.getRasterDataType;
import static ucar.ma2.DataType.FLOAT;

public class PaceOCICachedProductReader extends AbstractProductReader implements CacheDataProvider {

    private NetcdfFile netcdfFile;
    private ProductCache productCache;
    private final Map<String, String> variablePaths;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be {@code null} for internal reader
     *                     implementations
     */
    protected PaceOCICachedProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);

        variablePaths = new HashMap<>();
        variablePaths.put("height", "geolocation_data/height");
        variablePaths.put("sensor_azimuth", "geolocation_data/sensor_azimuth");
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

        final String productName = getProductName();
        final Product product = new Product(productName, "dat", width, height, this);

        final Band heightBand = new BandUsingReaderDirectly("height", ProductData.TYPE_INT16, width, height);
        product.addBand(heightBand);

        // @todo 2 tb/tb this should be float - find a clever way to convert.
        final Band sensorAzimuthBand = new BandUsingReaderDirectly("sensor_azimuth", ProductData.TYPE_FLOAT64, width, height);
        product.addBand(sensorAzimuthBand);

        return product;
    }

    private String getProductName() {
        String productName;
        final Attribute nameAttribute = netcdfFile.findAttribute("product_name");
        if (nameAttribute != null) {
            productName = nameAttribute.getStringValue();
        } else {
            // @todo 1 tb there must be code to extract a meaningful input name
            productName = getInput().toString();
        }
        return productName;
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
        final int[] targetOffsets = {destOffsetY, destOffsetX};
        final int[] targetShapes = {destHeight, destWidth};

        ProductData read = productCache.read(destBand.getName(), destBuffer, offsets, shapes, targetOffsets, targetShapes);

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
    public VariableDescriptor getVariableDescriptor(String variableName) throws IOException {
        // @todo 1 tb/tb foresee that it is not stored in mapping
        final String ncPath = variablePaths.get(variableName);
        final Variable netcdVariable = netcdfFile.findVariable(ncPath);
        if (netcdVariable == null) {
            throw new IOException("Variable not known: " + variableName);
        }

        final VariableDescriptor variableDescriptor = new VariableDescriptor();
        // @todo 2 tb/tb find out how to used neCDF MAMath to scale to a desired data type.
        if (ReaderUtils.mustScale(netcdVariable)) {
            variableDescriptor.dataType = ProductData.TYPE_FLOAT64;
        } else {
            variableDescriptor.dataType = getRasterDataType(netcdVariable.getDataType(), false);
        }

        int[] shape = netcdVariable.getShape();

        final Array chunkSizesValues;
        final Attribute chunkSizes = netcdVariable.findAttribute("_ChunkSizes");
        if (chunkSizes != null) {
            chunkSizesValues = chunkSizes.getValues();
        } else {
            // @todo 2 tb/tb missing default values? 2025-12-02
            chunkSizesValues = Array.factory(netcdVariable.getDataType(), shape);
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

    @Override
    public ProductData readCacheBlock(String variableName, int[] offsets, int[] shapes, ProductData targetData) throws IOException {
        // @todo 1 tb/tb foresee that it is not stored in mapping
        final String netcdfPath = variablePaths.get(variableName);
        final Variable netcdfVariable = netcdfFile.findVariable(netcdfPath);
        // todo 2 tb/tb shall we check for null? Should never happen, if so it is a programming error.
        int rasterDataType = getRasterDataType(netcdfVariable);

        System.out.println(variableName + " - read x: " + offsets[1] + " y: " + offsets[0] + " width: " + shapes[1] + " height: " + shapes[0]);
        Array rawBuffer;
        try {
            rawBuffer = netcdfVariable.read(offsets, shapes);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        }
        // @todo 2 tb/tb foresee that users may want the raw data 2025-12-05
        if (ReaderUtils.mustScale(netcdfVariable)) {
            rawBuffer = ReaderUtils.scaleArray(rawBuffer, netcdfVariable);
            rasterDataType = ProductData.TYPE_FLOAT64;
        }

        // @todo 1 tb/tb allocate depending on data type
        if (targetData == null) {
            targetData = ProductData.createInstance(rasterDataType, shapes[0] * shapes[1]);
        }

        switch (rasterDataType) {
            case ProductData.TYPE_FLOAT32:
                targetData.setElems(rawBuffer.get1DJavaArray(DataType.FLOAT));
                break;
            case ProductData.TYPE_FLOAT64:
                targetData.setElems(rawBuffer.get1DJavaArray(DataType.DOUBLE));
                break;
            case ProductData.TYPE_INT16:
                targetData.setElems(rawBuffer.get1DJavaArray(DataType.SHORT));
                break;
            default:
                throw new IOException("Unknown data type: " + rasterDataType);
        }

        return targetData;
    }
}
