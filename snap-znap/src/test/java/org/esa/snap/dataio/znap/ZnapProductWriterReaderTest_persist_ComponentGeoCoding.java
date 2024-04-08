/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.dataio.znap;

import org.esa.snap.core.dataio.geocoding.ComponentFactory;
import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.ForwardCoding;
import org.esa.snap.core.dataio.geocoding.GeoChecks;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants;
import org.esa.snap.runtime.Config;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

public class ZnapProductWriterReaderTest_persist_ComponentGeoCoding {

    public static final String CRS_WKT =
            "GEOGCS[\"TestWGS8.4(DD)\", DATUM[\"TestWGS8.4\", SPHEROID[\"WGTest\", 637813.70, 29.8257223563]], PRIMEM[\"Greenwich\", 0.0], " +
            "UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geolon\", EAST], AXIS[\"Geolat\", NORTH]]";

    private final List<Path> tempDirectories = new ArrayList<>();
    private Product product;
    private ZnapProductWriter productWriter;
    private ZnapProductReader productReader;
    private Preferences preferences;
    private String oldValue;

    @Before
    public void setUp() throws Exception {
        final boolean containsAngles = true;
        final float[] fDataLon = {
                170, 180, -170,
                168, 178, -172,
                166, 176, -174,
                164, 174, -176
        };
        final float[] fDataLat = {
                50, 49, 48,
                49, 48, 47,
                48, 47, 46,
                47, 46, 45
        };
        final TiePointGrid lon = new TiePointGrid("lon", 3, 4, 0.5, 0.5, 5, 5, fDataLon, containsAngles);
        final TiePointGrid lat = new TiePointGrid("lat", 3, 4, 0.5, 0.5, 5, 5, fDataLat);

        product = new Product("test", "type", 11, 16);
        final Date now = new Date();
        product.setStartTime(ProductData.UTC.create(now, 0));
        product.setEndTime(ProductData.UTC.create(new Date(now.getTime() + 4000), 0));
        product.addTiePointGrid(lon);
        product.addTiePointGrid(lat);

        final double[] longitudes = IntStream.range(0, fDataLon.length).mapToDouble(i -> fDataLon[i]).toArray();
        final double[] latitudes = IntStream.range(0, fDataLat.length).mapToDouble(i -> fDataLat[i]).toArray();
        final GeoRaster geoRaster = new GeoRaster(longitudes, latitudes, lon.getName(), lat.getName(), 3, 4, 11, 16, 253, 0.5, 0.5, 5, 5);
        final ForwardCoding forwardCoding = ComponentFactory.getForward(TiePointBilinearForward.KEY);
        final InverseCoding inverseCoding = ComponentFactory.getInverse(TiePointInverse.KEY);
        final CoordinateReferenceSystem geoCRS = CRS.parseWKT(CRS_WKT);
        final ComponentGeoCoding sceneGeoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.ANTIMERIDIAN, geoCRS);
        sceneGeoCoding.initialize();
        product.setSceneGeoCoding(sceneGeoCoding);

        preferences = Config.instance("snap").load().preferences();
        oldValue = preferences.get(PROPERTY_NAME_USE_ZIP_ARCHIVE, "true");
        preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");

        final Properties properties = new Properties();
        properties.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");

