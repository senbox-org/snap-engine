package org.esa.s3tbx.idepix.algorithms.modis;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.BitSetter;

import java.awt.*;
import java.util.Random;

/**
 * Utility class for Idepix MODIS
 *
 * @author olafd
 */
public class ModisUtils {


    /**
     * Provides MODIS pixel classification flag coding
     *
     * @param flagId - the flag ID
     *
     * @return - the flag coding
     */
    public static FlagCoding createOccciFlagCoding(String flagId) {

        FlagCoding flagCoding = new FlagCoding(flagId);

        flagCoding.addFlag("F_INVALID", BitSetter.setFlag(0, ModisConstants.F_INVALID),
                           ModisConstants.F_INVALID_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, ModisConstants.F_CLOUD),
                           ModisConstants.F_CLOUD_DESRC_TEXT);
        flagCoding.addFlag("F_CLOUD_AMBIGUOUS", BitSetter.setFlag(0, ModisConstants.F_CLOUD_AMBIGUOUS),
                           ModisConstants.F_CLOUD_AMBIGUOUS_DESRC_TEXT);
        flagCoding.addFlag("F_CLOUD_SURE", BitSetter.setFlag(0, ModisConstants.F_CLOUD_SURE),
                           ModisConstants.F_CLOUD_SURE_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_BUFFER", BitSetter.setFlag(0, ModisConstants.F_CLOUD_BUFFER),
                           ModisConstants.F_CLOUD_BUFFER_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SHADOW", BitSetter.setFlag(0, ModisConstants.F_CLOUD_SHADOW),
                           ModisConstants.F_CLOUD_SHADOW_DESCR_TEXT);
        flagCoding.addFlag("F_SNOW_ICE", BitSetter.setFlag(0, ModisConstants.F_SNOW_ICE),
                           ModisConstants.F_SNOW_ICE_DESCR_TEXT);
        flagCoding.addFlag("F_MIXED_PIXEL", BitSetter.setFlag(0, ModisConstants.F_MIXED_PIXEL),
                           ModisConstants.F_MIXED_PIXEL_DESCR_TEXT);
        flagCoding.addFlag("F_GLINT_RISK", BitSetter.setFlag(0, ModisConstants.F_GLINT_RISK),
                           ModisConstants.F_GLINT_RISK_DESCR_TEXT);
        flagCoding.addFlag("F_COASTLINE", BitSetter.setFlag(0, ModisConstants.F_COASTLINE),
                           ModisConstants.F_COASTLINE_DESCR_TEXT);
        flagCoding.addFlag("F_LAND", BitSetter.setFlag(0, ModisConstants.F_LAND),
                           ModisConstants.F_LAND_DESCR_TEXT);
        flagCoding.addFlag("F_BRIGHT", BitSetter.setFlag(0, ModisConstants.F_BRIGHT),
                           ModisConstants.F_BRIGHT_DESCR_TEXT);

        return flagCoding;
    }


    /**
     * Provides MODIS pixel classification flag bitmask
     *
     * @param classifProduct - the pixel classification product
     */
    public static void setupClassifBitmask(Product classifProduct) {

        int index = 0;
        int w = classifProduct.getSceneRasterWidth();
        int h = classifProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random();

        mask = Mask.BandMathsType.create("F_INVALID", "Invalid pixel", w, h,
                                         "pixel_classif_flags.F_INVALID",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD", "Cloudy pixel (sure or ambiguous)", w, h,
                                         "pixel_classif_flags.F_CLOUD",
                                         Color.yellow, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_AMBIGUOUS", "Cloudy pixel (ambiguous)", w, h,
                                         "pixel_classif_flags.F_CLOUD_AMBIGUOUS",
                                         Color.blue, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_SURE", "Cloudy pixel (sure)", w, h,
                                         "pixel_classif_flags.F_CLOUD_SURE",
                                         Color.red, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_BUFFER", "Cloud buffer pixel", w, h,
                                         "pixel_classif_flags.F_CLOUD_BUFFER",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

//        mask = Mask.BandMathsType.create("F_CLOUD_SHADOW", "Cloud shadow pixel", w, h,
//                                         "pixel_classif_flags.F_CLOUD_SHADOW",
//                                         getRandomColour(r), 0.5f);
//        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_SNOW_ICE", "Snow/ice pixel", w, h,
                                         "pixel_classif_flags.F_SNOW_ICE",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

//        mask = Mask.BandMathsType.create("F_MIXED_PIXEL", "Mixed pixel", w, h,
//                                         "pixel_classif_flags.F_MIXED_PIXEL",
//                                         getRandomColour(r), 0.5f);
//        classifProduct.getMaskGroup().add(index++, mask);

//        mask = Mask.BandMathsType.create("F_GLINT_RISK", "Glint risk pixel", w, h,
//                                         "pixel_classif_flags.F_GLINT_RISK",
//                                         getRandomColour(r), 0.5f);
//        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_COASTLINE", "Coastline pixel", w, h,
                                         "pixel_classif_flags.F_COASTLINE",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_LAND", "Land pixel", w, h,
                                         "pixel_classif_flags.F_LAND",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_BRIGHT", "Bright pixel", w, h,
                                         "pixel_classif_flags.F_BRIGHT",
                                         getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
    }

    private static Color getRandomColour(Random random) {
        int rColor = random.nextInt(256);
        int gColor = random.nextInt(256);
        int bColor = random.nextInt(256);
        return new Color(rColor, gColor, bColor);
    }
}
