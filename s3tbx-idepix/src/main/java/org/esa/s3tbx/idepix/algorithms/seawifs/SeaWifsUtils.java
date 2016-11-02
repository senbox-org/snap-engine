package org.esa.s3tbx.idepix.algorithms.seawifs;

import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.IdepixFlagCoding;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.BitSetter;

import java.awt.*;
import java.util.Random;

/**
 * Utility class for Idepix SeaWiFS
 *
 * @author olafd
 */
public class SeaWifsUtils {

    /**
     * Provides SeaWiFS pixel classification flag coding
     *
     * @param flagId - the flag ID
     *
     * @return - the flag coding
     */
    public static FlagCoding createSeawifsFlagCoding(String flagId) {
        FlagCoding flagCoding = IdepixFlagCoding.createDefaultFlagCoding(flagId);

        flagCoding.addFlag("IDEPIX_MIXED_PIXEL", BitSetter.setFlag(0, SeaWifsConstants.IDEPIX_MIXED_PIXEL),
                           SeaWifsConstants.IDEPIX_MIXED_PIXEL_DESCR_TEXT);

        return flagCoding;
    }

    /**
     * Provides SeaWiFS pixel classification flag bitmask
     *
     * @param classifProduct - the pixel classification product
     */
    public static void setupSeawifsClassifBitmask(Product classifProduct) {
        int index = IdepixFlagCoding.setupDefaultClassifBitmask(classifProduct);

        int w = classifProduct.getSceneRasterWidth();
        int h = classifProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random();

        mask = Mask.BandMathsType.create("IDEPIX_MIXED_PIXEL", SeaWifsConstants.IDEPIX_MIXED_PIXEL_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_MIXED_PIXEL",
                                         IdepixFlagCoding.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index, mask);
    }

}
