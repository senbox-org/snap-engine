package org.esa.s3tbx.dataio.modis.bandreader;

import org.esa.s3tbx.dataio.modis.ModisConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ModisBandReaderTest {

    @Test
    public void testDecodeScalingMethod() {
        assertEquals(ModisBandReader.SCALE_UNKNOWN, ModisBandReader.decodeScalingMethod(null));
        assertEquals(ModisBandReader.SCALE_UNKNOWN, ModisBandReader.decodeScalingMethod(""));

        assertEquals(ModisBandReader.SCALE_LINEAR, ModisBandReader.decodeScalingMethod(ModisConstants.LINEAR_SCALE_NAME));
        assertEquals(ModisBandReader.SCALE_EXPONENTIAL, ModisBandReader.decodeScalingMethod(ModisConstants.EXPONENTIAL_SCALE_NAME));
        assertEquals(ModisBandReader.SCALE_POW_10, ModisBandReader.decodeScalingMethod(ModisConstants.POW_10_SCALE_NAME));
        assertEquals(ModisBandReader.SCALE_SLOPE_INTERCEPT, ModisBandReader.decodeScalingMethod(ModisConstants.SLOPE_INTERCEPT_SCALE_NAME));
    }
}
