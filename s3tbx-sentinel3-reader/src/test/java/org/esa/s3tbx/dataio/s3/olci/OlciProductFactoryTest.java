package org.esa.s3tbx.dataio.s3.olci;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class OlciProductFactoryTest {

    @Test
    public void testUnitExtrationFromLogScaledUnit() {
        String logUnit = "lg(re g.m-3)";
        Pattern pattern = Pattern.compile("lg\\s*\\(\\s*re:?\\s*(.*)\\)");
        final Matcher m = pattern.matcher(logUnit);
        assertTrue(m.matches());

        assertEquals(logUnit, m.group(0));
        assertEquals("g.m-3", m.group(1));
    }

}
