package org.esa.snap.statistics.output;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.esa.snap.statistics.tools.TimeInterval;

public class TimeConsideringCsvStatisticsWriter implements StatisticsOutputter {

    private final PrintStream csvOutput;
    private String[] algorithmNames;
    private final Statistics statisticsContainer;

    /**
     * Creates a new instance.
     *
     * @param csvOutput The target print stream where the statistics are written to.
     */
    public TimeConsideringCsvStatisticsWriter(PrintStream csvOutput) {
        this.csvOutput = csvOutput;
        statisticsContainer = new Statistics();
    }

    /**
     * {@inheritDoc}
     *
     * @param statisticsOutputContext A context providing meta-information about the statistics.
     */
    @Override
    public void initialiseOutput(StatisticsOutputContext statisticsOutputContext) {
        this.algorithmNames = statisticsOutputContext.algorithmNames;
        Arrays.sort(algorithmNames);
    }

    /**
     * {@inheritDoc}
     *
     * @param bandName   The name of the band the statistics have been computed for.
     * @param regionId   The id of the region the statistics have been computed for.
     * @param statistics The actual statistics as map. Keys are the algorithm names, values are the actual statistical values.
     */
    @Override
    public void addToOutput(String bandName, String regionId, Map<String, Object> statistics) {
        // do nothing
    }

    @Override
    public void addToOutput(String bandName, TimeInterval interval, String regionId, Map<String, Object> statistics) {
        if (!statisticsContainer.containsInterval(interval)) {
            statisticsContainer.put(interval, new TimeIntervalStatistics());
        }
        final TimeIntervalStatistics dataForTimeInterval = statisticsContainer.getDataForTimeInterval(interval);
        if (!dataForTimeInterval.containsBand(bandName)) {
            dataForTimeInterval.put(bandName, new BandStatistics());
        }
        final BandStatistics dataForBandName = dataForTimeInterval.getDataForBandName(bandName);
        if (!dataForBandName.containsRegion(regionId)) {
            dataForBandName.put(regionId, new RegionStatistics());
        }
        final RegionStatistics dataForRegionName = dataForBandName.getDataForRegionName(regionId);
        for (Map.Entry<String, Object> entry : statistics.entrySet()) {
            dataForRegionName.put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException Never.
     */
    @Override
    public void finaliseOutput() {
        if (algorithmNames == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " not initialised.");
        }

        writeHeader();

        for (TimeInterval timeInterval : statisticsContainer.getTimeIntervals()) {
            TimeIntervalStatistics dataForTimeInterval = statisticsContainer.getDataForTimeInterval(timeInterval);
            for (String bandName : dataForTimeInterval.getBandNames()) {
                final BandStatistics bandStatistics = dataForTimeInterval.getDataForBandName(bandName);
                for (String regionName : bandStatistics.getRegionNames()) {
                    csvOutput.append(regionName)
                            .append("\t")
                            .append(timeInterval.getIntervalStart().format())
                            .append("\t")
                            .append(timeInterval.getIntervalEnd().format())
                            .append("\t")
                            .append(bandName);
                    for (String algorithmName : algorithmNames) {
                        csvOutput.append("\t");
                        final RegionStatistics dataForRegionName = bandStatistics.getDataForRegionName(regionName);
                        if (dataForRegionName.containsAlgorithm(algorithmName)) {
                            Object value = dataForRegionName.getDataForAlgorithmName(algorithmName);
                            if (value instanceof Number) {
                                csvOutput.append(getValueAsString((Number) value));
                            } else {
                                csvOutput.append(value.toString());
                            }
                        }
                    }
                    csvOutput.append("\n");
                }
            }
        }
    }

    private void writeHeader() {
        csvOutput.append("# Region")
                .append("\t")
                .append("Interval Start")
                .append("\t")
                .append("Interval End")
                .append("\t")
                .append("Band");

        for (String algorithmName : algorithmNames) {
            csvOutput.append("\t")
                    .append(algorithmName);
        }
        csvOutput.append("\n");
    }

    private static String getValueAsString(Number numberValue) {
        if (numberValue instanceof Float || numberValue instanceof Double) {
            return String.format(Locale.ENGLISH, "%.4f", numberValue.doubleValue());
        }
        return numberValue.toString();
    }

    static class Statistics {

        Map<TimeInterval, TimeIntervalStatistics> statistics = new TreeMap<>();

        TimeIntervalStatistics getDataForTimeInterval(TimeInterval interval) {
            return statistics.get(interval);
        }

        boolean containsInterval(TimeInterval interval) {
            return statistics.containsKey(interval);
        }

        TimeInterval[] getTimeIntervals() {
            final Set<TimeInterval> timeIntervals = statistics.keySet();
            return timeIntervals.toArray(new TimeInterval[0]);
        }

        void put(TimeInterval timeInterval, TimeIntervalStatistics timeIntervalStatistics) {
            statistics.put(timeInterval, timeIntervalStatistics);
        }
    }

    static class TimeIntervalStatistics {

        Map<String, BandStatistics> statistics = new TreeMap<>();

        BandStatistics getDataForBandName(String bandName) {
            return statistics.get(bandName);
        }

        boolean containsBand(String bandName) {
            return statistics.containsKey(bandName);
        }

        String[] getBandNames() {
            final Set<String> bandNames = statistics.keySet();
            return bandNames.toArray(new String[0]);
        }

        void put(String bandName, BandStatistics bandStatistics) {
            statistics.put(bandName, bandStatistics);
        }

    }

    static class BandStatistics {

        Map<String, RegionStatistics> bandStatistics = new TreeMap<>();

        RegionStatistics getDataForRegionName(String regionName) {
            return bandStatistics.get(regionName);
        }

        boolean containsRegion(String regionName) {
            return bandStatistics.containsKey(regionName);
        }

        String[] getRegionNames() {
            final Set<String> regionNames = bandStatistics.keySet();
            return regionNames.toArray(new String[0]);
        }

        void put(String regionName, RegionStatistics regionStatistics) {
            bandStatistics.put(regionName, regionStatistics);
        }
    }

    static class RegionStatistics {

        Map<String, Object> regionStatistics = new HashMap<>();

        Object getDataForAlgorithmName(String algorithmName) {
            return regionStatistics.get(algorithmName);
        }

        boolean containsAlgorithm(String algorithmName) {
            return regionStatistics.containsKey(algorithmName);
        }

        void put(String algorithmName, Object value) {
            regionStatistics.put(algorithmName, value);
        }
    }

}
