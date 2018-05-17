package org.esa.snap.statistics.output;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.util.ArrayList;
import org.esa.snap.statistics.StatisticsOp;
import org.esa.snap.statistics.tools.TimeInterval;

/**
 * The context used to initialise instances of {@link StatisticsOutputter}.
 */
public class StatisticsOutputContext {

    /**
     * The statistics' source products.
     */
    public final String[] sourceProductNames;

    /**
     * The names of the bands considered in the statistics.
     */
    public final String[] bandNames;

    /**
     * The names of the algorithms considered in the statistics.
     */
    public final String[] algorithmNames;

    /**
     * The start date of the statistics.
     */
    public final ProductData.UTC startDate;

    /**
     * The end data of the statistics.
     */
    public final ProductData.UTC endDate;

    /**
     * The ids of the regions where statistics are computed.
     */
    public final String[] regionIds;

    /**
     * The time intervals for which statistics are computed.
     */
    public final TimeInterval[] timeIntervals;


    public final boolean isNotNumber(String algorithm) {
        return algorithm.equals(StatisticsOp.MAJORITY_CLASS) || algorithm.equals(StatisticsOp.SECOND_MAJORITY_CLASS);
    }

    // todo doku ... add context object to factory ... initialize Context object

    // todo refactor this mess! maybe use builder pattern (as in StxFactory)?

    /**
     * Factory method for creating a fully specified instance.
     *
     * @param sourceProductNames The statistics' source product names.
     * @param bandNames          The names of the bands considered in the statistics.
     * @param algorithmNames     The names of the algorithms considered in the statistics.
     * @param timeIntervals      The time intervals of the statistics.
     * @param regionIds          The ids of the regions where statistics are computed.
     * @return An instance of {@link StatisticsOutputContext}.
     */
    public static StatisticsOutputContext create(String[] sourceProductNames, String[] bandNames, String[] algorithmNames, TimeInterval[] timeIntervals, String[] regionIds) {
        return new StatisticsOutputContext(sourceProductNames, bandNames, algorithmNames, timeIntervals, regionIds);
    }

    /**
     * Factory method for creating a fully specified instance.
     *
     * @param sourceProducts The statistics' source products.
     * @param bandNames      The names of the bands considered in the statistics.
     * @param algorithmNames The names of the algorithms considered in the statistics.
     * @param startDate      The start date of the statistics.
     * @param endDate        The end data of the statistics.
     * @param regionIds      The ids of the regions where statistics are computed.
     * @return An instance of {@link StatisticsOutputContext}.
     */
    public static StatisticsOutputContext create(Product[] sourceProducts, String[] bandNames, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate, String[] regionIds) {
        final String[] sourceProductNames = extractSourceProductNames(sourceProducts);
        TimeInterval[] timeIntervals = {new TimeInterval(0, startDate, endDate)};
        return new StatisticsOutputContext(sourceProductNames, bandNames, algorithmNames, timeIntervals, regionIds);
    }

    // todo doku ... add context object to factory ... initialize Context object

    /**
     * Factory method for creating a fully specified instance.
     *
     * @param sourceProductNames The statistics' source product names.
     * @param bandNames          The names of the bands considered in the statistics.
     * @param algorithmNames     The names of the algorithms considered in the statistics.
     * @param startDate          The start date of the statistics.
     * @param endDate            The end data of the statistics.
     * @param regionIds          The ids of the regions where statistics are computed.
     * @return An instance of {@link StatisticsOutputContext}.
     */
    public static StatisticsOutputContext create(String[] sourceProductNames, String[] bandNames, String[] algorithmNames, ProductData.UTC startDate, ProductData.UTC endDate, String[] regionIds) {
        TimeInterval[] timeIntervals = {new TimeInterval(0, startDate, endDate)};
        return new StatisticsOutputContext(sourceProductNames, bandNames, algorithmNames, timeIntervals, regionIds);
    }

    /**
     * Convenience factory method for creating an instance which does not use all possible fields.
     *
     * @param sourceProducts The statistics' source products.
     * @param algorithmNames The names of the algorithms considered in the statistics.
     * @param regionIds      The ids of the regions where statistics are computed.
     * @return An instance of {@link StatisticsOutputContext}.
     */
    public static StatisticsOutputContext create(Product[] sourceProducts, String[] algorithmNames, String[] regionIds) {
        final String[] sourceProductNames = extractSourceProductNames(sourceProducts);
        return new StatisticsOutputContext(sourceProductNames, null, algorithmNames, null, regionIds);
    }


    /**
     * Convenience factory method for creating an instance which does not use all possible fields.
     *
     * @param bandNames      The names of the bands considered in the statistics.
     * @param algorithmNames The names of the algorithms considered in the statistics.
     * @return An instance of {@link StatisticsOutputContext}.
     */
    public static StatisticsOutputContext create(String[] bandNames, String[] algorithmNames) {
        return new StatisticsOutputContext(null, bandNames, algorithmNames, null, null);
    }

    private StatisticsOutputContext(String[] sourceProductNames, String[] bandNames, String[] algorithmNames, TimeInterval[] timeIntervals, String[] regionIds) {
        this.sourceProductNames = sourceProductNames;
        this.bandNames = bandNames;
        this.algorithmNames = algorithmNames;
        this.timeIntervals = timeIntervals;
        if (timeIntervals != null && timeIntervals.length > 0) {
            this.startDate = timeIntervals[0].getIntervalStart();
            this.endDate = timeIntervals[timeIntervals.length - 1].getIntervalEnd();
        } else {
            this.startDate = null;
            this.endDate = null;
        }
        this.regionIds = regionIds;
    }

    private static String[] extractSourceProductNames(Product[] sourceProducts) {
        final ArrayList<String> productNames = new ArrayList<>();
        for (Product sourceProduct : sourceProducts) {
            productNames.add(sourceProduct.getName());
        }
        return productNames.toArray(new String[productNames.size()]);
    }
}
