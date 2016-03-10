package org.esa.s3tbx.idepix.algorithms.landsat8;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.Stx;
import org.esa.snap.core.util.BitSetter;

import javax.media.jai.Histogram;
import java.awt.*;

/**
 * Utility class for Idepix Landsat 8
 *
 * @author olafd
 */
public class Landsat8Utils {

    /**
     * Provides Landsat-8 pixel classification flag coding
     *
     * @param flagId - the flag ID
     *
     * @return - the flag coding
     */
    public static FlagCoding createLandsat8FlagCoding(String flagId) {
        FlagCoding flagCoding = new FlagCoding(flagId);
        flagCoding.addFlag("F_INVALID", BitSetter.setFlag(0, Landsat8Constants.F_INVALID), Landsat8Constants.F_INVALID_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SHIMEZ", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_SHIMEZ),
                           Landsat8Constants.F_CLOUD_DESCR_TEXT + "[SHIMEZ]");
        flagCoding.addFlag("F_CLOUD_SHIMEZ_BUFFER", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_SHIMEZ_BUFFER),
                           Landsat8Constants.F_CLOUD_BUFFER_SHIMEZ_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_HOT", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_HOT),
                           Landsat8Constants.F_CLOUD_DESCR_TEXT + "[HOT]");
        flagCoding.addFlag("F_CLOUD_HOT_BUFFER", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_HOT_BUFFER),
                           Landsat8Constants.F_CLOUD_BUFFER_HOT_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_OTSU", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_OTSU),
                           Landsat8Constants.F_CLOUD_DESCR_TEXT + "[OTSU]");
        flagCoding.addFlag("F_CLOUD_OTSU_BUFFER", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_OTSU_BUFFER),
                           Landsat8Constants.F_CLOUD_BUFFER_OTSU_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_CLOST", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_CLOST),
                           Landsat8Constants.F_CLOUD_DESCR_TEXT + "[CLOST]");
        flagCoding.addFlag("F_CLOUD_CLOST_BUFFER", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_CLOST_BUFFER),
                           Landsat8Constants.F_CLOUD_BUFFER_CLOST_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_AMBIGUOUS", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_AMBIGUOUS), Landsat8Constants.F_CLOUD_AMBIGUOUS_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SURE", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_SURE), Landsat8Constants.F_CLOUD_SURE_DESCR_TEXT);
//        flagCoding.addFlag("F_CLOUD_SHADOW", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_SHADOW), Landsat8Constants.F_CLOUD_SHADOW_DESCR_TEXT);
        flagCoding.addFlag("F_BRIGHT", BitSetter.setFlag(0, Landsat8Constants.F_BRIGHT), Landsat8Constants.F_BRIGHT_DESCR_TEXT);
        flagCoding.addFlag("F_WHITE", BitSetter.setFlag(0, Landsat8Constants.F_WHITE), Landsat8Constants.F_WHITE_DESCR_TEXT);
        flagCoding.addFlag("F_SNOW_ICE", BitSetter.setFlag(0, Landsat8Constants.F_SNOW_ICE), Landsat8Constants.F_SNOW_ICE_DESCR_TEXT);
