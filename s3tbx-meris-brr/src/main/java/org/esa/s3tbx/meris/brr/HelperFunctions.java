package org.esa.s3tbx.meris.brr;

import org.esa.snap.core.util.math.MathUtils;

public class HelperFunctions {

    public static double calculateAirMass(float viewZenith, float sunZenith) {
        final double muv = Math.cos(viewZenith * MathUtils.DTOR);
        final double mus = Math.cos(sunZenith * MathUtils.DTOR);

        return calculateAirMassMusMuv(muv, mus);
    }
    
    public static double calculateAirMassMusMuv(double muv, double mus) {
        // DPM #2.1.12-1, Air Mass Computation
        return 1.0 / mus + 1.0 / muv;
    }

    public static float correctEcmwfPressure(float ecmwfPressure, float altitude, double pressScaleHeight) {
        // ECMWF pressure is only corrected for positive altitudes and only for land pixels */
        double factor = Math.exp(-Math.max(0.0, altitude) / pressScaleHeight);
        return (float) (ecmwfPressure * factor);
    }

    /**
     * Computes the azimuth difference from the given
     *
     * @param vaa viewing azimuth angle [degree]
     * @param saa sun azimuth angle [degree]
     * @return the azimuth difference [degree]
     */
    public static double computeAzimuthDifference(final double vaa, final double saa) {
        return MathUtils.RTOD * Math.acos(Math.cos(MathUtils.DTOR * (vaa - saa)));
    }
}
