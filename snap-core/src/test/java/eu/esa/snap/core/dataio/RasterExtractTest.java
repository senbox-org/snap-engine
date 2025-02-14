package eu.esa.snap.core.dataio;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RasterExtractTest {

    @Test
    @STTM("SNAP-1696,SNAP-3711")
    public void testConstruction()  {
        final RasterExtract rasterExtract = new RasterExtract(1, 2, 3, 4, 5, 6);

        assertEquals(1, rasterExtract.getXOffset());
        assertEquals(2, rasterExtract.getYOffset());
        assertEquals(3, rasterExtract.getWidth());
        assertEquals(4, rasterExtract.getHeight());
        assertEquals(5, rasterExtract.getStepX());
        assertEquals(6, rasterExtract.getStepY());
        assertEquals(-1, rasterExtract.getLayerIdx());
    }

    @Test
    @STTM("SNAP-1696,SNAP-3711")
    public void testConstruction_withLayer()  {
        final RasterExtract rasterExtract = new RasterExtract(1, 2, 3, 4, 5, 6, 7);

        assertEquals(1, rasterExtract.getXOffset());
        assertEquals(2, rasterExtract.getYOffset());
        assertEquals(3, rasterExtract.getWidth());
        assertEquals(4, rasterExtract.getHeight());
        assertEquals(5, rasterExtract.getStepX());
        assertEquals(6, rasterExtract.getStepY());
        assertEquals(7, rasterExtract.getLayerIdx());
    }
}
