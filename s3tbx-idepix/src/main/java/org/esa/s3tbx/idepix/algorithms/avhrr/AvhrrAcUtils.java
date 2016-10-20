package org.esa.s3tbx.idepix.algorithms.avhrr;

import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.IdepixFlagCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.BitSetter;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.TimeZone;

/**
 * Utility class for Idepix AVHRR-AC
 *
 * @author olafd
 */
public class AvhrrAcUtils {

    public static FlagCoding createAvhrrAcFlagCoding(String flagId) {

        FlagCoding flagCoding = IdepixFlagCoding.createDefaultFlagCoding(flagId);

        // additional flags for AVHRR-AC (tests):
        flagCoding.addFlag("F_REFL1_ABOVE_THRESH", BitSetter.setFlag(0, IdepixConstants.NUM_DEFAULT_FLAGS + 1), null);
        flagCoding.addFlag("F_REFL2_ABOVE_THRESH", BitSetter.setFlag(0, IdepixConstants.NUM_DEFAULT_FLAGS + 2), null);
        flagCoding.addFlag("F_RATIO_REFL21_ABOVE_THRESH", BitSetter.setFlag(0, IdepixConstants.NUM_DEFAULT_FLAGS + 3), null);
        flagCoding.addFlag("F_RATIO_REFL31_ABOVE_THRESH", BitSetter.setFlag(0, IdepixConstants.NUM_DEFAULT_FLAGS + 4), null);
        flagCoding.addFlag("F_BT4_ABOVE_THRESH", BitSetter.setFlag(0, IdepixConstants.NUM_DEFAULT_FLAGS + 5), null);
        flagCoding.addFlag("F_BT5_ABOVE_THRESH", BitSetter.setFlag(0, IdepixConstants.NUM_DEFAULT_FLAGS + 6), null);

        return flagCoding;
    }


    public static int setupAvhrrAcClassifBitmask(Product classifProduct) {

        int index = IdepixFlagCoding.setupDefaultClassifBitmask(classifProduct);

        int w = classifProduct.getSceneRasterWidth();
        int h = classifProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random();

        // tests:
        mask = Mask.BandMathsType.create("F_REFL1_ABOVE_THRESH", "TOA reflectance Channel 1 above threshold", w, h,
                                         "pixel_classif_flags.F_REFL1_ABOVE_THRESH",
                                         IdepixFlagCoding.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_REFL2_ABOVE_THRESH", "TOA reflectance Channel 2 above threshold", w, h,
                                         "pixel_classif_flags.F_REFL2_ABOVE_THRESH",
                                         IdepixFlagCoding.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_RATIO_REFL21_ABOVE_THRESH", "Ratio of TOA reflectance Channel 2/1 above threshold", w, h,
                                         "pixel_classif_flags.F_RATIO_REFL21_ABOVE_THRESH",
                                         IdepixFlagCoding.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_RATIO_REFL31_ABOVE_THRESH", "Ratio of TOA reflectance Channel 3/1 above threshold", w, h,
                                         "pixel_classif_flags.F_RATIO_REFL31_ABOVE_THRESH",
                                         IdepixFlagCoding.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_BT4_ABOVE_THRESH", "Brightness temperature Channel 4 above threshold", w, h,
                                         "pixel_classif_flags.F_BT4_ABOVE_THRESH",
                                         IdepixFlagCoding.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

        mask = Mask.BandMathsType.create("F_BT5_ABOVE_THRESH", "Brightness temperature Channel 5 above threshold", w, h,
                                         "pixel_classif_flags.F_BT5_ABOVE_THRESH",
                                         IdepixFlagCoding.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index++, mask);

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
//        final double eps = 1.E-6;
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

}
