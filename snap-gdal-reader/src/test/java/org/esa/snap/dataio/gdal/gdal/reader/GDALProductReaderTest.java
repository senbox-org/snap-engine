package org.esa.snap.dataio.gdal.gdal.reader;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.dataio.gdal.reader.GDALProductReader;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class GDALProductReaderTest {

    @Test
    @STTM("SNAP-4138")
    public void testComputeExpression() {
        String expression = GDALProductReader.computeExpression("band_1", "nodata_ms1", 0);
        assertEquals("feq('band_1.raw',0,000000)", expression);
        expression = GDALProductReader.computeExpression("band_1", "ms1", 0);
        assertEquals("band_1", expression);
    }

}
