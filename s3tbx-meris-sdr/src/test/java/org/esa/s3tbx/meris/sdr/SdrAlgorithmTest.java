/*
 * $Id: SdrAlgorithmTest.java,v 1.1 2007/03/27 12:52:23 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.sdr;

import com.bc.jnn.Jnn;
import com.bc.jnn.JnnException;
import com.bc.jnn.JnnNet;
import org.esa.snap.core.util.io.CsvReader;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SdrAlgorithmTest {
    private static final String NEURAL_NET_RESOURCE_PATH = "run05_100.nna";
    private static final String TEST_PIXEL_RESOURCE_PATH = "run05_100_test_pixel.dat";
    private static final double EPS = 1e-5;

    @Test
    public void testValidConstructorCall() {
        try {
            new SdrAlgorithm(new JnnNet());
        } catch (IllegalArgumentException notExpected) {
            fail();
        }
    }

    @Test
    public void testInvalidConstructorCall() {
        try {
            new SdrAlgorithm(null);
            fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testSdaComputation() throws IOException, JnnException {
        final JnnNet neuralNet = readNeuralNet();
        final SdrAlgorithm algorithm = new SdrAlgorithm(neuralNet);
        final double sdr = algorithm.computeSdr(+1.86672E-02,
                                                +4.12500E+02,
                                                +5.10563E+00,
                                                +3.86851E+01,
                                                -1.39682E+02,
                                                +4.96000E-01,
                                                +2.90000E-01,
                                                +1.58081E+00);
        assertEquals(2.42241E-02, sdr, EPS);
    }

    @Test
    public void testNeuralNetFunction() throws IOException, JnnException {
        final JnnNet neuralNet = readNeuralNet();
        final SdrAlgorithm algorithm = new SdrAlgorithm(neuralNet);
        final double[][] testVectors = readTestPixels();
        for (double[] testVector : testVectors) {
            double[] input = new double[testVector.length - 1];
            double[] actualOutput = new double[1];
            double[] expectedOutput = new double[1];
            System.arraycopy(testVector, 0, input, 0, input.length);
            expectedOutput[0] = testVector[testVector.length - 1];
            algorithm.computeSdr(input, actualOutput);
            assertEquals(expectedOutput[0], actualOutput[0], EPS);
        }
    }

    private JnnNet readNeuralNet() throws IOException, JnnException {
        final InputStream stream = SdrAlgorithmTest.class.getResourceAsStream(NEURAL_NET_RESOURCE_PATH);
        try (Reader reader = new InputStreamReader(stream)) {
            return Jnn.readNna(reader);
        }
    }

    private double[][] readTestPixels() throws IOException {
        final InputStream stream = SdrAlgorithmTest.class.getResourceAsStream(TEST_PIXEL_RESOURCE_PATH);
        final Reader reader = new InputStreamReader(stream);
        final List<String[]> recordList;
        try (CsvReader csvReader = new CsvReader(reader, new char[]{' ', '\t'}, true, "#")) {
            recordList = csvReader.readStringRecords();
        }
        final String[] header = recordList.get(0);
        recordList.remove(0);
        final double[][] testData = new double[recordList.size()][header.length];
        for (int i = 0; i < recordList.size(); i++) {
            String[] record = recordList.get(i);
            if (record.length != header.length) {
                throw new IOException("record.length != header.length");
            }
            for (int j = 0; j < record.length; j++) {
                final String value = record[j];
                try {
                    testData[i][j] = Double.parseDouble(value);
                } catch (NumberFormatException ignored) {
                    throw new IOException("record #" + (j + 1) + ": invalid number: " + value);
                }
            }
        }
        return testData;
    }
}
