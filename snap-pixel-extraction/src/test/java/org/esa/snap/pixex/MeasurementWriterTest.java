/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.pixex;

import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.test.LongTestRunner;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.measurement.Measurement;
import org.esa.snap.measurement.writer.MeasurementWriter;
import org.esa.snap.pixex.output.DefaultFormatStrategy;
import org.esa.snap.pixex.output.PixExMeasurementFactory;
import org.esa.snap.pixex.output.PixExProductRegistry;
import org.esa.snap.pixex.output.PixExRasterNamesFactory;
import org.esa.snap.pixex.output.TargetWriterFactoryAndMap;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Locale;
import java.util.Scanner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
@RunWith(LongTestRunner.class)
public class MeasurementWriterTest {

    private Path outputDir;
    private MeasurementWriter writer;

    @Before
    public void setup() {
        final Path tmpDir = IOHelper.createATempDir();
        outputDir = tmpDir.resolve(getClass().getSimpleName());
        try {
            Files.createDirectory(outputDir);
        } catch (IOException e) {
            fail("unable to create test directory");
        }
    }

    @After
    public void tearDown() {
        if (writer != null) {
            writer.close();
        }
        FileUtils.deleteTree(outputDir);
    }

    @Test
    public void testFileCreation() throws Exception {
        final int windowSize = 1;
        final String filenamePrefix = "testFileCreation";
        final String expression = "";
        final boolean exportExpressionResult = true;

        writer = createMeasurementWriter(windowSize, filenamePrefix, expression, exportExpressionResult);

        Path productMapFile = outputDir.resolve("testFileCreation_productIdMap.txt");
        Path t1CoordFile = outputDir.resolve("testFileCreation_T1_measurements.txt");
        Path t2CoordFile = outputDir.resolve("testFileCreation_T2_measurements.txt");

        assertEquals(0, IOHelper.listFiles(productMapFile).size());
        assertFalse(Files.exists(productMapFile));
        assertFalse(Files.exists(t1CoordFile));
        assertFalse(Files.exists(t2CoordFile));

        final Product p1 = createTestProduct("N1", "T1", new String[0], 360, 180);
        writeRegion(writer, p1, 1, windowSize);

        assertEquals(2, IOHelper.listFiles(outputDir).size());
        assertTrue(Files.exists(productMapFile));
        assertTrue(Files.exists(t1CoordFile));

        final Product p2 = createTestProduct("N2", "T2", new String[0], 360, 180);
        writeRegion(writer, p2, 1, windowSize);

        assertEquals(3, IOHelper.listFiles(outputDir).size());
        assertTrue(Files.exists(productMapFile));
        assertTrue(Files.exists(t1CoordFile));
        assertTrue(Files.exists(t2CoordFile));
    }

    @Test
    public void testWritingMeasurements() throws Exception {
        final int windowSize = 1;
        final String filenamePrefix = "testWritingMeasurements";

        writer = createMeasurementWriter(windowSize, false, filenamePrefix, null, true);

        final String[] varNames = {"abc", "def"};
        final Product testProduct1 = createTestProduct("N1", "T1", varNames, 360, 180);
        final Product testProduct2 = createTestProduct("N2", "T1", varNames, 360, 180);
        final Product testProduct3 = createTestProduct("N3", "T2", varNames, 360, 180);
        writeRegion(writer, testProduct1, 1, windowSize);
        writeRegion(writer, testProduct1, 2, windowSize);
        writeRegion(writer, testProduct2, 3, windowSize);
        writeRegion(writer, testProduct3, 4, windowSize);

        Path t1CoordFile = outputDir.resolve("testWritingMeasurements_T1_measurements.txt");

        try (BufferedReader reader = Files.newBufferedReader(t1CoordFile)) {
            skipLines(reader, 7);    //skip file header and table header lines
            assertMeasurementEquals(getMeasurement(1, 0), reader.readLine(), false);
            assertMeasurementEquals(getMeasurement(2, 0), reader.readLine(), false);
            assertMeasurementEquals(getMeasurement(3, 1), reader.readLine(), false);
        }

        Path t2CoordFile = outputDir.resolve("testWritingMeasurements_T2_measurements.txt");
        try (BufferedReader reader = Files.newBufferedReader(t2CoordFile)) {
            skipLines(reader, 7);    //skip file header and table header lines
            assertMeasurementEquals(getMeasurement(4, 2), reader.readLine(), false);
        }
    }

