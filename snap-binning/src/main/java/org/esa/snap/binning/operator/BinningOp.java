/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.binning.operator;

import com.bc.ceres.core.ProgressMonitor;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.CellProcessorConfig;
import org.esa.snap.binning.CompositingType;
import org.esa.snap.binning.DataPeriod;
import org.esa.snap.binning.ProductCustomizerConfig;
import org.esa.snap.binning.SpatialBin;
import org.esa.snap.binning.SpatialBinner;
import org.esa.snap.binning.TemporalBin;
import org.esa.snap.binning.TemporalBinSource;
import org.esa.snap.binning.TemporalBinner;
import org.esa.snap.binning.cellprocessor.CellProcessorChain;
import org.esa.snap.binning.operator.formatter.Formatter;
import org.esa.snap.binning.operator.formatter.FormatterConfig;
import org.esa.snap.binning.operator.formatter.FormatterFactory;
import org.esa.snap.binning.operator.metadata.GlobalMetadata;
import org.esa.snap.binning.operator.metadata.MetadataAggregator;
import org.esa.snap.binning.operator.metadata.MetadataAggregatorFactory;
import org.esa.snap.binning.support.SpatialDataPeriod;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphContext;
import org.esa.snap.core.gpf.graph.GraphIO;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StopWatch;
import org.esa.snap.core.util.converters.JtsGeometryConverter;
import org.esa.snap.core.util.io.WildcardMatcher;
import org.geotools.geometry.jts.JTS;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;

/*

todo - address the following BinningOp requirements (nf, 2012-03-09)

(1) allow for reading a metadata attributes file (e.g. Java Properties file) whose content will be converted
    to NetCDF global attributes. See http://oceancolor.gsfc.nasa.gov/DOCS/Ocean_Level-3_Binned_Data_Products.pdf
    for possible attributes. Ideally, we treat the metadata file as a template and fill in placeholders, e.g.
    ${operatorParameters}, or ${operatorName} or ${operatorVersion} ...
(2) For simplicity, we shall introduce a Boolean parameter 'global'. If it is true, 'region' will be ignored.

*/

/**
 * An operator that is used to perform spatial and temporal aggregations into "bin" cells for any number of source
 * product. The output is either a file comprising the resulting bins or a reprojected "map" of the bin cells
 * represented by a usual data product.
 * <p>
 * Unlike most other operators, that can compute single {@link Tile tiles},
 * the binning operator processes all
 * of its source products in its {@link #initialize()} method.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @author Thomas Storm
 */
@SuppressWarnings("UnusedDeclaration")
@OperatorMetadata(alias = "Binning",
        category = "Raster/Geometric",
        version = "1.0",
        authors = "Norman Fomferra, Marco Zühlke, Thomas Storm",
        copyright = "(c) 2014 by Brockmann Consult GmbH",
        description = "Performs spatial and temporal aggregation of pixel values into cells ('bins') of a planetary grid",
        autoWriteDisabled = true)
public class BinningOp extends Operator {

    public enum TimeFilterMethod {
        NONE,
        TIME_RANGE,
        SPATIOTEMPORAL_DATA_DAY,
    }

    public static final String DATE_INPUT_PATTERN = "yyyy-MM-dd";
    public static final String DATETIME_INPUT_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATETIME_OUTPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    @SourceProducts(description = "The source products to be binned. Must be all of the same structure.\n" +
                                  "If not given, the parameter 'sourceProductPaths' must be provided.")
    Product[] sourceProducts;

    @TargetProduct
    Product targetProduct;

    @Parameter(description = "A comma-separated list of file paths specifying the source products.\n" +
                             "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
                             "'*' (matches any character sequence in path names) and\n" +
                             "'?' (matches any single character).")
    String[] sourceProductPaths;

    @Parameter(description = "The common product format of all source products.\n" +
                             "This parameter is optional and may be used in conjunction with\n" +
                             "parameter 'sourceProductPaths'. Can be set if multiple reader are \n" +
                             "available for the source files and a specific one shall be used." +
                             "Try \"NetCDF-CF\", \"GeoTIFF\", \"BEAM-DIMAP\", or \"ENVISAT\", etc.")
    private String sourceProductFormat;

    @Parameter(description = "A comma-separated list of file paths specifying the source graphs.\n" +
            "Each path may contain the wildcards '**' (matches recursively any directory),\n" +
            "'*' (matches any character sequence in path names) and\n" +
            "'?' (matches any single character).")
    String[] sourceGraphPaths;

