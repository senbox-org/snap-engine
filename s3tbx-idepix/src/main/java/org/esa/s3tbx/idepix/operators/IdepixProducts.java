package org.esa.s3tbx.idepix.operators;

import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.s3tbx.meris.brr.GaseousCorrectionOp;
import org.esa.s3tbx.meris.brr.LandClassificationOp;
import org.esa.s3tbx.meris.brr.Rad2ReflOp;
import org.esa.s3tbx.meris.brr.RayleighCorrectionOp;
import org.esa.s3tbx.meris.brr.operator.BrrOp;
import org.esa.s3tbx.meris.cloud.BlueBandOp;
import org.esa.s3tbx.meris.cloud.CloudProbabilityOp;
import org.esa.s3tbx.meris.cloud.CombinedCloudOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.unmixing.Endmember;
import org.esa.snap.unmixing.SpectralUnmixingOp;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides intermediate products required in Idepix processing chains.
 *
 * @author olafd
 */
public class IdepixProducts {

    public static Product computeRadiance2ReflectanceProduct(Product sourceProduct) {
        return GPF.createProduct(OperatorSpi.getOperatorAlias(Rad2ReflOp.class), GPF.NO_PARAMS, sourceProduct);
    }

    public static Product computeCloudTopPressureProduct(Product sourceProduct) {
        return GPF.createProduct("Meris.CloudTopPressureOp", GPF.NO_PARAMS, sourceProduct);
    }

    public static Product computeCloudTopPressureStraylightProduct(Product sourceProduct, boolean straylightCorr) {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("straylightCorr", straylightCorr);
        return GPF.createProduct("Meris.CloudTopPressureOp", params, sourceProduct);
    }

