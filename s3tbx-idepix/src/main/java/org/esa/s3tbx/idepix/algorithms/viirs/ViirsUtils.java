package org.esa.s3tbx.idepix.algorithms.viirs;

import org.esa.s3tbx.idepix.core.IdepixFlagCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.BitSetter;

import java.util.Random;

/**
 * Utility class for Idepix VIIRS
 *
 * @author olafd
 */
public class ViirsUtils {

    /**
     * Provides VIIRS pixel classification flag coding
     *
     * @param flagId - the flag ID
     *
     * @return - the flag coding
     */
    public static FlagCoding createViirsFlagCoding(String flagId) {
        FlagCoding flagCoding = IdepixFlagCoding.createDefaultFlagCoding(flagId);

        flagCoding.addFlag("IDEPIX_MIXED_PIXEL", BitSetter.setFlag(0, ViirsConstants.IDEPIX_MIXED_PIXEL),
                           ViirsConstants.IDEPIX_MIXED_PIXEL_DESCR_TEXT);

        return flagCoding;
    }

    /**
     * Provides MODIS pixel classification flag bitmask
     *
     * @param classifProduct - the pixel classification product
     */
    public static void setupViirsClassifBitmask(Product classifProduct) {
        int index = IdepixFlagCoding.setupDefaultClassifBitmask(classifProduct);

        int w = classifProduct.getSceneRasterWidth();
        int h = classifProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random();

        mask = Mask.BandMathsType.create("IDEPIX_MIXED_PIXEL", ViirsConstants.IDEPIX_MIXED_PIXEL_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_MIXED_PIXEL",
                                         IdepixFlagCoding.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index, mask);
    }

}
