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

import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.GeoChecks;
import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import java.util.stream.Collectors;

import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_FORWARD_CODING_KEY;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_GEO_CHECKS;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_GEO_CRS;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_INVERSE_CODING_KEY;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_LAT_VARIABLE_NAME;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_LON_VARIABLE_NAME;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_OFFSET_X;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_OFFSET_Y;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_RASTER_RESOLUTION_KM;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_SUBSAMPLING_X;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter.NAME_SUBSAMPLING_Y;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;


public class ZarrProductReaderTest_createGeocoding_ComponentGeoCoding {

    public static final String geoCRS_WKT =
            "GEOGCS[\"TestWGS8.4(DD)\", DATUM[\"TestWGS8.4\", SPHEROID[\"WGTest\", 637813.70, 29.8257223563]], PRIMEM[\"Greenwich\", 0.0], " +
            "UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geolon\", EAST], AXIS[\"Geolat\", NORTH]]";

    private Product product;
    private final List<Path> tempDirectories = new ArrayList<>();
    private ZarrProductReader productReader;
    private HashMap<String, Object> gcAttribs;
    private ByteArrayOutputStream logOutput;
    private StreamHandler handler;
    private HashMap<Object, Object> theGeoCodingMap;

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

        final double[] dDataLon = new double[11 * 16];
        final double[] dDataLat = new double[11 * 16];
        double lonStart = 170;
        double latStart = 50;
        for (int y = 0; y < 16; y++) {
            lonStart -= y * 0.5;
            latStart -= y * 0.15;
            for (int x = 0; x < 11; x++) {
                final int idx = y * 11 + x;
                final double lonVal = lonStart + x * 2;
                dDataLon[idx] = lonVal > 180 ? lonVal - 360 : lonVal;
                dDataLat[idx] = latStart - x * 0.5;
            }
        }

        product = new Product("TestProduct", "type", 11, 16);
        final Date now = new Date();
        product.setStartTime(ProductData.UTC.create(now, 0));
        product.setEndTime(ProductData.UTC.create(new Date(now.getTime() + 4000), 0));
        product.addTiePointGrid(lon);
        product.addTiePointGrid(lat);
        final Band lonBand = product.addBand("Long", ProductData.TYPE_FLOAT64);
        final Band latBand = product.addBand("Lati", ProductData.TYPE_FLOAT64);
        lonBand.setDataElems(dDataLon);
        latBand.setDataElems(dDataLat);

        productReader = (ZarrProductReader) new ZarrProductReaderPlugIn().createReaderInstance();

        gcAttribs = new HashMap<>();

        theGeoCodingMap = new HashMap<>();
        gcAttribs.put("ComponentGeoCoding", theGeoCodingMap);
        theGeoCodingMap.put(NAME_FORWARD_CODING_KEY, TiePointBilinearForward.KEY);
        theGeoCodingMap.put(NAME_INVERSE_CODING_KEY, TiePointInverse.KEY);
        theGeoCodingMap.put(NAME_GEO_CHECKS, GeoChecks.ANTIMERIDIAN.name());
        theGeoCodingMap.put(NAME_GEO_CRS, geoCRS_WKT);
        theGeoCodingMap.put(NAME_LON_VARIABLE_NAME, "lon");
        theGeoCodingMap.put(NAME_LAT_VARIABLE_NAME, "lat");
        theGeoCodingMap.put(NAME_RASTER_RESOLUTION_KM, 234.0);
        theGeoCodingMap.put(NAME_OFFSET_X, 0.5);
        theGeoCodingMap.put(NAME_OFFSET_Y, 0.5);
        theGeoCodingMap.put(NAME_SUBSAMPLING_X, 5.0);
        theGeoCodingMap.put(NAME_SUBSAMPLING_Y, 5.0);

