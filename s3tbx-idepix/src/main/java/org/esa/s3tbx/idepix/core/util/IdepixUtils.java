package org.esa.s3tbx.idepix.core.util;


import org.esa.s3tbx.idepix.core.AlgorithmSelector;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.ProductUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Calendar;
import java.util.Random;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class IdepixUtils {

    public static final String F_INVALID_DESCR_TEXT = "Invalid pixels";
    public static final String F_CLOUD_DESCR_TEXT = "Pixels which are either cloud_sure or cloud_ambiguous";
    public static final String F_CLOUD_AMBIGUOUS_DESCR_TEXT = "Semi transparent clouds, or clouds where the detection level is uncertain";
    public static final String F_CLOUD_SURE_DESCR_TEXT = "Fully opaque clouds with full confidence of their detection";
    public static final String F_CLOUD_BUFFER_DESCR_TEXT = "A buffer of n pixels around a cloud. n is a user supplied parameter. Applied to pixels masked as 'cloud'";
    public static final String F_CLOUD_SHADOW_DESCR_TEXT = "Pixels is affect by a cloud shadow";
    public static final String F_COASTLINE_DESCR_TEXT = "Pixels at a coastline";
    public static final String F_CLEAR_SNOW_DESCR_TEXT = "Clear snow/ice pixels";
    public static final String F_CLEAR_LAND_DESCR_TEXT = "Clear land pixels";
    public static final String F_CLEAR_WATER_DESCR_TEXT = "Clear water pixels";
    public static final String F_LAND_DESCR_TEXT = "Land pixels";
    public static final String F_WATER_DESCR_TEXT = "Water pixels";
    public static final String F_BRIGHT_DESCR_TEXT = "Bright pixels";
    public static final String F_WHITE_DESCR_TEXT = "White pixels";
    public static final String F_BRIGHTWHITE_DESCR_TEXT = "'Brightwhite' pixels";
    public static final String F_HIGH_DESCR_TEXT = "High pixels";
    public static final String F_VEG_RISK_DESCR_TEXT = "Pixels with vegetation risk";
    public static final String F_SEAICE_DESCR_TEXT = "Sea ice pixels";

    private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger("idepix");

    public static final String IDEPIX_CLOUD_FLAGS = "cloud_classif_flags";
    public static final String IDEPIX_PIXEL_CLASSIF_FLAGS = "pixel_classif_flags";

    private IdepixUtils() {
    }

    public static Product cloneProduct(Product sourceProduct) {
        Product clonedProduct = new Product(sourceProduct.getName(),
                                            sourceProduct.getProductType(),
                                            sourceProduct.getSceneRasterWidth(),
                                            sourceProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(sourceProduct, clonedProduct);
        ProductUtils.copyGeoCoding(sourceProduct, clonedProduct);
        ProductUtils.copyFlagCodings(sourceProduct, clonedProduct);
        ProductUtils.copyFlagBands(sourceProduct, clonedProduct, true);
        ProductUtils.copyMasks(sourceProduct, clonedProduct);
        clonedProduct.setStartTime(sourceProduct.getStartTime());
        clonedProduct.setEndTime(sourceProduct.getEndTime());

        // copy all bands from source product
        for (Band b : sourceProduct.getBands()) {
            if (!clonedProduct.containsBand(b.getName())) {
                ProductUtils.copyBand(b.getName(), sourceProduct, clonedProduct, true);
                if (isIdepixSpectralBand(b)) {
                    ProductUtils.copyRasterDataNodeProperties(b, clonedProduct.getBand(b.getName()));
                }
            }
        }

        for (int i = 0; i < sourceProduct.getNumTiePointGrids(); i++) {
            TiePointGrid srcTPG = sourceProduct.getTiePointGridAt(i);
            if (!clonedProduct.containsTiePointGrid(srcTPG.getName())) {
                clonedProduct.addTiePointGrid(srcTPG.cloneTiePointGrid());
            }
        }


        return clonedProduct;
    }

    public static boolean isIdepixSpectralBand(Band b) {
        return b.getName().startsWith("radiance") || b.getName().startsWith("refl") ||
                b.getName().startsWith("brr") || b.getName().startsWith("rho_toa");
    }


    public static boolean validateInputProduct(Product inputProduct, AlgorithmSelector algorithm) {
        return isInputValid(inputProduct) && isInputConsistent(inputProduct, algorithm);
    }

    public static boolean isInputValid(Product inputProduct) {
        if (!isValidAvhrrProduct(inputProduct) &&
                !isValidLandsat8Product(inputProduct) &&
                !isValidProbavProduct(inputProduct) &&
                !isValidModisProduct(inputProduct) &&
                !isValidMsiProduct(inputProduct) &&
                !isValidSeawifsProduct(inputProduct) &&
                !isValidVgtProduct(inputProduct)) {
            logErrorMessage("Input sensor must be either MERIS, AATSR, AVHRR, colocated MERIS/AATSR, MODIS/SeaWiFS, PROBA-V or VGT!");
        }
        return true;
    }

    public static boolean isValidAvhrrProduct(Product product) {
        return product.getProductType().equalsIgnoreCase(IdepixConstants.AVHRR_L1b_PRODUCT_TYPE) ||
                product.getProductType().equalsIgnoreCase(IdepixConstants.AVHRR_L1b_USGS_PRODUCT_TYPE);
    }

    public static boolean isValidLandsat8Product(Product product) {
        return product.containsBand("coastal_aerosol") &&
                product.containsBand("blue") &&
                product.containsBand("green") &&
                product.containsBand("red") &&
                product.containsBand("near_infrared") &&
                product.containsBand("swir_1") &&
                product.containsBand("swir_2") &&
                product.containsBand("panchromatic") &&
                product.containsBand("cirrus") &&
                product.containsBand("thermal_infrared_(tirs)_1") &&
                product.containsBand("thermal_infrared_(tirs)_2");

    }

    public static boolean isValidModisProduct(Product product) {
        //        return (product.getName().matches("MOD021KM.A[0-9]{7}.[0-9]{4}.[0-9]{3}.[0-9]{13}.(?i)(hdf)") ||
        //                product.getName().matches("MOD021KM.A[0-9]{7}.[0-9]{4}.[0-9]{3}.[0-9]{13}") ||
        //                product.getName().matches("A[0-9]{13}.(?i)(L1B_LAC)"));
        return (product.getName().contains("MOD021KM") ||
                //                product.getName().contains("L1B_LAC"));
                product.getName().contains("L1B_"));  // seems that we have various extensions :-(
    }

    private static boolean isValidMsiProduct(Product inputProduct) {
        // todo
        return false;
    }

    public static boolean isValidSeawifsProduct(Product product) {
//        S2006131120520.L1B_LAC
//        S2005141121515.L1B_MLAC
        return (product.getName().matches("S[0-9]{13}.(?i)(L1B_LAC)") ||
                product.getName().matches("S[0-9]{13}.(?i)(L1B_MLAC)") ||
                product.getName().matches("S[0-9]{13}.(?i)(L1B_HRPT)") ||
                product.getName().matches("S[0-9]{13}.(?i)(L1C)"));
    }

    public static boolean isValidProbavProduct(Product product) {
        return product.getProductType().startsWith(IdepixConstants.PROBAV_PRODUCT_TYPE_PREFIX);
    }

    public static boolean isValidVgtProduct(Product product) {
        return product.getProductType().startsWith(IdepixConstants.SPOT_VGT_PRODUCT_TYPE_PREFIX);
    }

    private static boolean isInputConsistent(Product sourceProduct, AlgorithmSelector algorithm) {
        if (AlgorithmSelector.AVHRR == algorithm) {
            return (isValidAvhrrProduct(sourceProduct));
        } else if (AlgorithmSelector.LANDSAT8 == algorithm) {
            return (isValidLandsat8Product(sourceProduct));
        } else if (AlgorithmSelector.MODIS == algorithm) {
            return (isValidModisProduct(sourceProduct));
        } else if (AlgorithmSelector.MSI == algorithm) {
            return (isValidMsiProduct(sourceProduct));
        } else if (AlgorithmSelector.PROBAV == algorithm) {
            return (isValidProbavProduct(sourceProduct));
        } else if (AlgorithmSelector.SEAWIFS == algorithm) {
            return (isValidSeawifsProduct(sourceProduct));
        } else if (AlgorithmSelector.VGT == algorithm) {
            return (isValidVgtProduct(sourceProduct));
        } else {
            throw new OperatorException("Algorithm " + algorithm.toString() + "not supported.");
        }
    }

    public static void logErrorMessage(String msg) {
        if (System.getProperty("gpfMode") != null && "GUI".equals(System.getProperty("gpfMode"))) {
            JOptionPane.showOptionDialog(null, msg, "IDEPIX - Error Message", JOptionPane.DEFAULT_OPTION,
                                         JOptionPane.ERROR_MESSAGE, null, null, null);
        } else {
            info(msg);
        }
    }


    public static void info(final String msg) {
        logger.info(msg);
        System.out.println(msg);
    }


    public static float spectralSlope(float ch1, float ch2, float wl1, float wl2) {
        return (ch2 - ch1) / (wl2 - wl1);
    }

    public static float[] correctSaturatedReflectances(float[] reflectance) {

        // if all reflectances are NaN, do not correct
        if (isNoReflectanceValid(reflectance)) {
            return reflectance;
        }

        float[] correctedReflectance = new float[reflectance.length];

        // search for first non-NaN value from end of spectrum...
        correctedReflectance[reflectance.length - 1] = Float.NaN;
        for (int i = reflectance.length - 1; i >= 0; i--) {
            if (!Float.isNaN(reflectance[i])) {
                correctedReflectance[reflectance.length - 1] = reflectance[i];
                break;
            }
        }

        // correct NaN values from end of spectrum, start with first non-NaN value found above...
        for (int i = reflectance.length - 1; i > 0; i--) {
            if (Float.isNaN(reflectance[i - 1])) {
                correctedReflectance[i - 1] = correctedReflectance[i];
            } else {
                correctedReflectance[i - 1] = reflectance[i - 1];
            }
        }
        return correctedReflectance;
    }


    public static boolean areAllReflectancesValid(float[] reflectance) {
        for (float aReflectance : reflectance) {
            if (Float.isNaN(aReflectance) || aReflectance <= 0.0f) {
                return false;
            }
        }
        return true;
    }

    public static boolean areAllReflectancesValid(double[] reflectance) {
        for (double aReflectance : reflectance) {
            if (Double.isNaN(aReflectance) || aReflectance <= 0.0f) {
                return false;
            }
        }
        return true;
    }

    public static void setNewBandProperties(Band band, String description, String unit, double noDataValue,
                                            boolean useNoDataValue) {
        band.setDescription(description);
        band.setUnit(unit);
        band.setNoDataValue(noDataValue);
        band.setNoDataValueUsed(useNoDataValue);
    }

    public static FlagCoding createIdepixFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("F_INVALID", BitSetter.setFlag(0, IdepixConstants.F_INVALID), F_INVALID_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, IdepixConstants.F_CLOUD), F_CLOUD_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_AMBIGUOUS", BitSetter.setFlag(0, IdepixConstants.F_CLOUD_AMBIGUOUS), F_CLOUD_AMBIGUOUS_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SURE", BitSetter.setFlag(0, IdepixConstants.F_CLOUD_SURE), F_CLOUD_SURE_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_BUFFER", BitSetter.setFlag(0, IdepixConstants.F_CLOUD_BUFFER), F_CLOUD_BUFFER_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SHADOW", BitSetter.setFlag(0, IdepixConstants.F_CLOUD_SHADOW), F_CLOUD_SHADOW_DESCR_TEXT);
        flagCoding.addFlag("F_COASTLINE", BitSetter.setFlag(0, IdepixConstants.F_COASTLINE), F_COASTLINE_DESCR_TEXT);
        flagCoding.addFlag("F_CLEAR_SNOW", BitSetter.setFlag(0, IdepixConstants.F_CLEAR_SNOW), F_CLEAR_SNOW_DESCR_TEXT);
        flagCoding.addFlag("F_CLEAR_LAND", BitSetter.setFlag(0, IdepixConstants.F_CLEAR_LAND), F_CLEAR_LAND_DESCR_TEXT);
        flagCoding.addFlag("F_CLEAR_WATER", BitSetter.setFlag(0, IdepixConstants.F_CLEAR_WATER), F_CLEAR_WATER_DESCR_TEXT);
        flagCoding.addFlag("F_LAND", BitSetter.setFlag(0, IdepixConstants.F_LAND), F_LAND_DESCR_TEXT);
        flagCoding.addFlag("F_WATER", BitSetter.setFlag(0, IdepixConstants.F_WATER), F_WATER_DESCR_TEXT);
        flagCoding.addFlag("F_BRIGHT", BitSetter.setFlag(0, IdepixConstants.F_BRIGHT), F_BRIGHT_DESCR_TEXT);
        flagCoding.addFlag("F_WHITE", BitSetter.setFlag(0, IdepixConstants.F_WHITE), F_WHITE_DESCR_TEXT);
        flagCoding.addFlag("F_BRIGHTWHITE", BitSetter.setFlag(0, IdepixConstants.F_BRIGHTWHITE), F_BRIGHTWHITE_DESCR_TEXT);
        flagCoding.addFlag("F_HIGH", BitSetter.setFlag(0, IdepixConstants.F_HIGH), F_HIGH_DESCR_TEXT);
        flagCoding.addFlag("F_VEG_RISK", BitSetter.setFlag(0, IdepixConstants.F_VEG_RISK), F_VEG_RISK_DESCR_TEXT);
        flagCoding.addFlag("F_SEAICE", BitSetter.setFlag(0, IdepixConstants.F_SEAICE), F_SEAICE_DESCR_TEXT);

        return flagCoding;
    }


    public static int setupIdepixCloudscreeningBitmasks(Product gaCloudProduct) {

        int index = 0;
        int w = gaCloudProduct.getSceneRasterWidth();
        int h = gaCloudProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random();

        mask = Mask.BandMathsType.create("lc_invalid",
                                         F_INVALID_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_INVALID",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_cloud",
                                         F_CLOUD_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLOUD or cloud_classif_flags.F_CLOUD_SURE or cloud_classif_flags.F_CLOUD_AMBIGUOUS",
                                         new Color(178, 178, 0), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_cloud_ambiguous",
                                         F_CLOUD_AMBIGUOUS_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLOUD_AMBIGUOUS",
                                         new Color(255, 219, 156), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_cloud_sure",
                                         F_CLOUD_SURE_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLOUD_SURE",
                                         new Color(224, 224, 30), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_cloud_buffer",
                                         F_CLOUD_BUFFER_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLOUD_BUFFER",
                                         Color.red, 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_cloud_shadow",
                                         F_CLOUD_SHADOW_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLOUD_SHADOW",
                                         Color.cyan, 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_coastline",
                                         F_COASTLINE_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_COASTLINE",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_clear_snow",
                                         F_CLEAR_SNOW_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLEAR_SNOW",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_clear_land",
                                         F_CLEAR_LAND_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLEAR_LAND",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_clear_water",
                                         F_CLEAR_WATER_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLEAR_WATER",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_land",
                                         F_LAND_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_LAND",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_water",
                                         F_WATER_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_WATER",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_bright",
                                         F_BRIGHT_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_BRIGHT",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_white",
                                         F_WHITE_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_WHITE",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_brightwhite",
                                         F_BRIGHTWHITE_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_BRIGHTWHITE",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_high",
                                         F_HIGH_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_HIGH",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("lc_veg_risk",
                                         F_VEG_RISK_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_VEG_RISK",
                                         getRandomColour(r), 0.5f);
        gaCloudProduct.getMaskGroup().add(index++, mask);

        return index;
    }

    public static double convertGeophysicalToMathematicalAngle(double inAngle) {
        if (0.0 <= inAngle && inAngle < 90.0) {
            return (90.0 - inAngle);
        } else if (90.0 <= inAngle && inAngle < 360.0) {
            return (90.0 - inAngle + 360.0);
        } else {
            // invalid
            return Double.NaN;
        }
    }

    public static boolean isNoReflectanceValid(float[] reflectance) {
        for (float aReflectance : reflectance) {
            if (!Float.isNaN(aReflectance) && aReflectance > 0.0f) {
                return false;
            }
        }
        return true;
    }

    public static int getDoyFromYYMMDD(String yymmdd) {
        Calendar cal = Calendar.getInstance();
        int doy = -1;
        try {
            final int year = Integer.parseInt(yymmdd.substring(0, 2));
            final int month = Integer.parseInt(yymmdd.substring(2, 4)) - 1;
            final int day = Integer.parseInt(yymmdd.substring(4, 6));
            cal.set(year, month, day);
            doy = cal.get(Calendar.DAY_OF_YEAR);
        } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
            e.printStackTrace();
        }
        return doy;
    }

    public static boolean isLeapYear(int year) {
        return ((year % 400) == 0) || (((year % 4) == 0) && ((year % 100) != 0));
    }

    private static Color getRandomColour(Random random) {
        int rColor = random.nextInt(256);
        int gColor = random.nextInt(256);
        int bColor = random.nextInt(256);
        return new Color(rColor, gColor, bColor);
    }

}
