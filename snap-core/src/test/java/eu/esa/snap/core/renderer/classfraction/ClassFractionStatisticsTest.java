package eu.esa.snap.core.renderer.classfraction;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.ProgressMonitor;
import eu.esa.snap.core.renderer.classfraction.ClassBandGroupConfig.ValidityPolicy;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.Color;
import java.util.Arrays;

import static org.junit.Assert.*;


public class ClassFractionStatisticsTest {


    @Test
    @STTM("SNAP-4339")
    public void computesCommonValidityStatisticsAndDistributionsFromOriginalBands() {
        Product product = product();
        Band first = band(product, "first", new float[]{1, 2, 99, Float.NaN, 5});
        first.setNoDataValueUsed(true);
        first.setNoDataValue(99);
        Band second = band(product, "second", new float[]{2, 4, 6, 8, Float.POSITIVE_INFINITY});

        ClassFractionStatisticsResult result = ClassFractionStatistics.compute(config(product, ValidityPolicy.STRICT_SOURCE_VALIDITY,
                true, true), Arrays.asList(first, second), null, 4, ProgressMonitor.NULL);

        assertEquals(5, result.getSelectedPixelCount());
        assertEquals(2, result.getValidPixelCount());
        assertEquals(3, result.getInvalidPixelCount());
        assertEquals(2, result.getBandStatistics().size());
        assertBand(result.getBandStatistics().get(0), 1, 2, 1.5, Math.sqrt(0.5), 2);
        assertBand(result.getBandStatistics().get(1), 2, 4, 3, Math.sqrt(2), 2);
        assertEquals(2, sum(result.getBandStatistics().get(0).getDistribution().getBinCounts()));
        assertEquals("second", result.getBandStatistics().get(1).getClassBand().getDisplayName());
    }

    @Test
    @STTM("SNAP-4339")
    public void appliesCompatibleRoiAndReportsItsRule() {
        Product product = product();
        Band first = band(product, "first", new float[]{1, 2, 3, 4, 5});
        Band second = band(product, "second", new float[]{2, 4, 6, 8, 10});
        Mask roi = Mask.BandMathsType.create("selected", "selected", 5, 1, "first >= 3", Color.RED, 0.5);
        product.getMaskGroup().add(roi);

        ClassFractionStatisticsResult result = ClassFractionStatistics.compute(config(product, ValidityPolicy.STRICT_SOURCE_VALIDITY,
                true, true), Arrays.asList(first, second), roi, 3, ProgressMonitor.NULL);

        assertEquals("selected", result.getRoiName());
        assertEquals(3, result.getSelectedPixelCount());
        assertEquals(3, result.getValidPixelCount());
        assertBand(result.getBandStatistics().get(0), 3, 5, 4, 1, 3);
        assertTrue(result.getValidityRule().contains("ROI 'selected'"));
    }

    @Test
    @STTM("SNAP-4339")
    public void finiteAndDeclaredNoDataAcceptsOwtStyleZeroDespiteValidMaskAndVisibility() {
        Product product = new Product("p", "t", 2, 1);
        Band first = band(product, "first", new float[]{0, 0.5f});
        first.setValidPixelExpression("first > 0");
        Band second = band(product, "second", new float[]{1, 0.5f});

        ClassFractionStatisticsResult finiteResult = ClassFractionStatistics.compute(config(product,
                ValidityPolicy.FINITE_AND_DECLARED_NODATA, false, true), Arrays.asList(first, second), null, 2, ProgressMonitor.NULL);
        ClassFractionStatisticsResult strictResult = ClassFractionStatistics.compute(config(product,
                ValidityPolicy.STRICT_SOURCE_VALIDITY, false, true), Arrays.asList(first, second), null, 2, ProgressMonitor.NULL);

        assertEquals(2, finiteResult.getValidPixelCount());
        assertEquals(0, finiteResult.getBandStatistics().get(0).getMinimum(), 0.0);
        assertEquals(1, strictResult.getValidPixelCount());
        assertEquals(0.5, strictResult.getBandStatistics().get(0).getMinimum(), 0.0);
        assertEquals("first", finiteResult.getBandStatistics().get(0).getClassBand().getDisplayName());
    }

    @Test
    @STTM("SNAP-4339")
    public void estimatesRobustPercentilesFromTheCommonValidSamples() {
        Product product = new Product("p", "t", 21, 1);
        float[] values = new float[21];

        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }
        Band first = band(product, "first", values);
        Band second = band(product, "second", values);

        ClassFractionStatisticsResult.BandStatistics statistics = ClassFractionStatistics.compute(
                config(product, ValidityPolicy.STRICT_SOURCE_VALIDITY, true, true), Arrays.asList(first, second),
                null, 4, ProgressMonitor.NULL).getBandStatistics().get(0);

        assertEquals(1.0, statistics.getPercentile05(), 0.02);
        assertEquals(5.0, statistics.getPercentile25(), 0.02);
        assertEquals(10.0, statistics.getMedian(), 0.02);
        assertEquals(15.0, statistics.getPercentile75(), 0.02);
        assertEquals(19.0, statistics.getPercentile95(), 0.02);
    }

    @Test
    @STTM("SNAP-4339")
    public void rejectsIncompatibleRoi() {
        Product product = product();
        Band first = band(product, "first", new float[]{1, 2, 3, 4, 5});
        Band second = band(product, "second", new float[]{2, 4, 6, 8, 10});
        Product other = new Product("other", "t", 4, 1);
        Mask roi = Mask.BandMathsType.create("wrong", "wrong", 4, 1, "true", Color.RED, 0.5);
        other.getMaskGroup().add(roi);

        try {
            ClassFractionStatistics.compute(config(product, ValidityPolicy.STRICT_SOURCE_VALIDITY, true, true),
                    Arrays.asList(first, second), roi, 4, ProgressMonitor.NULL);
            fail("Expected incompatible ROI rejection");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("ROI mask"));
        }
    }


    private static void assertBand(ClassFractionStatisticsResult.BandStatistics statistics, double minimum, double maximum, double mean, double standardDeviation, long count) {
        assertEquals(count, statistics.getValidPixelCount());
        assertEquals(minimum, statistics.getMinimum(), 1.0e-7);
        assertEquals(maximum, statistics.getMaximum(), 1.0e-7);
        assertEquals(mean, statistics.getMean(), 1.0e-7);
        assertEquals(standardDeviation, statistics.getStandardDeviation(), 1.0e-7);
    }

    private static long sum(long[] values) {
        long sum = 0;
        for (long value : values) {
            sum += value;
        }
        return sum;
    }

    private static Product product() {
        return new Product("p", "t", 5, 1);
    }

    private static Band band(Product product, String name, float[] values) {
        Band band = product.addBand(name, ProductData.TYPE_FLOAT32);
        band.setRasterData(ProductData.createInstance(values));
        return band;
    }

    private static ClassFractionRendererConfig config(Product product, ValidityPolicy validity, boolean firstVisible, boolean secondVisible) {
        return new ClassFractionRendererConfig(
                "statistics",
                new ClassBandGroupConfig(
                        Arrays.asList(
                                new ClassBandConfig(product.getName(), "first", "first", 0xFFFF0000, firstVisible),
                                new ClassBandConfig(product.getName(), "second", "second", 0xFF00FF00, secondVisible)
                        ),
                        1.0,
                        validity
                )
        );
    }
}
