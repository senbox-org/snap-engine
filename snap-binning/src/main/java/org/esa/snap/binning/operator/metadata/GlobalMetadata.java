package org.esa.snap.binning.operator.metadata;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.esa.snap.binning.AggregatorConfig;
import org.esa.snap.binning.operator.BinningConfig;
import org.esa.snap.binning.operator.BinningOp;
import org.esa.snap.binning.operator.VariableConfig;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.locationtech.jts.geom.Geometry;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalMetadata {

    private static final String DATETIME_OUTPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final SimpleDateFormat DATETIME_OUTPUT_FORMAT = new SimpleDateFormat(DATETIME_OUTPUT_PATTERN);
    static {
        DATETIME_OUTPUT_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static final double RE = 6378.145;

    private final SortedMap<String, String> metaProperties;

    public static GlobalMetadata create(BinningOp operator) {
        final GlobalMetadata globalMetadata = new GlobalMetadata();

        final OperatorSpi spi = operator.getSpi();
        if (spi != null) {
            globalMetadata.extractSpiMetadata(spi);
        }
        final BinningConfig config = operator.createConfig();
        globalMetadata.extractConfigMetadata(config);
        
        if (StringUtils.isNotNullAndNotEmpty(operator.getSourceProductFormat())) {
            globalMetadata.metaProperties.put("sourceProductFormat", operator.getSourceProductFormat());
        }

        return globalMetadata;
    }

    public static GlobalMetadata create(BinningConfig config) {
        final GlobalMetadata globalMetadata = new GlobalMetadata();

        globalMetadata.extractConfigMetadata(config);

        return globalMetadata;
    }

    public void processMetadataTemplates(Path metadataTemplateDir, BinningOp operator, Product targetProduct, Logger logger) {
        final Path absTemplateDir = metadataTemplateDir.toAbsolutePath();
        final Collection<Path> files = IOHelper.listFiles(absTemplateDir, new VelocityTemplateFilter());
        if (files == null || files.isEmpty()) {
            return;
        }

        final VelocityEngine ve = createVelocityEngine(absTemplateDir, logger);
        if (ve == null) {
            return;
        }

        final VelocityContext vc = new VelocityContext();
        for (Map.Entry<String, String> entry : metaProperties.entrySet()) {
            vc.internalPut(entry.getKey(), entry.getValue());
        }
        vc.put("operator", operator);
        vc.put("targetProduct", targetProduct);
        vc.put("metadataProperties", metaProperties);

        for (Path file : files) {
            processMetadataTemplate(file, ve, vc, logger);
        }
    }

    public SortedMap<String, String> asSortedMap() {
        return metaProperties;
    }

    public MetadataElement asMetadataElement() {
        final MetadataElement processingGraph = new MetadataElement("Processing_Graph");
        final MetadataElement node_0 = new MetadataElement("node.0");
        final MetadataElement parameters = new MetadataElement("parameters");
        for (final String name : metaProperties.keySet()) {
            final String value = metaProperties.get(name);
            parameters.addAttribute(new MetadataAttribute(name, ProductData.createInstance(value), true));
        }
        ProductData processingTime = ProductData.createInstance(DATETIME_OUTPUT_FORMAT.format(new Date()));
        node_0.addAttribute(new MetadataAttribute("processingTime", processingTime, false));
        node_0.addElement(parameters);
        processingGraph.addElement(node_0);
        return processingGraph;
    }

    public void load(Path propertiesFile, Logger logger) {
        if (propertiesFile == null) {
            return;
        }
        if (!Files.isRegularFile(propertiesFile)) {
            logger.warning(String.format("Metadata properties file '%s' not found", propertiesFile));
            return;
        }

        logger.info(String.format("Reading metadata properties file '%s'...", propertiesFile));
        try (Reader reader = Files.newBufferedReader(propertiesFile)) {
            final Properties properties = new Properties();
            properties.load(reader);
            for (String name : properties.stringPropertyNames()) {
                metaProperties.put(name, properties.getProperty(name));
            }
        } catch (IOException e) {
            final String msgPattern = "Failed to load metadata properties file '%s': %s";
            logger.warning(String.format(msgPattern, propertiesFile, e.getMessage()));
        }
    }

    private static VelocityEngine createVelocityEngine(Path absTemplateDir, Logger logger) {
        final Properties veConfig = new Properties();
        if (absTemplateDir.equals(Path.of(".").toAbsolutePath())) {
            veConfig.setProperty("file.resource.loader.path", absTemplateDir.toString());
        }

        final VelocityEngine ve = new VelocityEngine();
        try {
            ve.init(veConfig);
        } catch (Exception e) {
            final String msgPattern = "Can't generate metadata file(s): Failed to initialise Velocity engine: %s";
            logger.log(Level.SEVERE, String.format(msgPattern, e.getMessage()), e);
            return null;
        }
        return ve;
    }

    private static void processMetadataTemplate(Path templateFile, VelocityEngine ve, VelocityContext vc, Logger logger) {
        final String templateName = templateFile.getFileName().toString();
        final String outputName = templateName.substring(0, templateName.lastIndexOf('.'));
        logger.info(String.format("Writing metadata file '%s'...", outputName));

        try (Writer writer = Files.newBufferedWriter(Path.of(outputName))) {
            ve.mergeTemplate(templateName, RuntimeConstants.ENCODING_DEFAULT, vc, writer);
        } catch (Exception e) {
            final String msgPattern = "Failed to generate metadata file from template '%s': %s";
            logger.log(Level.SEVERE, String.format(msgPattern, templateName, e.getMessage()), e);
        }
    }

    private void extractConfigMetadata(BinningConfig config) {
        final String outputPath = config.getOutputFile();
        if (StringUtils.isNotNullAndNotEmpty(outputPath)) {
            final Path outputFile = Path.of(outputPath);
            metaProperties.put("product_name", FileUtils.getFilenameWithoutExtension(outputFile.getFileName()));
        }

        // provisionally moved to node attributes as it is not a binning parameter, boe, 2014-11-04
        //final SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_OUTPUT_PATTERN, Locale.ENGLISH);
        //metaProperties.put("processing_time", dateFormat.format(new Date()));

        final String startDateTime = config.getStartDateTime();
        if (StringUtils.isNotNullAndNotEmpty(startDateTime)) {
            metaProperties.put("aggregation_period_start", startDateTime);
        }

        final Double periodDuration = config.getPeriodDuration();
        if (periodDuration != null) {
            metaProperties.put("aggregation_period_duration", Double.toString(periodDuration) + " day(s)");
        }

        final Geometry region = config.getRegion();
        if (region != null) {
            metaProperties.put("region", region.toString());
        }

        final BinningOp.TimeFilterMethod timeFilterMethod = config.getTimeFilterMethod();
        if (isTimeFilterMetadataRequired(timeFilterMethod)) {
            metaProperties.put("time_filter_method", timeFilterMethod.toString());
            if (timeFilterMethod == BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY) {
                final Double minDataHour = config.getMinDataHour();
                if (minDataHour != null) {
                    metaProperties.put("min_data_hour", Double.toString(minDataHour));
                }
            }
        }

        final int numRows = config.getNumRows();
        if (numRows > 0) {
            metaProperties.put("num_rows", Integer.toString(numRows));
            metaProperties.put("pixel_size_in_km", toPixelSizeString(numRows));
        }

        final Integer superSampling = config.getSuperSampling();
        if (superSampling != null) {
            metaProperties.put("super_sampling", Integer.toString(superSampling));
        }

        final String maskExpr = config.getMaskExpr();
        if (StringUtils.isNullOrEmpty(maskExpr)) {
            metaProperties.put("mask_expression", "");
        } else {
            metaProperties.put("mask_expression", maskExpr);
        }

        final VariableConfig[] variableConfigs = config.getVariableConfigs();
        if (variableConfigs != null) {
            int index = 0;
            for (final VariableConfig variableConfig : variableConfigs) {
                metaProperties.put("variable_config." + Integer.toString(index) + ":name", variableConfig.getName());
                metaProperties.put("variable_config." + Integer.toString(index) + ":expr", variableConfig.getExpr());
                String validExpr = variableConfig.getValidExpr();
                if (validExpr != null) {
                    metaProperties.put("variable_config." + Integer.toString(index) + ":validExpr", validExpr);
                }
                ++index;
            }
        }

        final AggregatorConfig[] aggregatorConfigs = config.getAggregatorConfigs();
        if (aggregatorConfigs != null) {
            int index = 0;
            for (final AggregatorConfig aggregatorConfig : aggregatorConfigs) {
                final PropertySet propertySet = aggregatorConfig.asPropertySet();
                final Property[] properties = propertySet.getProperties();
                for (final Property property : properties) {
                    String value = property.getValueAsText();
                    if (StringUtils.isNotNullAndNotEmpty(value)) {
                        metaProperties.put("aggregator_config." + Integer.toString(index) + ":" + property.getName(), value);
                    }
                }
                ++index;
            }
        }

        final String aggregatorName = config.getMetadataAggregatorName();
        if (StringUtils.isNotNullAndNotEmpty(aggregatorName)) {
            metaProperties.put("metadata_aggregator_name", aggregatorName);
        }
    }

    static boolean isTimeFilterMetadataRequired(BinningOp.TimeFilterMethod timeFilterMethod) {
        return timeFilterMethod != null && timeFilterMethod != BinningOp.TimeFilterMethod.NONE;
    }

    static String toPixelSizeString(int numRows) {
        return Double.toString((RE * Math.PI) / (numRows - 1));
    }

    private void extractSpiMetadata(OperatorSpi spi) {
        final OperatorDescriptor descriptor = spi.getOperatorDescriptor();
        if (descriptor != null) {
            metaProperties.put("software_qualified_name", descriptor.getName());
            metaProperties.put("software_name", descriptor.getAlias());
            metaProperties.put("software_version", descriptor.getVersion());
        }
    }

    GlobalMetadata() {
        metaProperties = new TreeMap<>();
    }

    private static class VelocityTemplateFilter implements Predicate<Path> {
        @Override
        public boolean test(Path dir) {
            return dir.getFileName().toString().endsWith(".vm");
        }
    }
}
