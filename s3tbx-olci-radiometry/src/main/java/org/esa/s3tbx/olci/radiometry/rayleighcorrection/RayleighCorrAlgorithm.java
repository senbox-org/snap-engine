package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.esa.s3tbx.olci.radiometry.gaseousabsorption.GaseousAbsorptionAlgo;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileUtils;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrAlgorithm {

    public static final double STD_SEA_LEVEL_PRESSURE = 1013.0;

    //Copied from org.esa.beam.meris.case2.MerisCase2BasisWaterOp
    protected double getAzimuthDifference(double viewAzimuthAngle, double sunAzimuthAngle) {
        double azi_diff_deg = Math.abs(viewAzimuthAngle - sunAzimuthAngle); /* azimuth difference */
              /* reverse azi difference */
        if (azi_diff_deg > 180.0) {
            azi_diff_deg = 360.0 - azi_diff_deg;
        }
//        azi_diff_deg = 180.0 - azi_diff_deg; /* different definitions in MERIS data and MC /HL simulation */
        return azi_diff_deg;
    }

    public double[] getTaurStd(double[] lam) {
        final double[] taurs = new double[lam.length];
        for (int i = 0; i < lam.length; i++) {
            taurs[i] = Math.exp(-4.637) * Math.pow((lam[i] / 1000), -4.0679);
        }
        return taurs;
    }

    public double[] getPressureAtSurface(double[] seaLevelPressure, double[] height) {
        double pressureAtms[] = new double[seaLevelPressure.length];
        for (int i = 0; i < seaLevelPressure.length; i++) {
            pressureAtms[i] = seaLevelPressure[i] * Math.exp(-height[i] / 8000);
        }
        return pressureAtms;
    }

    public double getPressureAtSurface(double seaLevelPressure, double height) {
        return seaLevelPressure * Math.exp(-height / 8000);
    }

    public double[] getRayleighOpticalThickness(double[] pressureAtms, double taur_std) {
        final double thickness[] = new double[pressureAtms.length];
        double pressureFactor = taur_std / STD_SEA_LEVEL_PRESSURE;
        for (int i = 0; i < pressureAtms.length; i++) {
            thickness[i] = pressureAtms[i] * pressureFactor;
        }
        return thickness;
    }

    public double getRayleighOpticalThickness(double pressureAtms, double taur_std) {
        return pressureAtms * taur_std / STD_SEA_LEVEL_PRESSURE;
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
        return (-cos_view * cos_sun) - (sin_view * sin_sun * cos_azi_dif);
    }

    public double[] getRayleighReflectance(double[] taurPoZ, double[] sunZenithAngles, double[] sunAzimuthAngles, double[] viewZenithAngles, double[] viewAzimuthAngles) {

        final double reflRaly[] = new double[viewZenithAngles.length];

        double[] sunZenithAngleRad = SmileUtils.convertDegreesToRadians(sunZenithAngles);
        double[] viewZenithAngleRad = SmileUtils.convertDegreesToRadians(viewZenithAngles);

        for (int i = 0; i < viewZenithAngles.length; i++) {
            final double azimuthDifferenceRad = Math.toRadians(getAzimuthDifference(viewAzimuthAngles[i], sunAzimuthAngles[i]));
            final double sunZenithAngle = sunZenithAngleRad[i];
            final double viewZenithAngle = viewZenithAngleRad[i];

            final double phaseRaylMin = phaseRaylMin(sunZenithAngle, viewZenithAngle, azimuthDifferenceRad);
            final double cos_sunZenith = Math.cos(sunZenithAngle);
            final double cos_viewZenith = Math.cos(viewZenithAngle);
            reflRaly[i] = cos_sunZenith * taurPoZ[i] * phaseRaylMin / (4 * Math.PI) * (1 / cos_viewZenith) * Math.PI;

        }
        return reflRaly;
    }

    public double getRayleighReflectance(double taurPoZ, double sunZenithAngles, double sunAzimuthAngles, double viewZenithAngles, double viewAzimuthAngles) {
        final double sunZenithAngleRad = Math.toRadians(sunZenithAngles);
        final double viewZenithAngleRad = Math.toRadians(viewZenithAngles);
        final double azimuthDifferenceRad = Math.toRadians(getAzimuthDifference(viewAzimuthAngles, sunAzimuthAngles));

        final double phaseRaylMin = phaseRaylMin(sunZenithAngleRad, viewZenithAngleRad, azimuthDifferenceRad);
        final double cos_sunZenith = Math.cos(sunZenithAngleRad);
        final double cos_viewZenith = Math.cos(viewZenithAngleRad);
        return cos_sunZenith * taurPoZ * phaseRaylMin / (4 * Math.PI) * (1 / cos_viewZenith) * Math.PI;
    }


    /**
     * @param sourceWL
     * @return sigma
     */
    public float[] getCrossSectionSigma(float[] sourceWL) {
        double n_ratio = 1 + 0.54 * (RayleighConstants.CO2 - 0.0003);
        double molecularDen = Math.pow(RayleighConstants.Molecular_cm3, 2);
        float[] sigma = new float[sourceWL.length];

        for (int i = 0; i < sourceWL.length; i++) {

            double sourceWLmm = sourceWL[i] / 1000f;
            double sourceWLcm = sourceWL[i] / 10000f;

            double F_N2 = 1.034 + 0.000317 / Math.pow(sourceWLmm, 2);
            double F_O2 = 1.096 + 0.001385 / Math.pow(sourceWLmm, 2) + 0.0001448 / Math.pow(sourceWLmm, 4);
            double F_air = (78.084 * F_N2 + 20.946 * F_O2 + 0.934 * 1 + RayleighConstants.C_CO2 * 1.15) / (78.084 + 20.946 + 0.934 + RayleighConstants.C_CO2);
            double n_1_300 = (8060.51 + (2480990. / (132.274 - Math.pow(sourceWLmm, -2))) + (17455.7 / (39.32957 - Math.pow(sourceWLmm, -2)))) / 100000000.0;
            double nCO2 = n_ratio * (1 + n_1_300);


            sigma[i] = (float) (Math.pow((Math.pow(24 * Math.pow(Math.PI, 3) * Math.pow(nCO2, 2) - 1, 2) / Math.pow(sourceWLcm, 4) * molecularDen * Math.pow(nCO2, 2) + 2), 2) * F_air);
        }
        return sigma;
    }

    public float[] getRayleighOpticalThickness(float seaLevelPressure[], float altitude[], float latitude[], float sigma[]) {

        float rayeighOpticalThickness[] = new float[altitude.length];
        for (int i = 0; i < altitude.length; i++) {

            double airPressurePixelcm2 = seaLevelPressure[i] * Math.pow((1. - 0.0065 * altitude[i] / 288.15), 5.255) * 1000;
            double latRad = Math.toRadians(latitude[i]);
            double cos2LatRad = Math.cos(2 * latRad);
            double g0 = RayleighConstants.ACCELERATION_GRAVITY_SEA_LEVEL_458_LATITUDE * (1 - 0.0026373 * cos2LatRad + 0.0000059 * Math.pow(cos2LatRad, 2));
            double effectiveMassWeightAltitude = 0.73737 * altitude[i] + 5517.56;

            double g = g0 - (0.0003085462 + 0.000000227 * cos2LatRad) * effectiveMassWeightAltitude +
                    (0.00000000007254 + 0.0000000000001 * cos2LatRad) * Math.pow(effectiveMassWeightAltitude, 2) -
                    (1.517E-17 + 6E-20 * cos2LatRad) * Math.pow(effectiveMassWeightAltitude, 3);

            double rayleighOptical = (airPressurePixelcm2 * RayleighConstants.AVOGADRO_NUMBER) / (RayleighConstants.MEAN_MOLECULAR_WEIGHT_C02 * g);

            rayeighOpticalThickness[i] = (float) (rayleighOptical * sigma[i]);
        }
        return rayeighOpticalThickness;
    }

    public float[] getRayleighPhaseMin(float[] sza, float[] oza, float[] saa, float[] aoo, float[] taur) {
        GaseousAbsorptionAlgo gaseousAbsorptionAlgo = new GaseousAbsorptionAlgo();

        float[] ozaRads = SmileUtils.convertDegreesToRadians(oza);
        float[] szaRads = SmileUtils.convertDegreesToRadians(sza);
        float[] saaRads = SmileUtils.convertDegreesToRadians(saa);
        float[] aooRads = SmileUtils.convertDegreesToRadians(aoo);

        float[] fourierSeries = new float[3];
        float[] phase_rayl_min = new float[sza.length];

        double rho_Rm[] = new double[3];
        double tpoly[] = new double[4];
        double sARay = 1;
        double reflectance = 1;
        double rho_R = 1;

        LinearInterpolator innz = new LinearInterpolator();
//        innz.

        // Fourier components of multiple scattering
        float[] massAir = gaseousAbsorptionAlgo.getMassAir(sza, oza);
        float[] rayScattCoeffA = new float[4];
        float[] rayScattCoeffB = new float[4];
        float[] rayScattCoeffC = new float[4];
        float[] rayScattCoeffD = new float[4];
        // Rayleigh primary scattering
/*
        for (int i = 0; i < fourierSeries.length; i++) {
            rayPrimaryScatters[i] = fourierSeries[i] / (4.0 * (cosSZARad + cosOZARad)) * (1. - Math.exp(-massAir * taur));
            rho_Rm[i] = rayScattCoeffA[i] + rayScattCoeffB[i] * taur + rayScattCoeffC[i] * Math.pow(taur, 2) + rayScattCoeffD[i] * Math.pow(taur, 3);
        }
*/
        for (int x = 0; x < ozaRads.length; x++) {
            double sinSZARad = Math.sin(szaRads[x]);
            double cosSZARad = Math.cos(szaRads[x]);
            double cosOZARad = Math.cos(ozaRads[x]);
            double sinOZARad = Math.sin(ozaRads[x]);

            double sinSZA2 = Math.pow(sinSZARad, 2);
            double sinOZA2 = Math.pow(sinOZARad, 2);

            //Rayleigh Phase function, 3 Fourier terms
            fourierSeries[0] = (float) (3 * RayleighConstants.PA /
                    4 * (1 + Math.pow(cosSZARad, 2) * Math.pow(cosOZARad, 2) + (sinSZA2 * sinOZA2) / 2) + RayleighConstants.PB);
            fourierSeries[1] = (float) (-3 * RayleighConstants.PA / 4 * cosSZARad * cosOZARad * sinSZARad * sinOZARad);
            fourierSeries[2] = (float) (3 * RayleighConstants.PA / 16 * sinSZA2 * sinOZA2);

            double cosDelta = Math.cos(aooRads[x] - saaRads[x]);
            double aziDiff = Math.acos(cosDelta); // in radian
            //sum of the fourier
            double rayRef = rho_Rm[0] + 2.0 * rho_Rm[1] * Math.cos(aziDiff) + 2. * rho_Rm[2] * Math.cos(2. * aziDiff);
            // polynomial coefficients tpoly0, tpoly1 and tpoly2 from MERIS LUT
            double tRs = ((2. / 3. + cosSZARad) + (2. / 3. - cosSZARad) * Math.exp(-taur[x] / cosSZARad)) / (4. / 3. + taur[x]);
            double tR_thetaS = tpoly[0] + tpoly[1] * tRs + tpoly[2] * Math.pow(tRs, 2);
            //#Rayleigh Transmittance sun - surface
            double tRv = ((2. / 3. + cosOZARad) + (2. / 3. - cosOZARad) * Math.exp(-taur[x] / cosOZARad)) / (4. / 3. + taur[x]);
            //#Rayleigh Transmittance surface - sensor
            double tR_thetaV = tpoly[0] + tpoly[1] * tRv + tpoly[2] * Math.pow(tRv, 2);
            double rho_toaR = (reflectance - rho_R) / (tR_thetaS * tR_thetaV); //toa reflectance corrected for Rayleigh scattering

            double sphericalFactor = 1.0 / (1.0 + sARay * rho_toaR); //#factor used in the next equation to account for the spherical albedo
            //#top of aerosol reflectance, which is equal to bottom of Rayleigh reflectance
            double rho_BRR = rho_toaR * sphericalFactor;
            double aziDiffDeg = Math.abs(aoo[x] - saa[x]);

            if (aziDiffDeg > 180.0) {
                aziDiffDeg = 360.0 - aziDiffDeg;
            }
            double aziDiffRad = Math.toRadians(aziDiffDeg);
            double cos_scat_ang = (-cosOZARad * cosSZARad) - (sinOZARad * sinSZARad * Math.cos(aziDiffRad));
            phase_rayl_min[x] = (float) (0.75 * (1.0 + cos_scat_ang * cos_scat_ang));
        }
        return phase_rayl_min;
    }

    public float[] correct(float[] waveLenght, float[] seaAirPressure, float[] altitude, float[] sza, float[] oza, float[] ray_phase_min) {

        float[] szaRads = SmileUtils.convertDegreesToRadians(sza);
        float[] ozaRads = SmileUtils.convertDegreesToRadians(oza);
        float[] rRaySimple = new float[oza.length];

        for (int i = 0; i < ray_phase_min.length; i++) {
            double taurS = Math.exp(-4.637) * Math.pow((waveLenght[i] / 1000.0), -4.0679);
            double pressureAtms = seaAirPressure[i] * Math.exp(-altitude[i] / 8000.0);
            double pressureFactor = taurS / 1013.0;
            taurS = pressureAtms * pressureFactor;
            rRaySimple[i] = (float) (Math.cos(szaRads[i]) * taurS * ray_phase_min[i] / (4 * 3.1415926) * (1 / Math.cos(ozaRads[i]) * 3.1415926));
        }
        return rRaySimple;
    }

    public float[] getCorrOzone(float[] totalOzones) {
        return new float[0];
    }
}