//        flagCoding.addFlag("F_GLINTRISK", BitSetter.setFlag(0, Landsat8Constants.F_GLINTRISK), Landsat8Constants.F_GLINTRISK_DESCR_TEXT);
        flagCoding.addFlag("F_COASTLINE", BitSetter.setFlag(0, Landsat8Constants.F_COASTLINE), Landsat8Constants.F_COASTLINE_DESCR_TEXT);
        flagCoding.addFlag("F_LAND", BitSetter.setFlag(0, Landsat8Constants.F_LAND), Landsat8Constants.F_LAND_DESCR_TEXT);
        return flagCoding;
    }

    /**
     * Provides Landsat-8 pixel classification flag bitmask
     *
     * @param classifProduct - the pixel classification product
     */
    public static void setupLandsat8Bitmasks(Product classifProduct) {

        int index = 0;
        int w = classifProduct.getSceneRasterWidth();
        int h = classifProduct.getSceneRasterHeight();
        Mask mask;

        mask = Mask.BandMathsType.create("F_INVALID",
                                         Landsat8Constants.F_INVALID_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_INVALID",
                                         Color.red.darker(), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        // SHIMEZ
        mask = Mask.BandMathsType.create("F_CLOUD_SHIMEZ",
                                         Landsat8Constants.F_CLOUD_DESCR_TEXT + "[SHIMEZ]", w, h,
                                         "pixel_classif_flags.F_CLOUD_SHIMEZ",
                                         Color.magenta, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_CLOUD_SHIMEZ_BUFFER",
                                         Landsat8Constants.F_CLOUD_BUFFER_SHIMEZ_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_SHIMEZ_BUFFER",
                                         Color.orange, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        // HOT
        mask = Mask.BandMathsType.create("F_CLOUD_HOT",
                                         Landsat8Constants.F_CLOUD_DESCR_TEXT + "[HOT]", w, h,
                                         "pixel_classif_flags.F_CLOUD_HOT",
                                         Color.magenta, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_CLOUD_HOT_BUFFER",
                                         Landsat8Constants.F_CLOUD_BUFFER_HOT_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_HOT_BUFFER",
                                         Color.orange, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        // OTSU
        mask = Mask.BandMathsType.create("F_CLOUD_OTSU",
                                         Landsat8Constants.F_CLOUD_DESCR_TEXT + "[OTSU]", w, h,
                                         "pixel_classif_flags.F_CLOUD_OTSU",
                                         Color.magenta, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_CLOUD_OTSU_BUFFER",
                                         Landsat8Constants.F_CLOUD_BUFFER_OTSU_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_OTSU_BUFFER",
                                         Color.orange, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        // CLOST
        mask = Mask.BandMathsType.create("F_CLOUD_CLOST",
                                         Landsat8Constants.F_CLOUD_DESCR_TEXT + "[CLOST]", w, h,
                                         "pixel_classif_flags.F_CLOUD_CLOST",
                                         Color.magenta, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_CLOUD_CLOST_BUFFER",
                                         Landsat8Constants.F_CLOUD_BUFFER_CLOST_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_CLOST_BUFFER",
                                         Color.orange, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_AMBIGUOUS",
                                         Landsat8Constants.F_CLOUD_AMBIGUOUS_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_AMBIGUOUS",
                                         Color.yellow, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_CLOUD_SURE",
                                         Landsat8Constants.F_CLOUD_SURE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_SURE",
                                         Color.red, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
//        mask = Mask.BandMathsType.create("F_CLOUD_SHADOW",
//                Landsat8Constants.F_CLOUD_SHADOW_DESCR_TEXT, w, h,
//                "pixel_classif_flags.F_CLOUD_SHADOW",
//                Color.red.darker(), 0.5f);
//        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_BRIGHT",
                                         Landsat8Constants.F_BRIGHT_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_BRIGHT",
                                         Color.yellow.darker(), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_WHITE",
                                         Landsat8Constants.F_WHITE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_WHITE",
                                         Color.red.brighter(), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_SNOW_ICE",
                                         Landsat8Constants.F_SNOW_ICE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_SNOW_ICE",
                                         Color.cyan, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
//        mask = Mask.BandMathsType.create("F_GLINTRISK",
//                Landsat8Constants.F_GLINTRISK_DESCR_TEXT, w, h,
//                "pixel_classif_flags.F_GLINTRISK",
//                Color.pink, 0.5f);
//        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_COASTLINE",
                                         Landsat8Constants.F_COASTLINE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_COASTLINE",
                                         Color.green.darker(), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("F_LAND",
                                         Landsat8Constants.F_LAND_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_LAND",
                                         Color.green.brighter(), 0.5f);
        classifProduct.getMaskGroup().add(index, mask);
    }

    /**
     * Provides wavelength as int from string
     *
     * @param wvlString - the wavelength as string
     *
     * @return - the wavelength as int
     */
    public static int getWavelengthFromString(String wvlString) {
        if (wvlString.toUpperCase().contains("PANCHROMATIC")) {
            return 590;
        } else {
            return Integer.parseInt(wvlString);
        }
    }

    /**
     * Computes the histogram bin where histogram value is N percent of histogram maximum value
     *
     * @param stx - the statistics object
     * @param percent - the N percent value
     *
     * @return - the bin value
     */
    public static double getHistogramBinAtNPercentOfMaximum(Stx stx, double percent) {
        final Histogram h = stx.getHistogram();
        final double highValue = h.getHighValue()[0];
        final double lowValue = h.getLowValue()[0];
        final int numBins = h.getNumBins(0);
        final double binWidth = (highValue - lowValue) / numBins;
        final double peakValue = getHistogramPeakValue(h);

        for (int i = numBins - 1; i >= 0; i--) {
            final double currValue = highValue - (numBins - i) * binWidth;
            if (h.getBins()[0][i] >= percent * peakValue / 100.0) {
                return currValue;
            }
        }
        return peakValue;
    }

    private static double getHistogramPeakValue(Histogram h) {
        int peakValue = 0;
        for (int i = 0; i < h.getNumBins(0); i++) {
            if (h.getBins()[0][i] > peakValue) {
                peakValue = h.getBins()[0][i];
            }
        }
        return (double) peakValue;
    }
}
