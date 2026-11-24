/*
 * Copyright (C) 2026 by SkyWatch Space Applications Inc. http://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.dataio;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;

import static org.junit.Assert.assertEquals;

/**
 * Pins the SAR {@code slant_range_to_first_pixel} and SRGR {@code ground_range_origin} that
 * {@link ProductSubsetBuilder} writes for a subset region. The expected values are those of the
 * subset's near-range column.
 * <p>
 * The synthetic product has slant range {@code SR0 + n * SPACING} at {@code n} columns from near
 * range, and the {@code slant_range_time} tie-point grid holds the same values as two-way time.
 * When near range is on the right (RS2/RCM descending), column 0 is far range.
 */
public class ProductSubsetBuilderSarMetadataTest {

    private static final int WIDTH = 200;
    private static final int HEIGHT = 20;
    private static final double SR0 = 800000.0;      // near-range slant range [m]
    private static final double SPACING = 10.0;      // range pixel spacing [m]
    private static final double HALF_LIGHT_SPEED = 299792458.0 / 2.0;
    // The tie points are float (~0.5 ns ulp at 5.3e6 ns = ~0.08 m). The defects are hundreds of metres.
    private static final double TOLERANCE = 0.5;
    private static final double GR0 = 5000.0;        // SRGR ground_range_origin of the source [m]

    // ground range product (srgr_flag = 1): value taken from the slant_range_time tie-point grid

    @Test
    @STTM("SNAP-4378")
    public void testGroundRangeSubsetAtOriginIsUnchanged() throws Exception {
        assertNearRange(subset(sarProduct(true, true), 0, 5, 80, 10), 0);
    }

    @Test
    @STTM("SNAP-4378")
    public void testGroundRangeSubsetNearRangeLeft() throws Exception {
        assertNearRange(subset(sarProduct(true, true), 50, 5, 80, 10), 50);
    }

    @Test
    @STTM("SNAP-4378")
    public void testGroundRangeSubsetNearRangeRight() throws Exception {
        // subset near-range column is source column 50 + 80 - 1 = 129, i.e. 200 - 50 - 80 = 70 from near range
        assertNearRange(subset(sarProduct(true, false), 50, 5, 80, 10), 70);
    }

    @Test
    @STTM("SNAP-4378")
    public void testGroundRangeNestedSubsetNearRangeLeft() throws Exception {
        // (50, w=100) then (20, w=50) is source columns 70..119
        assertNearRange(subset(subset(sarProduct(true, true), 50, 0, 100, 15), 20, 0, 50, 10), 70);
    }

    @Test
    @STTM("SNAP-4378")
    public void testGroundRangeNestedSubsetNearRangeRight() throws Exception {
        // source columns 70..119 -> near-range column 119 is 200 - 70 - 50 = 80 from near range
        assertNearRange(subset(subset(sarProduct(true, false), 50, 0, 100, 15), 20, 0, 50, 10), 80);
    }

    // slant range product (srgr_flag = 0): value derived from the source slant_range_to_first_pixel

    @Test
    @STTM("SNAP-4378")
    public void testSlantRangeSubsetNearRangeLeft() throws Exception {
        assertNearRange(subset(sarProduct(false, true), 50, 5, 80, 10), 50);
    }

    @Test
    @STTM("SNAP-4378")
    public void testSlantRangeSubsetNearRangeRight() throws Exception {
        assertNearRange(subset(sarProduct(false, false), 50, 5, 80, 10), 70);
    }

    @Test
    @STTM("SNAP-4378")
    public void testSlantRangeNestedSubsetNearRangeRight() throws Exception {
        assertNearRange(subset(subset(sarProduct(false, false), 50, 0, 100, 15), 20, 0, 50, 10), 80);
    }

    // sub-sampled: the last subset column is x + (w' - 1) * step with w' = (w - 1) / step + 1, which can
    // fall short of the region's right edge

    @Test
    @STTM("SNAP-4378")
    public void testSlantRangeSubsampledNearRangeRight() throws Exception {
        // (50, w=80) step 3 keeps 27 columns, the last at source column 50 + 26 * 3 = 128: 199 - 128 = 71
        final Product subset = subset(sarProduct(false, false), 50, 5, 80, 10, 3, null);
        assertEquals(27, subset.getSceneRasterWidth());
        assertNearRange(subset, 71);
    }

    @Test
    @STTM("SNAP-4378")
    public void testGroundRangeSubsampledNearRangeRight() throws Exception {
        assertNearRange(subset(sarProduct(true, false), 50, 5, 80, 10, 3, null), 71);
    }

    @Test
    @STTM("SNAP-4378")
    public void testSlantRangeSubsampledNearRangeLeft() throws Exception {
        assertNearRange(subset(sarProduct(false, true), 50, 5, 80, 10, 3, null), 50);
    }

    // SRGR ground_range_origin is shifted by the same number of columns

    @Test
    @STTM("SNAP-4378")
    public void testGroundRangeOriginNearRangeLeft() throws Exception {
        assertGroundRangeOrigin(subset(sarProduct(true, true), 50, 5, 80, 10), 50);
    }

    @Test
    @STTM("SNAP-4378")
    public void testGroundRangeOriginNearRangeRight() throws Exception {
        assertGroundRangeOrigin(subset(sarProduct(true, false), 50, 5, 80, 10), 70);
    }

    @Test
    @STTM("SNAP-4378")
    public void testGroundRangeOriginSubsampledNearRangeRight() throws Exception {
        assertGroundRangeOrigin(subset(sarProduct(true, false), 50, 5, 80, 10, 3, null), 71);
    }

