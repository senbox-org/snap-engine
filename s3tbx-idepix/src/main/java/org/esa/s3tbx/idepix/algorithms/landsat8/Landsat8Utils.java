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
    private Landsat8Utils() {
    }
    // if possible, put here everything which is common for both land and water parts

    public static int setupLandsat8Bitmasks(Product cloudProduct) {

        int index = 0;
        int w = cloudProduct.getSceneRasterWidth();
        int h = cloudProduct.getSceneRasterHeight();
        Mask mask;

        mask = Mask.BandMathsType.create("landsat8_invalid",
                                         Landsat8Constants.F_INVALID_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_INVALID",
                                         Color.red.darker(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);

        // SHIMEZ
        mask = Mask.BandMathsType.create("landsat8_cloud_shimez",
                                         Landsat8Constants.F_CLOUD_DESCR_TEXT + "[SHIMEZ]", w, h,
                                         "cloud_classif_flags.F_CLOUD_SHIMEZ",
                                         Color.magenta, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("landsat8_cloud_buffer_shimez",
                                         Landsat8Constants.F_CLOUD_BUFFER_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLOUD_SHIMEZ_BUFFER",
                                         Color.orange, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);

        // HOT
        mask = Mask.BandMathsType.create("landsat8_cloud_hot",
                                         Landsat8Constants.F_CLOUD_DESCR_TEXT + "[HOT]", w, h,
                                         "cloud_classif_flags.F_CLOUD_HOT",
                                         Color.magenta, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("landsat8_cloud_buffer_hot",
                                         Landsat8Constants.F_CLOUD_BUFFER_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLOUD_HOT_BUFFER",
                                         Color.orange, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);

        // OTSU
        mask = Mask.BandMathsType.create("landsat8_cloud_otsu",
                                         Landsat8Constants.F_CLOUD_DESCR_TEXT + "[OTSU]", w, h,
                                         "cloud_classif_flags.F_CLOUD_OTSU",
                                         Color.magenta, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("landsat8_cloud_buffer_otsu",
                                         Landsat8Constants.F_CLOUD_BUFFER_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLOUD_OTSU_BUFFER",
                                         Color.orange, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);

        // CLOST
        mask = Mask.BandMathsType.create("landsat8_cloud_clost",
                                         Landsat8Constants.F_CLOUD_DESCR_TEXT + "[CLOST]", w, h,
                                         "cloud_classif_flags.F_CLOUD_CLOST",
                                         Color.magenta, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("landsat8_cloud_buffer_clost",
                                         Landsat8Constants.F_CLOUD_BUFFER_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLOUD_CLOST_BUFFER",
                                         Color.orange, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("landsat8_cloud_ambiguous",
                                         Landsat8Constants.F_CLOUD_AMBIGUOUS_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLOUD_AMBIGUOUS",
                                         Color.yellow, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("landsat8_cloud_sure",
                                         Landsat8Constants.F_CLOUD_SURE_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_CLOUD_SURE",
                                         Color.red, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
//        mask = Mask.BandMathsType.create("landsat8_cloud_shadow",
//                Landsat8Constants.F_CLOUD_SHADOW_DESCR_TEXT, w, h,
//                "cloud_classif_flags.F_CLOUD_SHADOW",
//                Color.red.darker(), 0.5f);
//        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("landsat8_bright_experimental",
                                         Landsat8Constants.F_BRIGHT_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_BRIGHT",
                                         Color.yellow.darker(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("landsat8_white_experimental",
                                         Landsat8Constants.F_WHITE_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_WHITE",
                                         Color.red.brighter(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("landsat8_snow_ice",
                                         Landsat8Constants.F_SNOW_ICE_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_SNOW_ICE",
                                         Color.cyan, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
//        mask = Mask.BandMathsType.create("landsat8_glint_risk",
//                Landsat8Constants.F_GLINTRISK_DESCR_TEXT, w, h,
//                "cloud_classif_flags.F_GLINTRISK",
//                Color.pink, 0.5f);
//        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("landsat8_coastline",
                                         Landsat8Constants.F_COASTLINE_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_COASTLINE",
                                         Color.green.darker(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("landsat8_land",
                                         Landsat8Constants.F_LAND_DESCR_TEXT, w, h,
                                         "cloud_classif_flags.F_LAND",
                                         Color.green.brighter(), 0.5f);
        cloudProduct.getMaskGroup().add(index, mask);

        return index;
    }

    public static FlagCoding createLandsat8FlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("F_INVALID", BitSetter.setFlag(0, Landsat8Constants.F_INVALID), Landsat8Constants.F_INVALID_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SHIMEZ", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_SHIMEZ),
                           Landsat8Constants.F_CLOUD_DESCR_TEXT + "[SHIMEZ]");
        flagCoding.addFlag("F_CLOUD_SHIMEZ_BUFFER", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_SHIMEZ_BUFFER), Landsat8Constants.F_CLOUD_BUFFER_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_HOT", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_HOT),
                           Landsat8Constants.F_CLOUD_DESCR_TEXT + "[HOT]");
        flagCoding.addFlag("F_CLOUD_HOT_BUFFER", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_HOT_BUFFER), Landsat8Constants.F_CLOUD_BUFFER_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_OTSU", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_OTSU),
                           Landsat8Constants.F_CLOUD_DESCR_TEXT + "[OTSU]");
        flagCoding.addFlag("F_CLOUD_OTSU_BUFFER", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_OTSU_BUFFER), Landsat8Constants.F_CLOUD_BUFFER_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_CLOST", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_CLOST),
                           Landsat8Constants.F_CLOUD_DESCR_TEXT + "[CLOST]");
        flagCoding.addFlag("F_CLOUD_CLOST_BUFFER", BitSetter.setFlag(0, Landsat8Constants.F_CLOUD_CLOST_BUFFER), Landsat8Constants.F_CLOUD_BUFFER_DESCR_TEXT);
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

//        final double mean = stx.getMean();
//        final double stdev = stx.getStandardDeviation();
        // assume Gaussian shape of histogram: H := H(x, mean, stdev) = percent*H(x=mean)
        // --> solve quadratic equation for x, return the solution which is larger than mean
//        return mean + Math.sqrt(2.0*stdev*stdev*Math.log(100.0/percent));
    }

    public static int getWavelengthFromString(String wvlString) {
        if (wvlString.toUpperCase().contains("PANCHROMATIC")) {
            return 590;
        } else {
            return Integer.parseInt(wvlString);
        }
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
