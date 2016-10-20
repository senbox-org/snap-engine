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
        return IdepixFlagCoding.createDefaultFlagCoding(flagId);
    }

    /**
     * Provides SeaWiFS pixel classification flag bitmask
     *
     * @param classifProduct - the pixel classification product
     */
    public static void setupSeawifsClassifBitmask(Product classifProduct) {

        IdepixFlagCoding.setupDefaultClassifBitmask(classifProduct);
    }

}
