package org.esa.snap.dataio.geotiff;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

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
}