    @Test
    public void testWritingMeasurementsWithExpression() throws Exception {
        final boolean withExpression = true;
        final int windowSize = 1;
        final String filenamePrefix = "testWritingMeasurementsWithExpression";

        writer = createMeasurementWriter(windowSize, false, filenamePrefix, "Is Valid",
                withExpression);

        final String[] varNames = {"abc", "def"};
        final Product testProduct = createTestProduct("N1", "T1", varNames, 360, 180);
        writeRegion(writer, testProduct, 1, windowSize);
        writeRegion(writer, testProduct, 2, windowSize);

        Path t1CoordFile = outputDir.resolve("testWritingMeasurementsWithExpression_T1_measurements.txt");
        try (BufferedReader reader = Files.newBufferedReader(t1CoordFile)) {
            skipLines(reader, 8);    //skip file header and table header lines
            assertMeasurementEquals(getMeasurement(1, 0), reader.readLine(), withExpression);
            assertMeasurementEquals(getMeasurement(2, 0), reader.readLine(), withExpression);
        }
    }

    @Test
    public void testWritingProductMap() throws Exception {
        final int windowSize = 3;
        final String filenamePrefix = "testWritingProductMap";
        final String expression = null;
        final boolean exportExpressionResult = true;

        writer = createMeasurementWriter(windowSize, filenamePrefix, expression,
                exportExpressionResult);

        final String[] varNames = {"abc", "def"};
        final Product testProduct = createTestProduct("N1", "T1", varNames, 360, 180);
        testProduct.setFileLocation(IOHelper.createATempDir().resolve("somewhere/on/disk.txt"));
        writeRegion(writer, testProduct, 1, windowSize);

        Path t1CoordFile = outputDir.resolve("testWritingProductMap_productIdMap.txt");
        try (BufferedReader reader = Files.newBufferedReader(t1CoordFile)) {
            skipLines(reader, 2);    //skip file header and table header lines
            String line = reader.readLine();
            assertNotNull("Nothing written to ProductMap.", line);
            assertFalse(line.isEmpty());
            assertProductMapEntryEquals(0, "T1", testProduct.getFileLocation().toAbsolutePath().toString(), line);

            writeRegion(writer, testProduct, 2, windowSize);
            assertNull("No new entry expected.", reader.readLine());

            final Product testProduct2 = createTestProduct("N2", "T1", varNames, 360, 180);
            testProduct2.setFileLocation(IOHelper.createATempDir().resolve("somewhere/on/disk2.txt"));
            writeRegion(writer, testProduct2, 1, windowSize);
            final Product testProduct3 = createTestProduct("N2", "T2", varNames, 360, 180);
            testProduct3.setFileLocation(IOHelper.createATempDir().resolve("somewhere/on/disk3.txt"));
            writeRegion(writer, testProduct3, 1, windowSize);
            writer.close();

            line = reader.readLine();
            assertNotNull("Nothing written to ProductMap.", line);
            assertFalse(line.isEmpty());
            assertProductMapEntryEquals(1, "T1", testProduct2.getFileLocation().toAbsolutePath().toString(), line);
            line = reader.readLine();
            assertNotNull("Nothing written to ProductMap.", line);
            assertFalse(line.isEmpty());
            assertProductMapEntryEquals(2, "T2", testProduct3.getFileLocation().toAbsolutePath().toString(), line);
        }
    }

    @Test
    public void testClosing() throws IOException, FactoryException, TransformException {
        final int windowSize = 1;
        final String filenamePrefix = "testClosing";

        writer = createMeasurementWriter(windowSize, filenamePrefix, null, true);

        final Product testProduct = createTestProduct("N1", "T1", new String[0], 360, 180);
        writeRegion(writer, testProduct, 1, windowSize);

        writer.close();

        try {
            writeRegion(writer, testProduct, 2, windowSize);
            fail("IllegalStateException expected: The writer is closed.");
        } catch (IllegalStateException e) {
        }
    }

    private void writeRegion(MeasurementWriter writer, Product p1, int coordId, int windowSize) throws IOException {
        final int pixelX = 20;
        final int pixelY = 42;
        final int pixelBorder = windowSize / 2;
        final int centerX = pixelX + pixelBorder;
        final int centerY = pixelY + pixelBorder;
        final byte validValue = (byte) (coordId % 2 == 0 ? -1 : 0);
        final RenderedOp renderedOp = ConstantDescriptor.create((float) p1.getSceneRasterWidth(),
                (float) p1.getSceneRasterHeight(),
                new Byte[]{validValue}, null);
        final Raster validData = renderedOp.getData(new Rectangle(pixelX, pixelY, windowSize, windowSize));

        writer.writeMeasurements(centerX, centerY, coordId, "coord" + coordId, p1, validData);
    }