    @Parameter(converter = JtsGeometryConverter.class,
            description = "The considered geographical region as a geometry in well-known text format (WKT).\n" +
                          "If not given, the geographical region will be computed according to the extents of the input products.")
    Geometry region;

    @Parameter(pattern = "\\d{4}-\\d{2}-\\d{2}(\\s\\d{2}:\\d{2}:\\d{2})?",
            description = "The UTC start date of the binning period.\n" +
                          "The format is either 'yyyy-MM-dd HH:mm:ss' or 'yyyy-MM-dd'.\n" +
                          " If only the date part is given, the time 00:00:00 is assumed.")
    private String startDateTime;

    @Parameter(description = "Duration of the binning period in days.")
    private Double periodDuration;

    @Parameter(description = "The method that is used to decide which source pixels are used with respect to their observation time.\n" +
                             "'NONE': ignore pixel observation time, use all source pixels.\n" +
                             "'TIME_RANGE': use all pixels that have been acquired in the given binning period.\n" +
                             "'SPATIOTEMPORAL_DATA_DAY': use a sensor-dependent, spatial \"data-day\" definition with the goal\n" +
                             "to minimise the time between the first and last observation contributing to the same bin in the given binning period.\n" +
                             "The decision, whether a source pixel contributes to a bin or not, is a function of the pixel's observation longitude and time.\n" +
                             "Requires the parameter 'minDataHour'.",
            defaultValue = "NONE")
    private TimeFilterMethod timeFilterMethod;

    @Parameter(interval = "[0,24]",
            description = "A sensor-dependent constant given in hours of a day (0 to 24)\n" +
                          "at which a sensor has a minimum number of observations at the date line (the 180 degree meridian).\n" +
                          "Only used if parameter 'dataDayMode' is set to 'SPATIOTEMPORAL_DATADAY'.")
    private Double minDataHour;

    @Parameter(description = "Number of rows in the (global) planetary grid. Must be even.", defaultValue = "2160")
    private int numRows;

    @Parameter(description = "The square of the number of pixels used for super-sampling an input pixel into multiple sub-pixels",
            defaultValue = "1")
    private Integer superSampling;

    @Parameter(description = "Skips binning of sub-pixel if distance on earth to the center of the main-pixel is larger as this value. A value <=0 disables this check", defaultValue = "-1")
    private Integer maxDistanceOnEarth;

    @Parameter(description = "The band maths expression used to filter input pixels")
    private String maskExpr;

    @Parameter(alias = "variables", itemAlias = "variable",
            description = "List of variables. A variable will generate a virtual band\n" +
                          "in each source data product, so that it can be used as input for the binning.")
    private VariableConfig[] variableConfigs;

    @Parameter(alias = "aggregators", domConverter = AggregatorConfigDomConverter.class,
            description = "List of aggregators. Aggregators generate the bands in the binned output products")
    private AggregatorConfig[] aggregatorConfigs;

    @Parameter(alias = "postProcessor", domConverter = CellProcessorConfigDomConverter.class)
    private CellProcessorConfig postProcessorConfig;

    @Parameter(valueSet = {"Product", "RGB", "Grey"}, defaultValue = "Product")
    private String outputType;
    @Parameter
    private String outputFile;
    @Parameter(defaultValue = "BEAM-DIMAP")
    private String outputFormat;
    @Parameter(alias = "outputBands", itemAlias = "band", description = "Configures the target bands. Not needed " +
                                                                        "if output type 'Product' is chosen.")
    private BandConfiguration[] bandConfigurations;
    @Parameter(alias = "productCustomizer", domConverter = ProductCustomizerConfigDomConverter.class)
    private ProductCustomizerConfig productCustomizerConfig;

    @Parameter(description = "If true, a SeaDAS-style, binned data NetCDF file is written in addition to the\n" +
                             "target product. The output file name will be <target>-bins.nc",
            defaultValue = "false")
    private boolean outputBinnedData;

    @Parameter(description = "If true, a mapped product is written. Set this to 'false' if only a binned product is needed.",
            alias = "outputMappedProduct",
            defaultValue = "true")
    private boolean outputTargetProduct;

