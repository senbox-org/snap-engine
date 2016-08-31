package org.esa.s3tbx.idepix.algorithms.probav;

import org.esa.s3tbx.idepix.algorithms.vgt.VgtConstants;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.BitSetter;

import java.awt.*;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 31.08.2016
 * Time: 15:17
 *
 * @author olafd
 */
public class ProbaVUtils {

    public static int setupProbavBitmasks(Product cloudProduct) {

        int index = 0;
        int w = cloudProduct.getSceneRasterWidth();
        int h = cloudProduct.getSceneRasterHeight();
        Mask mask;

        mask = Mask.BandMathsType.create("vgt_invalid",
                                         ProbaVConstants.F_INVALID_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_INVALID",
                                         Color.red.darker(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("vgt_cloud",
                                         ProbaVConstants.F_CLOUD_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD",
                                         Color.magenta, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("vgt_cloud_ambiguous",
                                         ProbaVConstants.F_CLOUD_AMBIGUOUS_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_AMBIGUOUS",
                                         Color.yellow, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("vgt_cloud_sure",
                                         ProbaVConstants.F_CLOUD_SURE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_SURE",
                                         Color.red, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("vgt_cloud_buffer",
                                         ProbaVConstants.F_CLOUD_BUFFER_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_CLOUD_BUFFER",
                                         Color.orange, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("vgt_snow_ice",
                                         ProbaVConstants.F_SNOW_ICE_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_SNOW_ICE",
                                         Color.cyan, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("vgt_land",
                                         ProbaVConstants.F_LAND_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.F_LAND",
                                         Color.green.brighter(), 0.5f);
        cloudProduct.getMaskGroup().add(index, mask);

        return index;
    }


    public static FlagCoding createProbavFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("F_INVALID",
                           BitSetter.setFlag(0, IdepixConstants.F_INVALID), ProbaVConstants.F_INVALID_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD",
                           BitSetter.setFlag(0, IdepixConstants.F_CLOUD), ProbaVConstants.F_CLOUD_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_AMBIGUOUS",
                           BitSetter.setFlag(0, IdepixConstants.F_CLOUD_AMBIGUOUS), ProbaVConstants.F_CLOUD_AMBIGUOUS_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_SURE",
                           BitSetter.setFlag(0, IdepixConstants.F_CLOUD_SURE), ProbaVConstants.F_CLOUD_SURE_DESCR_TEXT);
        flagCoding.addFlag("F_CLOUD_BUFFER",
                           BitSetter.setFlag(0, IdepixConstants.F_CLOUD_BUFFER), ProbaVConstants.F_CLOUD_BUFFER_DESCR_TEXT);
        flagCoding.addFlag("F_SNOW_ICE",
                           BitSetter.setFlag(0, IdepixConstants.F_CLEAR_SNOW), ProbaVConstants.F_SNOW_ICE_DESCR_TEXT);
        flagCoding.addFlag("F_LAND",
                           BitSetter.setFlag(0, IdepixConstants.F_LAND), ProbaVConstants.F_LAND_DESCR_TEXT);
        return flagCoding;
    }

}
