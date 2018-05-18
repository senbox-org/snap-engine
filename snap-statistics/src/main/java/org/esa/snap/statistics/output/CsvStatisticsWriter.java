package org.esa.snap.statistics.output;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import org.esa.snap.statistics.tools.TimeInterval;

public class CsvStatisticsWriter implements StatisticsOutputter {

    private final PrintStream csvOutput;
    private String[] measureNames;
    private Measures measures;

    /**
     * Creates a new instance.
     *
     * @param csvOutput The target print stream where the statistics are written to.
     */
    public CsvStatisticsWriter(PrintStream csvOutput) {
        this.csvOutput = csvOutput;
        this.measures = new Measures();
    }

    /**
     * {@inheritDoc}
     *
     * @param statisticsOutputContext A context providing meta-information about the statistics.
     */
    @Override
    public void initialiseOutput(StatisticsOutputContext statisticsOutputContext) {
        this.measureNames = statisticsOutputContext.algorithmNames;
        Arrays.sort(measureNames);
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
        measures.addMeasure(new Measure(bandName, null, regionId, statistics));
    }

    @Override
    public void addToOutput(String bandName, TimeInterval interval, String regionId, Map<String, Object> statistics) {
        measures.addMeasure(new Measure(bandName, interval, regionId, statistics));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IOException Never.
     */
    @Override
    public void finaliseOutput() {
        if (measureNames == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " not initialised.");
        }
        writeHeader();
        measures.sort();
        for (int i = 0; i < measures.getSize(); i++) {
            Measure measure = measures.getMeasure(i);
            if (measures.hasRegions()) {
                if (measure.regionId != null) {
                    csvOutput.append(measure.regionId).append("\t");
                } else {
                    csvOutput.append("\t").append("\t");
                }
            }
            if (measures.hasTimeIntervals()) {
                if (measure.interval != null) {
                    csvOutput.append(measure.interval.getIntervalStart().format()).append("\t")
                            .append(measure.interval.getIntervalEnd().format()).append("\t");
                } else {
                    csvOutput.append("\t").append("\t").append("\t").append("\t");
                }
            }
            if (measures.hasBands()) {
                if (measure.bandName != null) {
                    csvOutput.append(measure.bandName);
                } else {
                    csvOutput.append("\t");
                }
            }
            for (String algorithmName : measureNames) {
                csvOutput.append("\t");
                if (measure.statistics.containsKey(algorithmName)) {
                    Object value = measure.statistics.get(algorithmName);
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

    private void writeHeader() {
        if (measures.hasRegions()) {
            csvOutput.append("# Region")
                    .append("\t");
        }
        if (measures.hasTimeIntervals()) {
            csvOutput.append("Interval_Start")
                    .append("\t")
                    .append("Interval_End")
                    .append("\t");
        }
        if (measures.hasBands()) {
            csvOutput.append("Band").append("\t");
        }

        for (int i = 0; i < measureNames.length; i++) {
            String algorithmName = measureNames[i];
            csvOutput.append(algorithmName);
            if (i < measureNames.length - 1) {
                csvOutput.append("\t");
            }
        }
        csvOutput.append("\n");
    }

    static String getValueAsString(Number numberValue) {
        if (numberValue instanceof Float || numberValue instanceof Double) {
            return String.format(Locale.ENGLISH, "%.4f", numberValue.doubleValue());
        }
        return numberValue.toString();
    }

    private class Measure {

        private final String bandName;
        private final TimeInterval interval;
        private final String regionId;
        private final Map<String, Object> statistics;

        Measure(String bandName, TimeInterval interval, String regionId, Map<String, Object> statistics) {
            this.bandName = bandName;
            this.interval = interval;
            this.regionId = regionId;
            this.statistics = new TreeMap<>();
            this.statistics.putAll(statistics);
        }

    }

    private class Measures {

        final static int POLYGON_INTERVAL_BAND = 0;
        final static int POLYGON_BAND_INTERVAL = 1;
        final static int INTERVAL_POLYGON_BAND = 2;
        final static int INTERVAL_BAND_POLYGON = 3;
        final static int BAND_POLYGON_INTERVAL = 4;
        final static int BAND_INTERVAL_POLYGON = 5;

        private int order;
        private List<Measure> measures;
        private final BandNamesManager bandNamesManager;
        private final RegionIDsManager regionIDsManager;
        private final TimeIntervalsManager timeIntervalsManager;

        Measures() {
            this(0);
        }

        Measures(int order) {
            this.order = order;
            measures = new ArrayList<>();
            bandNamesManager = new BandNamesManager();
            regionIDsManager = new RegionIDsManager();
            timeIntervalsManager = new TimeIntervalsManager();
        }

        boolean hasRegions() {
            return regionIDsManager.size() > 0;
        }

        boolean hasBands() {
            return bandNamesManager.size() > 0;
        }

        boolean hasTimeIntervals() {
            return timeIntervalsManager.size() > 0;
        }

        void addMeasure(Measure measure) {
            measures.add(measure);
            if (measure.bandName != null) {
                bandNamesManager.add(measure.bandName);
            }
            if (measure.interval != null) {
                timeIntervalsManager.add(measure.interval);
            }
            if (measure.regionId != null) {
                regionIDsManager.add(measure.regionId);
            }
        }

        Measure getMeasure(int index) {
            return measures.get(index);
        }

        void sort() {
            List<Measure> sortedMeasures = new ArrayList<>();
            Manager[] managers = getManagers();
            sort(measures, sortedMeasures, 0, managers);
            measures = sortedMeasures;
        }

        private void sort(List<Measure> measureList, List<Measure> sortedMeasures, int recursionDepth, Manager[] managers) {
            if (recursionDepth < managers.length) {
                if (managers[recursionDepth].size() == 0) {
                    sort(measureList, sortedMeasures, recursionDepth + 1, managers);
                } else {
                    for (int i = 0; i < managers[recursionDepth].size(); i++) {
                        List<Measure> subList = managers[recursionDepth].getSubListforIndex(measureList, i);
                        sort(subList, sortedMeasures, recursionDepth + 1, managers);
                    }
                }
            } else {
                sortedMeasures.addAll(measureList);
            }
        }

        void setOrder(int order) {
            this.order = order;
        }

        int getSize() {
            return measures.size();
        }

        private Manager[] getManagers() {
            switch (order) {
                case POLYGON_INTERVAL_BAND:
                    return new Manager[]{regionIDsManager, timeIntervalsManager, bandNamesManager};
                case POLYGON_BAND_INTERVAL:
                    return new Manager[]{regionIDsManager, bandNamesManager, timeIntervalsManager};
                case INTERVAL_POLYGON_BAND:
                    return new Manager[]{timeIntervalsManager, regionIDsManager, bandNamesManager};
                case INTERVAL_BAND_POLYGON:
                    return new Manager[]{timeIntervalsManager, bandNamesManager, regionIDsManager};
                case BAND_POLYGON_INTERVAL:
                    return new Manager[]{bandNamesManager, regionIDsManager, timeIntervalsManager};
                default:
                    return new Manager[]{bandNamesManager, timeIntervalsManager, regionIDsManager};
            }
        }

    }

    private interface Manager {

        List<Measure> getSubListforIndex(List<Measure> measures, int index);

        void add(Object o);

        int size();

    }

    private class BandNamesManager implements Manager {

        private final TreeSet<String> bandNames;

        BandNamesManager() {
            bandNames = new TreeSet<>();
        }

        @Override
        public void add(Object o) {
            if (!(o instanceof String)) {
                throw new IllegalArgumentException("String expected");
            }
            bandNames.add((String) o);
        }

        @Override
        public List<Measure> getSubListforIndex(List<Measure> measures, int index) {
            String bandName = bandNames.toArray(new String[0])[index];
            ArrayList<Measure> subMeasures = new ArrayList<>();
            for (Measure measure : measures) {
                if (measure.bandName.equals(bandName)) {
                    subMeasures.add(measure);
                }
            }
            return subMeasures;
        }

        @Override
        public int size() {
            return bandNames.size();
        }
    }

    private class RegionIDsManager implements Manager {

        private final TreeSet<String> regionIDs;

        RegionIDsManager() {
            regionIDs = new TreeSet<>();
        }

        @Override
        public void add(Object o) {
            if (!(o instanceof String)) {
                throw new IllegalArgumentException("String expected");
            }
            regionIDs.add((String) o);
        }

        @Override
        public List<Measure> getSubListforIndex(List<Measure> measures, int index) {
            String regionId = regionIDs.toArray(new String[0])[index];
            ArrayList<Measure> subMeasures = new ArrayList<>();
            for (Measure measure : measures) {
                if (measure.regionId.equals(regionId)) {
                    subMeasures.add(measure);
                }
            }
            return subMeasures;
        }

        @Override
        public int size() {
            return regionIDs.size();
        }
    }

    private class TimeIntervalsManager implements Manager {

        private final TreeSet<TimeInterval> timeIntervals;

        TimeIntervalsManager() {
            timeIntervals = new TreeSet<>();
        }

        @Override
        public void add(Object o) {
            if (!(o instanceof TimeInterval)) {
                throw new IllegalArgumentException("TimeInterval expected");
            }
            timeIntervals.add((TimeInterval) o);
        }

        @Override
        public List<Measure> getSubListforIndex(List<Measure> measures, int index) {
            TimeInterval timeInterval = timeIntervals.toArray(new TimeInterval[0])[index];
            ArrayList<Measure> subMeasures = new ArrayList<>();
            for (Measure measure : measures) {
                if (measure.interval.equals(timeInterval)) {
                    subMeasures.add(measure);
                }
            }
            return subMeasures;
        }

        @Override
        public int size() {
            return timeIntervals.size();
        }
    }

}