    @Parameter(description = "The name of the file containing metadata key-value pairs (google \"Java Properties file format\").",
            defaultValue = "./metadata.properties")
    File metadataPropertiesFile;

    @Parameter(description = "The name of the directory containing metadata templates (google \"Apache Velocity VTL format\").",
            defaultValue = ".")
    File metadataTemplateDir;

    @Parameter(description = "The type of metadata aggregation to be used. Possible values are:\n" +
                             "'NAME': aggregate the name of each input product\n" +
                             "'FIRST_HISTORY': aggregates all input product names and the processing history of the first product\n" +
                             "'ALL_HISTORIES': aggregates all input product names and processing histories",
            defaultValue = "NAME")
    private String metadataAggregatorName;


    private transient BinningContext binningContext;
    private transient FormatterConfig formatterConfig;
    private transient int numProductsAggregated;
    private transient ProductData.UTC minDateUtc;
    private transient ProductData.UTC maxDateUtc;
    private transient GlobalMetadata globalMetadata;
    private transient BinWriter binWriter;
    private transient Area regionArea;
    private transient MetadataAggregator metadataAggregator;

    @Parameter(defaultValue = "org.esa.snap.binning.support.SEAGrid")
    private String planetaryGridClass;
    private transient CompositingType compositingType;

    private final Map<Product, List<Band>> addedVariableBands;
    private Product writtenProduct;

    public BinningOp() throws OperatorException {
        addedVariableBands = new HashMap<>();
    }

    public Geometry getRegion() {
        return region;
    }

