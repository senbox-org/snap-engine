package org.esa.s3tbx.idepix.algorithms.olci;

import org.esa.s3tbx.processor.rad2refl.Rad2ReflConstants;
import org.esa.s3tbx.processor.rad2refl.Rad2ReflOp;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 12.08.2016
 * Time: 11:39
 *
 * @author olafd
 */
public class OlciUtils {

    public static int setupOlciBitmasks(Product cloudProduct) {

        int index = 0;
        int w = cloudProduct.getSceneRasterWidth();
        int h = cloudProduct.getSceneRasterHeight();
        Mask mask;

        mask = Mask.BandMathsType.create("olci_invalid",
                                         OlciConstants.F_INVALID_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_INVALID",
                                         Color.red.darker(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("olci_cloud",
                                         OlciConstants.F_CLOUD_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD",
                                         Color.magenta, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("olci_cloud_ambiguous",
                                         OlciConstants.F_CLOUD_AMBIGUOUS_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_AMBIGUOUS",
                                         Color.yellow, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("olci_cloud_sure",
                                         OlciConstants.F_CLOUD_SURE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_SURE",
                                         Color.red, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("olci_cloud_buffer",
                                         OlciConstants.F_CLOUD_BUFFER_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_BUFFER",
                                         Color.orange, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("olci_cloud_shadow",
                                         OlciConstants.F_CLOUD_SHADOW_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_SHADOW",
                                         Color.red.darker(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("olci_snow_ice",
                                         OlciConstants.F_SNOW_ICE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_SNOW_ICE",
                                         Color.cyan, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("olci_glint_risk",
                                         OlciConstants.F_GLINTRISK_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_GLINTRISK",
                                         Color.pink, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("olci_coastline",
                                         OlciConstants.F_COASTLINE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_COASTLINE",
                                         Color.green.darker(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("olci_land",
                                         OlciConstants.F_LAND_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_LAND",
                                         Color.green.brighter(), 0.5f);
        cloudProduct.getMaskGroup().add(index, mask);

        return index;
    }


    public static FlagCoding createOlciFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("F_INVALID", BitSetter.setFlag(0, OlciConstants.F_INVALID), OlciConstants.F_INVALID_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, OlciConstants.F_CLOUD), OlciConstants.F_CLOUD_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_AMBIGUOUS", BitSetter.setFlag(0, OlciConstants.F_CLOUD_AMBIGUOUS), OlciConstants.F_CLOUD_AMBIGUOUS_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SURE", BitSetter.setFlag(0, OlciConstants.F_CLOUD_SURE), OlciConstants.F_CLOUD_SURE_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_BUFFER", BitSetter.setFlag(0, OlciConstants.F_CLOUD_BUFFER), OlciConstants.F_CLOUD_BUFFER_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SHADOW", BitSetter.setFlag(0, OlciConstants.F_CLOUD_SHADOW), OlciConstants.F_CLOUD_SHADOW_DESCR_TEXT);
        flagCoding.addFlag("F_SNOW_ICE", BitSetter.setFlag(0, OlciConstants.F_SNOW_ICE), OlciConstants.F_SNOW_ICE_DESCR_TEXT);
        flagCoding.addFlag("F_GLINTRISK", BitSetter.setFlag(0, OlciConstants.F_GLINTRISK), OlciConstants.F_GLINTRISK_DESCR_TEXT);
        flagCoding.addFlag("F_COASTLINE", BitSetter.setFlag(0, OlciConstants.F_COASTLINE), OlciConstants.F_COASTLINE_DESCR_TEXT);
        flagCoding.addFlag("F_LAND", BitSetter.setFlag(0, OlciConstants.F_LAND), OlciConstants.F_LAND_DESCR_TEXT);
        return flagCoding;
    }

    public static void addRadiance2ReflectanceBands(Product rad2reflProduct, Product targetProduct) {
        addRadiance2ReflectanceBands(rad2reflProduct, targetProduct, 1, Rad2ReflConstants.OLCI_REFL_BAND_NAMES.length);
    }

    public static void addRadiance2ReflectanceBands(Product rad2reflProduct, Product targetProduct, int minBand, int maxBand) {
        for (int i = minBand; i <= maxBand; i++) {
            for (String bandname : rad2reflProduct.getBandNames()) {
                // e.g. Oa01_reflectance
                if (!targetProduct.containsBand(bandname) && bandname.equals("Oa" + String.format("%02d", i) + "_reflectance")) {
                    System.out.println("adding band: " + bandname);
                    ProductUtils.copyBand(bandname, rad2reflProduct, targetProduct, true);
                    targetProduct.getBand(bandname).setUnit("dl");
                }
            }
        }
    }

}
