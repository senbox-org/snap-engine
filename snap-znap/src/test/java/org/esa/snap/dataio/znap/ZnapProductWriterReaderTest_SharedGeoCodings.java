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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.znap.preferences.ZnapPreferencesConstants;
import org.esa.snap.runtime.Config;
import org.geotools.referencing.crs.DefaultGeographicCRS;
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

public class ZnapProductWriterReaderTest_SharedGeoCodings {

    private Product product;
    private final List<Path> tempDirectories = new ArrayList<>();
    private Properties properties;
    private String oldValue;
    private Preferences preferences;

    @Before
    public void setUp() throws Exception {
        preferences = Config.instance("snap").load().preferences();
        oldValue = preferences.get(PROPERTY_NAME_USE_ZIP_ARCHIVE, "true");
        preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");

        properties = new Properties();
        properties.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, "false");
        product = new Product("test", "type", 3, 4);
        final Date start = new Date();
        final Date end = new Date(start.getTime() + 4000);
        product.setStartTime(ProductData.UTC.create(start, 123));
        product.setEndTime(ProductData.UTC.create(end, 123));

        final Band b0 = product.addBand("b0", ProductData.TYPE_INT8);
        final Band b1 = product.addBand("b1", ProductData.TYPE_INT8);
        final Band b2 = product.addBand("b2", ProductData.TYPE_INT8);
        final Band b3 = product.addBand("b3", ProductData.TYPE_INT8);
        final Band b4 = product.addBand("b4", ProductData.TYPE_INT8);
        final Band b5 = product.addBand("b5", ProductData.TYPE_INT8);
        final Band b6 = product.addBand("b6", ProductData.TYPE_INT8);
        final Band b7 = product.addBand("b7", ProductData.TYPE_INT8);
        final Band b8 = product.addBand("b8", ProductData.TYPE_INT8);
        final Band b9 = product.addBand("b9", ProductData.TYPE_INT8);

        CrsGeoCoding sceneGeoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, 3, 4, 14.0, 15.0, 0.2, 0.1);
        CrsGeoCoding sharedGC = new CrsGeoCoding(DefaultGeographicCRS.WGS84, 3, 4, 13.0, 15.0, 0.2, 0.1);
        CrsGeoCoding single_1 = new CrsGeoCoding(DefaultGeographicCRS.WGS84, 3, 4, 12.0, 15.0, 0.2, 0.1);
        CrsGeoCoding single_2 = new CrsGeoCoding(DefaultGeographicCRS.WGS84, 3, 4, 11.0, 15.0, 0.2, 0.1);
        product.setSceneGeoCoding(sceneGeoCoding);
        b4.setGeoCoding(sharedGC);
        b5.setGeoCoding(sharedGC);
        b6.setGeoCoding(sharedGC);
        b7.setGeoCoding(single_1);
        b8.setGeoCoding(single_2);

        assertSame(b0.getGeoCoding(), sceneGeoCoding);
        assertSame(b1.getGeoCoding(), sceneGeoCoding);
        assertSame(b2.getGeoCoding(), sceneGeoCoding);
        assertSame(b3.getGeoCoding(), sceneGeoCoding);
        assertSame(b4.getGeoCoding(), sharedGC);
        assertSame(b5.getGeoCoding(), sharedGC);
        assertSame(b6.getGeoCoding(), sharedGC);
        assertSame(b7.getGeoCoding(), single_1);
        assertSame(b8.getGeoCoding(), single_2);
        assertSame(b9.getGeoCoding(), sceneGeoCoding);
    }

    @After
    public void tearDown() {
        preferences.put(PROPERTY_NAME_USE_ZIP_ARCHIVE, oldValue);

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
    }

    @Test
    public void writeAndRead() throws IOException {
        final Path tempPath = createTempDirectory();
        final Path rootPath = tempPath.resolve("test.znap");
        final ZnapProductWriter writer = new ZnapProductWriter(new ZnapProductWriterPlugIn());
        writer.setPreferencesForTestPurposesOnly(properties);
        writer.writeProductNodes(product, rootPath);

        final ZnapProductReader reader = new ZnapProductReader(new ZnapProductReaderPlugIn());
        final Product product = reader.readProductNodes(rootPath, null);

        assertNotNull(product);
        final GeoCoding sceneGeoCoding = product.getSceneGeoCoding();
        final GeoCoding sharedGeoCoding = product.getBand("b4").getGeoCoding();
        assertNotNull(sceneGeoCoding);
        assertNotNull(sharedGeoCoding);
        assertNotEquals(sharedGeoCoding, sceneGeoCoding);

        assertSame(sceneGeoCoding, product.getBand("b0").getGeoCoding());
        assertSame(sceneGeoCoding, product.getBand("b1").getGeoCoding());
        assertSame(sceneGeoCoding, product.getBand("b2").getGeoCoding());
        assertSame(sceneGeoCoding, product.getBand("b3").getGeoCoding());
        assertSame(sceneGeoCoding, product.getBand("b9").getGeoCoding());

        assertSame(sharedGeoCoding, product.getBand("b5").getGeoCoding());
        assertSame(sharedGeoCoding, product.getBand("b6").getGeoCoding());

        assertNotEquals(sceneGeoCoding, product.getBand("b7").getGeoCoding());
        assertNotEquals(sharedGeoCoding, product.getBand("b7").getGeoCoding());
        assertNotEquals(sceneGeoCoding, product.getBand("b8").getGeoCoding());
        assertNotEquals(sharedGeoCoding, product.getBand("b8").getGeoCoding());

        assertNotEquals(product.getBand("b7").getGeoCoding(), product.getBand("b8").getGeoCoding());
    }

    @Test
    public void testThatTheGeneratedOutputsAreEqual() throws IOException {
        final Path tempPath = createTempDirectory();
        final Path rootPath = tempPath.resolve("test.znap");

        final ZnapProductWriter writer = new ZnapProductWriter(new ZnapProductWriterPlugIn());
        writer.setPreferencesForTestPurposesOnly(properties);
        writer.writeProductNodes(product, rootPath);

        final ZnapProductReader reader = new ZnapProductReader(new ZnapProductReaderPlugIn());
        final Product product = reader.readProductNodes(rootPath, null);

        final Path secondRoot = tempPath.resolve("secondTest.znap");
        final ZnapProductWriter secondWriter = new ZnapProductWriter(new ZnapProductWriterPlugIn());
        secondWriter.setPreferencesForTestPurposesOnly(properties);
        secondWriter.writeProductNodes(product, secondRoot);

        final List<Path> firstList = Files.walk(rootPath).filter(path -> path.getFileName().toString().equals(".zattrs")).collect(Collectors.toList());
        final List<Path> secondList = Files.walk(secondRoot).filter(path -> path.getFileName().toString().equals(".zattrs")).collect(Collectors.toList());
        assertEquals(firstList.size(), secondList.size());
        for (int i = 0; i < firstList.size(); i++) {
            Path firstPath = firstList.get(i);
            Path secondPath = secondList.get(i);
            final List<String> firstLines = Files.readAllLines(firstPath);
            final List<String> secondLines = Files.readAllLines(secondPath);
            assertEquals(firstLines.size(), secondLines.size());
            for (int j = 0; j < firstLines.size(); j++) {
                String firstLine = firstLines.get(j);
                String secondLine = secondLines.get(j);
                assertEquals(firstLine, secondLine);
            }
        }
    }

    private Path createTempDirectory() throws IOException {
        final Path tempDirectory = Files.createTempDirectory("1111_out_" + getClass().getSimpleName());
        tempDirectories.add(tempDirectory);
        return tempDirectory;
    }
}