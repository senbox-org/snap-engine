package eu.esa.snap.core.renderer.classfraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Diagnostic result for statistics calculated from original class-band samples.
 */
public final class ClassFractionStatisticsResult {


    private final String roiName;
    private final long selectedPixelCount;
    private final long validPixelCount;
    private final long invalidPixelCount;
    private final String validityRule;
    private final List<ClassFractionBandStatistics> bandStatistics;


    ClassFractionStatisticsResult(String roiName, long selectedPixelCount, long validPixelCount,
                                  long invalidPixelCount, String validityRule,
                                  List<ClassFractionBandStatistics> bandStatistics) {
        this.roiName = roiName;
        this.selectedPixelCount = selectedPixelCount;
        this.validPixelCount = validPixelCount;
        this.invalidPixelCount = invalidPixelCount;
        this.validityRule = validityRule;
        this.bandStatistics = Collections.unmodifiableList(new ArrayList<>(bandStatistics));
    }


    public String getRoiName() {
        return roiName;
    }

    public long getSelectedPixelCount() {
        return selectedPixelCount;
    }

    public long getValidPixelCount() {
        return validPixelCount;
    }

    public long getInvalidPixelCount() {
        return invalidPixelCount;
    }

    public String getValidityRule() {
        return validityRule;
    }

    public List<ClassFractionBandStatistics> getBandStatistics() {
        return bandStatistics;
    }
}
