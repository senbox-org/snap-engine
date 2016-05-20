package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrAlgorithm {

    public static final int STD_SEA_LEVEL_PRESSURE = 1013;

    //Copied from org.esa.beam.meris.case2.MerisCase2BasisWaterOp
    protected double getAzimuthDifference(double viewAzimuthAngle, double sunAzimuthAngle) {
        double azi_diff_deg = Math.abs(viewAzimuthAngle - sunAzimuthAngle); /* azimuth difference */
              /* reverse azi difference */
        if (azi_diff_deg > 180.0) {
            azi_diff_deg = 360.0 - azi_diff_deg;
        }
        azi_diff_deg = 180.0 - azi_diff_deg; /* different definitions in MERIS data and MC /HL simulation */
        return azi_diff_deg;
    }

    protected double[] getTaurStd(double[] lam) {
        final double[] taurs = new double[lam.length];
        for (int i = 0; i < lam.length; i++) {
            taurs[i] = Math.exp(-4.637) * Math.pow((lam[i] / 1000), -4.0679);
        }
        return taurs;
    }

    protected double[] getPressureAtSurface(double[] seaLevelPressure, double[] height) {
        double pressureAtms[] = new double[seaLevelPressure.length];
        for (int i = 0; i < seaLevelPressure.length; i++) {
            pressureAtms[i] = seaLevelPressure[i] * Math.exp(-height[i] / 8000);
        }
        return pressureAtms;
    }

    public double[] getRayleighOpticalThickness(double[] pressureAtms, double taur_std) {
        final double thickness[] = new double[pressureAtms.length];
        double pressureFactor = taur_std / STD_SEA_LEVEL_PRESSURE;
        for (int i = 0; i < pressureAtms.length; i++) {
            thickness[i] = pressureAtms[i] * pressureFactor;
        }
        return thickness;
    }

    protected double phaseRaylMin(double sunZenithAngle, double viewZenithAngle, double azimuthDifference) {
        double cosScatterAngle = cosScatterAngle(sunZenithAngle, viewZenithAngle, azimuthDifference);
        return 0.75 * (1.0 + cosScatterAngle * cosScatterAngle);
    }

    protected double cosScatterAngle(double sunZenithAngle, double viewZenithAngle, double azimuthDifferent) {
        final double cos_view = Math.cos(viewZenithAngle);
        final double cos_sun = Math.cos(sunZenithAngle);

        final double sin_view = Math.sin(viewZenithAngle);
        final double sin_sun = Math.sin(sunZenithAngle);
        final double cos_azi_dif = Math.cos(azimuthDifferent);

//        return Math.acos((-cos_view * cos_sun) + (sin_view * sin_sun * cos_azi_dif));
        return (-cos_view * cos_sun) - (sin_view * sin_sun * cos_azi_dif);
    }

    public double[] getRayleighReflectance(double[] taurPoZ, double[] sunZenithAngles, double[] sunAzimuthAngles, double[] viewZenithAngles, double[] viewAzimuthAngles) {

        final double reflRaly[] = new double[viewZenithAngles.length];

        double[] sunZenithAngleRad = convertDegreesToRadians(sunZenithAngles);
        double[] viewZenithAngleRad = convertDegreesToRadians(viewZenithAngles);

        for (int i = 0; i < viewZenithAngles.length; i++) {
            final double azimuthDifferenceRad = Math.toRadians(getAzimuthDifference(viewAzimuthAngles[i], sunAzimuthAngles[i]));
            final double sunZenithAngle = sunZenithAngleRad[i];
            final double viewZenithAngle = viewZenithAngleRad[i];

            final double phaseRaylMin = phaseRaylMin(sunZenithAngle, viewZenithAngle, azimuthDifferenceRad);
            final double cos_sun = Math.cos(sunZenithAngle);
            final double cos_view = Math.cos(viewZenithAngle);
            reflRaly[i] = cos_sun * taurPoZ[i] * phaseRaylMin / ((4 * Math.PI) * (1 / cos_view) * Math.PI);
        }
        return reflRaly;
    }

    public double[] convertDegreesToRadians(double[] angle) {
        final double[] rads = new double[angle.length];
        for (int i = 0; i < rads.length; i++) {
            rads[i] = Math.toRadians(angle[i]);
        }
        return rads;
    }
}
