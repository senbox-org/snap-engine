package org.esa.snap.dataio.envi;

import com.bc.ceres.test.LongTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

@RunWith(LongTestRunner.class)
public class EnviProductReaderTest {

    @Test
    public void testParseBandNames_emptyBandnameProperty() throws IOException {
        final StringReader reader = new StringReader(HeaderTest.createMandatoryHeader() + "band names = {}"); // empty bandname property
        final Header header = new Header(new BufferedReader(reader));

        final String[] bandNames = EnviProductReader.getBandNames(header);

        assertEquals(1, bandNames.length);
        assertEquals("Band", bandNames[0]);
    }

    @Test
    public void testParseBandNames_noBandnameProperty() throws IOException {
        final StringReader reader = new StringReader(HeaderTest.createMandatoryHeader()); // no bandname property
        final Header header = new Header(new BufferedReader(reader));

        final String[] bandNames = EnviProductReader.getBandNames(header);

        assertEquals(1, bandNames.length);
        assertEquals("Band", bandNames[0]);
    }

    @Test
    public void testParseBandNames_withBandnameProperty() throws IOException {
        final StringReader reader = new StringReader(HeaderTest.createMandatoryHeader() + "band names = { myband_1, myband_2}");
        final Header header = new Header(new BufferedReader(reader));

        final String[] bandNames = EnviProductReader.getBandNames(header);

        assertEquals(2, bandNames.length);
        assertEquals("myband_1", bandNames[0]);
        assertEquals("myband_2", bandNames[1]);
    }

    @Test
    public void testParseBandNames_withBandNumberProperty() throws IOException {
        final StringReader reader = new StringReader(HeaderTest.createMandatoryHeader() + "bands = 3");
        final Header header = new Header(new BufferedReader(reader));

        final String[] bandNames = EnviProductReader.getBandNames(header);

        assertEquals(3, bandNames.length);
        assertEquals("Band_1", bandNames[0]);
        assertEquals("Band_2", bandNames[1]);
        assertEquals("Band_3", bandNames[2]);
    }
}
