package org.esa.snap.core.dataio.cache;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TileBoundaryCalculatorTest {

    @Test
    public void testGetBounds_upper_left_tile() {
        final TileBoundaryCalculator calculator = new TileBoundaryCalculator(1000, 1600, 200, 250);

        final TileRegion region = calculator.getBounds(0, 0);
        assertEquals(0, region.getTile_X_min());
        assertEquals(199, region.getTile_X_max());
        assertEquals(0, region.getTile_Y_min());
        assertEquals(249, region.getTile_Y_max());
    }

    @Test
    public void testGetBounds_upper_right_tile() {
        final TileBoundaryCalculator calculator = new TileBoundaryCalculator(800, 1200, 200, 300);

        final TileRegion region = calculator.getBounds(3, 0);
        assertEquals(600, region.getTile_X_min());
        assertEquals(799, region.getTile_X_max());
        assertEquals(0, region.getTile_Y_min());
        assertEquals(299, region.getTile_Y_max());
    }

    @Test
    public void testGetBounds_upper_right_tile_overlapping_product_boundary() {
        final TileBoundaryCalculator calculator = new TileBoundaryCalculator(800, 1200, 250, 300);

        final TileRegion region = calculator.getBounds(3, 0);
        assertEquals(750, region.getTile_X_min());
        assertEquals(799, region.getTile_X_max());
        assertEquals(0, region.getTile_Y_min());
        assertEquals(299, region.getTile_Y_max());
    }

    @Test
    public void testGetBounds_lower_right_tile() {
        final TileBoundaryCalculator calculator = new TileBoundaryCalculator(500, 800, 100, 200);

        final TileRegion region = calculator.getBounds(4, 3);
        assertEquals(400, region.getTile_X_min());
        assertEquals(499, region.getTile_X_max());
        assertEquals(600, region.getTile_Y_min());
        assertEquals(799, region.getTile_Y_max());
    }

    @Test
    public void testGetBounds_lower_right_tile_overlapping_product_boundaries() {
        final TileBoundaryCalculator calculator = new TileBoundaryCalculator(500, 800, 120, 220);

        final TileRegion region = calculator.getBounds(4, 3);
        assertEquals(480, region.getTile_X_min());
        assertEquals(499, region.getTile_X_max());
        assertEquals(660, region.getTile_Y_min());
        assertEquals(799, region.getTile_Y_max());
    }

    @Test
    public void testGetBounds_lower_left_tile() {
        final TileBoundaryCalculator calculator = new TileBoundaryCalculator(3600, 4400, 400, 400);

        final TileRegion region = calculator.getBounds(0, 10);
        assertEquals(0, region.getTile_X_min());
        assertEquals(399, region.getTile_X_max());
        assertEquals(4000, region.getTile_Y_min());
        assertEquals(4399, region.getTile_Y_max());
    }

    @Test
    public void testGetBounds_lower_left_tile_overlapping_product_boundary() {
        final TileBoundaryCalculator calculator = new TileBoundaryCalculator(2100, 3300, 300, 400);

        final TileRegion region = calculator.getBounds(0, 8);
        assertEquals(0, region.getTile_X_min());
        assertEquals(299, region.getTile_X_max());
        assertEquals(3200, region.getTile_Y_min());
        assertEquals(3299, region.getTile_Y_max());
    }
}
