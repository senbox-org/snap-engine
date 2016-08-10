package org.esa.s3tbx.idepix.algorithms.meris;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.math.MathUtils;

import java.awt.*;

/**
 * Utility class for Idepix MERIS
 *
 * @author olafd
 */
public class MerisUtils {

    public static int setupMerisBitmasks(Product cloudProduct) {

        int index = 0;
        int w = cloudProduct.getSceneRasterWidth();
        int h = cloudProduct.getSceneRasterHeight();
        Mask mask;

        mask = Mask.BandMathsType.create("meris_invalid",
                                         MerisConstants.F_INVALID_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_INVALID",
                                         Color.red.darker(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("meris_cloud",
                                         MerisConstants.F_CLOUD_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD",
                                         Color.magenta, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("meris_cloud_ambiguous",
                                         MerisConstants.F_CLOUD_AMBIGUOUS_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_AMBIGUOUS",
                                         Color.yellow, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("meris_cloud_sure",
                                         MerisConstants.F_CLOUD_SURE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_SURE",
                                         Color.red, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("meris_cloud_buffer",
                                         MerisConstants.F_CLOUD_BUFFER_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_BUFFER",
                                         Color.orange, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("meris_cloud_shadow",
                                         MerisConstants.F_CLOUD_SHADOW_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_SHADOW",
                                         Color.red.darker(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("meris_snow_ice",
                                         MerisConstants.F_SNOW_ICE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_SNOW_ICE",
                                         Color.cyan, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("meris_glint_risk",
                                         MerisConstants.F_GLINTRISK_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_GLINTRISK",
                                         Color.pink, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("meris_coastline",
                                         MerisConstants.F_COASTLINE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_COASTLINE",
                                         Color.green.darker(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("meris_land",
                                         MerisConstants.F_LAND_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_LAND",
                                         Color.green.brighter(), 0.5f);
        cloudProduct.getMaskGroup().add(index, mask);

        return index;
    }

    public static FlagCoding createMerisFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("F_INVALID", BitSetter.setFlag(0, MerisConstants.F_INVALID), MerisConstants.F_INVALID_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, MerisConstants.F_CLOUD), MerisConstants.F_CLOUD_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_AMBIGUOUS", BitSetter.setFlag(0, MerisConstants.F_CLOUD_AMBIGUOUS), MerisConstants.F_CLOUD_AMBIGUOUS_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SURE", BitSetter.setFlag(0, MerisConstants.F_CLOUD_SURE), MerisConstants.F_CLOUD_SURE_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_BUFFER", BitSetter.setFlag(0, MerisConstants.F_CLOUD_BUFFER), MerisConstants.F_CLOUD_BUFFER_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SHADOW", BitSetter.setFlag(0, MerisConstants.F_CLOUD_SHADOW), MerisConstants.F_CLOUD_SHADOW_DESCR_TEXT);
        flagCoding.addFlag("F_SNOW_ICE", BitSetter.setFlag(0, MerisConstants.F_SNOW_ICE), MerisConstants.F_SNOW_ICE_DESCR_TEXT);
        flagCoding.addFlag("F_GLINTRISK", BitSetter.setFlag(0, MerisConstants.F_GLINTRISK), MerisConstants.F_GLINTRISK_DESCR_TEXT);
        flagCoding.addFlag("F_COASTLINE", BitSetter.setFlag(0, MerisConstants.F_COASTLINE), MerisConstants.F_COASTLINE_DESCR_TEXT);
        flagCoding.addFlag("F_LAND", BitSetter.setFlag(0, MerisConstants.F_LAND), MerisConstants.F_LAND_DESCR_TEXT);
        return flagCoding;
    }

    /**
     * Computes the azimuth difference from the given
     *
     * @param vaa viewing azimuth angle [degree]
     * @param saa sun azimuth angle [degree]
     * @return the azimuth difference [degree]
     */
    public static double computeAzimuthDifference(final double vaa, final double saa) {
        return MathUtils.RTOD * Math.acos(Math.cos(MathUtils.DTOR * (vaa - saa)));
    }
}
