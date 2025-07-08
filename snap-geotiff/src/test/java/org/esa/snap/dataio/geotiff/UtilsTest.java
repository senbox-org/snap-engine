package org.esa.snap.dataio.geotiff;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class UtilsTest {

    @Test
    public void testGetTiffMode() throws IOException {
        String tiffMode;
        ByteArrayInputStream bigEndianBigTiff = new ByteArrayInputStream(new byte[]{0x4d, 0x4d, 0x00, 0x2b});
        tiffMode = Utils.getTiffMode(ImageIO.createImageInputStream(bigEndianBigTiff));
        assertEquals("BigTiff", tiffMode);

        ByteArrayInputStream bigEndianTiff = new ByteArrayInputStream(new byte[]{0x4d, 0x4d, 0x00, 0x2a});
        tiffMode = Utils.getTiffMode(ImageIO.createImageInputStream(bigEndianTiff));
        assertEquals("Tiff", tiffMode);

        ByteArrayInputStream littleEndianBigTiff = new ByteArrayInputStream(new byte[]{0x49, 0x49, 0x2b, 0x00});
        tiffMode = Utils.getTiffMode(ImageIO.createImageInputStream(littleEndianBigTiff));
        assertEquals("BigTiff", tiffMode);

        ByteArrayInputStream littleEndianTiff = new ByteArrayInputStream(new byte[]{0x49, 0x49, 0x2a, 0x00});
        tiffMode = Utils.getTiffMode(ImageIO.createImageInputStream(littleEndianTiff));
        assertEquals("Tiff", tiffMode);

    }

    @Test
    @STTM("SNAP-3888")
    public void testIsDegreesPerPixel_NullArray() {
        assertFalse(Utils.isDegreesPerPixel(null));
    }

    @Test
    @STTM("SNAP-3888")
    public void testIsDegreesPerPixel_InvalidArray() {
        assertFalse(Utils.isDegreesPerPixel(new double[]{-0.1, 0.5}));
        assertFalse(Utils.isDegreesPerPixel(new double[]{0.5, -0.1}));
        assertFalse(Utils.isDegreesPerPixel(new double[]{0.0, 0.5}));
        assertFalse(Utils.isDegreesPerPixel(new double[]{0.5, 0.0}));
        assertFalse(Utils.isDegreesPerPixel(new double[]{1.1, 0.5}));
        assertFalse(Utils.isDegreesPerPixel(new double[]{0.5, 1.1}));
    }

    @Test
    @STTM("SNAP-3888")
    public void testIsDegreesPerPixel_ValidArray() {
        assertTrue(Utils.isDegreesPerPixel(new double[]{1.0, 1.0}));
        assertTrue(Utils.isDegreesPerPixel(new double[]{0.0001, 0.5}));
    }

}