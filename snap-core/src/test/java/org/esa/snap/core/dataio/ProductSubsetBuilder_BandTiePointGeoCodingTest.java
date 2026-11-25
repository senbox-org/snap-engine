/*
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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.subset.PixelSubsetRegion;
import org.esa.snap.core.subset.SubsetRegionInfo;
import org.junit.Test;

import java.awt.Rectangle;
import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * A multi-size product whose smaller band has its own tie-point geocoding, on tie-point grids of its own.
 * A subset that does not select those grids still transfers the band's geocoding: SNAP adds the grids to
 * the subset while transferring it, so they must not be required to have an entry in the region map.
 */
public class ProductSubsetBuilder_BandTiePointGeoCodingTest {

    private static TiePointGrid grid(final String name, final double subX, final double subY, final float base,
                                     final float stepX, final float stepY) {
        final float[] v = new float[5 * 5];
        for (int y = 0; y < 5; y++) {
            for (int x = 0; x < 5; x++) {
                v[y * 5 + x] = base + x * stepX + y * stepY;
            }
        }
        return new TiePointGrid(name, 5, 5, 0.5, 0.5, subX, subY, v);
    }

    private static Product createMultiSizeProduct() {
        final Product product = new Product("p", "t", 40, 20);
        product.addBand("a", ProductData.TYPE_FLOAT32);
        final TiePointGrid lat = grid("latitude", 10, 5, 50f, 0f, -0.01f);
        final TiePointGrid lon = grid("longitude", 10, 5, 10f, 0.01f, 0f);
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setSceneGeoCoding(new TiePointGeoCoding(lat, lon));

        final Band b = new Band("b_S", ProductData.TYPE_FLOAT32, 10, 20);
        product.addBand(b);
        final TiePointGrid latS = grid("latitude_S", 2.5, 5, 50f, 0f, -0.01f);
        final TiePointGrid lonS = grid("longitude_S", 2.5, 5, 10f, 0.01f, 0f);
        product.addTiePointGrid(latS);
        product.addTiePointGrid(lonS);
        final TiePointGeoCoding gcS = new TiePointGeoCoding(latS, lonS);
        latS.setGeoCoding(gcS);
        lonS.setGeoCoding(gcS);
        b.setGeoCoding(gcS);
        return product;
    }

    @Test
    @STTM("SNAP-4385")
    public void subsetWithoutTheBandsOwnGridsKeepsItsGeoCoding() throws Exception {
        final Product source = createMultiSizeProduct();
        final ProductSubsetDef def = new ProductSubsetDef();
        def.setNodeNames(new String[]{"a", "b_S", "latitude", "longitude"});
        final HashMap<String, SubsetRegionInfo> regions = new HashMap<>();
        regions.put("a", new SubsetRegionInfo(new Rectangle(8, 4, 20, 10), null));
        regions.put("latitude", new SubsetRegionInfo(new Rectangle(8, 4, 20, 10), null));
        regions.put("longitude", new SubsetRegionInfo(new Rectangle(8, 4, 20, 10), null));
        regions.put("b_S", new SubsetRegionInfo(new Rectangle(2, 4, 5, 10), null));
        def.setRegionMap(regions);
        def.setSubsetRegion(new PixelSubsetRegion(new Rectangle(8, 4, 20, 10), 0));

        final Product subset = ProductSubsetBuilder.createProductSubset(source, def, "s", "d");

        final GeoCoding gc = subset.getBand("b_S").getGeoCoding();
        final GeoCoding sourceGc = source.getBand("b_S").getGeoCoding();
        for (double[] p : new double[][]{{0.5, 0.5}, {4.5, 9.5}}) {
            final GeoPos mine = gc.getGeoPos(new PixelPos(p[0], p[1]), null);
            final GeoPos theirs = sourceGc.getGeoPos(new PixelPos(p[0] + 2, p[1] + 4), null);
            assertEquals(theirs.lat, mine.lat, 1e-6);
            assertEquals(theirs.lon, mine.lon, 1e-6);
        }
    }
}
