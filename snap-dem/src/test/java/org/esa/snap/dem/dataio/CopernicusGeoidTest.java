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
package org.esa.snap.dem.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.dem.dataio.copernicus.CopernicusDirectElevationTile;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationTile;
import org.esa.snap.dem.dataio.copernicus.CopernicusTileSource;
import org.esa.snap.dem.dataio.copernicus.copernicus30m.Copernicus30mElevationModel;
import org.esa.snap.dem.dataio.copernicus.copernicus30m.Copernicus30mElevationModelDescriptor;
import org.esa.snap.dem.dataio.copernicus.copernicus90m.Copernicus90mElevationModelDescriptor;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;
import org.esa.snap.runtime.Config;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.esa.snap.dem.dataio.EarthGravitationalModel2008.NUM_LATS;
import static org.esa.snap.dem.dataio.EarthGravitationalModel2008.NUM_LONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * The Copernicus DEM must be converted with EGM2008, and its no-data (sea) pixels filled with EGM2008 too.
 * The EGM2008 grid here is a synthetic constant 12.34 m, which no EGM96 value matches everywhere, so these tests
 * fail if any of those paths goes back to EGM96, with or without the real grid or network access.
 */
public class CopernicusGeoidTest {

    private static final float N = 12.34f;
    private static final String EGM_KEY = "snap.useDEMGravitationalModel";

    @ClassRule
    public static final TemporaryFolder tmp = new TemporaryFolder();

