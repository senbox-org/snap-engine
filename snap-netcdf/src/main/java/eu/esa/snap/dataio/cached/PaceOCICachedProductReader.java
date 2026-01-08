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
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.esa.snap.dataio.netcdf.util.DataTypeUtils.getRasterDataType;

public class PaceOCICachedProductReader extends AbstractProductReader implements CacheDataProvider {

    private NetcdfFile netcdfFile;
    private ProductCache productCache;
    private final Map<String, Variable> variablesMap;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be {@code null} for internal reader
     *                     implementations
     */
    protected PaceOCICachedProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);

        variablesMap = new HashMap<>();
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File fileLocation = new File(getInput().toString());

        // just for testing. A reader should never alter the state of a global setting
        //NetcdfDatasets.disableNetcdfFileCache();

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

        final List<Variable> variables = netcdfFile.getVariables();
        addVariables(product, variables);

        return product;
    }

    private void addVariables(Product product, List<Variable> variables) throws IOException {
        for (Variable variable : variables) {
            final String parentGroupName = variable.getParentGroup().getShortName();
            if (parentGroupName.equals("sensor_band_parameters") || parentGroupName.equals("scan_line_attributes")) {
                continue;
            }

            final int rank = variable.getRank();
            if (rank == 2) {
                add2DVariable(product, variable);
            } else if (rank == 3) {
                add3DVariable(product, variable);
            } else {
                continue;
            }

            variablesMap.put(variable.getShortName(), variable);
        }
    }

    private void add2DVariable(Product product, Variable variable) {
        final int[] dimensions = variable.getShape();
        final int height = dimensions[0];
        final int width = dimensions[1];

        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        if (height != sceneRasterHeight || width != sceneRasterWidth) {
            return;
        }

        int rasterDataType;
        if (ReaderUtils.mustScale(variable)) {
            rasterDataType = ProductData.TYPE_FLOAT64;
        } else {
            rasterDataType = getRasterDataType(variable.getDataType(), false);
        }

        final Band band = new BandUsingReaderDirectly(variable.getShortName(), rasterDataType, width, height);
        product.addBand(band);
    }

    private void add3DVariable(Product product, Variable variable) {
        final int[] dimensions = variable.getShape();
        final int height;
        final int width;
        final int numLayers;
        if (dimensions.length == 2) {
            height = dimensions[0];
            width = dimensions[1];
            numLayers = 1;
        } else if (dimensions.length == 3) {
            numLayers = dimensions[0];
            height = dimensions[1];
            width = dimensions[2];
        } else {
            throw new RuntimeException("Invalid dimensionality");
        }

        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        if (height != sceneRasterHeight || width != sceneRasterWidth) {
            return;
        }

        int rasterDataType;
        if (ReaderUtils.mustScale(variable)) {
            rasterDataType = ProductData.TYPE_FLOAT64;
        } else {
            rasterDataType = getRasterDataType(variable.getDataType(), false);
        }

        final String shortName = variable.getShortName();
        DecimalFormat decimalFormat = getDecimalFormat(numLayers);
        for (int layer = 1; layer <= numLayers; layer++) {
            final String layer_ext = decimalFormat.format(layer);
            final String layeredVariableName = shortName + "_" + layer_ext;
            final Band band = new BandUsingReaderDirectly(layeredVariableName, rasterDataType, width, height);
            product.addBand(band);
        }
    }

    static DecimalFormat getDecimalFormat(int numLayers) {
        String pattern = "0";
        if (numLayers > 10) {
            pattern = pattern.concat("0");
        }
        if (numLayers > 100) {
            pattern = pattern.concat("0");
        }
        return new DecimalFormat(pattern);
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
        final int[] offsets;
        final int[] shapes;

        String destBandName = destBand.getName();
        int layerIndex = -1;
        final int underscoreIdx = destBandName.lastIndexOf("_");

        if (underscoreIdx > 0) {
            final String numberExt = destBandName.substring(underscoreIdx + 1);
            destBandName = destBandName.substring(0, underscoreIdx);
            layerIndex = Integer.parseInt(numberExt) - 1;

            offsets = new int[]{layerIndex, sourceOffsetY, sourceOffsetX};
            shapes = new int[]{1, sourceHeight, sourceWidth};
        } else {
            offsets = new int[]{sourceOffsetY, sourceOffsetX};
            shapes = new int[]{sourceHeight, sourceWidth};
        }

        final int[] targetOffsets = {destOffsetY, destOffsetX};
        final int[] targetShapes = {destHeight, destWidth};
        ProductData read = productCache.read(destBandName, destBuffer, offsets, shapes, targetOffsets, targetShapes);

        // @todo 2 tb/tb take subsampling into account
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
        variablesMap.clear();

        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
    }

    @Override
    public VariableDescriptor getVariableDescriptor(String variableName) throws IOException {
        // @todo 2 tb/tb look for a better solution - now we're caching all variables internally - develop a lazy solutions
        final Variable netcdVariable = variablesMap.get(variableName);
        if (netcdVariable == null) {
            throw new IOException("Variable not known: " + variableName);
        }

        final VariableDescriptor variableDescriptor = new VariableDescriptor();
        // @todo 2 tb/tb find out how to used NetCDF MAMath to scale to a desired data type.
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
            variableDescriptor.layers = -1;

            variableDescriptor.tileWidth = chunkSizesValues.getInt(1);
            variableDescriptor.tileHeight = chunkSizesValues.getInt(0);
            variableDescriptor.tileLayers = -1;
        } else if (shape.length == 3) {
            variableDescriptor.width = shape[2];
            variableDescriptor.height = shape[1];
            variableDescriptor.layers = shape[0];

            variableDescriptor.tileWidth = chunkSizesValues.getInt(2);
            variableDescriptor.tileHeight = chunkSizesValues.getInt(1);
            variableDescriptor.tileLayers = chunkSizesValues.getInt(0);
        }

        return variableDescriptor;
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    @Override
    public ProductData readCacheBlock(String variableName, int[] offsets, int[] shapes, ProductData targetData) throws IOException {
        final Variable netcdfVariable = variablesMap.get(variableName);
        int rasterDataType = getRasterDataType(netcdfVariable);

        Array rawBuffer;
        synchronized (netcdfFile) {
            try {
                rawBuffer = netcdfVariable.read(offsets, shapes);
            } catch (InvalidRangeException e) {
                throw new IOException(e);
            }
        }
        // @todo 2 tb/tb foresee that users may want the raw data 2025-12-05
        if (ReaderUtils.mustScale(netcdfVariable)) {
            rawBuffer = ReaderUtils.scaleArray(rawBuffer, netcdfVariable);
            rasterDataType = ProductData.TYPE_FLOAT64;
        }

        // @todo 1 tb/tb allocate depending on data type
        if (targetData == null) {
            if (shapes.length == 2) {
                targetData = ProductData.createInstance(rasterDataType, shapes[0] * shapes[1]);
             } else if (shapes.length == 3) {
                targetData = ProductData.createInstance(rasterDataType, shapes[0] * shapes[1] * shapes[2]);
            } else {
                throw new IOException("Illegal shaped variable");
            }
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