    // slant-range products need no slant_range_time grid (NISAR RSLC has none)

    @Test
    @STTM("SNAP-4378")
    public void testSlantRangeWithoutSlantRangeTimeGrid() throws Exception {
        final Product source = sarProduct(false, true);
        source.removeTiePointGrid(source.getTiePointGrid("slant_range_time"));
        assertNearRange(subset(source, 50, 5, 80, 10), 50);
    }

    // which side is near range is a property of the source product

    @Test
    @STTM("SNAP-4378")
    public void testNearRangeRightWhenSubsetDropsIncidenceAngle() throws Exception {
        final Product subset = subset(sarProduct(false, false), 50, 5, 80, 10, 1,
                                      new String[]{"Amplitude", "slant_range_time"});
        assertEquals(null, subset.getTiePointGrid("incident_angle"));
        assertNearRange(subset, 70);
    }

    @Test
    @STTM("SNAP-4378")
    public void testOneColumnSubsetNearRangeRight() throws Exception {
        assertNearRange(subset(sarProduct(false, false), 50, 5, 1, 10), 149);
    }

    @Test
    @STTM("SNAP-4378")
    public void testOneColumnSubsetNearRangeLeft() throws Exception {
        assertNearRange(subset(sarProduct(false, true), 50, 5, 1, 10), 50);
    }

    private static void assertGroundRangeOrigin(final Product subset, final int expectedColumnsFromNearRange) {
        final MetadataElement srgrList = absRoot(subset).getElement("SRGR_Coefficients").getElementAt(0);
        assertEquals("ground_range_origin [m]", GR0 + expectedColumnsFromNearRange * SPACING,
                     srgrList.getAttributeDouble("ground_range_origin"), 1e-9);
    }

    private static void assertNearRange(final Product subset, final int expectedColumnsFromNearRange) {
        final double expected = SR0 + expectedColumnsFromNearRange * SPACING;
        final double actual = absRoot(subset).getAttributeDouble("slant_range_to_first_pixel");
        assertEquals("slant_range_to_first_pixel [m]; error " + (actual - expected) + " m",
                     expected, actual, TOLERANCE);
    }

    private static Product subset(final Product source, final int x, final int y, final int w, final int h)
            throws IOException {
        return subset(source, x, y, w, h, 1, null);
    }

    private static Product subset(final Product source, final int x, final int y, final int w, final int h,
                                  final int stepX, final String[] nodeNames) throws IOException {
        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setSubsetRegion(new PixelSubsetRegion(x, y, w, h, 0));
        subsetDef.setSubSampling(stepX, 1);
        if (nodeNames != null) {
            subsetDef.setNodeNames(nodeNames);
        }
        return ProductSubsetBuilder.createProductSubset(source, subsetDef, "subset", "");
    }

    private static Product sarProduct(final boolean groundRange, final boolean nearRangeOnLeft)
            throws ParseException {
        final Product product = new Product("sar", "SAR", WIDTH, HEIGHT);
        product.addBand("Amplitude", ProductData.TYPE_FLOAT32);

        // one tie point per column at pixel centres, two rows spanning the image
        final float[] srTime = new float[2 * WIDTH];
        final float[] incidence = new float[2 * WIDTH];
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < WIDTH; col++) {
                final int fromNear = nearRangeOnLeft ? col : WIDTH - 1 - col;
                srTime[row * WIDTH + col] = (float) ((SR0 + fromNear * SPACING) / HALF_LIGHT_SPEED * 1.0e9); // ns
                incidence[row * WIDTH + col] = (float) (30.0 + 0.05 * fromNear);
            }
        }
        product.addTiePointGrid(new TiePointGrid("slant_range_time", WIDTH, 2, 0.5, 0.5, 1, HEIGHT - 1, srTime));
        product.addTiePointGrid(new TiePointGrid("incident_angle", WIDTH, 2, 0.5, 0.5, 1, HEIGHT - 1, incidence));

        final MetadataElement absRoot = new MetadataElement("Abstracted_Metadata");
        absRoot.addAttribute(attr("radar_frequency", 5405.0));
        absRoot.addAttribute(attr("slant_range_to_first_pixel", SR0));
        absRoot.addAttribute(attr("RANGE_SPACING", SPACING));
        final MetadataAttribute srgrFlag = new MetadataAttribute("srgr_flag", ProductData.TYPE_INT32);
        srgrFlag.getData().setElemInt(groundRange ? 1 : 0);
        absRoot.addAttribute(srgrFlag);
        if (groundRange) {
            final MetadataElement srgrList = new MetadataElement("srgr_coef_list");
            srgrList.addAttribute(attr("ground_range_origin", GR0));
            final MetadataElement srgrCoefficients = new MetadataElement("SRGR_Coefficients");
            srgrCoefficients.addElement(srgrList);
            absRoot.addElement(srgrCoefficients);
        }
        absRoot.addAttribute(new MetadataAttribute("first_line_time",
                                                   ProductData.UTC.parse("01-JAN-2024 00:00:00.000000"), true));
        absRoot.addAttribute(new MetadataAttribute("last_line_time",
                                                   ProductData.UTC.parse("01-JAN-2024 00:00:01.900000"), true));
        product.getMetadataRoot().addElement(absRoot);
        return product;
    }

    private static MetadataAttribute attr(final String name, final double value) {
        final MetadataAttribute attribute = new MetadataAttribute(name, ProductData.TYPE_FLOAT64);
        attribute.getData().setElemDouble(value);
        return attribute;
    }

    private static MetadataElement absRoot(final Product product) {
        return product.getMetadataRoot().getElement("Abstracted_Metadata");
    }
}