    public void setRegion(Geometry region) {
        this.region = region;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public Double getPeriodDuration() {
        return periodDuration;
    }

    public void setPeriodDuration(Double periodDuration) {
        this.periodDuration = periodDuration;
    }

    public TimeFilterMethod getTimeFilterMethod() {
        return timeFilterMethod;
    }

    public void setTimeFilterMethod(TimeFilterMethod timeFilterMethod) {
        this.timeFilterMethod = timeFilterMethod;
    }

    public Double getMinDataHour() {
        return minDataHour;
    }

    public void setMinDataHour(Double minDataHour) {
        this.minDataHour = minDataHour;
    }

    public int getNumRows() {
        return numRows;
    }

    public void setNumRows(int numRows) {
        this.numRows = numRows;
    }

    public Integer getSuperSampling() {
        return superSampling;
    }

    public void setSuperSampling(Integer superSampling) {
        this.superSampling = superSampling;
    }

    public String getMaskExpr() {
        return maskExpr;
    }

    public void setMaskExpr(String maskExpr) {
        this.maskExpr = maskExpr;
    }

    public String getSourceProductFormat() {
        return sourceProductFormat;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public VariableConfig[] getVariableConfigs() {
        return variableConfigs;
    }

    public void setVariableConfigs(VariableConfig... variableConfigs) {
        this.variableConfigs = variableConfigs;
    }

    public AggregatorConfig[] getAggregatorConfigs() {
        return aggregatorConfigs;
    }

    public void setAggregatorConfigs(AggregatorConfig... aggregatorConfigs) {
        this.aggregatorConfigs = aggregatorConfigs;
    }

    public CellProcessorConfig getPostProcessorConfig() {
        return postProcessorConfig;
    }

    public void setPostProcessorConfig(CellProcessorConfig postProcessorConfig) {
        this.postProcessorConfig = postProcessorConfig;
    }

    SortedMap<String, String> getMetadataProperties() {
        if (globalMetadata == null) {
            return null;
        }
        return globalMetadata.asSortedMap();
    }

    public void setBinWriter(BinWriter binWriter) {
        this.binWriter = binWriter;
    }

    public void setOutputTargetProduct(boolean outputTargetProduct) {
        this.outputTargetProduct = outputTargetProduct;
    }

    public void setMetadataAggregatorName(String metadataAggregatorName) {
        this.metadataAggregatorName = metadataAggregatorName;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public void setPlanetaryGridClass(String planetaryGridClass) {
        this.planetaryGridClass = planetaryGridClass;
    }

    public void setCompositingType(CompositingType compositingType) {
        this.compositingType = compositingType;
    }

    /**
     * Processes all source products and writes the output file.
     * The target product represents the written output file
     *
     * @throws OperatorException If a processing error occurs.
     */
    @Override
    public void initialize() throws OperatorException {
        formatterConfig = new FormatterConfig();
        formatterConfig.setBandConfigurations(bandConfigurations);
        formatterConfig.setOutputFile(outputFile);
        formatterConfig.setOutputFormat(outputFormat);
        formatterConfig.setOutputType(outputType);
        formatterConfig.setProductCustomizerConfig(productCustomizerConfig);

        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        validateInput();

        ProductData.UTC startDateUtc = null;
        ProductData.UTC endDateUtc = null;
        if (startDateTime != null) {
            startDateUtc = parseStartDateUtc(startDateTime);
            double startMJD = startDateUtc.getMJD();
            double endMJD = startMJD + periodDuration;
            endDateUtc = new ProductData.UTC(endMJD);
        }


        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        if (region == null) {
            // TODO use JTS directly (nf 2013-11-06)
            regionArea = new Area();
        }

        final BinningConfig binningConfig = createConfig();

        binningContext = binningConfig.createBinningContext(region, startDateUtc, periodDuration);

        BinningProductFilter productFilter = createSourceProductFilter(binningContext.getDataPeriod(),
                                                                       startDateUtc,
                                                                       endDateUtc,
                                                                       region);

        metadataAggregator = MetadataAggregatorFactory.create(metadataAggregatorName);
        numProductsAggregated = 0;

        try {
            // Step 1: Spatial binning - creates time-series of spatial bins for each bin ID ordered by ID. The tree map structure is <ID, time-series>
            SpatialBinCollection spatialBinMap = doSpatialBinning(productFilter);
            if (!spatialBinMap.isEmpty()) {
                // update region
                if (region == null && regionArea != null) {
                    region = JTS.toGeometry(regionArea, new GeometryFactory());
                }
                // Step 2: Temporal binning - creates a list of temporal bins, sorted by bin ID
                TemporalBinList temporalBins = doTemporalBinning(spatialBinMap);
                // Step 3: Formatting
                try {
                    if (startDateTime != null) {
                        writeOutput(temporalBins, startDateUtc, endDateUtc);
                    } else {
                        writeOutput(temporalBins, minDateUtc, maxDateUtc);
                    }
                } finally {
                    temporalBins.close();

                }
            } else {
                getLogger().warning("No bins have been generated, no output has been written");
            }
        } catch (OperatorException e) {
            throw e;
        } catch (Exception e) {
            throw new OperatorException(e);
        } finally {
            cleanSourceProducts();
        }

        stopWatch.stopAndTrace(String.format("Total time for binning %d product(s)", numProductsAggregated));

        globalMetadata.processMetadataTemplates(metadataTemplateDir, this, targetProduct, getLogger());
    }

    @Override
    public void dispose() {
        if (writtenProduct != null) {
            writtenProduct.dispose();
        }
        super.dispose();
    }

    public BinningConfig createConfig() {
        final BinningConfig config = new BinningConfig();
        config.setNumRows(numRows);
        config.setSuperSampling(superSampling);
        config.setMaxDistanceOnEarth(maxDistanceOnEarth);
        config.setMaskExpr(maskExpr);
        config.setVariableConfigs(variableConfigs);
        config.setAggregatorConfigs(aggregatorConfigs);
        config.setPostProcessorConfig(postProcessorConfig);
        config.setMinDataHour(minDataHour);
        config.setMetadataAggregatorName(metadataAggregatorName);
        config.setStartDateTime(startDateTime);
        config.setPeriodDuration(periodDuration);
        config.setTimeFilterMethod(timeFilterMethod);
        config.setOutputFile(outputFile);
        config.setRegion(region);
        if (planetaryGridClass != null) {
            config.setPlanetaryGrid(planetaryGridClass);
        }
        if (compositingType!= null) {
            config.setCompositingType(compositingType);
        }
        return config;
    }

    private void validateInput() {
        if (timeFilterMethod == null) {
            timeFilterMethod = TimeFilterMethod.NONE;
        }
        if (timeFilterMethod != TimeFilterMethod.NONE && (startDateTime == null || periodDuration == null)) {
            throw new OperatorException("Using a time filer requires the parameters 'startDateTime' and 'periodDuration'");
        }
        if (periodDuration != null && periodDuration < 0.0) {
            throw new OperatorException("The parameter 'periodDuration' must be a positive value");
        }
        if (timeFilterMethod == TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY && minDataHour == null) {
            throw new OperatorException("If SPATIOTEMPORAL_DATADAY filtering is used the parameters 'minDataHour' must be given");
        }
        if (sourceProducts == null
                && (sourceProductPaths == null || sourceProductPaths.length == 0)
                && (sourceGraphPaths == null || sourceGraphPaths.length == 0)) {
            String msg = "Either source products must be given or parameter 'sourceProductPaths' or parameter 'sourceGraphPaths' must be specified";
            throw new OperatorException(msg);
        }
        if (numRows < 2 || numRows % 2 != 0) {
            throw new OperatorException("Operator parameter 'numRows' must be greater than 0 and even");
        }
        if (aggregatorConfigs == null || aggregatorConfigs.length == 0) {
            throw new OperatorException("No aggregators have been defined");
        }
        if (formatterConfig.getOutputFile() == null) {
            throw new OperatorException("Missing operator parameter 'formatterConfig.outputFile'");
        }
        if (metadataTemplateDir == null || "".equals(metadataTemplateDir.getPath())) {
            metadataTemplateDir = new File(".");
        }
        if (!metadataTemplateDir.exists()) {
            String msgPattern = "Directory given by 'metadataTemplateDir' does not exist: %s";
            throw new OperatorException(String.format(msgPattern, metadataTemplateDir));
        }
        if (!outputBinnedData && !outputTargetProduct) {
            throw new OperatorException("At least one of the parameters 'outputBinnedData' and 'outputTargetProduct' must be 'true'");
        }
    }

    static BinningProductFilter createSourceProductFilter(DataPeriod dataPeriod, ProductData.UTC startTime, ProductData.UTC endTime,
                                                          Geometry region) {
        BinningProductFilter productFilter = new GeoCodingProductFilter();

        productFilter = new MultiSizeProductFilter(productFilter);

        if (dataPeriod != null) {
            if (dataPeriod instanceof SpatialDataPeriod) {
                productFilter = new SpatialDataDaySourceProductFilter(productFilter, dataPeriod);
            } else {
                productFilter = new TimeRangeProductFilter(productFilter, startTime, endTime);
            }
        }

        if (region != null) {
            productFilter = new RegionProductFilter(productFilter, region);
        }

        return productFilter;
    }

    private void cleanSourceProducts() {
        for (Map.Entry<Product, List<Band>> entry : addedVariableBands.entrySet()) {
            for (Band band : entry.getValue()) {
                entry.getKey().removeBand(band);
            }
        }
    }

    private void initMetadataProperties() {
        globalMetadata = GlobalMetadata.create(this);
        globalMetadata.load(metadataPropertiesFile, getLogger());
    }

    private static Product copyProduct(Product writtenProduct) {
        Product targetProduct = new Product(writtenProduct.getName(), writtenProduct.getProductType(),
                                            writtenProduct.getSceneRasterWidth(),
                                            writtenProduct.getSceneRasterHeight());
        targetProduct.setStartTime(writtenProduct.getStartTime());
        targetProduct.setEndTime(writtenProduct.getEndTime());
        ProductUtils.copyMetadata(writtenProduct, targetProduct);
        ProductUtils.copyGeoCoding(writtenProduct, targetProduct);
        ProductUtils.copyTiePointGrids(writtenProduct, targetProduct);
        ProductUtils.copyMasks(writtenProduct, targetProduct);
        ProductUtils.copyVectorData(writtenProduct, targetProduct);
        for (Band band : writtenProduct.getBands()) {
            // Force setting source image, otherwise GPF will set an OperatorImage and invoke computeTile()!!
            ProductUtils.copyBand(band.getName(), writtenProduct, targetProduct, true);
        }
        return targetProduct;
    }

    private SpatialBinCollection doSpatialBinning(BinningProductFilter productFilter) throws IOException {
        SpatialBinCollector spatialBinCollector = new GeneralSpatialBinCollector(binningContext.getPlanetaryGrid().getNumBins());
        final SpatialBinner spatialBinner = new SpatialBinner(binningContext, spatialBinCollector);
        if (sourceProducts != null) {
            for (Product sourceProduct : sourceProducts) {
                if (productFilter.accept(sourceProduct)) {
                    processSource(sourceProduct, spatialBinner);
                } else {
                    getLogger().warning("Filtered out product '" + sourceProduct.getFileLocation() + "'");
                    getLogger().warning("              reason: " + productFilter.getReason());
                }
            }
        }
        if (sourceProductPaths != null) {
            getLogger().info("expanding sourceProductPaths wildcards.");
            SortedSet<File> fileSet = new TreeSet<>();
            for (String filePattern : sourceProductPaths) {
                WildcardMatcher.glob(filePattern, fileSet);
            }
            if (fileSet.isEmpty()) {
                getLogger().warning("The given source file patterns did not match any files");
            } else {
                getLogger().info("found " + fileSet.size() + " product files.");
                for (File file : fileSet) {
                    getLogger().info(file.getCanonicalPath());
                }
            }
            for (File file : fileSet) {
                Product sourceProduct = null;
                try {
                    if (sourceProductFormat != null) {
                        sourceProduct = ProductIO.readProduct(file, sourceProductFormat);
                    } else {
                        sourceProduct = ProductIO.readProduct(file);
                    }
                } catch (Exception e) {
                    String msgPattern = "Failed to read file '%s'. %s: %s";
                    getLogger().severe(String.format(msgPattern, file, e.getClass().getSimpleName(), e.getMessage()));
                }
                if (sourceProduct != null) {
                    try {
                        if (productFilter.accept(sourceProduct)) {
                            processSource(sourceProduct, spatialBinner);
                        } else {
                            getLogger().warning("Filtered out product '" + sourceProduct.getFileLocation() + "'");
                            getLogger().warning("              reason: " + productFilter.getReason());
                        }
                    } finally {
                        sourceProduct.dispose();
                    }
                } else {
                    String msgPattern = "Failed to read file '%s' (not a data product or reader missing)";
                    getLogger().severe(String.format(msgPattern, file));
                }
            }
        }
        if (sourceGraphPaths != null) {
            getLogger().info("expanding sourceGraphPaths wildcards.");
            SortedSet<File> fileSet = new TreeSet<>();
            for (String filePattern : sourceGraphPaths) {
                WildcardMatcher.glob(filePattern, fileSet);
            }
            if (fileSet.isEmpty()) {
                getLogger().warning("The given graph file patterns did not match any files");
            } else {
                getLogger().info("found " + fileSet.size() + " graph files.");
                for (File file : fileSet) {
                    getLogger().info(file.getCanonicalPath());
                }
            }
            for (File file : fileSet) {
                Product sourceProduct = null;
                GraphContext graphContext = null;
                try {
                    Graph graph = GraphIO.read(new FileReader(file));
                    graphContext = new GraphContext(graph);
                    Product[] outputProducts = graphContext.getOutputProducts();
                    if (outputProducts.length != 1) {
                        getLogger().warning("Filtered out graph '" + file + "'");
                        getLogger().warning("            reason: graph has more than one 'outputNode'.");
                    } else {
                        sourceProduct = outputProducts[0];
                    }
                } catch (Exception e) {
                    String msgPattern = "Failed to execute graph from file '%s'. %s: %s";
                    getLogger().severe(String.format(msgPattern, file, e.getClass().getSimpleName(), e.getMessage()));
                }
                if (sourceProduct != null) {
                    try {
                        if (productFilter.accept(sourceProduct)) {
                            processSource(sourceProduct, spatialBinner);
                        } else {
                            getLogger().warning("Filtered out result of graph '" + file + "'");
                            getLogger().warning("                      reason: " + productFilter.getReason());
                        }
                    } finally {
                        sourceProduct.dispose();
                    }
                } else {
                    String msgPattern = "Failed to use graph '%s'";
                    getLogger().severe(String.format(msgPattern, file));
                }
                if (graphContext != null) {
                    graphContext.dispose();
                }
            }
        }
        spatialBinCollector.consumingCompleted();
        return spatialBinCollector.getSpatialBinCollection();
    }


    private void processSource(Product sourceProduct, SpatialBinner spatialBinner) throws IOException {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        updateDateRangeUtc(sourceProduct);
        metadataAggregator.aggregateMetadata(sourceProduct);

        final String productName = sourceProduct.getName();
        getLogger().info(String.format("Spatial binning of product '%s'...", productName));
        getLogger().fine(String.format("Product start time: '%s'", sourceProduct.getStartTime()));
        getLogger().fine(String.format("Product end time:   '%s'", sourceProduct.getEndTime()));
        if (region != null) {
            SubsetOp subsetOp = new SubsetOp();
            subsetOp.setSourceProduct(sourceProduct);
            subsetOp.setGeoRegion(region);
            sourceProduct = subsetOp.getTargetProduct();
            // TODO mz/nf/mp 2013-11-06: avoid creation of subset products
            //  - replace subset with rectangle as parameter to SpatialProductBinner
            //  - grow rectangle by binSize in pixel units (see lc-tools of LC-CCI project)
        }
        final long numObs = SpatialProductBinner.processProduct(sourceProduct,
                                                                spatialBinner,
                                                                addedVariableBands,
                                                                ProgressMonitor.NULL);
        stopWatch.stop();

        getLogger().info(String.format("Spatial binning of product '%s' done, %d observations seen, took %s", productName, numObs, stopWatch));

        if (region == null && regionArea != null) {
            for (GeneralPath generalPath : ProductUtils.createGeoBoundaryPaths(sourceProduct)) {
                try {
                    Area area = new Area(generalPath);
                    regionArea.add(area);
                } catch (Throwable e) {
                    getLogger().log(Level.SEVERE, String.format("Failed to handle product boundary: %s", e.getMessage()), e);
                    // sometimes the Area constructor throw an "java.lang.InternalError: Odd number of new curves!"
                    // then just ignore this geometry
                }
            }
        }

        ++numProductsAggregated;
    }

    private TemporalBinList doTemporalBinning(SpatialBinCollection spatialBinMap) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        long numberOfBins = spatialBinMap.size();
        final TemporalBinner temporalBinner = new TemporalBinner(binningContext);
        final CellProcessorChain cellChain = new CellProcessorChain(binningContext);
        final TemporalBinList temporalBins = new TemporalBinList((int) numberOfBins);
        Iterable<List<SpatialBin>> spatialBinListCollection = spatialBinMap.getBinCollection();
        int binCounter = 0;
        int percentCounter = 0;
        long hundredthOfNumBins = numberOfBins / 100;
        for (List<SpatialBin> spatialBinList : spatialBinListCollection) {
            binCounter += spatialBinList.size();

            SpatialBin spatialBin = spatialBinList.get(0);
            long spatialBinIndex = spatialBin.getIndex();
            TemporalBin temporalBin = temporalBinner.processSpatialBins(spatialBinIndex, spatialBinList);

            temporalBin = temporalBinner.computeOutput(spatialBinIndex, temporalBin);
            temporalBin = cellChain.process(temporalBin);

            temporalBins.add(temporalBin);
            if (binCounter >= hundredthOfNumBins) {
                binCounter = 0;
                getLogger().info(String.format("Finished %d%% of temporal bins", ++percentCounter));
            }
        }
        stopWatch.stop();
        getLogger().info(String.format("Temporal binning of %d bins done, took %s", numberOfBins, stopWatch));

        return temporalBins;
    }

    private void writeOutput(List<TemporalBin> temporalBins, ProductData.UTC startTime, ProductData.UTC stopTime) throws
                                                                                                                  Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        initMetadataProperties();

        if (outputBinnedData) {
            try {
                writeNetCDFBinFile(temporalBins, startTime, stopTime);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, String.format("Failed to write binned data: %s", e.getMessage()), e);
            }
        }

        if (outputTargetProduct) {
            getLogger().info(String.format("Writing mapped product '%s'...", formatterConfig.getOutputFile()));
            final MetadataElement processingGraphMetadata = getProcessingGraphMetadata();

            final Formatter defaultFormatter = FormatterFactory.get("default");
            defaultFormatter.format(binningContext.getPlanetaryGrid(),
                             getTemporalBinSource(temporalBins),
                             binningContext.getBinManager().getResultFeatureNames(),
                             formatterConfig,
                             region,
                             startTime,
                             stopTime,
                             processingGraphMetadata);
            stopWatch.stop();

            String msgPattern = "Writing mapped product '%s' done, took %s";
            getLogger().info(String.format(msgPattern, formatterConfig.getOutputFile(), stopWatch));

            if (outputType.equalsIgnoreCase("Product")) {
                final File writtenProductFile = new File(outputFile);
                String format = FormatterFactory.getOutputFormat(formatterConfig, writtenProductFile);
                writtenProduct = ProductIO.readProduct(writtenProductFile, format);
                this.targetProduct = copyProduct(writtenProduct);
            } else {
                this.targetProduct = new Product("Dummy", "t", 10, 10);
            }
        } else {
            this.targetProduct = new Product("Dummy", "t", 10, 10);
        }
    }

