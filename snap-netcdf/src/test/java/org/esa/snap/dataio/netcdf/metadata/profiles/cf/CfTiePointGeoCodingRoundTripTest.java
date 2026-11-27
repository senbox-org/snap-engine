package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Writing a product whose scene geo-coding is a {@link TiePointGeoCoding} (e.g. Sentinel-1 SLC/GRD) to NetCDF4-CF
 * must keep the CF lat/lon coordinate variables and must be readable again with a working geo-coding.
 */
public class CfTiePointGeoCodingRoundTripTest {

    private static final int WIDTH = 40;
    private static final int HEIGHT = 30;
    private static final int GRID_W = 5;
    private static final int GRID_H = 4;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Product product;
    private File file;

    @AfterClass
    public static void afterClass() {
        // unload the netcdf native libs again, see GloballyShiftedDataTest
        System.gc();
    }

    @Before
    public void setUp() throws IOException {
        product = createTiePointGeoCodedProduct();
        file = new File(tempFolder.getRoot(), "tpg_geocoding.nc");
        ProductIO.writeProduct(product, file, "NetCDF4-CF", false);
    }

    @After
    public void tearDown() {
        product.dispose();
    }

    @Test
    @STTM("SNAP-4381")
    public void testCfLatLonCoordinateVariablesAreWritten() throws IOException {
        try (NetcdfFile ncFile = NetcdfFiles.open(file.getPath())) {
            assertCoordinateVariable(ncFile, "lat", "latitude", "degrees_north");
            assertCoordinateVariable(ncFile, "lon", "longitude", "degrees_east");
        }
    }

    @Test
    @STTM("SNAP-4381")
    public void testCoordinatesAttributeReferencesExistingVariables() throws IOException {
        try (NetcdfFile ncFile = NetcdfFiles.open(file.getPath())) {
            for (Variable variable : ncFile.getVariables()) {
                final Attribute coordinates = variable.findAttribute("coordinates");
                if (coordinates == null) {
                    continue;
                }
                for (String name : coordinates.getStringValue().trim().split("\\s+")) {
                    assertNotNull("variable '" + variable.getShortName() + "' references missing coordinate variable '" + name + "'",
                                  ncFile.findVariable(name));
                }
            }
        }
    }

    @Test
    @STTM("SNAP-4381")
    public void testReadBackHasEquivalentGeoCoding() throws IOException {
        final Product readProduct = ProductIO.readProduct(file);
        try {
            assertNotNull(readProduct);
            final GeoCoding geoCoding = readProduct.getSceneGeoCoding();
            assertNotNull(geoCoding);

            final GeoCoding expected = product.getSceneGeoCoding();
            // pixel centres: the read-back geo-coding may be pixel-based (no sub-pixel interpolation)
            final PixelPos[] pixels = {
                    new PixelPos(0.5, 0.5),
                    new PixelPos(WIDTH - 0.5, 0.5),
                    new PixelPos(WIDTH / 2 + 0.5, HEIGHT / 2 + 0.5),
                    new PixelPos(WIDTH - 0.5, HEIGHT - 0.5)
            };
            for (PixelPos pixel : pixels) {
                final GeoPos expectedPos = expected.getGeoPos(pixel, null);
                final GeoPos actualPos = geoCoding.getGeoPos(pixel, null);
                assertEquals("lat at " + pixel, expectedPos.lat, actualPos.lat, 1e-4);
                assertEquals("lon at " + pixel, expectedPos.lon, actualPos.lon, 1e-4);
            }
        } finally {
            if (readProduct != null) {
                readProduct.dispose();
            }
        }
    }

    private static void assertCoordinateVariable(NetcdfFile ncFile, String name, String standardName, String units) {
        final Variable variable = ncFile.findVariable(name);
        assertNotNull("CF coordinate variable '" + name + "' missing", variable);
        assertEquals(standardName, variable.findAttribute("standard_name").getStringValue());
        assertEquals(units, variable.findAttribute("units").getStringValue());
    }

    // mimics a Sentinel-1 SLC/GRD: geo-coding from 'latitude'/'longitude' tie-point grids
    private static Product createTiePointGeoCodedProduct() {
        final Product product = new Product("s1_like", "SLC", WIDTH, HEIGHT);
        final Band band = product.addBand("Amplitude_VV", ProductData.TYPE_FLOAT32);
        final float[] data = new float[WIDTH * HEIGHT];
        for (int i = 0; i < data.length; i++) {
            data[i] = i;
        }
        band.setRasterData(ProductData.createInstance(data));

        final float[] lats = new float[GRID_W * GRID_H];
        final float[] lons = new float[GRID_W * GRID_H];
        for (int y = 0; y < GRID_H; y++) {
            for (int x = 0; x < GRID_W; x++) {
                lats[y * GRID_W + x] = 10.0f - 0.1f * y - 0.01f * x;
                lons[y * GRID_W + x] = 20.0f + 0.1f * x + 0.02f * y;
            }
        }
        final double subX = (WIDTH - 1.0) / (GRID_W - 1);
        final double subY = (HEIGHT - 1.0) / (GRID_H - 1);
        final TiePointGrid latGrid = new TiePointGrid("latitude", GRID_W, GRID_H, 0.5, 0.5, subX, subY, lats);
        final TiePointGrid lonGrid = new TiePointGrid("longitude", GRID_W, GRID_H, 0.5, 0.5, subX, subY, lons);
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));
        return product;
    }
}
