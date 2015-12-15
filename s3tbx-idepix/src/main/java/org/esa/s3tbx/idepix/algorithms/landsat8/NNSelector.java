package org.esa.s3tbx.idepix.algorithms.landsat8;

/**
 * Enumeration for selection of Landsat8 Schiller NNs
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public enum NNSelector {
    ALL("ALL", "20x4x2_1012.9.net", new double[]{1.95, 3.45, 4.3}),
    LAND("LAND", "16x6x2_735.5.net", new double[]{2.15, 3.55, 4.4}),
    LAND_USE_THERMAL("LAND_USE_THERMAL", "18x6_509.5.net", new double[]{2.0, 3.65, 4.25}),
    WATER("WATER", "12x4_444.6.net", new double[]{2.0, 3.55, 4.4}),
    WATER_USE_THERMAL("WATER_USE_THERMAL", "12x4x2_305.0.net", new double[]{1.95, 3.55, 4.3}),
    WATER_NOTIDAL("WATER_NOTIDAL", "11x4x2_434.7.net", new double[]{1.95, 3.7, 4.2}),
    WATER_NOTIDAL_USE_THERMAL("WATER_NOTIDAL_USE_THERMAL", "12x4x2_307.5.net", new double[]{2.1, 3.55, 4.35});

    private final String label;
    private final String nnFileName;
    private final double[] separationValues;

    NNSelector(String label, String nnFileName, double[] separationValues) {
        this.label = label;
        this.nnFileName = nnFileName;
        this.separationValues = separationValues;
    }

    public String getLabel() {
        return label;
    }

    public String getNnFileName() {
        return nnFileName;
    }

    public double[] getSeparationValues() {
        return separationValues;
    }
}
