package org.esa.snap.classification.gpf;
import org.esa.snap.core.datamodel.Band;

public class FeatureInfo implements Comparable<FeatureInfo> {
    public final static int INT_NO_DATA_VALUE = -1;
    public final static double DOUBLE_NO_DATA_VALUE = Double.NaN;
    private final Band featureBand;
    double featureNoDataValue;
    final double featureOffsetValue;
    final double featureScaleValue;
    private final int id;

    public FeatureInfo(Band featureBand, int id) {
        this.featureBand = featureBand;
        this.id = id;

        featureNoDataValue = DOUBLE_NO_DATA_VALUE;
        if (featureBand.isNoDataValueSet()) {
            featureNoDataValue = featureBand.getNoDataValue();
        }
        featureOffsetValue = featureBand.getStx().getMinimum();
        featureScaleValue = 1.0 / (featureBand.getStx().getMaximum() - featureOffsetValue);
    }

    public FeatureInfo(Band featureBand, int id, double featureNoDataValue,
                double featureOffsetValue, double featureScaleValue) {
        this.featureBand = featureBand;
        this.id = id;
        this.featureNoDataValue = featureNoDataValue;
        this.featureOffsetValue = featureOffsetValue;
        this.featureScaleValue = featureScaleValue;
    }

    public int compareTo(FeatureInfo o) {
        return Integer.compare(id, o.id);
    }

    public Band getFeatureBand(){
        return featureBand;
    }
}

