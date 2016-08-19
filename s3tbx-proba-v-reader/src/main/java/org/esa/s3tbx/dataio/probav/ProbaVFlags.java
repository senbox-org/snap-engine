package org.esa.s3tbx.dataio.probav;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.BitSetter;

import java.awt.*;

/**
 * This class extracts and interprets the relevant bit information stored in the Proba-V SM flag.
 *
 * @author olafd
 */
public class ProbaVFlags {

    /**
     * Adds quality masks to given Proba-V product
     *
     * @param probavProduct - the product
     * @param probavProductType - the product type (LEVEL2A or LEVEL3)
     */
    public static void addQualityMasks(Product probavProduct, String probavProductType) {
        addLevel3QualityMasks(probavProduct);

        if (probavProductType.equals("LEVEL2A")) {
            ProductNodeGroup<Mask> maskGroup = probavProduct.getMaskGroup();
            addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_SWIR_COVERAGE_FLAG_NAME,
                    ProbaVConstants.SM_SWIR_COVERAGE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[10], 0.5f);
            addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_NIR_COVERAGE_FLAG_NAME,
                    ProbaVConstants.SM_NIR_COVERAGE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[11], 0.5f);
            addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_RED_COVERAGE_FLAG_NAME,
                    ProbaVConstants.SM_RED_COVERAGE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[12], 0.5f);
            addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_BLUE_COVERAGE_FLAG_NAME,
                    ProbaVConstants.SM_BLUE_COVERAGE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[13], 0.5f);
        }
    }

    /**
     * Adds Proba-V quality flags according to product type to given flag coding
     *
     * @param probavSmFlagCoding - the flag coding
     * @param probavProductType - the product type
     */
    public static void addQualityFlags(FlagCoding probavSmFlagCoding, String probavProductType) {
        addLevel3QualityFlags(probavSmFlagCoding);

        if (probavProductType.equals("LEVEL2A")) {
            probavSmFlagCoding.addFlag(ProbaVConstants.SM_SWIR_COVERAGE_FLAG_NAME,
                                       BitSetter.setFlag(0, ProbaVConstants.SM_SWIR_COVERAGE_INDEX),
                                       ProbaVConstants.SM_SWIR_COVERAGE_FLAG_DESCR);
            probavSmFlagCoding.addFlag(ProbaVConstants.SM_NIR_COVERAGE_FLAG_NAME,
                                       BitSetter.setFlag(0, ProbaVConstants.SM_NIR_COVERAGE_BIT_INDEX),
                                       ProbaVConstants.SM_NIR_COVERAGE_FLAG_DESCR);
            probavSmFlagCoding.addFlag(ProbaVConstants.SM_RED_COVERAGE_FLAG_NAME,
                                       BitSetter.setFlag(0, ProbaVConstants.SM_RED_COVERAGE_BIT_INDEX),
                                       ProbaVConstants.SM_RED_COVERAGE_FLAG_DESCR);
            probavSmFlagCoding.addFlag(ProbaVConstants.SM_BLUE_COVERAGE_FLAG_NAME,
                                       BitSetter.setFlag(0, ProbaVConstants.SM_BLUE_COVERAGE_BIT_INDEX),
                                       ProbaVConstants.SM_BLUE_COVERAGE_FLAG_DESCR);
        }
    }

    /**
     * Sets the target data buffer of the Proba-V SM flag, i.e. converts from original flags which use
     * combinations of bits.
     *
     * @param targetBuffer - the target data buffer
     * @param tmpBuffer - the original quality data buffer as read from HDF file
     * @param probavProductType - the product type (LEVEL2A or LEVEL3)
     */
    static void setSmFlagBuffer(ProductData targetBuffer, ProductData tmpBuffer, String probavProductType) {
        for (int i = 0; i < targetBuffer.getNumElems(); i++) {
            final int qualityValue = tmpBuffer.getElemIntAt(i);
            int smFlagValue = 0;
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_CLEAR_BIT_INDEX, isClear(qualityValue));
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_UNDEFINED_BIT_INDEX, isUndefined(qualityValue));
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_CLOUD_BIT_INDEX, isCloud(qualityValue));
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_SNOWICE_INDEX, isSnowIce(qualityValue));
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_CLOUD_SHADOW_BIT_INDEX, isCloudShadow(qualityValue));
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_LAND_BIT_INDEX, isLand(qualityValue));

            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_GOOD_SWIR_INDEX, isGoodSwir(qualityValue));
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_GOOD_NIR_BIT_INDEX, isGoodNir(qualityValue));
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_GOOD_RED_BIT_INDEX, isGoodRed(qualityValue));
            smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_GOOD_BLUE_BIT_INDEX, isGoodBlue(qualityValue));

            if (probavProductType.equals("LEVEL2A")) {
                smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_BLUE_COVERAGE_BIT_INDEX, isBlueCoverage(qualityValue));
                smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_RED_COVERAGE_BIT_INDEX, isRedCoverage(qualityValue));
                smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_NIR_COVERAGE_BIT_INDEX, isNirCoverage(qualityValue));
                smFlagValue = BitSetter.setFlag(smFlagValue, ProbaVConstants.SM_SWIR_COVERAGE_INDEX, isSwirCoverage(qualityValue));
            }

            targetBuffer.setElemIntAt(i, smFlagValue);
        }
    }


    private static void addLevel3QualityFlags(FlagCoding probavSmFlagCoding) {
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_CLEAR_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_CLEAR_BIT_INDEX),
                                   ProbaVConstants.SM_CLEAR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_UNDEFINED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_UNDEFINED_BIT_INDEX),
                                   ProbaVConstants.SM_UNDEFINED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_CLOUD_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_CLOUD_BIT_INDEX),
                                   ProbaVConstants.SM_CLOUD_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_SNOWICE_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_SNOWICE_INDEX),
                                   ProbaVConstants.SM_SNOWICE_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_CLOUD_SHADOW_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_CLOUD_SHADOW_BIT_INDEX),
                                   ProbaVConstants.SM_CLOUD_SHADOW_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_LAND_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_LAND_BIT_INDEX),
                                   ProbaVConstants.SM_LAND_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_SWIR_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_SWIR_INDEX),
                                   ProbaVConstants.SM_GOOD_SWIR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_NIR_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_NIR_BIT_INDEX),
                                   ProbaVConstants.SM_GOOD_NIR_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_RED_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_RED_BIT_INDEX),
                                   ProbaVConstants.SM_GOOD_RED_FLAG_DESCR);
        probavSmFlagCoding.addFlag(ProbaVConstants.SM_GOOD_BLUE_FLAG_NAME,
                                   BitSetter.setFlag(0, ProbaVConstants.SM_GOOD_BLUE_BIT_INDEX),
                                   ProbaVConstants.SM_GOOD_BLUE_FLAG_DESCR);
    }

    private static void addLevel3QualityMasks(Product probavProduct) {
        ProductNodeGroup<Mask> maskGroup = probavProduct.getMaskGroup();
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_CLEAR_FLAG_NAME,
                ProbaVConstants.SM_CLEAR_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[0], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_UNDEFINED_FLAG_NAME,
                ProbaVConstants.SM_UNDEFINED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[1], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_CLOUD_FLAG_NAME,
                ProbaVConstants.SM_CLOUD_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[2], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_SNOWICE_FLAG_NAME,
                ProbaVConstants.SM_SNOWICE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[3], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_CLOUD_SHADOW_FLAG_NAME,
                ProbaVConstants.SM_CLOUD_SHADOW_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[4], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_LAND_FLAG_NAME,
                ProbaVConstants.SM_LAND_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[5], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_SWIR_FLAG_NAME,
                ProbaVConstants.SM_GOOD_SWIR_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[6], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_NIR_FLAG_NAME,
                ProbaVConstants.SM_GOOD_NIR_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[7], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_RED_FLAG_NAME,
                ProbaVConstants.SM_GOOD_RED_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[8], 0.5f);
        addMask(probavProduct, maskGroup, ProbaVConstants.SM_FLAG_BAND_NAME, ProbaVConstants.SM_GOOD_BLUE_FLAG_NAME,
                ProbaVConstants.SM_GOOD_BLUE_FLAG_DESCR, ProbaVConstants.FLAG_COLORS[9], 0.5f);
    }


    private static void addMask(Product product, ProductNodeGroup<Mask> maskGroup,
                                String bandName, String flagName, String description, Color color, float transparency) {
        int width = product.getSceneRasterWidth();
        int height = product.getSceneRasterHeight();
        String maskPrefix = "";
        Mask mask = Mask.BandMathsType.create(maskPrefix + flagName,
                                              description, width, height,
                                              bandName + "." + flagName,
                                              color, transparency);
        maskGroup.add(mask);
    }


    private static boolean isClear(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private static boolean isUndefined(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private static boolean isCloud(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 0) &&
                BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private static boolean isSnowIce(int srcValue) {
        return (!BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                BitSetter.isFlagSet(srcValue, 2));
    }

    private static boolean isCloudShadow(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 0) &&
                !BitSetter.isFlagSet(srcValue, 1) &&
                !BitSetter.isFlagSet(srcValue, 2));
    }

    private static boolean isLand(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 3);
    }

    private static boolean isGoodSwir(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 4);
    }

    private static boolean isGoodNir(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 5);
    }

    private static boolean isGoodRed(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 6);
    }

    private static boolean isGoodBlue(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 7);
    }

    private static boolean isSwirCoverage(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 8);
    }

    private static boolean isNirCoverage(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 9);
    }

    private static boolean isRedCoverage(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 10);
    }

    private static boolean isBlueCoverage(int srcValue) {
        return BitSetter.isFlagSet(srcValue, 11);
    }
}