    private MetadataElement getProcessingGraphMetadata() {
        final MetadataElement processingGraphMetadata = globalMetadata.asMetadataElement();

        final MetadataElement node_0 = processingGraphMetadata.getElement("node.0");
        node_0.addElement(metadataAggregator.getMetadata());
        return processingGraphMetadata;
    }

    private TemporalBinSource getTemporalBinSource(List<TemporalBin> temporalBins) throws IOException {
        return new SimpleTemporalBinSource(temporalBins);
    }

    private void writeNetCDFBinFile(List<TemporalBin> temporalBins, ProductData.UTC startTime,
                                    ProductData.UTC stopTime) throws IOException {
        initBinWriter(startTime, stopTime);
        getLogger().info(String.format("Writing binned data to '%s'...", binWriter.getTargetFilePath()));
        binWriter.write(globalMetadata.asSortedMap(), temporalBins);
        getLogger().info(String.format("Writing binned data to '%s' done.", binWriter.getTargetFilePath()));

    }

    private void initBinWriter(ProductData.UTC startTime, ProductData.UTC stopTime) {
        if (binWriter == null) {
            binWriter = new SeaDASLevel3BinWriter(region, startTime, stopTime);
        }

        binWriter.setBinningContext(binningContext);
        binWriter.setTargetFileTemplatePath(formatterConfig.getOutputFile());
        binWriter.setLogger(getLogger());

    }

