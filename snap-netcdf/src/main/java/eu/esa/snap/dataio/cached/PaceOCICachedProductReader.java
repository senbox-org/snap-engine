package eu.esa.snap.dataio.cached;

import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.dataio.cache.*;
import eu.esa.snap.core.datamodel.band.BandUsingReaderDirectly;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.esa.snap.dataio.netcdf.util.ReaderUtils;
import org.jspecify.annotations.NonNull;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.esa.snap.dataio.netcdf.util.DataTypeUtils.getRasterDataType;
import static org.esa.snap.dataio.netcdf.util.MetadataUtils.readNetcdfMetadata;

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

        netcdfFile = NetcdfFileOpener.open(fileLocation.getPath());
        if (netcdfFile == null) {
            throw new IOException("Failed to open file " + fileLocation.getPath());
        }

        productCache = new ProductCache(this);
        CacheManager.getInstance().register(productCache);

        final int width = getProductWidth(netcdfFile);
        final int height = getProductHeight(netcdfFile);

        final String productName = getProductName();
        // @todo 2 tb product type copied from SeaDAS bundle - migrate later 2026-01-13
        final Product product = new Product(productName, "PaceOCI_L1B", width, height, this);

        setProductDescription(product);
        readNetcdfMetadata(netcdfFile, product.getMetadataRoot());

        final List<Variable> variables = netcdfFile.getVariables();
        addVariables(product, variables);

        product.setAutoGrouping("rhot_blue:rhot_red:rhot_SWIR:qual_blue:qual_red:qual_SWIR:Lt_blue:Lt_red:Lt_SWIR");

        return product;
    }

    private void setProductDescription(Product product) {
        final Attribute title = netcdfFile.getRootGroup().findAttribute("title");
        if (title != null) {
            product.setDescription(title.getStringValue());
        }
    }

    private void addVariables(Product product, List<Variable> variables) throws IOException {
        for (Variable variable : variables) {
            final String parentGroupName = variable.getParentGroup().getShortName();
            if (parentGroupName.equals("sensor_band_parameters") || parentGroupName.equals("scan_line_attributes")) {
                continue;
            }

            final int rank = variable.getRank();
            final Band[] bandsAdded;
            if (rank == 2) {
                bandsAdded = add2DVariable(product, variable);
            } else if (rank == 3) {
                bandsAdded = add3DVariable(product, variable);
            } else {
                continue;
            }

            addBandProperties(variable, bandsAdded);

            variablesMap.put(variable.getShortName(), variable);
        }

        final Variable blueWvlVariable = netcdfFile.findVariable("sensor_band_parameters/blue_wavelength");
        final Array blueWvlArray = blueWvlVariable.read();
        final float[] blueWvls = (float[]) blueWvlArray.get1DJavaArray(DataType.FLOAT);

        final Variable redWvlVariable = netcdfFile.findVariable("sensor_band_parameters/red_wavelength");
        final Array redWvlArray = redWvlVariable.read();
        final float[] redWvls = (float[]) redWvlArray.get1DJavaArray(DataType.FLOAT);

        final Variable swirWvlVariable = netcdfFile.findVariable("sensor_band_parameters/SWIR_wavelength");
        final Array swirWvlArray = swirWvlVariable.read();
        final float[] swirWvls = (float[]) swirWvlArray.get1DJavaArray(DataType.FLOAT);

        final float blueRedBandwidth = 5.0f;
        final Variable swirBandwidthVariable = netcdfFile.findVariable("sensor_band_parameters/SWIR_bandpass");
        final Array swirBandwidthArray = swirBandwidthVariable.read();
        final float[] swirBandwidths = (float[]) swirBandwidthArray.get1DJavaArray(DataType.FLOAT);
        final Band[] bands = product.getBands();
        int spectralBandIndex = 0;
        for (final Band band : bands) {
            final String bandName = band.getName();

            if (bandName.contains("_blue_")) {
                final int underscoreIdx = bandName.lastIndexOf("_");
                final int layerIndex = Integer.parseInt(bandName.substring(underscoreIdx + 1)) - 1;
                band.setSpectralWavelength(blueWvls[layerIndex]);
                band.setSpectralBandwidth(blueRedBandwidth);
                band.setSpectralBandIndex(spectralBandIndex++);
            } else if (bandName.contains("_red_")) {
                final int underscoreIdx = bandName.lastIndexOf("_");
                final int layerIndex = Integer.parseInt(bandName.substring(underscoreIdx + 1)) - 1;
                band.setSpectralWavelength(redWvls[layerIndex]);
                band.setSpectralBandwidth(blueRedBandwidth);
                band.setSpectralBandIndex(spectralBandIndex++);
            } else if (bandName.contains("_SWIR_")) {
                final int underscoreIdx = bandName.lastIndexOf("_");
                final int layerIndex = Integer.parseInt(bandName.substring(underscoreIdx + 1)) - 1;
                band.setSpectralWavelength(swirWvls[layerIndex]);
                band.setSpectralBandwidth(swirBandwidths[layerIndex]);
                band.setSpectralBandIndex(spectralBandIndex++);
            }
        }
    }

    // @todo 1 tb add tests 2026-01-14
    static void addBandProperties(Variable variable, Band[] bandsAdded) {
        for (final Band band : bandsAdded) {
            final String unitsString = variable.getUnitsString();
            if (StringUtils.isNotNullAndNotEmpty(unitsString)) {
                band.setUnit(unitsString);
            }

            final Attribute longNameAttribute = variable.findAttribute("long_name");
            if (longNameAttribute != null) {
                band.setDescription(longNameAttribute.getStringValue());
            }

            final Attribute scaleFactorAttribute = variable.findAttribute("scale_factor");
            if (scaleFactorAttribute != null) {
                final Number numericValue = scaleFactorAttribute.getNumericValue(0);
                if (numericValue != null) {
                    band.setScalingFactor(numericValue.doubleValue());
                }
            }

            final Attribute offsetAttribute = variable.findAttribute("add_offset");
            if (offsetAttribute != null) {
                final Number numericValue = offsetAttribute.getNumericValue(0);
                if (numericValue != null) {
                    band.setScalingOffset(numericValue.doubleValue());
                }
            }

            final Attribute fillValueAttribute = variable.findAttribute("_FillValue");
            if (fillValueAttribute != null) {
                final Number numericValue = fillValueAttribute.getNumericValue(0);
                if (numericValue != null) {
                    band.setNoDataValue(numericValue.doubleValue());
                    band.setNoDataValueUsed(true);
                }
            }

            final Attribute flagMasksAttribute = variable.findAttribute("flag_masks");
            final Attribute flagMeaningsAttribute = variable.findAttribute("flag_meanings");
            if (flagMasksAttribute != null && flagMeaningsAttribute != null) {
                // @todo 1 tb add flag coding - refactor functionality from S3reader 2026-01-14
            }
        }
    }

    private Band[] add2DVariable(Product product, Variable variable) {
        final int[] dimensions = variable.getShape();
        final int height = dimensions[0];
        final int width = dimensions[1];

        final int sceneRasterWidth = product.getSceneRasterWidth();
        final int sceneRasterHeight = product.getSceneRasterHeight();
        if (height != sceneRasterHeight || width != sceneRasterWidth) {
            return new Band[0];
        }

        int rasterDataType;
        if (ReaderUtils.mustScale(variable)) {
            rasterDataType = ProductData.TYPE_FLOAT64;
        } else {
            rasterDataType = getRasterDataType(variable.getDataType(), false);
        }

        final Band band = new BandUsingReaderDirectly(variable.getShortName(), rasterDataType, width, height);
        product.addBand(band);
        return new Band[]{band};
    }

    private Band[] add3DVariable(Product product, Variable variable) {
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
            return new Band[0];
        }

        int rasterDataType;
        if (ReaderUtils.mustScale(variable)) {
            rasterDataType = ProductData.TYPE_FLOAT64;
        } else {
            rasterDataType = getRasterDataType(variable.getDataType(), false);
        }

        final List<Band> bandList = new ArrayList<>();
        final String shortName = variable.getShortName();
        DecimalFormat decimalFormat = getDecimalFormat(numLayers);
        for (int layer = 1; layer <= numLayers; layer++) {
            final String layer_ext = decimalFormat.format(layer);
            final String layeredVariableName = shortName + "_" + layer_ext;
            final Band band = new BandUsingReaderDirectly(layeredVariableName, rasterDataType, width, height);
            product.addBand(band);
            bandList.add(band);
        }

        return bandList.toArray(new Band[0]);
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
        final Attribute nameAttribute = netcdfFile.getRootGroup().findAttribute("product_name");
        if (nameAttribute != null) {
            productName = nameAttribute.getStringValue();
        } else {
            productName = FileUtils.getFilenameFromPath(getInput().toString());
        }
        return productName;
    }

    static int getDimensionLength(String dimensionName, NetcdfFile netcdfFile) {
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
        final int spectralBandIndex = destBand.getSpectralBandIndex();
        if (spectralBandIndex >= 0) {
            final int underscoreIdx = destBandName.lastIndexOf("_");
            final int layerIndex = Integer.parseInt(destBandName.substring(underscoreIdx + 1)) - 1;
            destBandName = destBandName.substring(0, underscoreIdx);

            offsets = new int[]{layerIndex, sourceOffsetY, sourceOffsetX};
            shapes = new int[]{1, sourceHeight, sourceWidth};
        } else {
            offsets = new int[]{sourceOffsetY, sourceOffsetX};
            shapes = new int[]{sourceHeight, sourceWidth};
        }

        final int[] targetOffsets = {destOffsetY, destOffsetX};
        final int[] targetShapes = {destHeight, destWidth};
        final DataBuffer targetBuffer = new DataBuffer(destBuffer, targetOffsets, targetShapes);
        productCache.read(destBandName, offsets, shapes, targetBuffer);
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
        variableDescriptor.name = variableName;
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
    public DataBuffer readCacheBlock(String variableName, int[] offsets, int[] shapes, ProductData targetData) throws IOException {
        final Variable netcdfVariable = variablesMap.get(variableName);
        int rasterDataType = getRasterDataType(netcdfVariable);

        Array rawBuffer;
        synchronized (netcdfFile) {
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

            if (targetData == null) {
                targetData = createTargetDataBuffer(shapes, rasterDataType);
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
                case ProductData.TYPE_UINT8:
                    targetData.setElems(rawBuffer.get1DJavaArray(DataType.BYTE));
                    break;
                default:
                    throw new IOException("Unknown data type: " + rasterDataType);
            }
        }

        return new DataBuffer(targetData, offsets, shapes);
    }

    private static @NonNull ProductData createTargetDataBuffer(int[] shapes, int rasterDataType) throws IOException {
        ProductData targetData;
        if (shapes.length == 2) {
            targetData = ProductData.createInstance(rasterDataType, shapes[0] * shapes[1]);
        } else if (shapes.length == 3) {
            targetData = ProductData.createInstance(rasterDataType, shapes[0] * shapes[1] * shapes[2]);
        } else {
            throw new IOException("Illegal shaped variable");
        }
        return targetData;
    }

    static int getProductHeight(NetcdfFile netcdfFile) {
        int numberOfScans = getDimensionLength("scans", netcdfFile);
        if (numberOfScans <= 0) {
            numberOfScans = getDimensionLength("number_of_scans", netcdfFile);
        }
        return numberOfScans;
    }

    static int getProductWidth(NetcdfFile netcdfFile) {
        int ccdPixels = getDimensionLength("pixels", netcdfFile);
        if (ccdPixels <= 0) {
            ccdPixels = getDimensionLength("ccd_pixels", netcdfFile);
        }
        return ccdPixels;
    }
}