    public static Product computeBarometricPressureProduct(Product sourceProduct, boolean useGetasseDem) {
        Map<String, Object> params = new HashMap<String, Object>(1);
        params.put("useGetasseDem", useGetasseDem);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(BarometricPressureOp.class), params, sourceProduct);
    }

    public static Product computePressureLiseProduct(Product sourceProduct, Product rad2reflProduct,
                                                     boolean ipfOutputL2CloudDetection,
                                                     boolean straylightCorr,
                                                     boolean outputP1,
                                                     boolean outputPressureSurface,
                                                     boolean outputP2,
                                                     boolean outputPScatt) {
        Map<String, Product> input = new HashMap<String, Product>(2);
        input.put("l1b", sourceProduct);
        input.put("rhotoa", rad2reflProduct);
        Map<String, Object> params = new HashMap<String, Object>(6);
        params.put("straylightCorr", straylightCorr);
        params.put("outputP1", outputP1);
        params.put("outputPressureSurface", outputPressureSurface);
        params.put("outputP2", outputP2);
        params.put("outputPScatt", outputPScatt);
        params.put("l2CloudDetection", ipfOutputL2CloudDetection);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(LisePressureOp.class), params, input);
    }

    public static Product computePsurfNNProduct(Product sourceProduct, Product merisCloudProduct,
                                                boolean pressureFubTropicalAtmosphere,
                                                boolean straylightCorr) {
        Map<String, Product> input = new HashMap<String, Product>(2);
        input.put("l1b", sourceProduct);
        input.put("cloud", merisCloudProduct);
        Map<String, Object> params = new HashMap<String, Object>(2);
        params.put("tropicalAtmosphere", pressureFubTropicalAtmosphere);
        // mail from RL, 2009/03/19: always apply correction on FUB pressure
        // currently only for RR (FR coefficients still missing)
        params.put("straylightCorr", straylightCorr);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(SurfacePressureFubOp.class), params, input);
    }

    public static Product computeGaseousCorrectionProduct(Product sourceProduct, Product rad2reflProduct, Product merisCloudProduct,
                                                          boolean correctWater) {
        Map<String, Product> input = new HashMap<String, Product>(3);
        input.put("l1b", sourceProduct);
        input.put("rhotoa", rad2reflProduct);
        input.put("cloud", merisCloudProduct);
        Map<String, Object> params = new HashMap<String, Object>(2);
        params.put("correctWater", correctWater);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(GaseousCorrectionOp.class), params, input);
    }

    public static Product computeRayleighCorrectionProduct(Product sourceProduct,
                                                           Product gasProduct,
                                                           Product rad2reflProduct,
                                                           Product landProduct,
                                                           Product merisCloudProduct,
                                                           boolean ccOutputRayleigh,
                                                           String landExpression) {
        Map<String, Product> input = new HashMap<String, Product>(3);
        input.put("l1b", sourceProduct);
        input.put("input", gasProduct);
        input.put("rhotoa", rad2reflProduct);
        input.put("land", landProduct);
        input.put("cloud", merisCloudProduct);
        Map<String, Object> params = new HashMap<String, Object>(2);
        params.put("correctWater", true);
        params.put("landExpression", landExpression);
        params.put("exportBrrNormalized", ccOutputRayleigh);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(IdepixRayleighCorrectionOp.class), params,
                input);
    }

    public static Product computeSpectralUnmixingProduct(Product rayleighProduct, boolean computeErrorBands) {
        Map<String, Product> input = new HashMap<String, Product>(1);
        input.put("sourceProduct", rayleighProduct);
        Map<String, Object> params = new HashMap<String, Object>(3);
        params.put("sourceBandNames", IdepixConstants.SMA_SOURCE_BAND_NAMES);
        final Endmember[] endmembers = IdepixUtils.setupCCSpectralUnmixingEndmembers();
        params.put("endmembers", endmembers);
        params.put("computeErrorBands", computeErrorBands);
        params.put("minBandwidth", 5.0);
        params.put("unmixingModelName", "Fully Constrained LSU");
        return GPF.createProduct(OperatorSpi.getOperatorAlias(SpectralUnmixingOp.class), params, input);
    }

    public static Product computeMerisCloudProduct(Product sourceProduct,
                                                   Product rad2reflProduct,
                                                   Product ctpProduct,
                                                   Product pressureLiseProduct,
                                                   Product pbaroProduct,
                                                   boolean computeL2Pressure) {
        Map<String, Product> input = new HashMap<String, Product>(4);
        input.put("l1b", sourceProduct);
        input.put("rhotoa", rad2reflProduct);
        input.put("ctp", ctpProduct);
        input.put("pressureOutputLise", pressureLiseProduct);
        input.put("pressureBaro", pbaroProduct);

        Map<String, Object> params = new HashMap<String, Object>(11);
        params.put("l2Pressures", computeL2Pressure);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(MerisClassificationOp.class),
                params, input);
    }

    public static Product computeLandClassificationProduct(Product sourceProduct, Product gasProduct) {
        Map<String, Product> input = new HashMap<String, Product>(2);
        input.put("l1b", sourceProduct);
        input.put("gascor", gasProduct);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(LandClassificationOp.class), GPF.NO_PARAMS, input);
    }

    public static Product computeBlueBandProduct(Product sourceProduct, Product brrProduct) {
        Map<String, Product> input = new HashMap<String, Product>(2);
        input.put("l1b", sourceProduct);
        input.put("toar", brrProduct);
        return GPF.createProduct("Meris.BlueBand", GPF.NO_PARAMS, input);
    }

    public static Product computeBrrProduct(Product sourceProduct, boolean outputToar, boolean correctWater) {
        Map<String, Product> input = new HashMap<String, Product>(1);
        input.put("input", sourceProduct);
        Map<String, Object> params = new HashMap<String, Object>(2);
        params.put("outputToar", outputToar);
        params.put("correctWater", correctWater);
        return GPF.createProduct(OperatorSpi.getOperatorAlias(BrrOp.class), params, input);
    }

    public static Product computeCombinedCloudProduct(Product blueBandProduct, Product cloudProbabilityProduct) {
        Map<String, Product> input = new HashMap<String, Product>(2);
        input.put("cloudProb", cloudProbabilityProduct);
        input.put("blueBand", blueBandProduct);
        return GPF.createProduct("Meris.CombinedCloud", GPF.NO_PARAMS, input);
    }

    public static Product computeCloudProbabilityProduct(Product sourceProduct) {
        Map<String, Product> input = new HashMap<String, Product>(1);
        input.put("input", sourceProduct);
        Map<String, Object> params = new HashMap<String, Object>(3);
        params.put("configFile", "cloud_config.txt");
        params.put("validLandExpression", "not l1_flags.INVALID and dem_alt > -50");
        params.put("validOceanExpression", "not l1_flags.INVALID and dem_alt <= -50");
        return GPF.createProduct("Meris.CloudProbability", params, input);
    }

    public static void addRadianceBands(Product l1bProduct, Product targetProduct) {
        for (String bandname : l1bProduct.getBandNames()) {
            if (!targetProduct.containsBand(bandname) && bandname.startsWith(Rad2ReflOp.RADIANCE_BAND_PREFIX)) {
                System.out.println("adding band: " + bandname);
                ProductUtils.copyBand(bandname, l1bProduct, targetProduct, true);
            }
        }
    }

    public static void addRadiance2ReflectanceBands(Product rad2reflProduct, Product targetProduct) {
        for (String bandname : rad2reflProduct.getBandNames()) {
            if (!targetProduct.containsBand(bandname) && bandname.startsWith(Rad2ReflOp.RHO_TOA_BAND_PREFIX)) {
                System.out.println("adding band: " + bandname);
                ProductUtils.copyBand(bandname, rad2reflProduct, targetProduct, true);
                targetProduct.getBand(bandname).setUnit("dl");
            }
        }
    }

    public static void addCCSeaiceClimatologyValueBand(Product merisCloudProduct, Product targetProduct) {
        for (String bandname : merisCloudProduct.getBandNames()) {
            if (bandname.equalsIgnoreCase("sea_ice_climatology_value")) {
                moveBand(targetProduct, merisCloudProduct, bandname);
            }
        }
    }

    public static void addCCCloudProbabilityValueBand(Product merisCloudProduct, Product targetProduct) {
        for (String bandname : merisCloudProduct.getBandNames()) {
            if (bandname.equalsIgnoreCase(MerisClassificationOp.CLOUD_PROBABILITY_VALUE)) {
                moveBand(targetProduct, merisCloudProduct, bandname);
            }
        }
    }

    public static void addMERISAlternativeNNOutputBand(Product merisCloudProduct, Product targetProduct) {
        for (String bandname : merisCloudProduct.getBandNames()) {
            if (bandname.equalsIgnoreCase("meris_water_nn_value")) {
                moveBand(targetProduct, merisCloudProduct, bandname);
            }
        }
    }
