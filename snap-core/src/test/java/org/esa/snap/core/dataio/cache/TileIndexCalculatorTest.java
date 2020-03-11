package org.esa.snap.core.dataio.cache;

import org.junit.Test;

import java.awt.*;
import java.awt.geom.Area;

import static org.junit.Assert.assertEquals;

public class TileIndexCalculatorTest {

    @Test
    public void test_oneTile() {
        final TileIndexCalculator calculator = new TileIndexCalculator(100, 100);
        final Area searchRegion = new Area(new Rectangle(20, 20, 40, 60));

        final TileRegion region = calculator.getTileIndexRegion(searchRegion);
        assertEquals(0, region.getTile_X_min());
        assertEquals(0, region.getTile_X_max());
        assertEquals(0, region.getTile_Y_min());
        assertEquals(0, region.getTile_Y_max());
    }

    @Test
    public void test_twoTiles_horizontal() {
        final TileIndexCalculator calculator = new TileIndexCalculator(100, 100);
        final Area searchRegion = new Area(new Rectangle(90, 20, 40, 60));

        final TileRegion region = calculator.getTileIndexRegion(searchRegion);
        assertEquals(0, region.getTile_X_min());
        assertEquals(1, region.getTile_X_max());
        assertEquals(0, region.getTile_Y_min());
        assertEquals(0, region.getTile_Y_max());
    }

    @Test
    public void test_twoTiles_vertical() {
        final TileIndexCalculator calculator = new TileIndexCalculator(50, 100);
        final Area searchRegion = new Area(new Rectangle(10, 80, 20, 60));

        final TileRegion region = calculator.getTileIndexRegion(searchRegion);
        assertEquals(0, region.getTile_X_min());
        assertEquals(0, region.getTile_X_max());
        assertEquals(0, region.getTile_Y_min());
        assertEquals(1, region.getTile_Y_max());
    }

    @Test
    public void test_threeTiles_vertical() {
        final TileIndexCalculator calculator = new TileIndexCalculator(100, 40);
        final Area searchRegion = new Area(new Rectangle(10, 30, 20, 80));

        final TileRegion region = calculator.getTileIndexRegion(searchRegion);
        assertEquals(0, region.getTile_X_min());
        assertEquals(0, region.getTile_X_max());
        assertEquals(0, region.getTile_Y_min());
        assertEquals(2, region.getTile_Y_max());
    }

    @Test
    public void test_sixTiles_3_horizontal_and_2_vertical() {
        final TileIndexCalculator calculator = new TileIndexCalculator(50, 40);
        final Area searchRegion = new Area(new Rectangle(120, 340, 85, 60));

        final TileRegion region = calculator.getTileIndexRegion(searchRegion);
        assertEquals(2, region.getTile_X_min());
        assertEquals(4, region.getTile_X_max());
        assertEquals(8, region.getTile_Y_min());
        assertEquals(9, region.getTile_Y_max());
    }
}
