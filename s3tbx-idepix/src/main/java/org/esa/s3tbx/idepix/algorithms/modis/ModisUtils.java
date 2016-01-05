package org.esa.s3tbx.idepix.algorithms.modis;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.BitSetter;

import java.awt.*;
import java.util.Random;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 27.06.2014
 * Time: 13:46
 *
 * @author olafd
 */
public class ModisUtils {

    public static FlagCoding createOccciFlagCoding(String flagIdentifier) {

        FlagCoding flagCoding = new FlagCoding(flagIdentifier);

        flagCoding.addFlag("F_INVALID", BitSetter.setFlag(0, ModisConstants.F_INVALID), null);
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, ModisConstants.F_CLOUD), null);
        flagCoding.addFlag("F_CLOUD_AMBIGUOUS", BitSetter.setFlag(0, ModisConstants.F_CLOUD_AMBIGUOUS), null);
        flagCoding.addFlag("F_CLOUD_SURE", BitSetter.setFlag(0, ModisConstants.F_CLOUD_SURE), null);
        flagCoding.addFlag("F_CLOUD_BUFFER", BitSetter.setFlag(0, ModisConstants.F_CLOUD_BUFFER), null);
        flagCoding.addFlag("F_CLOUD_SHADOW", BitSetter.setFlag(0, ModisConstants.F_CLOUD_SHADOW), null);
        flagCoding.addFlag("F_SNOW_ICE", BitSetter.setFlag(0, ModisConstants.F_SNOW_ICE), null);
        flagCoding.addFlag("F_MIXED_PIXEL", BitSetter.setFlag(0, ModisConstants.F_MIXED_PIXEL), null);
        flagCoding.addFlag("F_GLINT_RISK", BitSetter.setFlag(0, ModisConstants.F_GLINT_RISK), null);
        flagCoding.addFlag("F_COASTLINE", BitSetter.setFlag(0, ModisConstants.F_COASTLINE), null);
        flagCoding.addFlag("F_LAND", BitSetter.setFlag(0, ModisConstants.F_LAND), null);
        flagCoding.addFlag("F_BRIGHT", BitSetter.setFlag(0, ModisConstants.F_BRIGHT), null);

        return flagCoding;
    }


    public static int setupClassifBitmask(Product occciProduct) {

        int index = 0;
        int w = occciProduct.getSceneRasterWidth();
        int h = occciProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random();

        mask = Mask.BandMathsType.create("F_INVALID", "Invalid pixel", w, h,
                                         "pixel_classif_flags.F_INVALID",
                                         getRandomColour(r), 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD", "Cloudy pixel (sure or ambiguous)", w, h,
                                         "pixel_classif_flags.F_CLOUD",
                                         Color.yellow, 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_AMBIGUOUS", "Cloudy pixel (ambiguous)", w, h,
                                         "pixel_classif_flags.F_CLOUD_AMBIGUOUS",
                                         Color.blue, 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_SURE", "Cloudy pixel (sure)", w, h,
                                         "pixel_classif_flags.F_CLOUD_SURE",
                                         Color.red, 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_BUFFER", "Cloud buffer pixel", w, h,
                                         "pixel_classif_flags.F_CLOUD_BUFFER",
                                         getRandomColour(r), 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_SHADOW", "Cloud shadow pixel", w, h,
                                         "pixel_classif_flags.F_CLOUD_SHADOW",
                                         getRandomColour(r), 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_SNOW_ICE", "Snow/ice pixel", w, h,
                                         "pixel_classif_flags.F_SNOW_ICE",
                                         getRandomColour(r), 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_MIXED_PIXEL", "Mixed pixel", w, h,
                                         "pixel_classif_flags.F_MIXED_PIXEL",
                                         getRandomColour(r), 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_GLINT_RISK", "Glint risk pixel", w, h,
                                         "pixel_classif_flags.F_GLINT_RISK",
                                         getRandomColour(r), 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_COASTLINE", "Coastline pixel", w, h,
                                         "pixel_classif_flags.F_COASTLINE",
                                         getRandomColour(r), 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_LAND", "Land pixel", w, h,
                                         "pixel_classif_flags.F_LAND",
                                         getRandomColour(r), 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_BRIGHT", "Bright pixel", w, h,
                                         "pixel_classif_flags.F_BRIGHT",
                                         getRandomColour(r), 0.5f);
        occciProduct.getMaskGroup().add(index++, mask);

        return index;
    }

    private static Color getRandomColour(Random random) {
        int rColor = random.nextInt(256);
        int gColor = random.nextInt(256);
        int bColor = random.nextInt(256);
        return new Color(rColor, gColor, bColor);
    }
}
