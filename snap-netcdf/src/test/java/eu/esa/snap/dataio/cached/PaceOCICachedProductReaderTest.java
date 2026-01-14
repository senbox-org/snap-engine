package eu.esa.snap.dataio.cached;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;

import java.text.DecimalFormat;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PaceOCICachedProductReaderTest {

    @Test
    @STTM("SNAP-4122")
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

    @Test
    @STTM("SNAP-4122")
    public void testGetDimensionLength() {
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);
        when(netcdfFile.findDimension("real_dim")).thenReturn(new Dimension("real_dim", 22));

        int dimensionLength = PaceOCICachedProductReader.getDimensionLength("real_dim", netcdfFile);
        assertEquals(22, dimensionLength);

        dimensionLength = PaceOCICachedProductReader.getDimensionLength("wrong_dim", netcdfFile);
        assertEquals(-1, dimensionLength);
    }

    @Test
    @STTM("SNAP-4122")
    public void testGetProductHeight() {
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);
        when(netcdfFile.findDimension("scans")).thenReturn(new Dimension("scans", 23));

        int productHeight = PaceOCICachedProductReader.getProductHeight(netcdfFile);
        assertEquals(23, productHeight);

        when(netcdfFile.findDimension("scans")).thenReturn(null);
        when(netcdfFile.findDimension("number_of_scans")).thenReturn(new Dimension("number_of_scans", 24));
        productHeight = PaceOCICachedProductReader.getProductHeight(netcdfFile);
        assertEquals(24, productHeight);

        when(netcdfFile.findDimension("number_of_scans")).thenReturn(null);
        productHeight = PaceOCICachedProductReader.getProductHeight(netcdfFile);
        assertEquals(-1, productHeight);
    }

    @Test
    @STTM("SNAP-4122")
    public void testGetProductWidth() {
        final NetcdfFile netcdfFile = mock(NetcdfFile.class);
        when(netcdfFile.findDimension("pixels")).thenReturn(new Dimension("pixels", 25));

        int productWidth = PaceOCICachedProductReader.getProductWidth(netcdfFile);
        assertEquals(25, productWidth);

        when(netcdfFile.findDimension("pixels")).thenReturn(null);
        when(netcdfFile.findDimension("ccd_pixels")).thenReturn(new Dimension("ccd_pixels", 26));
        productWidth = PaceOCICachedProductReader.getProductWidth(netcdfFile);
        assertEquals(26, productWidth);

        when(netcdfFile.findDimension("ccd_pixels")).thenReturn(null);
        productWidth = PaceOCICachedProductReader.getProductWidth(netcdfFile);
        assertEquals(-1, productWidth);
    }
}
