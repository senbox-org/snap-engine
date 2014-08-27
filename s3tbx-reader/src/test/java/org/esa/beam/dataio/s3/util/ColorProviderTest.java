package org.esa.beam.dataio.s3.util;

import org.junit.Before;
import org.junit.Test;

import java.awt.Color;

import static junit.framework.TestCase.assertEquals;

/**
 * @author Tonio Fincke
 */
public class ColorProviderTest {

    private ColorProvider colorProvider;
    private Color firstDefaultColour;

    @Before
    public void setUp() {
        colorProvider = new ColorProvider();
        firstDefaultColour = new Color(0, 0, 255);
    }

    @Test
    public void testGetMaskForColor_coastline() throws Exception {
        final Color coastlineColor = new Color(0, 255, 0);
        final Color coastlineColor1 = colorProvider.getMaskColor("rtwftgcoastlinerttzg");
        final Color coastlineColor2 = colorProvider.getMaskColor("csdfgCOASTLINEctfsg");
        final Color coastlineColor4 = colorProvider.getMaskColor("csdfgCoAsltInEctfsg");
        assertEquals(coastlineColor, coastlineColor1);
        assertEquals(coastlineColor, coastlineColor2);
        assertEquals(firstDefaultColour, coastlineColor4);
    }

    @Test
    public void testGetMaskForColor_land() throws Exception {
        final Color landColor = new Color(51, 153, 0);
        final Color landColor1 = colorProvider.getMaskColor("yawsdelandaesd");
        final Color landColor2 = colorProvider.getMaskColor("eswdLANDcdsfrg");
        final Color landColor4 = colorProvider.getMaskColor("eswdLnadcdsfrg");
        assertEquals(landColor, landColor1);
        assertEquals(landColor, landColor2);
        assertEquals(firstDefaultColour, landColor4);
    }

    @Test
    public void testGetMaskForColor_water() throws Exception {
        final Color waterColor = new Color(153, 153, 255);
        final Color waterColor1 = colorProvider.getMaskColor("dfgbnzwaterkj");
        final Color waterColor2 = colorProvider.getMaskColor("nfujiWATERcrtvf");
        final Color waterColor4 = colorProvider.getMaskColor("nfujiWeTaRcrtvf");
        assertEquals(waterColor, waterColor1);
        assertEquals(waterColor, waterColor2);
        assertEquals(firstDefaultColour, waterColor4);
    }

    @Test
    public void testGetMaskForColor() {
        assertEquals(new Color(0, 0, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 255, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 255, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 0, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 0, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 255, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 255, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 0, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 127, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 127, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 127, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 255, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 0, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 0, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 0, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 127, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 127, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 127, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 255, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 255, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 255, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 0, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 127, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 127, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 127, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 255, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 0, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 0, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 63, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 63, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 63, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 63, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 63, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 127, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 127, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 191, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 191, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 191, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 191, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 191, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 255, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(0, 255, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 0, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 0, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 0, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 0, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 0, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 63, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 63, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 63, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 63, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 63, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 127, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 127, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 127, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 127, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 127, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 191, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 191, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 191, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 191, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 191, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 255, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 255, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 255, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 255, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(63, 255, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 0, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 0, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 63, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 63, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 63, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 63, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 63, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 127, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 127, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 191, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 191, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 191, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 191, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 191, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 255, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(127, 255, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 0, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 0, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 0, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 0, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 0, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 63, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 63, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 63, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 63, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 63, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 127, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 127, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 127, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 127, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 127, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 191, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 191, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 191, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 191, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 191, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 255, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 255, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 255, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 255, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(191, 255, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 0, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 0, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 63, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 63, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 63, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 63, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 63, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 127, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 127, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 191, 0), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 191, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 191, 127), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 191, 191), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 191, 255), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 255, 63), colorProvider.getMaskColor("dummy"));
        assertEquals(new Color(255, 255, 191), colorProvider.getMaskColor("dummy"));
    }

}
