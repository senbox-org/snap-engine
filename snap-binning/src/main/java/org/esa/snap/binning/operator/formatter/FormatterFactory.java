package org.esa.snap.binning.operator.formatter;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.util.ServiceLoader;
import org.esa.snap.core.util.StringUtils;

import java.io.File;
import java.util.Set;

public class FormatterFactory {

    // do not refactor - other service plugins are used by calvalus tb 2019-09-23
    private static ServiceRegistry<FormatterPlugin> formatterPlugins = null;

    public static Formatter get(String key) {
        ensureServicesLoaded();

        if (StringUtils.isNullOrEmpty(key)) {
            key = "default";
        }

        final Set<FormatterPlugin> services = formatterPlugins.getServices();
        for (final FormatterPlugin plugin : services) {
            if (plugin.getName().equalsIgnoreCase(key)) {
                return plugin.create();
            }
        }

        throw new RuntimeException("Unknown formatter key: " + key);
    }

    public static String getOutputFormat(FormatterConfig formatterConfig, File outputFile) {
        final String fileName = outputFile.getName();
        final int extPos = fileName.lastIndexOf(".");
        String outputFileNameExt = fileName.substring(extPos + 1);
        String outputFormat = formatterConfig.getOutputFormat();
        if (outputFormat == null) {
            outputFormat = outputFileNameExt.equalsIgnoreCase("nc") ? "NetCDF"
                    : outputFileNameExt.equalsIgnoreCase("dim") ? "BEAM-DIMAP"
                    : outputFileNameExt.equalsIgnoreCase("tiff") ? "GeoTIFF"
                    : outputFileNameExt.equalsIgnoreCase("png") ? "PNG"
                    : outputFileNameExt.equalsIgnoreCase("jpg") ? "JPEG" : null;
        }
        if (outputFormat == null) {
            throw new IllegalArgumentException("No output format given");
        }
        if (!outputFormat.startsWith("NetCDF")
                && !outputFormat.equalsIgnoreCase("BEAM-DIMAP")
                && !outputFormat.equalsIgnoreCase("GeoTIFF")
                && !outputFormat.startsWith("GeoTIFF")
                && !outputFormat.equalsIgnoreCase("PNG")
                && !outputFormat.equalsIgnoreCase("JPEG")) {
            throw new IllegalArgumentException("Unknown output format: " + outputFormat);
        }
        if (outputFormat.equalsIgnoreCase("NetCDF")) {
            outputFormat = "NetCDF-BEAM"; // use NetCDF with beam extensions
        }
        return outputFormat;
    }

    private static void ensureServicesLoaded() {
        if (formatterPlugins == null) {
            final ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
            formatterPlugins = serviceRegistryManager.getServiceRegistry(FormatterPlugin.class);

            ServiceLoader.loadServices(formatterPlugins);
        }
    }
}
