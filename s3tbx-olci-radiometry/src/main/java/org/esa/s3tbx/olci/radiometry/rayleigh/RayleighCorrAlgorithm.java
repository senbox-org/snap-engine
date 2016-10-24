package org.esa.s3tbx.olci.radiometry.rayleigh;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.math.RsMathUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrAlgorithm {


    public int numBands = 21;
    public String bandNamePattern = "Oa%02d_radiance";

    public RayleighCorrAlgorithm() {
    }

    public RayleighCorrAlgorithm(String bandPattern, int numBand) {
        this.numBands = numBand;
        this.bandNamePattern = bandPattern;
    }

    //todo mba/* write test
    public double[] waterVaporCorrection709(double[] reflectances, double[] bWVRefTile, double[] bWVTile) {
        double[] H2O_COR_POLY = new double[]{0.3832989, 1.6527957, -1.5635101, 0.5311913};  // Polynomial coefficients for WV transmission @ 709nm
        // in order to optimise performance we do:
        // trans709 = H2O_COR_POLY[0] + (H2O_COR_POLY[1] + (H2O_COR_POLY[2] + H2O_COR_POLY[3] * X2) * X2) * X2
        // when X2 = 1
        // trans709 = 0.3832989 + ( 1.6527957+ (-1.5635101+ 0.5311913*1)*1)*1
        double trans709 = 1.0037757999999999;
        for (int i = 0; i < bWVTile.length; i++) {
            if (bWVRefTile[i] > 0) {
                double X2 = bWVTile[i] / bWVRefTile[i];
                trans709 = H2O_COR_POLY[0] + (H2O_COR_POLY[1] + (H2O_COR_POLY[2] + H2O_COR_POLY[3] * X2) * X2) * X2;
            }
            reflectances[i] = reflectances[i] / trans709;
        }
        return reflectances;
    }

    public double[] getCrossSectionSigma(Product sourceProduct, int numBands, String getBandNamePattern) {
        double[] waveLength = new double[numBands];
        for (int i = 0; i < numBands; i++) {
            waveLength[i] = sourceProduct.getBand(String.format(getBandNamePattern, i + 1)).getSpectralWavelength();
        }

        return getCrossSection(waveLength);
    }

    public double[] getCrossSection(double[] lambdas) {
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


    public double[] getCorrOzone(double[] rho_ng_ref, double absorpO, double[] ozones, double[] cosOZARads, double[] cosSZARads) {
        for (int i = 0; i < rho_ng_ref.length; i++) {
            double cts = cosSZARads[i]; //#cosine of sun zenith angle
            double ctv = cosOZARads[i];//#cosine of view zenith angle
            double ozone = ozones[i];
            rho_ng_ref[i] = getCorrOzone(rho_ng_ref[i], absorpO, ozone, cts, ctv);
        }
        return rho_ng_ref;
    }

    public double getCorrOzone(double rho_ng, double absorpO, double ozone, double cts, double ctv) {
        if (cts == 0 || ctv == 0) {
            throw new ArithmeticException("The sun angel and the view angle must not be zero.");
        }
        double model_ozone = 0;
        double trans_ozoned12 = Math.exp(-(absorpO * ozone / 1000.0 - model_ozone) / cts);
        double trans_ozoneu12 = Math.exp(-(absorpO * ozone / 1000.0 - model_ozone) / ctv);
        double trans_ozone12 = trans_ozoned12 * trans_ozoneu12;
        return rho_ng / trans_ozone12;
    }

    public double[] getRhoBrr(RayleighAux rayleighAux, double[] rayleighOpticalThickness, double[] corrOzoneRefl) {
        final double[] airMasses = rayleighAux.getAirMass();
        final double[] aziDiffs = rayleighAux.getAziDifferent();
        final double[] cosSZARads = rayleighAux.getCosSZARads();
        final double[] cosOZARads = rayleighAux.getCosOZARads();
        final double[] sARay = rayleighAux.getInterpolateRayleighThickness(rayleighOpticalThickness);
        final double[] tau_ray = rayleighAux.getTaur();

        final Map<Integer, double[]> fourier = rayleighAux.getFourier();
        final Map<Integer, List<double[]>> interpolation = rayleighAux.getInterpolation();
        final int length = cosOZARads.length;

        final double[] rho_BRR = new double[length];


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

            double[] rho_Rm = getFourierSeries(taurVal, massAir, cosOZARad, cosSZARad, interpolateValues, fourierSeries);

            double rho_R = rho_Rm[0] + 2.0 * rho_Rm[1] * Math.cos(aziDiff) + 2.0 * rho_Rm[2] * Math.cos(2.0 * aziDiff);
            // polynomial coefficients tpoly0, tpoly1 and tpoly2 from MERIS LUT

            double tRs = ((2.0 / 3.0 + cosSZARad) + (2.0 / 3.0 - cosSZARad) * Math.exp(-taurVal / cosSZARad)) / (4.0 / 3.0 + taurVal);

            double tR_thetaS = tau_ray[0] + tau_ray[1] * tRs + tau_ray[2] * Math.pow(tRs, 2);
            //#Rayleigh Transmittance sun - surface
            double tRv = ((2.0 / 3.0 + cosOZARad) + (2.0 / 3.0 - cosOZARad) * Math.exp(-taurVal / cosOZARad)) / (4.0 / 3.0 + taurVal);
            //#Rayleigh Transmittance surface - sensor
            double tR_thetaV = tau_ray[0] + tau_ray[1] * tRv + tau_ray[2] * Math.pow(tRv, 2);

            double saRay = sARay[index];

            double rho_toaR = (corrOzoneRefl[index] - rho_R) / (tR_thetaS * tR_thetaV); //toa corrOzoneRefl corrected for Rayleigh scattering
            double sphericalFactor = 1.0 / (1.0 + saRay * rho_toaR); //#factor used in the next equation to account for the spherical albedo
            //#top of aerosol reflectance, which is equal to bottom of Rayleigh reflectance
            rho_BRR[index] = (rho_toaR * sphericalFactor);
        }
        return rho_BRR;
    }

    public double[] getFourierSeries(double rayleighOpticalThickness, double massAir, double cosOZARad, double cosSZARad, List<double[]> interpolateValues, double[] fourierSeriesCof) {
        double rho_Rm[] = new double[fourierSeriesCof.length];
        for (int i = 0; i < fourierSeriesCof.length; i++) {
            double[] interpolatedValueABCD = interpolateValues.get(i);
            double a = interpolatedValueABCD[0];
            double b = interpolatedValueABCD[1];
            double c = interpolatedValueABCD[2];
            double d = interpolatedValueABCD[3];

            double rayPrimaryScatters = (fourierSeriesCof[i] / (4.0 * (cosSZARad + cosOZARad))) * (1.0 - Math.exp(-massAir * rayleighOpticalThickness));
            double rayMultiCorr = a + b * rayleighOpticalThickness + c * Math.pow(rayleighOpticalThickness, 2) + d * Math.pow(rayleighOpticalThickness, 3);
            rho_Rm[i] = rayMultiCorr * rayPrimaryScatters;
        }
        return rho_Rm;
    }


    public RayleighOutput getRayleighReflectance(RayleighInput rayleighInput, RayleighAux rayleighAux, double[] absorptionOfBand, Product product) {
        int sourceIndex = rayleighInput.getSourceIndex();
        int lowerWaterIndex = rayleighInput.getLowerWaterIndex();
        int upperWaterIndex = rayleighInput.getUpperWaterIndex();

        OpticalThickness opticalThickness = new OpticalThickness(rayleighAux, product);
        double[] bandThicknessSource = opticalThickness.getThicknessBand(sourceIndex);
        double[] bandThicknessLower = opticalThickness.getThicknessBand(lowerWaterIndex);
        double[] bandThicknessUpper = opticalThickness.getThicknessBand(upperWaterIndex);

        double absorpSourceBand = absorptionOfBand[sourceIndex];
        double absorpLowerBand = absorptionOfBand[lowerWaterIndex];
        double absorpUppereBand = absorptionOfBand[upperWaterIndex];

        float[] sourceRayRefl = getRayleigh(rayleighAux, absorpSourceBand, bandThicknessSource, rayleighInput.getSourceReflectences());
        float[] lowerRayRefl = getRayleigh(rayleighAux, absorpLowerBand, bandThicknessLower, rayleighInput.getLowerReflectences());
        float[] upperRayRefl = getRayleigh(rayleighAux, absorpUppereBand, bandThicknessUpper, rayleighInput.getUpperReflectences());

        RayleighOutput rayleighOutput = new RayleighOutput(sourceRayRefl, lowerRayRefl, upperRayRefl);
        return rayleighOutput;
    }

    private float[] getRayleigh(RayleighAux rayleighAux, double absorptionOfBand, double[] thicknessAllBand, float[] ref) {

        double[] ozones = rayleighAux.getTotalOzones();
        double[] cosOZARads = rayleighAux.getCosOZARads();
        double[] cosSZARads = rayleighAux.getCosSZARads();

        double[] refDoube = convertFloatToDouble(ref);
        double[] corrOzone = getCorrOzone(refDoube, absorptionOfBand, ozones, cosSZARads, cosOZARads);

        double[] rhoBrr = getRhoBrr(rayleighAux, thicknessAllBand, corrOzone);
        return convertDoubleToFloat(rhoBrr);
    }

    private double[] convertFloatToDouble(float[] ref) {
        return IntStream.range(0, ref.length).mapToDouble(p -> ref[p]).toArray();
    }

    private float[] convertDoubleToFloat(double[] ref) {
        return Floats.toArray(Doubles.asList(ref));
    }


    public double[] getRayleighThickness(RayleighAux rayleighAux, double[] crossSectionSigma, int sourceBandIndex) {
        double[] seaLevels = rayleighAux.getSeaLevels();
        double[] altitudes = rayleighAux.getAltitudes();
        double[] latitudes = rayleighAux.getLatitudes();
        double sigma = crossSectionSigma[sourceBandIndex - 1];

        double rayleighOpticalThickness[] = new double[altitudes.length];
        for (int i = 0; i < altitudes.length; i++) {
            rayleighOpticalThickness[i] = getRayleighOpticalThickness(sigma, seaLevels[i], altitudes[i], latitudes[i]);
        }

        return rayleighOpticalThickness;
    }

    public double getRayleighOpticalThickness(double sigma, double seaLevelPressure, double altitude, double latitude) {
        double P = seaLevelPressure * Math.pow((1.0 - 0.0065 * altitude / 288.15), 5.255) * 1000;
        double latRad = Math.toRadians(latitude);
        double cos2LatRad = Math.cos(2 * latRad);
        double g0 = 980.616 * (1 - 0.0026373 * cos2LatRad + 0.0000059 * Math.pow(cos2LatRad, 2));
        double effectiveMassWeightAltitude = 0.73737 * altitude + 5517.56;

        double g = g0 - (0.0003085462 + 0.000000227 * cos2LatRad) * effectiveMassWeightAltitude +
                (0.00000000007254 + 0.0000000000001 * cos2LatRad) * Math.pow(effectiveMassWeightAltitude, 2) -
                (1.517E-17 + 6E-20 * cos2LatRad) * Math.pow(effectiveMassWeightAltitude, 3);

        double factor = (P * RayleighConstants.AVOGADRO_NUMBER) / (RayleighConstants.MEAN_MOLECULAR_WEIGHT_C02 * g);
        return factor * sigma;
    }

    public double[] convertRadsToRefls(double[] radiance, double[] solarIrradiance, double[] sza) {
        double[] ref = new double[radiance.length];
        for (int i = 0; i < ref.length; i++) {
            ref[i] = RsMathUtils.radianceToReflectance((float) radiance[i], (float) sza[i], (float) solarIrradiance[i]);
        }
        return ref;
    }


    private class OpticalThickness {
        RayleighAux rayleighAux;
        private Product product;
        private Map<Integer, double[]> thicknessAllBands;

        public OpticalThickness(RayleighAux rayleighAux, Product product) {
            this.rayleighAux = rayleighAux;
            this.product = product;
        }

        private Map<Integer, double[]> getThicknessAllBands() {
            double[] crossSectionSigma = getCrossSectionSigma(product, numBands, bandNamePattern);
            Map<Integer, double[]> thicknessPerBand = new HashMap<>();
            for (int bandIndex = 1; bandIndex <= numBands; bandIndex++) {
                double[] rayleighThickness = getRayleighThickness(rayleighAux, crossSectionSigma, bandIndex);
                thicknessPerBand.put(bandIndex, rayleighThickness);
            }
            return thicknessPerBand;
        }

        public double[] getThicknessBand(int bandIndex) {
            if (thicknessAllBands == null) {
                thicknessAllBands = getThicknessAllBands();
            }
            return thicknessAllBands.get(bandIndex + 1);
        }
    }
}
