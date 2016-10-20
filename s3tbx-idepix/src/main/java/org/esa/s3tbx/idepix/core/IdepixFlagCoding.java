package org.esa.s3tbx.idepix.core;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.BitSetter;

import java.awt.*;
import java.util.Random;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 20.10.2016
 * Time: 14:00
 *
 * @author olafd
 */
public class IdepixFlagCoding {

    /**
     * Provides Idepix default flag coding
     *
     * @param flagId - the flag ID
     *
     * @return - the flag coding
     */
    public static FlagCoding createDefaultFlagCoding(String flagId) {

        FlagCoding flagCoding = new FlagCoding(flagId);

        flagCoding.addFlag("F_INVALID", BitSetter.setFlag(0, IdepixConstants.F_INVALID),
                           IdepixConstants.F_INVALID_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, IdepixConstants.F_CLOUD),
                           IdepixConstants.F_CLOUD_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_AMBIGUOUS", BitSetter.setFlag(0, IdepixConstants.F_CLOUD_AMBIGUOUS),
                           IdepixConstants.F_CLOUD_AMBIGUOUS_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SURE", BitSetter.setFlag(0, IdepixConstants.F_CLOUD_SURE),
                           IdepixConstants.F_CLOUD_SURE_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_BUFFER", BitSetter.setFlag(0, IdepixConstants.F_CLOUD_BUFFER),
                           IdepixConstants.F_CLOUD_BUFFER_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SHADOW", BitSetter.setFlag(0, IdepixConstants.F_CLOUD_SHADOW),
                           IdepixConstants.F_CLOUD_SHADOW_DESCR_TEXT);
        flagCoding.addFlag("F_SNOW_ICE", BitSetter.setFlag(0, IdepixConstants.F_SNOW_ICE),
                           IdepixConstants.F_SNOW_ICE_DESCR_TEXT);
        flagCoding.addFlag("F_CLEAR_LAND", BitSetter.setFlag(0, IdepixConstants.F_CLEAR_LAND),
                           IdepixConstants.F_CLEAR_LAND_DESCR_TEXT);
        flagCoding.addFlag("F_CLEAR_WATER", BitSetter.setFlag(0, IdepixConstants.F_CLEAR_WATER),
                           IdepixConstants.F_CLEAR_WATER_DESCR_TEXT);
        flagCoding.addFlag("F_LAND", BitSetter.setFlag(0, IdepixConstants.F_LAND),
                           IdepixConstants.F_LAND_DESCR_TEXT);
        flagCoding.addFlag("F_WATER", BitSetter.setFlag(0, IdepixConstants.F_WATER),
                           IdepixConstants.F_WATER_DESCR_TEXT);
        flagCoding.addFlag("F_COASTLINE", BitSetter.setFlag(0, IdepixConstants.F_COASTLINE),
                           IdepixConstants.F_COASTLINE_DESCR_TEXT);
        flagCoding.addFlag("F_MIXED_PIXEL", BitSetter.setFlag(0, IdepixConstants.F_MIXED_PIXEL),
                           IdepixConstants.F_MIXED_PIXEL_DESCR_TEXT);
        flagCoding.addFlag("F_BRIGHT", BitSetter.setFlag(0, IdepixConstants.F_BRIGHT),
                           IdepixConstants.F_BRIGHT_DESCR_TEXT);
        flagCoding.addFlag("F_WHITE", BitSetter.setFlag(0, IdepixConstants.F_WHITE),
                           IdepixConstants.F_WHITE_DESCR_TEXT);
        flagCoding.addFlag("F_GLINT_RISK", BitSetter.setFlag(0, IdepixConstants.F_GLINT_RISK),
                           IdepixConstants.F_GLINT_RISK_DESCR_TEXT);

        return flagCoding;
    }


    /**
     * Provides MODIS pixel classification flag bitmask
     *
     * @param classifProduct - the pixel classification product
     *
     * @return the number of bitmasks set
     */
    public static int setupDefaultClassifBitmask(Product classifProduct) {

        int index = 0;
        int w = classifProduct.getSceneRasterWidth();
        int h = classifProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random();

        mask = Mask.BandMathsType.create("F_INVALID", IdepixConstants.F_INVALID_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_INVALID",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD", IdepixConstants.F_CLOUD_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD",
                                         Color.yellow, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_AMBIGUOUS", IdepixConstants.F_CLOUD_AMBIGUOUS_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_AMBIGUOUS",
                                         Color.blue, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_SURE", IdepixConstants.F_CLOUD_SURE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_SURE",
                                         Color.red, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_BUFFER", IdepixConstants.F_CLOUD_BUFFER_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_BUFFER",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_SHADOW", IdepixConstants.F_CLOUD_SHADOW_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_SHADOW",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_SNOW_ICE", IdepixConstants.F_SNOW_ICE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_SNOW_ICE",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLEAR_LAND", IdepixConstants.F_CLEAR_LAND_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLEAR_LAND",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLEAR_WATER", IdepixConstants.F_CLEAR_WATER_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLEAR_WATER",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_LAND", IdepixConstants.F_LAND_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_LAND",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_WATER", IdepixConstants.F_WATER_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_WATER",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_COASTLINE", IdepixConstants.F_COASTLINE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_COASTLINE",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_MIXED_PIXEL", IdepixConstants.F_MIXED_PIXEL_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_MIXED_PIXEL",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_BRIGHT", IdepixConstants.F_BRIGHT_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_BRIGHT",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_WHITE", IdepixConstants.F_WHITE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_WHITE",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_GLINT_RISK", IdepixConstants.F_GLINT_RISK_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_GLINT_RISK",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        return index;
    }

    public static Color getRandomColour(Random random) {
        int rColor = random.nextInt(256);
        int gColor = random.nextInt(256);
        int bColor = random.nextInt(256);
        return new Color(rColor, gColor, bColor);
    }

}
