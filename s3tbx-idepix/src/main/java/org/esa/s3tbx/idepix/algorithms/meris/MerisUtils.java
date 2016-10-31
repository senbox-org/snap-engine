package org.esa.s3tbx.idepix.algorithms.meris;

import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.IdepixFlagCoding;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.BitSetter;

import java.util.Random;

/**
 * Utility class for Idepix MERIS
 *
 * @author olafd
 */
public class MerisUtils {

    /**
     * Provides MERIS pixel classification flag coding
     *
     * @param flagId - the flag ID
     *
     * @return - the flag coding
     */
    public static FlagCoding createMerisFlagCoding(String flagId) {
        FlagCoding flagCoding = IdepixFlagCoding.createDefaultFlagCoding(flagId);

        flagCoding.addFlag("IDEPIX_GLINT_RISK", BitSetter.setFlag(0, MerisConstants.IDEPIX_GLINT_RISK),
                           MerisConstants.IDEPIX_GLINT_RISK_DESCR_TEXT);

        return flagCoding;
    }

    /**
     * Provides MERIS pixel classification flag bitmask
     *
     * @param classifProduct - the pixel classification product
     */
    public static void setupMerisClassifBitmask(Product classifProduct) {
        int index = IdepixFlagCoding.setupDefaultClassifBitmask(classifProduct);

        int w = classifProduct.getSceneRasterWidth();
        int h = classifProduct.getSceneRasterHeight();
        Mask mask;
        Random r = new Random();

        mask = Mask.BandMathsType.create("IDEPIX_GLINT_RISK", MerisConstants.IDEPIX_GLINT_RISK_DESCR_TEXT, w, h,
                                         "pixel_classif_flags.IDEPIX_GLINT_RISK",
                                         IdepixFlagCoding.getRandomColour(r), 0.5f);
        classifProduct.getMaskGroup().add(index, mask);

    }

}
