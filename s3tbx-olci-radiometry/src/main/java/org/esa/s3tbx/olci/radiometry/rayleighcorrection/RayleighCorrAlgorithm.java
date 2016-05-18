package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import org.apache.commons.math3.util.Precision;
import org.esa.snap.core.gpf.OperatorException;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrAlgorithm {

    protected double getTaur(double lam) {
        return Precision.round(Math.exp(-4.637) * Math.pow((lam / 1000), -4.0679), 10);
    }

    protected double[] getTaur(double[] lam) {
        final double[] taurs = new double[lam.length];
        for (int i = 0; i < lam.length; i++) {
            taurs[i] = getTaur(lam[i]);
        }
        return taurs;
    }

    protected double[] pressureAtSurface(double[] seaLevelPressure, double[] height) {
        if (seaLevelPressure.length != height.length) {
            throw new OperatorException("");
        }
        double pressureAtms[] = new double[seaLevelPressure.length];
        for (int i = 0; i < seaLevelPressure.length; i++) {
            pressureAtms[i] = seaLevelPressure[i] * Math.exp(-height[i] / 8000);
        }
        return pressureAtms;
    }

    protected double[] getTaurPoZ(double[] pressureAtms, double taur_std) {
        final double thickness[] = new double[pressureAtms.length];
        for (int i = 0; i < pressureAtms.length; i++) {
            thickness[i] = pressureAtms[i] * taur_std / 1300;
        }
        return thickness;
    }

    protected double phaseRaylMin(double sunAngle, double viewAngle, double aziDif) {
        double cosScatterAngle = cosScatterAngle(sunAngle, viewAngle, aziDif);
        return 0.75 * (1.0 + cosScatterAngle * cosScatterAngle);
    }

    protected double cosScatterAngle(double sunAngle, double viewAngle, double aziDif) {
        final double cos_view = Math.cos(viewAngle);
        final double cos_sun = Math.cos(sunAngle);

        final double sin_view = Math.sin(viewAngle);
        final double sin_sun = Math.sin(sunAngle);
        final double cos_azi_dif = Math.cos(aziDif);

        return -cos_view * cos_sun - sin_view * sin_sun * cos_azi_dif;
    }

    public double[] getReflRaly(double[] taursPoZ, double sza, double oza, double phaseRayMin) {
        double cos_sun = Math.cos(sza);
        double cos_view = Math.cos(oza);
        double reflRaly[] = new double[taursPoZ.length];
        for (int i = 0; i < taursPoZ.length; i++) {
            reflRaly[i] = cos_sun * taursPoZ[i] * phaseRayMin / ((4 * Math.PI) * (1 / cos_view) * Math.PI);
        }
        return reflRaly;
    }
}