        productWriter = (ZnapProductWriter) new ZnapProductWriterPlugIn().createWriterInstance();
        productWriter.setPreferencesForTestPurposesOnly(properties);
        productReader = (ZnapProductReader) new ZnapProductReaderPlugIn().createReaderInstance();
    }

    @After
    public void tearDown() {
        for (final Path tempDirectory : tempDirectories) {
            try {
                if (Files.exists(tempDirectory)) {
                    final List<Path> list = Files.walk(tempDirectory).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
                    for (Path path : list) {
                        Files.delete(path);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        tempDirectories.clear();

        preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, oldValue);
    }

    @Test
    public void writeAndRead() throws IOException {
        final Path tempDir = createTempDirectory();
        final Path rootDir = tempDir.resolve("test.znap");
        productWriter.writeProductNodes(product, rootDir);
        final Product readIn = productReader.readProductNodes(rootDir, null);

        assertNotNull(readIn);
        assertEquals(product.getSceneRasterWidth(), readIn.getSceneRasterWidth());
        assertEquals(product.getSceneRasterHeight(), readIn.getSceneRasterHeight());
        assertNotSame(product.getTiePointGrid("lon"), readIn.getTiePointGrid("lon"));
        assertNotSame(product.getTiePointGrid("lat"), readIn.getTiePointGrid("lat"));
        assertEquals(product.getTiePointGrid("lon").getDiscontinuity(), TiePointGrid.DISCONT_AT_180);
        assertEquals(product.getTiePointGrid("lon").getDiscontinuity(), readIn.getTiePointGrid("lon").getDiscontinuity());
        assertEquals(product.getTiePointGrid("lat").getDiscontinuity(), TiePointGrid.DISCONT_NONE);
        assertEquals(product.getTiePointGrid("lat").getDiscontinuity(), readIn.getTiePointGrid("lat").getDiscontinuity());

        final float[] srcLons = (float[]) product.getTiePointGrid("lon").getGridData().getElems();
        final float[] readLons = (float[]) readIn.getTiePointGrid("lon").getGridData().getElems();
        final float[] srcLats = (float[]) product.getTiePointGrid("lat").getGridData().getElems();
        final float[] readLats = (float[]) readIn.getTiePointGrid("lat").getGridData().getElems();

        assertArrayEquals(srcLons, readLons, Float.MIN_VALUE);
        assertArrayEquals(srcLats, readLats, Float.MIN_VALUE);
        assertNotSame(product.getSceneGeoCoding(), readIn.getSceneGeoCoding());
        assertThat(readIn.getSceneGeoCoding() instanceof ComponentGeoCoding).isTrue();

        final ComponentGeoCoding srcGC = (ComponentGeoCoding) product.getSceneGeoCoding();
        final ComponentGeoCoding readGC = (ComponentGeoCoding) readIn.getSceneGeoCoding();

        assertEquals(srcGC.isCrossingMeridianAt180(), readGC.isCrossingMeridianAt180());
        assertEquals(srcGC.getForwardCoding().getKey(), readGC.getForwardCoding().getKey());
        assertEquals(srcGC.getInverseCoding().getKey(), readGC.getInverseCoding().getKey());
        assertEquals(srcGC.getGeoChecks().name(), readGC.getGeoChecks().name());
        assertEquals(srcGC.getGeoCRS().toWKT(), readGC.getGeoCRS().toWKT());

        final GeoRaster srcGeoRaster = srcGC.getGeoRaster();
        final GeoRaster readGeoRaster = readGC.getGeoRaster();

        assertNotSame(srcGeoRaster.getLongitudes(), readGeoRaster.getLongitudes());
        assertArrayEquals(srcGeoRaster.getLongitudes(), readGeoRaster.getLongitudes(), Double.MIN_VALUE);

        assertNotSame(srcGeoRaster.getLatitudes(), readGeoRaster.getLatitudes());
        assertArrayEquals(srcGeoRaster.getLatitudes(), readGeoRaster.getLatitudes(), Double.MIN_VALUE);

        assertEquals(srcGeoRaster.getLonVariableName(), readGeoRaster.getLonVariableName());
        assertEquals(srcGeoRaster.getLatVariableName(), readGeoRaster.getLatVariableName());

        assertEquals(srcGeoRaster.getRasterWidth(), readGeoRaster.getRasterWidth());
        assertEquals(srcGeoRaster.getRasterHeight(), readGeoRaster.getRasterHeight());

        assertEquals(srcGeoRaster.getSceneWidth(), readGeoRaster.getSceneWidth());
        assertEquals(srcGeoRaster.getSceneHeight(), readGeoRaster.getSceneHeight());

        assertEquals(srcGeoRaster.getRasterResolutionInKm(), readGeoRaster.getRasterResolutionInKm(), Double.MIN_VALUE);

        assertEquals(srcGeoRaster.getOffsetX(), readGeoRaster.getOffsetX(), Double.MIN_VALUE);
        assertEquals(srcGeoRaster.getOffsetY(), readGeoRaster.getOffsetY(), Double.MIN_VALUE);

        assertEquals(srcGeoRaster.getSubsamplingX(), readGeoRaster.getSubsamplingX(), Double.MIN_VALUE);
        assertEquals(srcGeoRaster.getSubsamplingY(), readGeoRaster.getSubsamplingY(), Double.MIN_VALUE);
    }

    @Test
    public void writeAndRead_noGeoCoding_withBand() throws IOException {
//        product = new Product("name", "type");
//        Band band = new Band("band", ProductData.TYPE_INT32, 2 , 2);
//        band.setData(ProductData.createInstance(new int[]{12, 13, 14, 15}));
        product.setSceneGeoCoding(null);
        product.addBand("band", ProductData.TYPE_INT32);

        final Path tempDir = createTempDirectory();
        final Path rootDir = tempDir.resolve("test.znap");
        productWriter.writeProductNodes(product, rootDir);
        final Product readIn = productReader.readProductNodes(rootDir, null);

        assertNotNull(readIn);
        assertEquals(product.getSceneRasterWidth(), readIn.getSceneRasterWidth());
        assertEquals(product.getSceneRasterHeight(), readIn.getSceneRasterHeight());
        assertNull(readIn.getSceneGeoCoding());
    }

    private Path createTempDirectory() throws IOException {
        final Path tempDirectory = Files.createTempDirectory("1111_out_" + getClass().getSimpleName());
        tempDirectories.add(tempDirectory);
        return tempDirectory;
    }
}