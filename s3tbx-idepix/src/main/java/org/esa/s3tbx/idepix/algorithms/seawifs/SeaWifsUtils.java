package org.esa.s3tbx.idepix.algorithms.seawifs;

import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.BitSetter;

import java.awt.*;
import java.util.Random;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 18.10.2016
 * Time: 14:43
 *
 * @author olafd
 */
public class SeaWifsUtils {

    /**
     * Provides MODIS pixel classification flag coding
     *
     * @param flagId - the flag ID
     *
     * @return - the flag coding
     */
    public static FlagCoding createSeawifsFlagCoding(String flagId) {

        FlagCoding flagCoding = new FlagCoding(flagId);

        flagCoding.addFlag("F_INVALID", BitSetter.setFlag(0, SeaWifsConstants.F_INVALID),
                           SeaWifsConstants.F_INVALID_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, SeaWifsConstants.F_CLOUD),
                           SeaWifsConstants.F_CLOUD_DESRC_TEXT);
        flagCoding.addFlag("F_CLOUD_AMBIGUOUS", BitSetter.setFlag(0, SeaWifsConstants.F_CLOUD_AMBIGUOUS),
                           SeaWifsConstants.F_CLOUD_AMBIGUOUS_DESRC_TEXT);
        flagCoding.addFlag("F_CLOUD_SURE", BitSetter.setFlag(0, SeaWifsConstants.F_CLOUD_SURE),
                           SeaWifsConstants.F_CLOUD_SURE_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_BUFFER", BitSetter.setFlag(0, SeaWifsConstants.F_CLOUD_BUFFER),
                           SeaWifsConstants.F_CLOUD_BUFFER_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SHADOW", BitSetter.setFlag(0, SeaWifsConstants.F_CLOUD_SHADOW),
                           SeaWifsConstants.F_CLOUD_SHADOW_DESCR_TEXT);
        flagCoding.addFlag("F_SNOW_ICE", BitSetter.setFlag(0, SeaWifsConstants.F_SNOW_ICE),
                           SeaWifsConstants.F_SNOW_ICE_DESCR_TEXT);
        flagCoding.addFlag("F_MIXED_PIXEL", BitSetter.setFlag(0, SeaWifsConstants.F_MIXED_PIXEL),
                           SeaWifsConstants.F_MIXED_PIXEL_DESCR_TEXT);
        flagCoding.addFlag("F_GLINT_RISK", BitSetter.setFlag(0, SeaWifsConstants.F_GLINT_RISK),
                           SeaWifsConstants.F_GLINT_RISK_DESCR_TEXT);
        flagCoding.addFlag("F_COASTLINE", BitSetter.setFlag(0, SeaWifsConstants.F_COASTLINE),
                           SeaWifsConstants.F_COASTLINE_DESCR_TEXT);
        flagCoding.addFlag("F_LAND", BitSetter.setFlag(0, SeaWifsConstants.F_LAND),
                           SeaWifsConstants.F_LAND_DESCR_TEXT);
        flagCoding.addFlag("F_BRIGHT", BitSetter.setFlag(0, SeaWifsConstants.F_BRIGHT),
                           SeaWifsConstants.F_BRIGHT_DESCR_TEXT);

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
                                         IdepixUtils.getRandomColour(r), 0.5f);
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
                                         IdepixUtils.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_SNOW_ICE", "Snow/ice pixel", w, h,
                                         "pixel_classif_flags.F_SNOW_ICE",
                                         IdepixUtils.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_COASTLINE", "Coastline pixel", w, h,
                                         "pixel_classif_flags.F_COASTLINE",
                                         IdepixUtils.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_LAND", "Land pixel", w, h,
                                         "pixel_classif_flags.F_LAND",
                                         IdepixUtils.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_BRIGHT", "Bright pixel", w, h,
                                         "pixel_classif_flags.F_BRIGHT",
                                         IdepixUtils.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index, mask);
    }

}
