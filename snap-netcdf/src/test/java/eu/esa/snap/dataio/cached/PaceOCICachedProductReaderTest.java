package eu.esa.snap.dataio.cached;

import org.junit.Test;

import java.text.DecimalFormat;

import static org.junit.Assert.assertEquals;

public class PaceOCICachedProductReaderTest {

    @Test
    public void testGetDecimalFormat() {
        DecimalFormat decimalFormat = PaceOCICachedProductReader.getDecimalFormat(9);
        assertEquals("1", decimalFormat.format(1));
        assertEquals("9", decimalFormat.format(9));

        decimalFormat = PaceOCICachedProductReader.getDecimalFormat(19);
        assertEquals("01", decimalFormat.format(1));
        assertEquals("09", decimalFormat.format(9));
        assertEquals("11", decimalFormat.format(11));

        decimalFormat = PaceOCICachedProductReader.getDecimalFormat(198);
        assertEquals("001", decimalFormat.format(1));
        assertEquals("011", decimalFormat.format(11));
        assertEquals("609", decimalFormat.format(609));
    }
}