//    addMERISAlternativeNNOutputBand

    public static void addRayleighCorrectionBands(Product rayleighProduct, Product targetProduct) {
        FlagCoding flagCoding = RayleighCorrectionOp.createFlagCoding();
        targetProduct.getFlagCodingGroup().add(flagCoding);
        for (Band band : rayleighProduct.getBands()) {
            // do not add the normalized bands
            if (!targetProduct.containsBand(band.getName()) && !band.getName().endsWith("_n")) {
                if (band.getName().equals(RayleighCorrectionOp.RAY_CORR_FLAGS)) {
                    band.setSampleCoding(flagCoding);
                }
                System.out.println("adding band: " + band.getName());
                band.setUnit("dl");
                targetProduct.addBand(band);
                targetProduct.getBand(band.getName()).setSourceImage(band.getSourceImage());
            }
        }
    }

    public static void addSpectralUnmixingBands(Product smaProduct, Product targetProduct) {
        for (Band band : smaProduct.getBands()) {
            // do not add the normalized bands
            if (!targetProduct.containsBand(band.getName()) &&
                    (band.getName().startsWith("summary") || band.getName().endsWith("_abundance"))) {
                System.out.println("adding band: " + band.getName());
                targetProduct.addBand(band);
                targetProduct.getBand(band.getName()).setSourceImage(band.getSourceImage());
                final String origBandName = band.getName();
                targetProduct.getBand(origBandName).setName("spec_unmix_" + origBandName);
            }
        }
    }

    public static void addCloudClassificationFlagBand(Product merisCloudProduct, Product targetProduct) {
        FlagCoding flagCoding = MerisClassificationOp.createFlagCoding(
                MerisClassificationOp.CLOUD_FLAGS);
        targetProduct.getFlagCodingGroup().add(flagCoding);
        for (Band band : merisCloudProduct.getBands()) {
            if (band.getName().equals(MerisClassificationOp.CLOUD_FLAGS)) {
                band.setSampleCoding(flagCoding);
                targetProduct.addBand(band);
            }
        }
    }

    public static void addGaseousCorrectionBands(Product gasProduct, Product targetProduct) {
        FlagCoding flagCoding = GaseousCorrectionOp.createFlagCoding();
        targetProduct.getFlagCodingGroup().add(flagCoding);
        Band band = gasProduct.getBand(GaseousCorrectionOp.GAS_FLAGS);
        band.setSampleCoding(flagCoding);
        System.out.println("adding band: " + band.getName());
        targetProduct.addBand(band);
    }

    public static void addLandClassificationBand(Product landProduct, Product targetProduct) {
        FlagCoding flagCoding = LandClassificationOp.createFlagCoding();
        targetProduct.getFlagCodingGroup().add(flagCoding);
        for (Band band : landProduct.getBands()) {
            if (!targetProduct.containsBand(band.getName())) {
                if (band.getName().equals(LandClassificationOp.LAND_FLAGS)) {
                    band.setSampleCoding(flagCoding);
                }
                targetProduct.addBand(band);
            }
        }
    }

    public static void addCtpStraylightProductBands(Product ctpProductStraylight, Product targetProduct) {
        for (String bandname : ctpProductStraylight.getBandNames()) {
            if (!bandname.equals(MerisClassificationOp.CLOUD_FLAGS)) {
                moveBand(targetProduct, ctpProductStraylight, bandname);
            }
        }
    }

    public static void addPsurfNNProductBands(Product psurfNNProduct, Product targetProduct) {
        for (String bandname : psurfNNProduct.getBandNames()) {
            moveBand(targetProduct, psurfNNProduct, bandname);
        }
    }

    public static void addBarometricPressureProductBands(Product pbaroProduct, Product targetProduct) {
        for (String bandname : pbaroProduct.getBandNames()) {
            moveBand(targetProduct, pbaroProduct, bandname);
        }
    }

    public static void addPressureLiseProductBands(Product pressureLiseProduct, Product targetProduct,
                                                   boolean pressureOutputPSurfLise, boolean pressureOutputP2Lise) {
        addPressureLiseProductBand(pressureLiseProduct, targetProduct, LisePressureOp.PRESSURE_LISE_P1);
        if (pressureOutputPSurfLise) {
            addPressureLiseProductBand(pressureLiseProduct, targetProduct, LisePressureOp.PRESSURE_LISE_PSURF);
        }
        if (pressureOutputP2Lise) {
            addPressureLiseProductBand(pressureLiseProduct, targetProduct, LisePressureOp.PRESSURE_LISE_P2);
        }
        addPressureLiseProductBand(pressureLiseProduct, targetProduct, LisePressureOp.PRESSURE_LISE_PSCATT);
    }


    public static void addPressureLiseProductBand(Product pressureLiseProduct, Product targetProduct, String bandname) {
        moveBand(targetProduct, pressureLiseProduct, bandname);
    }

    public static void addBlueBandProductBands(Product blueBandProduct, Product targetProduct) {
        FlagCoding flagCoding = BlueBandOp.createFlagCoding();
        targetProduct.getFlagCodingGroup().add(flagCoding);
        Band band = blueBandProduct.getBand(BlueBandOp.BLUE_FLAG_BAND);
        band.setSampleCoding(flagCoding);
        moveBand(targetProduct, blueBandProduct, BlueBandOp.BLUE_FLAG_BAND);
    }

    public static void addCloudProbabilityProductBands(Product cloudProbabilityProduct, Product targetProduct) {
        FlagCoding flagCoding = CloudProbabilityOp.createCloudFlagCoding(targetProduct);
        targetProduct.getFlagCodingGroup().add(flagCoding);
        for (Band band : cloudProbabilityProduct.getBands()) {
            if (!targetProduct.containsBand(band.getName())) {
                if (band.getName().equals(CloudProbabilityOp.CLOUD_FLAG_BAND)) {
                    band.setSampleCoding(flagCoding);
                }
                targetProduct.addBand(band);
            }
        }
    }

    public static void addCombinedCloudProductBands(Product combinedCloudProduct, Product targetProduct) {
        FlagCoding flagCoding = CombinedCloudOp.createFlagCoding();
        targetProduct.getFlagCodingGroup().add(flagCoding);
        Band band = combinedCloudProduct.getBand(CombinedCloudOp.FLAG_BAND_NAME);
        band.setSampleCoding(flagCoding);
        moveBand(targetProduct, combinedCloudProduct, CombinedCloudOp.FLAG_BAND_NAME);
    }

    private static void moveBand(Product targetProduct, Product product, String bandname) {
        if (!targetProduct.containsBand(bandname)) {
            System.out.println("adding band: " + bandname);
            targetProduct.addBand(product.getBand(bandname));
        }
    }

}