    // if not time rage is given construct it from the source products
    private void updateDateRangeUtc(Product sourceProduct) {
        if (startDateTime == null) {
            if (sourceProduct.getStartTime() != null) {
                if (minDateUtc == null || sourceProduct.getStartTime().getAsDate().before(minDateUtc.getAsDate())) {
                    minDateUtc = sourceProduct.getStartTime();
                }
            }
            if (sourceProduct.getEndTime() != null) {
                if (maxDateUtc == null || sourceProduct.getEndTime().getAsDate().after(maxDateUtc.getAsDate())) {
                    maxDateUtc = sourceProduct.getStartTime();
                }
            }
        }
    }

    // package access for testing only tb 2013-05-07
    static ProductData.UTC parseStartDateUtc(String date) {
        try {
            if (date.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
                return ProductData.UTC.parse(date, DATETIME_INPUT_PATTERN);
            } else {
                return ProductData.UTC.parse(date, DATE_INPUT_PATTERN);
            }
        } catch (ParseException e) {
            throw new OperatorException(String.format("Error while parsing start date parameter '%s': %s", date, e.getMessage()));
        }
    }

    public static class SimpleTemporalBinSource implements TemporalBinSource {

        private final List<TemporalBin> temporalBins;

        public SimpleTemporalBinSource(List<TemporalBin> temporalBins) {
            this.temporalBins = temporalBins;
        }

