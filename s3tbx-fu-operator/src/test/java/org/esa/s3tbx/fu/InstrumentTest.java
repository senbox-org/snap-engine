package org.esa.s3tbx.fu;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class InstrumentTest {
    @Test
    public void testGetTheNumberOfReflectantBandInInstrument() throws Exception {
        assertEquals(6, Instrument.SEAWIFS.getWavelengths().length);
        assertEquals(7, Instrument.MODIS.getWavelengths().length);
        assertEquals(9, Instrument.MERIS.getWavelengths().length);
        assertEquals(11, Instrument.OLCI.getWavelengths().length);
    }


}