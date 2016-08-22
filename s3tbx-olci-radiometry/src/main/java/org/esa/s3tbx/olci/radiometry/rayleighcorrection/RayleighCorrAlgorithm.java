package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.esa.s3tbx.olci.radiometry.smilecorr.SmileUtils;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrAlgorithm {

    public static final double STD_SEA_LEVEL_PRESSURE = 1013.0;

    //Copied from org.esa.beam.meris.case2.MerisCase2BasisWaterOp
    public double getAzimuthDifference(double viewAzimuthAngle, double sunAzimuthAngle) {
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

    public double[] getPhaseRaylMin(AuxiliaryValues auxiliaryValues) {

        double[] sunZenithAngleRads = auxiliaryValues.getSunZenithAnglesRad();
        double[] sunAzimuthAngles = auxiliaryValues.getSunAzimuthAngles();

        double[] viewZenithAngleRads = auxiliaryValues.getViewZenithAnglesRad();
        double[] viewAzimuthAngles = auxiliaryValues.getViewAzimuthAngles();

        double phaseRaylMin[] = new double[sunAzimuthAngles.length];
        for (int i = 0; i < sunAzimuthAngles.length; i++) {
            double azi_diff_deg = Math.abs(viewAzimuthAngles[i] - sunAzimuthAngles[i]); /* azimuth difference */
              /* reverse azi difference */
            if (azi_diff_deg > 180.0) {
                azi_diff_deg = 360.0 - azi_diff_deg;
            }
            double cosScatterAngle = cosScatterAngle(sunZenithAngleRads[i], viewZenithAngleRads[i], Math.toRadians(azi_diff_deg));
            phaseRaylMin[i] = 0.75 * (1.0 + cosScatterAngle * cosScatterAngle);
        }
        return phaseRaylMin;
    }


    protected double cosScatterAngle(double sunZenithAngle, double viewZenithAngle, double azimuthDifferent) {
        final double cos_view = Math.cos(viewZenithAngle);
        final double cos_sun = Math.cos(sunZenithAngle);

        final double sin_view = Math.sin(viewZenithAngle);
        final double sin_sun = Math.sin(sunZenithAngle);
        final double cos_azi_dif = Math.cos(azimuthDifferent);
        return (-cos_view * cos_sun) - (sin_view * sin_sun * cos_azi_dif);
    }

    public double[] getCrossSectionSigma(double[] lambdas) {
        double n_ratio = 1 + 0.54 * (RayleighConstants.CO2 - 0.0003);
        double molecularDen = Math.pow(RayleighConstants.Molecular_cm3, 2);
        double[] sigma = new double[lambdas.length];

        for (int i = 0; i < lambdas.length; i++) {

            double lambdamm = lambdas[i] / 1000.0;
            double lambdaWLcm = lambdamm / 10000.0;

            double F_N2 = 1.034 + 0.000317 / Math.pow(lambdamm, 2);
            double F_O2 = 1.096 + 0.001385 / Math.pow(lambdamm, 2) + 0.0001448 / Math.pow(lambdamm, 4);

//            (78.084 * F_N2 + 20.946 * F_O2 + 0.934 * 1 + C_CO2 * 1.15) / (78.084 + 20.946 + 0.934 + C_CO2)
            double F_air = (78.084 * F_N2 + 20.946 * F_O2 + 0.934 * 1 + RayleighConstants.C_CO2 * 1.15) / (78.084 + 20.946 + 0.934 + RayleighConstants.C_CO2);
//            (8060.51 + (2480990. / (132.274 - lam ** (-2))) + (17455.7 / (39.32957 - lam ** (-2)))) / 100000000.0
            double n_1_300 = (8060.51 + (2480990.0 / (132.274 - Math.pow(lambdamm, (-2)))) + (17455.7 / (39.32957 - Math.pow(lambdamm, (-2))))) / 100000000;
            double nCO2 = n_ratio * (1 + n_1_300);
            sigma[i] = 24 * Math.pow(Math.PI, 3) * Math.pow((Math.pow(nCO2, 2) - 1), 2) / (Math.pow(lambdaWLcm, 4) * molecularDen * Math.pow((Math.pow(nCO2, 2) + 2), 2)) * F_air;
        }
        return sigma;
    }

    public double[] getRayleighOpticalThicknessII(AuxiliaryValues auxiliaryValues, double sigma) {
        double seaLevelPressure[] = auxiliaryValues.getSeaLevels();
        double altitude[] = auxiliaryValues.getAltitudes();
        double latitude[] = auxiliaryValues.getLatitudes();

        double rayleighOpticalThickness[] = new double[altitude.length];
        for (int i = 0; i < altitude.length; i++) {

            double P = seaLevelPressure[i] * Math.pow((1.0 - 0.0065 * altitude[i] / 288.15), 5.255) * 1000;
            double latRad = Math.toRadians(latitude[i]);
            double cos2LatRad = Math.cos(2 * latRad);
            double g0 = 980.616 * (1 - 0.0026373 * cos2LatRad + 0.0000059 * Math.pow(cos2LatRad, 2));
            double effectiveMassWeightAltitude = 0.73737 * altitude[i] + 5517.56;

            double g = g0 - (0.0003085462 + 0.000000227 * cos2LatRad) * effectiveMassWeightAltitude +
                    (0.00000000007254 + 0.0000000000001 * cos2LatRad) * Math.pow(effectiveMassWeightAltitude, 2) -
                    (1.517E-17 + 6E-20 * cos2LatRad) * Math.pow(effectiveMassWeightAltitude, 3);

            double factor = (P * RayleighConstants.AVOGADRO_NUMBER) / (RayleighConstants.MEAN_MOLECULAR_WEIGHT_C02 * g);
            rayleighOpticalThickness[i] = factor * sigma;
        }
        return rayleighOpticalThickness;
    }

    public double[] getCorrOzone(AuxiliaryValues auxiliaryValues, double[] rho_ng_ref, double absorpO) {
        double[] ozone = auxiliaryValues.getTotalOzones();
        double[] cosOZARads = auxiliaryValues.getCosOZARads();
        double[] cosSZARads = auxiliaryValues.getCosSZARads();

        for (int i = 0; i < rho_ng_ref.length; i++) {
            double model_ozone = 0;
            double cts = cosSZARads[i]; //#cosine of sun zenith angle
            double ctv = cosOZARads[i];//#cosine of view zenith angle
            double trans_ozoned12 = Math.exp(-(absorpO * ozone[i] / 1000.0 - model_ozone) / cts);
            double trans_ozoneu12 = Math.exp(-(absorpO * ozone[i] / 1000.0 - model_ozone) / ctv);
            double trans_ozone12 = trans_ozoned12 * trans_ozoneu12;
            rho_ng_ref[i] = rho_ng_ref[i] / trans_ozone12;
        }
        return rho_ng_ref;
    }

    public HashMap<String, double[]> getRhoBrr(AuxiliaryValues auxiliaryValues, double[] rayleighOpticalThickness, double[] reflectance, int sourceBandIndex) {
        auxiliaryValues.setRayleighThickness(rayleighOpticalThickness);

        final double[] ozaRads = auxiliaryValues.getViewZenithAnglesRad();
        final double[] airMasses = auxiliaryValues.getAirMass();
        final double[] aziDiffs = auxiliaryValues.getAziDifferent();
        final double[] cosSZARads = auxiliaryValues.getCosSZARads();
        final double[] cosOZARads = auxiliaryValues.getCosOZARads();
        final double[] sARay = auxiliaryValues.getInterpolateRayleighThickness();
        final double[] tau_ray = auxiliaryValues.getTaur();

        final Map<Integer, double[]> fourier = auxiliaryValues.getFourier();
        final Map<Integer, List<double[]>> interpolation = auxiliaryValues.getInterpolation();
        final int length = ozaRads.length;


        final HashMap<String, double[]> rayleighHashMap = new HashMap<>();
        final double[] rho_BRR = new double[length];
        final double[] sphericalFactor = new double[length];
        final double[] rho_toaR = new double[length];
        final double[] tR_thetaV = new double[length];
        final double[] tR_thetaS = new double[length];
        final double[] rho_R = new double[length];

        final double[] rRayF1 = new double[length];
        final double[] rRayF2 = new double[length];
        final double[] rRayF3 = new double[length];

        final double rho_Rm[] = new double[3];

        for (int index = 0; index < length; index++) {
            double taurVal = rayleighOpticalThickness[index];
            if (Double.isNaN(taurVal)) {
                rho_BRR[index] = taurVal;
                continue;
            }
            double aziDiff = aziDiffs[index];
            double massAir = airMasses[index];

            double cosOZARad = cosOZARads[index];
            double cosSZARad = cosSZARads[index];

            List<double[]> interpolateValues = interpolation.get(index);
            double[] fourierSeries = fourier.get(index);

            for (int i = 0; i < fourierSeries.length; i++) {
                double[] interpolatedValueABCD = interpolateValues.get(i);
                double a = interpolatedValueABCD[0];
                double b = interpolatedValueABCD[1];
                double c = interpolatedValueABCD[2];
                double d = interpolatedValueABCD[3];

                double rayPrimaryScatters = (fourierSeries[i] / (4.0 * (cosSZARad + cosOZARad))) * (1.0 - Math.exp(-massAir * taurVal));
                double rayMultiCorr = a + b * taurVal + c * Math.pow(taurVal, 2) + d * Math.pow(taurVal, 3);
                rho_Rm[i] = rayMultiCorr * rayPrimaryScatters;
            }
            // Fourier sum to get the Rayleigh Reflectance
            rRayF1[index] = rho_Rm[0];
            rRayF2[index] = rho_Rm[1];
            rRayF3[index] = rho_Rm[2];

            rho_R[index] = rho_Rm[0] + 2.0 * rho_Rm[1] * Math.cos(aziDiff) + 2.0 * rho_Rm[2] * Math.cos(2.0 * aziDiff);
            // polynomial coefficients tpoly0, tpoly1 and tpoly2 from MERIS LUT

            double tRs = ((2.0 / 3.0 + cosSZARad) + (2.0 / 3.0 - cosSZARad) * Math.exp(-taurVal / cosSZARad)) / (4.0 / 3.0 + taurVal);

            tR_thetaS[index] = tau_ray[0] + tau_ray[1] * tRs + tau_ray[2] * Math.pow(tRs, 2);
            //#Rayleigh Transmittance sun - surface
            double tRv = ((2.0 / 3.0 + cosOZARad) + (2.0 / 3.0 - cosOZARad) * Math.exp(-taurVal / cosOZARad)) / (4.0 / 3.0 + taurVal);
            //#Rayleigh Transmittance surface - sensor
            tR_thetaV[index] = tau_ray[0] + tau_ray[1] * tRv + tau_ray[2] * Math.pow(tRv, 2);

            double saRay = sARay[index];

            rho_toaR[index] = (reflectance[index] - rho_R[index]) / (tR_thetaS[index] * tR_thetaV[index]); //toa reflectance corrected for Rayleigh scattering
            sphericalFactor[index] = 1.0 / (1.0 + saRay * rho_toaR[index]); //#factor used in the next equation to account for the spherical albedo
            //#top of aerosol reflectance, which is equal to bottom of Rayleigh reflectance
            rho_BRR[index] = (rho_toaR[index] * sphericalFactor[index]);
        }

        rayleighHashMap.put(String.format("rBRR_%02d", sourceBandIndex), rho_BRR);
        rayleighHashMap.put(String.format("sphericalAlbedoFactor_%02d", sourceBandIndex), sphericalFactor);
        rayleighHashMap.put(String.format("rtoaRay_%02d", sourceBandIndex), rho_toaR);
        rayleighHashMap.put(String.format("transVRay_%02d", sourceBandIndex), tR_thetaV);
        rayleighHashMap.put(String.format("sARay_%02d", sourceBandIndex), sARay);
        rayleighHashMap.put(String.format("transSRay_%02d", sourceBandIndex), tR_thetaS);
        rayleighHashMap.put(String.format("rRay_%02d", sourceBandIndex), rho_R);

        rayleighHashMap.put(String.format("rRayF1_%02d", sourceBandIndex), rRayF1);
        rayleighHashMap.put(String.format("rRayF2_%02d", sourceBandIndex), rRayF2);
        rayleighHashMap.put(String.format("rRayF3_%02d", sourceBandIndex), rRayF3);





        return rayleighHashMap;
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


    public HashMap<String, double[]> correct(AuxiliaryValues auxiliaryValues, double[] ray_phase_min, int sourceBandIndex) {

        double[] lambda = auxiliaryValues.getLambdaSource();
        double[] seaAirPressure = auxiliaryValues.getSeaLevels();
        double[] altitude = auxiliaryValues.getAltitudes();
        double[] szaRads = auxiliaryValues.getSunZenithAnglesRad();
        double[] ozaRads = auxiliaryValues.getViewZenithAnglesRad();

        double[] rRaySimple = new double[ozaRads.length];
        double[] taurS = new double[ozaRads.length];
        HashMap<String, double[]> rayHashMap = new HashMap<>();
        rayHashMap.put(String.format("taurS_%02d", sourceBandIndex), taurS);
        rayHashMap.put(String.format("RayleighSimple_%02d", sourceBandIndex), rRaySimple);

        for (int i = 0; i < ray_phase_min.length; i++) {
            double taurSLocal = Math.exp(-4.637) * Math.pow((lambda[i] / 1000.0), -4.0679);
            double pressureAtms = seaAirPressure[i] * Math.exp(-altitude[i] / 8000.0);
            double pressureFactor = taurSLocal / 1013.0;
            taurS[i] = pressureAtms * pressureFactor;
            rRaySimple[i] = Math.cos(szaRads[i]) * taurS[i] * ray_phase_min[i] / (4 * Math.PI) * (1 / Math.cos(ozaRads[i]) * Math.PI);
        }

        return rayHashMap;
    }


    //todo mba/** write test
    public double[] convertRadsToRefls(AuxiliaryValues auxiliaryValues) {

        double[] radiance = auxiliaryValues.getSourceSampleRad();
        double[] solarIrradiance = auxiliaryValues.getSolarFluxs();
        double[] sza = auxiliaryValues.getSunZenithAngles();
        double[] ref = new double[radiance.length];
        double v = Math.PI / 180.0;
        for (int i = 0; i < ref.length; i++) {
            ref[i] = (radiance[i] * Math.PI) / (solarIrradiance[i] * Math.cos(sza[i] * v));
        }
        return ref;
    }
}