        @Override
        public int open() throws IOException {
            return 1;
        }

        @Override
        public Iterator<? extends TemporalBin> getPart(int index) throws IOException {
            return temporalBins.iterator();
        }

        @Override
        public void partProcessed(int index, Iterator<? extends TemporalBin> part) throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }

    /**
     * The service provider interface (SPI) which is referenced
     * in {@code /META-INF/services/OperatorSpi}.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BinningOp.class);
        }
    }

    public static class BandConfiguration {

        public String index;
        public String name;
        public String minValue;
        public String maxValue;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            BandConfiguration that = (BandConfiguration) o;

            if (index != null ? !index.equals(that.index) : that.index != null) {
                return false;
            }
            if (maxValue != null ? !maxValue.equals(that.maxValue) : that.maxValue != null) {
                return false;
            }
            if (minValue != null ? !minValue.equals(that.minValue) : that.minValue != null) {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = index != null ? index.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (minValue != null ? minValue.hashCode() : 0);
            result = 31 * result + (maxValue != null ? maxValue.hashCode() : 0);
            return result;
        }
    }

    private static class MultiSizeProductFilter extends BinningProductFilter {

        public MultiSizeProductFilter(BinningProductFilter parent) {
            setParent(parent);
        }

        @Override
        protected boolean acceptForBinning(Product product) {
            if (!product.isMultiSize()) {
                return true;
            } else {
                setReason("Product with rasters of different size are not supported yet.");
                return false;
            }
        }
    }
}
