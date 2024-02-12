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

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants;
import org.esa.snap.runtime.Config;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

import static org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants.PROPERTY_NAME_USE_ZIP_ARCHIVE;
import static org.junit.Assert.*;

public class ZnapProductWriterReaderTest_persist_TiePointGeoCoding {

    private Product product;
    private List<Path> tempDirectories = new ArrayList<>();
    private ZnapProductWriter productWriter;
    private ZnapProductReader productReader;
    private Preferences preferences;
    private String oldValue;

    @Before
    public void setUp() throws Exception {
        final boolean containsAngles = true;
        final TiePointGrid lon = new TiePointGrid("lon", 3, 4, 0.5, 0.5, 5, 5, new float[]{
                170, 180, -170,
                168, 178, -172,
                166, 176, -174,
                164, 174, -176,
        }, containsAngles);
        final TiePointGrid lat = new TiePointGrid("lat", 3, 4, 0.5, 0.5, 5, 5, new float[]{
                50, 49, 48,
                49, 48, 47,
                48, 47, 46,
                47, 46, 45,
        });

        product = new Product("test", "type", 10, 15);
        final Date now = new Date();
        product.setStartTime(ProductData.UTC.create(now, 0));
        product.setEndTime(ProductData.UTC.create(new Date(now.getTime() + 4000), 0));
        product.addTiePointGrid(lon);
        product.addTiePointGrid(lat);

        product.setSceneGeoCoding(new TiePointGeoCoding(lat, lon));

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
        final Path tempDirectory = createTempDirectory();
        final Path rootDir = tempDirectory.resolve("test.znap");
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
        assertTrue(readIn.getSceneGeoCoding() instanceof TiePointGeoCoding);
        final TiePointGeoCoding srcGC = (TiePointGeoCoding) product.getSceneGeoCoding();
        final TiePointGeoCoding readGC = (TiePointGeoCoding) readIn.getSceneGeoCoding();
        assertEquals(srcGC.getLonGrid().getName(), readGC.getLonGrid().getName());
        assertEquals(srcGC.getLatGrid().getName(), readGC.getLatGrid().getName());
        assertEquals(srcGC.isCrossingMeridianAt180(), readGC.isCrossingMeridianAt180());
        assertEquals(srcGC.getNumApproximations(), readGC.getNumApproximations());
        final List<RasterDataNode> rasterDataNodes = readIn.getRasterDataNodes();
        for (int i = 0, rasterDataNodesSize = rasterDataNodes.size(); i < rasterDataNodesSize; i++) {
            RasterDataNode rdn = rasterDataNodes.get(i);
            final String name = rdn.getName();
            final String message = "RasterDataNode name: " + name + " at index " + i;
            assertSame(message, readGC, rdn.getGeoCoding());
        }
    }

    private Path createTempDirectory() throws IOException {
        final Path tempDirectory = Files.createTempDirectory("1111_out_" + getClass().getSimpleName());
        tempDirectories.add(tempDirectory);
        return tempDirectory;
    }
}