        logOutput = new ByteArrayOutputStream();
        handler = new StreamHandler(logOutput, new SimpleFormatter());
        handler.setLevel(Level.FINE);
        SystemUtils.LOG.addHandler(handler);
        SystemUtils.LOG.setLevel(Level.FINE);
    }

    @After
    public void tearDown() {
        SystemUtils.LOG.removeHandler(handler);
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
    public void createGeoCoding__allIsFineWithTiePoint() {
        //execution
        final GeoCoding geoCoding = productReader.createGeoCoding(product, gcAttribs);

        //verification
        assertThat(getLogOutput().trim(), endsWith("Try to instantiate geo coding: ComponentGeoCoding for TestProduct"));

        assertThat(geoCoding, is(notNullValue()));
        assertThat(geoCoding, is(instanceOf(ComponentGeoCoding.class)));
        final ComponentGeoCoding componentGeoCoding = (ComponentGeoCoding) geoCoding;
        assertThat(componentGeoCoding.isCrossingMeridianAt180(), is(true));
        assertThat(componentGeoCoding.getForwardCoding().getKey(), is(TiePointBilinearForward.KEY));
        assertThat(componentGeoCoding.getInverseCoding().getKey(), is(TiePointInverse.KEY));
    }

    @Test
    public void createGeoCoding__allIsFineWithBands() {
        //preparation
        theGeoCodingMap.put(NAME_FORWARD_CODING_KEY, PixelForward.KEY);
        theGeoCodingMap.put(NAME_INVERSE_CODING_KEY, PixelQuadTreeInverse.KEY);
        theGeoCodingMap.put(NAME_LON_VARIABLE_NAME, "Long");
        theGeoCodingMap.put(NAME_LAT_VARIABLE_NAME, "Lati");

        //execution
        final GeoCoding geoCoding = productReader.createGeoCoding(product, gcAttribs);

        //verification
        assertThat(getLogOutput().trim(), endsWith("Try to instantiate geo coding: ComponentGeoCoding for TestProduct"));

        assertThat(geoCoding, is(notNullValue()));
        assertThat(geoCoding, is(instanceOf(ComponentGeoCoding.class)));
        final ComponentGeoCoding componentGeoCoding = (ComponentGeoCoding) geoCoding;
        assertThat(componentGeoCoding.isCrossingMeridianAt180(), is(true));
        assertThat(componentGeoCoding.getForwardCoding().getKey(), is(PixelForward.KEY));
        assertThat(componentGeoCoding.getInverseCoding().getKey(), is(PixelQuadTreeInverse.KEY));
    }

    @Test
    public void createGeoCoding_missingLonVariable() {
        //preparation
        product.removeTiePointGrid(product.getTiePointGrid("lon"));

        //execution
        final GeoCoding geoCoding = productReader.createGeoCoding(product, gcAttribs);

        //verification
        assertThat(geoCoding, is(nullValue()));

        final ArrayList<String> orderedExpectations = new ArrayList<>();
        orderedExpectations.add(productReader.getClass().getName());
        orderedExpectations.add("createGeoCoding");
        orderedExpectations.add("Try to instantiate geo coding: ComponentGeoCoding for TestProduct");
        orderedExpectations.add("org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter decode");
        orderedExpectations.add("Unable to find expected longitude raster 'lon' in product");
        orderedExpectations.add("org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter decode");
        orderedExpectations.add("Unable to create ComponentGeoCoding");
        orderedExpectations.add(productReader.getClass().getName());
        orderedExpectations.add("Unable to instantiate geo coding: ComponentGeoCoding");


        assertThat(getLogOutput(), stringContainsInOrder(orderedExpectations));
    }

    @Test
    public void createGeoCoding_missingLatVariable() {
        //preparation
        product.removeTiePointGrid(product.getTiePointGrid("lat"));

        //execution
        final GeoCoding geoCoding = productReader.createGeoCoding(product, gcAttribs);

        //verification
        assertThat(geoCoding, is(nullValue()));

        final ArrayList<String> orderedExpectations = new ArrayList<>();
        orderedExpectations.add(productReader.getClass().getName());
        orderedExpectations.add("createGeoCoding");
        orderedExpectations.add("Try to instantiate geo coding: ComponentGeoCoding for TestProduct");
        orderedExpectations.add("org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter decode");
        orderedExpectations.add("Unable to find expected latitude raster 'lat' in product");
        orderedExpectations.add("org.esa.snap.core.dataio.geocoding.ComponentGeoCodingPersistenceConverter decode");
        orderedExpectations.add("Unable to create ComponentGeoCoding");
        orderedExpectations.add(productReader.getClass().getName());
        orderedExpectations.add("Unable to instantiate geo coding: ComponentGeoCoding");

        assertThat(getLogOutput(), stringContainsInOrder(orderedExpectations));
    }

    public String getLogOutput() {
        handler.close();
        return logOutput.toString();
    }
}