    private void assertProductMapEntryEquals(int productId, String productType, String location, String line) {
        final Scanner scanner = new Scanner(line);
        scanner.useLocale(Locale.ENGLISH);
        scanner.useDelimiter("\t");
        assertEquals(productId, scanner.nextInt());
        assertEquals(productType, scanner.next());
        assertEquals(location, scanner.next());
        assertFalse("Too much information on single line.", scanner.hasNext());
    }

    private void assertMeasurementEquals(Measurement measurement, String line, boolean withExpression) throws ParseException {
        final Scanner scanner = new Scanner(line);
        scanner.useLocale(Locale.ENGLISH);
        scanner.useDelimiter("\t");
        if (withExpression) {
            assertEquals(measurement.isValid(), scanner.nextBoolean());
        }
        assertEquals(measurement.getProductId(), scanner.nextInt());
        assertEquals(measurement.getCoordinateID(), scanner.nextInt());
        assertEquals(measurement.getCoordinateName(), scanner.next());
        assertEquals(measurement.getLat(), scanner.nextDouble());
        assertEquals(measurement.getLon(), scanner.nextDouble());
        assertEquals(measurement.getPixelX(), scanner.nextDouble());
        assertEquals(measurement.getPixelY(), scanner.nextDouble());
        final String date = scanner.next().trim();
        final String time = scanner.next().trim();
        if (!date.isEmpty() && !time.isEmpty()) {
            final String dateTime = ProductData.UTC.parse(date + " " + time, "yyyy-MM-dd HH:mm:ss").format();
            assertEquals(measurement.getTime().format(), dateTime);
        }
        int numValues = 0;
        while (scanner.hasNextFloat()) {
            numValues++;
            scanner.nextFloat();
        }
        assertEquals(numValues, measurement.getValues().length);

    }

    private void skipLines(BufferedReader reader, int numLines) throws IOException {
        for (int i = 0; i < numLines; i++) {
            reader.readLine();
        }
    }

    public static Product createTestProduct(String name, String type, String[] bandNames, int width, int height) throws
            FactoryException,
            TransformException {
        Rectangle bounds = new Rectangle(width, height);
        Product product = new Product(name, type, bounds.width, bounds.height);
        AffineTransform i2mTransform = new AffineTransform();
        final int northing = 90;
        final int easting = -180;
        i2mTransform.translate(easting, northing);
        final double scaleX = 360 / bounds.width;
        final double scaleY = 180 / bounds.height;
        i2mTransform.scale(scaleX, -scaleY);
        CrsGeoCoding geoCoding = new CrsGeoCoding(DefaultGeographicCRS.WGS84, bounds, i2mTransform);
        product.setSceneGeoCoding(geoCoding);
        for (int i = 0; i < bandNames.length; i++) {
            Band band = product.addBand(bandNames[i], ProductData.TYPE_FLOAT32);
            band.setSourceImage(ConstantDescriptor.create((float) bounds.width, (float) bounds.height,
                    new Float[]{(float) i}, null));
        }
        return product;
    }

    private static Measurement getMeasurement(int coordId, int productId) throws ParseException {
        return new Measurement(coordId, "coord" + coordId, productId,
                20.5f, 42.5f,
                ProductData.UTC.parse("12-MAR-2008 17:12:56"),
                new GeoPos(47.5f, -159.5f),
                new Float[]{12.34f, 1234.56f}, coordId % 2 == 0);
    }

    private MeasurementWriter createMeasurementWriter(int windowSize, String filenamePrefix, String expression,
                                                      boolean exportExpressionResult) {
        return createMeasurementWriter(windowSize, true, filenamePrefix, expression, exportExpressionResult);
    }

    private MeasurementWriter createMeasurementWriter(int windowSize, boolean exportMasks, String filenamePrefix,
                                                      String expression, boolean exportExpressionResult) {
        final PixExRasterNamesFactory rasterNamesFactory = new PixExRasterNamesFactory(true, true, exportMasks, null);
        final PixExProductRegistry productRegistry = new PixExProductRegistry(filenamePrefix, outputDir);
        final PixExMeasurementFactory measurementFactory = new PixExMeasurementFactory(rasterNamesFactory, windowSize,
                productRegistry);
        final TargetWriterFactoryAndMap targetFactory = new TargetWriterFactoryAndMap(filenamePrefix, outputDir);
        final DefaultFormatStrategy formatStrategy = new DefaultFormatStrategy(rasterNamesFactory, windowSize,
                expression,
                exportExpressionResult);
        return new MeasurementWriter(measurementFactory, targetFactory, formatStrategy);
    }
}
