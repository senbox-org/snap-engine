package org.esa.s3tbx.idepix.algorithms.avhrr;

import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.BitSetter;

import java.awt.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.TimeZone;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 28.11.2014
 * Time: 14:58
 *
 * @author olafd
 */
public class AvhrrAcUtils {

    public static FlagCoding createAvhrrAcFlagCoding(String flagIdentifier) {

        FlagCoding flagCoding = new FlagCoding(flagIdentifier);

        flagCoding.addFlag("F_INVALID", BitSetter.setFlag(0, AvhrrConstants.F_INVALID), null);
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, AvhrrConstants.F_CLOUD), null);
        flagCoding.addFlag("F_CLOUD_AMBIGUOUS", BitSetter.setFlag(0, AvhrrConstants.F_CLOUD_AMBIGUOUS), null);
        flagCoding.addFlag("F_CLOUD_SURE", BitSetter.setFlag(0, AvhrrConstants.F_CLOUD_SURE), null);
        flagCoding.addFlag("F_CLOUD_BUFFER", BitSetter.setFlag(0, AvhrrConstants.F_CLOUD_BUFFER), null);
        flagCoding.addFlag("F_CLOUD_SHADOW", BitSetter.setFlag(0, AvhrrConstants.F_CLOUD_SHADOW), null);
        flagCoding.addFlag("F_SNOW_ICE", BitSetter.setFlag(0, AvhrrConstants.F_SNOW_ICE), null);
        flagCoding.addFlag("F_MIXED_PIXEL", BitSetter.setFlag(0, AvhrrConstants.F_MIXED_PIXEL), null);
        flagCoding.addFlag("F_GLINT_RISK", BitSetter.setFlag(0, AvhrrConstants.F_GLINT_RISK), null);
        flagCoding.addFlag("F_COASTLINE", BitSetter.setFlag(0, AvhrrConstants.F_COASTLINE), null);
        flagCoding.addFlag("F_LAND", BitSetter.setFlag(0, AvhrrConstants.F_LAND), null);
        // tests:
        flagCoding.addFlag("F_REFL1_ABOVE_THRESH", BitSetter.setFlag(0, AvhrrConstants.F_LAND + 1), null);
        flagCoding.addFlag("F_REFL2_ABOVE_THRESH", BitSetter.setFlag(0, AvhrrConstants.F_LAND + 2), null);
        flagCoding.addFlag("F_RATIO_REFL21_ABOVE_THRESH", BitSetter.setFlag(0, AvhrrConstants.F_LAND + 3), null);
        flagCoding.addFlag("F_RATIO_REFL31_ABOVE_THRESH", BitSetter.setFlag(0, AvhrrConstants.F_LAND + 4), null);
        flagCoding.addFlag("F_BT4_ABOVE_THRESH", BitSetter.setFlag(0, AvhrrConstants.F_LAND + 5), null);
        flagCoding.addFlag("F_BT5_ABOVE_THRESH", BitSetter.setFlag(0, AvhrrConstants.F_LAND + 6), null);

        return flagCoding;
    }


    public static int setupAvhrrAcClassifBitmask(Product avhrracProduct) {

        int index = 0;
        int w = avhrracProduct.getSceneRasterWidth();
        int h = avhrracProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random();

        mask = Mask.BandMathsType.create("F_INVALID", "Invalid pixel", w, h,
                                         "pixel_classif_flags.F_INVALID",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD", "Cloudy pixel (sure or ambiguous)", w, h,
                                         "pixel_classif_flags.F_CLOUD",
                                         Color.yellow, 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_AMBIGUOUS", "Cloudy pixel (ambiguous)", w, h,
                                         "pixel_classif_flags.F_CLOUD_AMBIGUOUS",
                                         Color.blue, 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_SURE", "Cloudy pixel (sure)", w, h,
                                         "pixel_classif_flags.F_CLOUD_SURE",
                                         Color.red, 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_BUFFER", "Cloud buffer pixel", w, h,
                                         "pixel_classif_flags.F_CLOUD_BUFFER",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_CLOUD_SHADOW", "Cloud shadow pixel", w, h,
                                         "pixel_classif_flags.F_CLOUD_SHADOW",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_SNOW_ICE", "Snow/ice pixel", w, h,
                                         "pixel_classif_flags.F_SNOW_ICE",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_GLINT_RISK", "Glint risk pixel", w, h,
                                         "pixel_classif_flags.F_GLINT_RISK",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_COASTLINE", "Coastline pixel", w, h,
                                         "pixel_classif_flags.F_COASTLINE",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_LAND", "Land pixel", w, h,
                                         "pixel_classif_flags.F_LAND",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        // tests:
        mask = Mask.BandMathsType.create("F_REFL1_ABOVE_THRESH", "TOA reflectance Channel 1 above threshold", w, h,
                                         "pixel_classif_flags.F_REFL1_ABOVE_THRESH",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_REFL2_ABOVE_THRESH", "TOA reflectance Channel 2 above threshold", w, h,
                                         "pixel_classif_flags.F_REFL2_ABOVE_THRESH",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_RATIO_REFL21_ABOVE_THRESH", "Ratio of TOA reflectance Channel 2/1 above threshold", w, h,
                                         "pixel_classif_flags.F_RATIO_REFL21_ABOVE_THRESH",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_RATIO_REFL31_ABOVE_THRESH", "Ratio of TOA reflectance Channel 3/1 above threshold", w, h,
                                         "pixel_classif_flags.F_RATIO_REFL31_ABOVE_THRESH",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_BT4_ABOVE_THRESH", "Brightness temperature Channel 4 above threshold", w, h,
                                         "pixel_classif_flags.F_BT4_ABOVE_THRESH",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_BT5_ABOVE_THRESH", "Brightness temperature Channel 5 above threshold", w, h,
                                         "pixel_classif_flags.F_BT5_ABOVE_THRESH",
                                         getRandomColour(r), 0.5f);
        avhrracProduct.getMaskGroup().add(index++, mask);

        return index;
    }

    public static Calendar getProductDateAsCalendar(String ddmmyy) {
        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        int year = Integer.parseInt(ddmmyy.substring(4, 6));
        if (year < 50) {
            year = 2000 + year;
        } else {
            year = 1900 + year;
        }
        final int month = Integer.parseInt(ddmmyy.substring(2, 4)) - 1;
        final int day = Integer.parseInt(ddmmyy.substring(0, 2));
        calendar.set(year, month, day, 12, 0, 0);
        return calendar;
    }

    public static boolean anglesInvalid(double sza, double vza, double saa, double vaa) {
        // todo: we have a discontinuity in angle retrieval at sza=90deg. Check!
        final double eps = 1.E-6;
//        final boolean szaInvalid = sza < 90.0 + eps && sza > 90.0 - eps;
//        final boolean szaInvalid = sza  > 85.0; // GK, 20150326
        final boolean szaInvalid = sza > 70.0; // GK, 20150922

        final boolean vzaInvalid = Double.isNaN(vza);
        final boolean saaInvalid = Double.isNaN(saa);
        final boolean vaaInvalid = Double.isNaN(vaa);

        return szaInvalid || saaInvalid || vzaInvalid || vaaInvalid;
    }

    public static double convertRadianceToBt(String noaaId, AvhrrAuxdata.Rad2BTTable rad2BTTable, double radianceOrig, int ch, float waterFraction) {
        final double c1 = 1.1910659E-5;
        final double c2 = 1.438833;

        double rad = rad2BTTable.getA(ch) * radianceOrig +
                rad2BTTable.getB(ch) * radianceOrig * radianceOrig + rad2BTTable.getD(ch);
        double nuStart = rad2BTTable.getNuMid(ch);
        double tRef = c2 * nuStart / (Math.log(1.0 + c1 * nuStart * nuStart * nuStart / rad));

        double nuFinal = nuStart;
        switch (noaaId) {
            case "11":
                if (tRef < 225.0) {
                    nuFinal = rad2BTTable.getNuLow(ch);
                } else if (tRef >= 225.0 && tRef < 275.0) {
                    if (waterFraction == 100.0f && tRef > 270.0) {
                        // water
                        nuFinal = rad2BTTable.getNuHighWater(ch);
                    } else {
                        nuFinal = rad2BTTable.getNuMid(ch);
                    }
                } else if (tRef >= 275.0 && tRef < 320.0) {
                    if (waterFraction == 100.0f && tRef < 310.0) {
                        // water
                        nuFinal = rad2BTTable.getNuHighWater(ch);
                    } else {
                        nuFinal = rad2BTTable.getNuHighLand(ch);
                    }
                }
                break;
            case "14":
                if (tRef < 230.0) {
                    nuFinal = rad2BTTable.getNuLow(ch);
                } else if (tRef >= 230.0 && tRef < 270.0) {
                    nuFinal = rad2BTTable.getNuMid(ch);
                } else if (tRef >= 270.0 && tRef < 330.0) {
                    if (waterFraction == 100.0f && tRef < 310.0) {
                        // water
                        nuFinal = rad2BTTable.getNuHighWater(ch);
                    } else {
                        nuFinal = rad2BTTable.getNuHighLand(ch);
                    }
                }
                break;
            default:
                throw new OperatorException("AVHRR version " + noaaId + " not supported.");
        }

        return c2 * nuFinal / (Math.log(1.0 + c1 * nuFinal * nuFinal * nuFinal / rad));
    }

    private static Color getRandomColour(Random random) {
        int rColor = random.nextInt(256);
        int gColor = random.nextInt(256);
        int bColor = random.nextInt(256);
        return new Color(rColor, gColor, bColor);
    }
}
