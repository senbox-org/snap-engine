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

        flagCoding.addFlag("IDEPIX_INVALID", BitSetter.setFlag(0, IdepixConstants.IDEPIX_INVALID),
                           IdepixConstants.IDEPIX_INVALID_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_CLOUD", BitSetter.setFlag(0, IdepixConstants.IDEPIX_CLOUD),
                           IdepixConstants.IDEPIX_CLOUD_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_CLOUD_AMBIGUOUS", BitSetter.setFlag(0, IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS),
                           IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_CLOUD_SURE", BitSetter.setFlag(0, IdepixConstants.IDEPIX_CLOUD_SURE),
                           IdepixConstants.IDEPIX_CLOUD_SURE_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_CLOUD_BUFFER", BitSetter.setFlag(0, IdepixConstants.IDEPIX_CLOUD_BUFFER),
                           IdepixConstants.IDEPIX_CLOUD_BUFFER_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_CLOUD_SHADOW", BitSetter.setFlag(0, IdepixConstants.IDEPIX_CLOUD_SHADOW),
                           IdepixConstants.IDEPIX_CLOUD_SHADOW_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_SNOW_ICE", BitSetter.setFlag(0, IdepixConstants.IDEPIX_SNOW_ICE),
                           IdepixConstants.IDEPIX_SNOW_ICE_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_BRIGHT", BitSetter.setFlag(0, IdepixConstants.IDEPIX_BRIGHT),
                           IdepixConstants.IDEPIX_BRIGHT_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_WHITE", BitSetter.setFlag(0, IdepixConstants.IDEPIX_WHITE),
                           IdepixConstants.IDEPIX_WHITE_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_COASTLINE", BitSetter.setFlag(0, IdepixConstants.IDEPIX_COASTLINE),
                           IdepixConstants.IDEPIX_COASTLINE_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_LAND", BitSetter.setFlag(0, IdepixConstants.IDEPIX_LAND),
                           IdepixConstants.IDEPIX_LAND_DESCR_TEXT);

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

        mask = Mask.BandMathsType.create("IDEPIX_INVALID", IdepixConstants.IDEPIX_INVALID_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_INVALID",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("IDEPIX_CLOUD", IdepixConstants.IDEPIX_CLOUD_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD",
                                         Color.yellow, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_AMBIGUOUS", IdepixConstants.IDEPIX_CLOUD_AMBIGUOUS_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_AMBIGUOUS",
                                         Color.blue, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_SURE", IdepixConstants.IDEPIX_CLOUD_SURE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_SURE",
                                         Color.red, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_BUFFER", IdepixConstants.IDEPIX_CLOUD_BUFFER_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_BUFFER",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_SHADOW", IdepixConstants.IDEPIX_CLOUD_SHADOW_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_SHADOW",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("IDEPIX_SNOW_ICE", IdepixConstants.IDEPIX_SNOW_ICE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_SNOW_ICE",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("IDEPIX_BRIGHT", IdepixConstants.IDEPIX_BRIGHT_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_BRIGHT",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("IDEPIX_WHITE", IdepixConstants.IDEPIX_WHITE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_WHITE",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("IDEPIX_COASTLINE", IdepixConstants.IDEPIX_COASTLINE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_COASTLINE",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("IDEPIX_LAND", IdepixConstants.IDEPIX_LAND_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_LAND",
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
