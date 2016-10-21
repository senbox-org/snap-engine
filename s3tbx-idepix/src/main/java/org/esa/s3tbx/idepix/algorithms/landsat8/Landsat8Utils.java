package org.esa.s3tbx.idepix.algorithms.landsat8;

import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.IdepixFlagCoding;
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
        FlagCoding flagCoding = IdepixFlagCoding.createDefaultFlagCoding(flagId);

        // additional flags for Landsat-8
        flagCoding.addFlag("IDEPIX_CLOUD_SHIMEZ", BitSetter.setFlag(0, Landsat8Constants.IDEPIX_CLOUD_SHIMEZ),
                           IdepixConstants.IDEPIX_CLOUD_DESCR_TEXT + "[SHIMEZ]");
        flagCoding.addFlag("IDEPIX_CLOUD_SHIMEZ_BUFFER", BitSetter.setFlag(0, Landsat8Constants.IDEPIX_CLOUD_SHIMEZ_BUFFER),
                           Landsat8Constants.IDEPIX_CLOUD_BUFFER_SHIMEZ_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_CLOUD_HOT", BitSetter.setFlag(0, Landsat8Constants.IDEPIX_CLOUD_HOT),
                           IdepixConstants.IDEPIX_CLOUD_DESCR_TEXT + "[HOT]");
        flagCoding.addFlag("IDEPIX_CLOUD_HOT_BUFFER", BitSetter.setFlag(0, Landsat8Constants.IDEPIX_CLOUD_HOT_BUFFER),
                           Landsat8Constants.IDEPIX_CLOUD_BUFFER_HOT_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_CLOUD_OTSU", BitSetter.setFlag(0, Landsat8Constants.IDEPIX_CLOUD_OTSU),
                           IdepixConstants.IDEPIX_CLOUD_DESCR_TEXT + "[OTSU]");
        flagCoding.addFlag("IDEPIX_CLOUD_OTSU_BUFFER", BitSetter.setFlag(0, Landsat8Constants.IDEPIX_CLOUD_OTSU_BUFFER),
                           Landsat8Constants.IDEPIX_CLOUD_BUFFER_OTSU_DESCR_TEXT);
        flagCoding.addFlag("IDEPIX_CLOUD_CLOST", BitSetter.setFlag(0, Landsat8Constants.IDEPIX_CLOUD_CLOST),
                           IdepixConstants.IDEPIX_CLOUD_DESCR_TEXT + "[CLOST]");
        flagCoding.addFlag("IDEPIX_CLOUD_CLOST_BUFFER", BitSetter.setFlag(0, Landsat8Constants.IDEPIX_CLOUD_CLOST_BUFFER),
                           Landsat8Constants.IDEPIX_CLOUD_BUFFER_CLOST_DESCR_TEXT);

        return flagCoding;
    }

    /**
     * Provides Landsat-8 pixel classification flag bitmask
     *
     * @param classifProduct - the pixel classification product
     */
    public static void setupLandsat8ClassifBitmask(Product classifProduct) {

        int index = IdepixFlagCoding.setupDefaultClassifBitmask(classifProduct);

        int w = classifProduct.getSceneRasterWidth();
        int h = classifProduct.getSceneRasterHeight();
        Mask mask;

        // SHIMEZ
        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_SHIMEZ",
                                         IdepixConstants.IDEPIX_CLOUD_DESCR_TEXT + "[SHIMEZ]", w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_SHIMEZ",
                                         Color.magenta, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_SHIMEZ_BUFFER",
                                         Landsat8Constants.IDEPIX_CLOUD_BUFFER_SHIMEZ_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_SHIMEZ_BUFFER",
                                         Color.orange, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        // HOT
        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_HOT",
                                         IdepixConstants.IDEPIX_CLOUD_DESCR_TEXT + "[HOT]", w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_HOT",
                                         Color.magenta, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_HOT_BUFFER",
                                         Landsat8Constants.IDEPIX_CLOUD_BUFFER_HOT_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_HOT_BUFFER",
                                         Color.orange, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        // OTSU
        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_OTSU",
                                         IdepixConstants.IDEPIX_CLOUD_DESCR_TEXT + "[OTSU]", w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_OTSU",
                                         Color.magenta, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_OTSU_BUFFER",
                                         Landsat8Constants.IDEPIX_CLOUD_BUFFER_OTSU_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_OTSU_BUFFER",
                                         Color.orange, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        // CLOST
        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_CLOST",
                                         IdepixConstants.IDEPIX_CLOUD_DESCR_TEXT + "[CLOST]", w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_CLOST",
                                         Color.magenta, 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("IDEPIX_CLOUD_CLOST_BUFFER",
                                         Landsat8Constants.IDEPIX_CLOUD_BUFFER_CLOST_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_CLOUD_CLOST_BUFFER",
                                         Color.orange, 0.5f);
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
