package org.esa.beam.binning.operator.metadata;

import com.vividsolutions.jts.geom.Geometry;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.esa.beam.binning.operator.BinningOp;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GlobalMetadata {

    private static final String DATETIME_OUTPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private final SortedMap<String, String> metaProperties;

    public static GlobalMetadata create(BinningOp operator) {
        return new GlobalMetadata(operator);
    }

    public void processMetadataTemplates(File metadataTemplateDir, BinningOp operator, Product targetProduct, Logger logger) {
        final File absTemplateDir = metadataTemplateDir.getAbsoluteFile();
        final File[] files = absTemplateDir.listFiles(new VelocityTemplateFilter());
        if (files == null || files.length == 0) {
            return;
        }

        final VelocityEngine ve = createVelocityEngine(absTemplateDir, logger);
        if (ve == null) {
            return;
        }

        final VelocityContext vc = new VelocityContext(metaProperties);
        vc.put("operator", operator);
        vc.put("targetProduct", targetProduct);
        vc.put("metadataProperties", metaProperties);

        for (File file : files) {
            processMetadataTemplate(file, ve, vc, logger);
        }
    }

    public SortedMap<String, String> asSortedMap() {
        return metaProperties;
    }

    public MetadataElement asMetadataElement() {
        final MetadataElement globalAttributes = new MetadataElement("Global_Attributes");
        for (final String name : metaProperties.keySet()) {
            final String value = metaProperties.get(name);
            globalAttributes.addAttribute(new MetadataAttribute(name, ProductData.createInstance(value), true));
        }
        return globalAttributes;
    }

    public void load(File propertiesFile, Logger logger) {
        if (propertiesFile == null) {
            return;
        }
        if (!propertiesFile.isFile()) {
            logger.warning(String.format("Metadata properties file '%s' not found", propertiesFile));
            return;
        }

        logger.info(String.format("Reading metadata properties file '%s'...", propertiesFile));
        try (FileReader reader = new FileReader(propertiesFile)) {
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

    private static VelocityEngine createVelocityEngine(File absTemplateDir, Logger logger) {
        final Properties veConfig = new Properties();
        if (absTemplateDir.equals(new File(".").getAbsoluteFile())) {
            veConfig.setProperty("file.resource.loader.path", absTemplateDir.getPath());
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

    private static void processMetadataTemplate(File templateFile, VelocityEngine ve, VelocityContext vc, Logger logger) {
        final String templateName = templateFile.getName();
        final String outputName = templateName.substring(0, templateName.lastIndexOf('.'));
        logger.info(String.format("Writing metadata file '%s'...", outputName));

        try (Writer writer = new FileWriter(outputName)) {
            ve.mergeTemplate(templateName, RuntimeConstants.ENCODING_DEFAULT, vc, writer);
        } catch (Exception e) {
            final String msgPattern = "Failed to generate metadata file from template '%s': %s";
            logger.log(Level.SEVERE, String.format(msgPattern, templateName, e.getMessage()), e);
        }
    }

    private GlobalMetadata(BinningOp operator) {
        this();

        final String outputPath = operator.getOutputFile();
        if (StringUtils.isNotNullAndNotEmpty(outputPath)) {
            final File outputFile = new File(outputPath);
            metaProperties.put("product_name", FileUtils.getFilenameWithoutExtension(outputFile.getName()));
        }

        final OperatorDescriptor descriptor = operator.getSpi().getOperatorDescriptor();
        if (descriptor != null) {
            metaProperties.put("software_qualified_name", descriptor.getName());
            metaProperties.put("software_name", descriptor.getAlias());
            metaProperties.put("software_version", descriptor.getVersion());
        }

        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_OUTPUT_PATTERN, Locale.ENGLISH);
        metaProperties.put("processing_time", dateFormat.format(new Date()));

        final String startDateTime = operator.getStartDateTime();
        if (StringUtils.isNotNullAndNotEmpty(startDateTime)) {
            metaProperties.put("aggregation_period_start", startDateTime);
        }

        final Double periodDuration = operator.getPeriodDuration();
        if (periodDuration != null) {
            metaProperties.put("aggregation_period_duration", Double.toString(periodDuration) + " day(s)");
        }

        // @todo 2 tb/tb write test 2014-10-08
        final Geometry region = operator.getRegion();
        if (region != null) {
            metaProperties.put("region", region.toString());
        }

        final BinningOp.TimeFilterMethod timeFilterMethod = operator.getTimeFilterMethod();
        if (timeFilterMethod != BinningOp.TimeFilterMethod.NONE) {
            metaProperties.put("time_filter_method", timeFilterMethod.toString());
            if (timeFilterMethod == BinningOp.TimeFilterMethod.SPATIOTEMPORAL_DATA_DAY) {
                final Double minDataHour = operator.getMinDataHour();
                if (minDataHour != null) {
                    metaProperties.put("min_data_hour", Double.toString(minDataHour));
                }
            }
        }
    }

    GlobalMetadata() {
        metaProperties = new TreeMap<>();
    }

    private static class VelocityTemplateFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".vm");
        }
    }
}