    private static File constantZip() throws IOException {
        final File zip = new File(tmp.getRoot(), "constant.zip");
        if (!zip.isFile()) {
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
                out.putNextEntry(new ZipEntry(EarthGravitationalModel2008.NAME));
                final DataOutputStream data = new DataOutputStream(new BufferedOutputStream(out, 1 << 16));
                for (int i = 0; i < NUM_LATS * NUM_LONS; i++) {
                    data.writeShort(Math.round(N * 100));
                }
                data.flush();
                out.closeEntry();
            }
        }
        return zip;
    }

    private String savedEgmPref;

    /** Points the EGM2008 singleton at the synthetic constant grid (fetched on first use). */
    private static void useConstantGrid() throws IOException {
        final File zip = constantZip();
        EarthGravitationalModel2008.resetForTests(tmp.newFolder(), target -> {
            Files.copy(zip.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return target;
        });
    }

    @Before
    public void setUp() throws IOException {
        useConstantGrid();
        final Preferences prefs = Config.instance().preferences();
        savedEgmPref = prefs.get(EGM_KEY, null);
        prefs.putBoolean(EGM_KEY, true);
    }

    @After
    public void restore() {
        EarthGravitationalModel2008.resetForTests(null, null);
        final Preferences prefs = Config.instance().preferences();
        if (savedEgmPref == null) {
            prefs.remove(EGM_KEY);
        } else {
            prefs.put(EGM_KEY, savedEgmPref);
        }
    }

    @Test
    public void testCopernicusDescriptorsUseEGM2008() throws IOException {
        final GeoidModel egm2008 = EarthGravitationalModel2008.instance();
        assertSame(egm2008, DEMFactory.getGeoidModel(new Copernicus30mElevationModelDescriptor()));
        assertSame(egm2008, DEMFactory.getGeoidModel(new Copernicus90mElevationModelDescriptor()));
        assertEquals(N, DEMFactory.getGeoidModel(new Copernicus30mElevationModelDescriptor()).getEGM(47.3, 8.5), 1e-4f);
    }

    @Test
    public void testOtherDEMsUseEGM96() throws IOException {
        final EarthGravitationalModel96 egm96;
        try {
            egm96 = EarthGravitationalModel96.instance();
        } catch (IOException e) {
            Assume.assumeNoException("EGM96 auxdata unavailable", e);
            return;
        }
        assertSame(egm96, DEMFactory.getGeoidModel(new FakeDem(new FakeDescriptor()).getDescriptor()));
        assertSame(egm96, DEMFactory.getGeoidModel((ElevationModel) null));
    }

    /** The direct-reader tile (GLO-30 and GLO-90) adds EGM2008 to every valid height. */
    @Test
    public void testDirectTileAddsEGM2008() throws Exception {
        final CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(
                new FakeDem(new Copernicus30mElevationModelDescriptor()), new ConstantSource(64, 64, 100f),
                64, 64, 10, 40);
        assertEquals(100f + N, tile.getSample(5, 7), 1e-3f);
        assertEquals(100f + N, tile.getSample(63, 63), 1e-3f);
        tile.dispose();
    }

    /** DEMFactory fills Copernicus no-data (sea) pixels with the EGM2008 height, matching the land. */
    @Test
    public void testSeaFillUsesEGM2008ForCopernicus() throws Exception {
        final FakeDem dem = new FakeDem(new Copernicus30mElevationModelDescriptor());
        final Rectangle rect = new Rectangle(0, 0, 4, 3);
        final ProductData data = ProductData.createInstance(ProductData.TYPE_FLOAT32, 12);
        DEMFactory.fillElevationData(dem, 0.0, (x, y, geoPos) -> geoPos.setLocation(10 + y * 0.01, 20 + x * 0.01),
                rect, data, false, ProgressMonitor.NULL);
        final float[] values = (float[]) data.getElems();
        final float[] expected = new float[12];
        Arrays.fill(expected, N);
        for (int i = 0; i < 12; i++) {
            assertEquals("pixel " + i, expected[i], values[i], 1e-3f);
        }
    }

    /** The sea fill path the operators use (getLocalDEM, with its one-pixel border) uses EGM2008 too. */
    @Test
    public void testLocalDEMSeaFillUsesEGM2008ForCopernicus() throws Exception {
        final Product product = geocodedProduct(4, 3, 100f);
        final TileGeoreferencing tileGeoRef = new TileGeoreferencing(product, -1, -1, 6, 5);
        final double[][] localDEM = new double[5][6];
        final boolean valid = DEMFactory.getLocalDEM(new FakeDem(new Copernicus30mElevationModelDescriptor()), 0.0,
                "BILINEAR_INTERPOLATION", tileGeoRef, 0, 0, 4, 3, product, false, localDEM);
        assertTrue(valid);
        for (double[] row : localDEM) {
            for (double v : row) {
                assertEquals(N, v, 1e-3);
            }
        }
    }

    /** The reprojected-tile path (CopernicusElevationTile) adds EGM2008 to the heights it reads. */
    @Test
    public void testReprojectedTileAddsEGM2008() throws Exception {
        final CopernicusElevationTile tile = new CopernicusElevationTile(
                new FakeDem(new Copernicus30mElevationModelDescriptor()), geocodedProduct(4, 3, 100f));
        assertEquals(100f + N, tile.getSample(2, 1), 1e-3f);
    }

    /**
     * The geoid is chosen once per DEM model. If EGM2008 cannot be loaded when the model first needs it, that model
     * keeps EGM96 for all its tiles and its sea fill, even after a later retry succeeds, so one product never mixes
     * geoids; a model created after the retry uses EGM2008.
     */
    @Test
    public void testGeoidIsChosenOncePerModel() throws Exception {
        final EarthGravitationalModel96 egm96;
        try {
            egm96 = EarthGravitationalModel96.instance();
        } catch (IOException e) {
            Assume.assumeNoException("EGM96 auxdata unavailable", e);
            return;
        }
        EarthGravitationalModel2008.resetForTests(tmp.newFolder(), target -> {
            throw new IOException("offline");
        });
        final Copernicus30mElevationModel before = newCopernicusModel();
        assertSame(egm96, before.getGeoidModel());

        // the EGM2008 grid becomes available (a successful retry)
        useConstantGrid();
        assertTrue(EarthGravitationalModel2008.instanceOrEGM96() instanceof EarthGravitationalModel2008);

        assertSame("the earlier model keeps its geoid", egm96, before.getGeoidModel());
        assertSame("its sea fill too", egm96, DEMFactory.getGeoidModel(before));
        final CopernicusDirectElevationTile tile = new CopernicusDirectElevationTile(
                before, new ConstantSource(64, 64, 100f), 64, 64, 10, 40);
        final double lat = 90.0 - 40 - 7 / 64.0;
        final double lon = 10 - 180.0 + 5 / 64.0;
        assertEquals("and its tiles", 100f + egm96.getEGM(lat, lon), tile.getSample(5, 7), 1e-3f);
        tile.dispose();

        final Copernicus30mElevationModel after = newCopernicusModel();
        assertEquals(N, after.getGeoidModel().getEGM(lat, lon), 1e-4f);
        assertSame(after.getGeoidModel(), DEMFactory.getGeoidModel(after));
    }

    private static Copernicus30mElevationModel newCopernicusModel() {
        return new Copernicus30mElevationModel(new Copernicus30mElevationModelDescriptor(),
                Resampling.BILINEAR_INTERPOLATION);
    }

    /** A constant-height product on a 0.01 degree WGS84 grid near 47N 8E. */
    private static Product geocodedProduct(final int w, final int h, final float height) throws Exception {
        final Product p = new Product("tile", "DEM", w, h);
        final float[] data = new float[w * h];
        Arrays.fill(data, height);
        final Band b = new Band("elevation", ProductData.TYPE_FLOAT32, w, h);
        b.setRasterData(ProductData.createInstance(data));
        p.addBand(b);
        p.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, w, h, 8.0, 47.0, 0.01, 0.01));
        return p;
    }

    private static final class ConstantSource implements CopernicusTileSource {
        private final int width;
        private final int height;
        private final float value;

        ConstantSource(final int width, final int height, final float value) {
            this.width = width;
            this.height = height;
            this.value = value;
        }

        @Override public int getWidth() { return width; }
        @Override public int getHeight() { return height; }

        @Override
        public float[] readRows(final int y, final int rowCount) {
            final float[] rows = new float[rowCount * width];
            Arrays.fill(rows, value);
            return rows;
        }

        @Override public void close() { }
    }

    /** Every pixel is no-data (0, the Copernicus no-data value). */
    private static final class FakeDem implements ElevationModel {
        private final ElevationModelDescriptor descriptor;

        FakeDem(final ElevationModelDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override public ElevationModelDescriptor getDescriptor() { return descriptor; }
        @Override public double getElevation(GeoPos g) { return 0.0; }
        @Override public PixelPos getIndex(GeoPos g) { return new PixelPos(); }
        @Override public GeoPos getGeoPos(PixelPos p) { return new GeoPos(); }
        @Override public double getSample(double x, double y) { return 0.0; }
        @Override public boolean getSamples(int[] x, int[] y, double[][] s) { return false; }
        @Override public Resampling getResampling() { return Resampling.NEAREST_NEIGHBOUR; }
        @Override public void dispose() { }
    }

    private static final class FakeDescriptor implements ElevationModelDescriptor {
        @Override public String getName() { return "Fake"; }
        @Override public float getNoDataValue() { return 0.0f; }
        @Override public int getRasterWidth() { return 3600; }
        @Override public int getRasterHeight() { return 3600; }
        @Override public int getTileWidthInDegrees() { return 1; }
        @Override public int getTileWidth() { return 3600; }
        @Override public int getNumXTiles() { return 360; }
        @Override public int getNumYTiles() { return 180; }
        @Override public ElevationModel createDem(Resampling r) { return null; }
        @Override public boolean canBeDownloaded() { return false; }
        @Override public File getDemInstallDir() { return new File("."); }
    }
}
