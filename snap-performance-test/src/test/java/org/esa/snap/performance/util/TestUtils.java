package org.esa.snap.performance.util;

import org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants;
import org.esa.snap.performance.actions.*;
import org.esa.snap.runtime.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class TestUtils {

    public static final String RESULTS_DIR = "Results";

    public static String buildProductPathString(String path, String product) {
        return path + "/" + product;
    }

    public static Action constructMeasurementActionsPipeline(Action baseAction, Parameters parameters) {
        Action currentAction = baseAction;

        if (parameters.isUseTimeAverage()) {
            currentAction = new MeasureTimeAction(currentAction);
        }
        if (parameters.isUseMaxMemoryConsumption()) {
            currentAction = new MemoryConsumptionAction(currentAction);
        }
        if (parameters.isUseThroughput()) {
            currentAction = new ThroughputAction(currentAction);
        }

        return currentAction;
    }

    public static Map<String, Double> calculateResults(List<Result> allResults, Parameters parameters) {

        boolean timeAverage = parameters.isUseTimeAverage();
        boolean memory = parameters.isUseMaxMemoryConsumption();
        boolean throughput = parameters.isUseThroughput();

        Map<String, List<Result>> groupedResults = allResults.stream()
                .filter(result -> result.getValue() instanceof Number)
                .collect(Collectors.groupingBy(Result::getName));

        Map<String, Double> averages = new HashMap<>();

        if (timeAverage && groupedResults.containsKey(ActionName.MEASURE_TIME.getName())) {
            double averageTime = groupedResults.get(ActionName.MEASURE_TIME.getName()).stream()
                    .mapToDouble(result -> ((Number) result.getValue()).doubleValue())
                    .average()
                    .orElse(0.0);
            averages.put(ActionName.MEASURE_TIME.getName(), averageTime);
        }

        if (memory && groupedResults.containsKey(ActionName.MEMORY.getName())) {
            double averageMemory = groupedResults.get(ActionName.MEMORY.getName()).stream()
                    .mapToDouble(result -> ((Number) result.getValue()).doubleValue())
                    .average()
                    .orElse(0.0);
            averages.put(ActionName.MEMORY.getName(), averageMemory);

            double maxMemory = groupedResults.get(ActionName.MEMORY.getName()).stream()
                    .mapToDouble(result -> ((Number) result.getValue()).doubleValue())
                    .max()
                    .orElse(0.0);
            averages.put("Max " + ActionName.MEMORY.getName(), maxMemory);
        }

        if (throughput && groupedResults.containsKey(ActionName.THROUGHPUT.getName())) {
            double averageThroughput = groupedResults.get(ActionName.THROUGHPUT.getName()).stream()
                    .mapToDouble(result -> ((Number) result.getValue()).doubleValue())
                    .average()
                    .orElse(0.0);
            averages.put(ActionName.THROUGHPUT.getName(), averageThroughput);
        }

        return averages;
    }

    public static PerformanceTestResult combineResults(List<Result> results1, List<Result> results2, Parameters parameters, String testName) {

        Map<String, Double> orderedResults1 = TestUtils.calculateResults(results1, parameters);
        Map<String, Double> orderedResults2 = TestUtils.calculateResults(results2, parameters);

        List<String> products = parameters.getProducts();
        String product1 = products.get(0);
        String product2 = products.size() > 1 ? products.get(1) : "";

        List<String> descriptions = new ArrayList<>();
        List<Double> run1Results = new ArrayList<>();
        List<Double> run2Results = new ArrayList<>();
        List<String> units = new ArrayList<>();

        List<String> formats = parameters.getOutputFormats();
        String format1;
        String format2;

        if (formats == null) {
            format1 = getFormat(results1);
            format2 = getFormat(results2);
        } else {
            format1 = formats.get(0);
            format2 = formats.get(1);
        }

        for (String key : orderedResults1.keySet()) {

            descriptions.add(key);
            run1Results.add(orderedResults1.get(key));
            run2Results.add(orderedResults2.get(key));

            if (key.contains(ActionName.THROUGHPUT.getName())) {
                units.add("kB/s");
            } else if (key.contains(ActionName.MEMORY.getName())) {
                units.add("MB");
            } else if (key.contains(ActionName.MEASURE_TIME.getName())) {
                units.add("ms");
            } else {
                units.add("");
            }
        }

        return new PerformanceTestResult(
                testName,
                product1,
                product2,
                format1,
                format2,
                descriptions,
                run1Results,
                run2Results,
                units
        );
    }

    private static String getFormat(List<Result> results) {
        Result formatResult = results.stream()
                .filter(result -> result.getName().equals("Format"))
                .findFirst()
                .orElse(null);
        if (formatResult == null) {
            return "";
        } else {
            return (String) formatResult.getValue();
        }
    }

    public static String createOutputDir(String outputDir, String testName, String productName, String threading) {
        String fullPath = outputDir + "/" + testName + "/" + productName + "/" + threading;
        new File(fullPath).mkdirs();
        return fullPath + "/" + productName;
    }

    public static String cutExtensionFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        if (fileName.endsWith(".znap.zip")) {
            return fileName.substring(0, fileName.length() - ".znap.zip".length());
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            // no extension present
            return fileName;
        }

        return fileName.substring(0, lastDotIndex);
    }

    public static String constructOutputFilePath(String outputPath, String outputFormat) {
        if ("BEAM-DIMAP".equalsIgnoreCase(outputFormat)) {
            outputPath += ".data";
        } else if ("NETCDF4".equalsIgnoreCase(outputFormat) || "NETCDF4-CF".equalsIgnoreCase(outputFormat)) {
            outputPath += ".nc";
        } else if ("ZNAP".equalsIgnoreCase(outputFormat)) {
            Preferences preferences = Config.instance("snap").load().preferences();
            String flavour = preferences.get(ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE, "true");

            if (flavour.equals("false")) {
                outputPath += ".znap";
            } else {
                outputPath += ".znap.zip";
            }
        }
        return outputPath;
    }

    public static void setFlavour(boolean useZip) {
        Preferences preferences = Config.instance("snap").load().preferences();

        if (!useZip) {
            preferences.put(ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE, String.valueOf(false));
            preferences.put(ZnapPreferencesConstants.PROPERTY_NAME_COMPRESSOR_ID, "zlib");
        } else {
            preferences.remove(ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE);
            preferences.remove(ZnapPreferencesConstants.PROPERTY_NAME_COMPRESSOR_ID);
        }

        try {
            preferences.flush();
        } catch (BackingStoreException e) {
            System.out.println("Config preferences for Flavour could not be stored.");
        }



    }
}
