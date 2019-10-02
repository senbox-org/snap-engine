package org.esa.snap.core.image;

import org.junit.Test;

import java.awt.*;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class BandOpImageTest_Static_PackageLocal {

    @Test
    public void testComputeTiledLevel0AxisIndexes() {
        final Map<Integer, List<BandOpImage.PositionCouple>> xSrcTiled;

        //preparation
        final LevelImageSupport lvlSup = new LevelImageSupport(40, 40, new ResolutionLevel(2, 4.0));
        final Rectangle targetRect = new Rectangle(4, 1, 5, 5);
        int tileWidth = 9;

        //execution
        xSrcTiled = BandOpImage.computeTiledL0AxisIdx(targetRect.x, targetRect.width, tileWidth, lvlSup::getSourceX);

        //verification
        assertEquals(3, xSrcTiled.size());
        assertTrue(xSrcTiled.containsKey(1));
        assertTrue(xSrcTiled.containsKey(2));
        assertTrue(xSrcTiled.containsKey(3));
        assertArrayEquals(new BandOpImage.PositionCouple[]{
                new BandOpImage.PositionCouple(16, 4)}, xSrcTiled.get(1).toArray(new Object[]{}));
        assertArrayEquals(new BandOpImage.PositionCouple[]{
                new BandOpImage.PositionCouple(20, 5),
                new BandOpImage.PositionCouple(24, 6)}, xSrcTiled.get(2).toArray(new Object[]{}));
        assertArrayEquals(new BandOpImage.PositionCouple[]{
                new BandOpImage.PositionCouple(28, 7),
                new BandOpImage.PositionCouple(32, 8)}, xSrcTiled.get(3).toArray(new Object[]{}));
    }
}