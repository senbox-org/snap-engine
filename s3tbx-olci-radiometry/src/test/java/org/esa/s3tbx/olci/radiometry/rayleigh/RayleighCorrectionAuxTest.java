/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.rayleigh;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrectionAuxTest {

    private Path pathJSON;
    private RayleighCorrectionAux rayleighCorrectionAux;

    @Before
    public void setUp() throws Exception {
        rayleighCorrectionAux = new RayleighCorrectionAux();
        Path installAuxdataPath = rayleighCorrectionAux.installAuxdata();
        pathJSON = installAuxdataPath.resolve("coeffMatrix.txt");
    }


    @Test
    public void testGetJSONTheta() throws Exception {
        JSONParser jsonObject = new JSONParser();
        JSONObject parse = (JSONObject) jsonObject.parse(new FileReader(pathJSON.toString()));

        double[] thetas = rayleighCorrectionAux.parseJSON1DimArray(parse, "theta");
        double[] expectedThetas = {
                2.840904951095581,
                17.638418197631836,
                28.7684268951416,
                36.189727783203125,
                43.61144256591797,
                51.033390045166016,
                58.45547866821289,
                65.87765502929688,
                69.58876037597656,
                73.29988098144531,
                77.0110092163086,
                80.7221450805664
        };

        assertNotNull(thetas);
        assertEquals(12, thetas.length);
        assertArrayEquals(expectedThetas, thetas, 1e-8);
    }


    @Test
    public void testGetJSONRay_Matrix() throws Exception {
        JSONParser jsonObject = new JSONParser();
        JSONObject parse = (JSONObject) jsonObject.parse(new FileReader(pathJSON.toString()));
        ArrayList<double[][][]> ray_coeff_matrix = rayleighCorrectionAux.parseJSON3DimArray(parse, "ray_coeff_matrix");
        assertNotNull(ray_coeff_matrix);
        assertEquals(4, ray_coeff_matrix.size());

        double[][][] doubles = ray_coeff_matrix.get(0);
        assertEquals(1.0046205520629883, doubles[0][0][0], 1e-8);
        assertEquals(1.0046125650405884, doubles[0][1][0], 1e-8);
        assertEquals( 1.0045990943908691, doubles[0][2][0], 1e-8);
    }

    @Test
    public void testAuxWithInterpolation() throws Exception {
        JSONParser jsonObject = new JSONParser();
        JSONObject parse = (JSONObject) jsonObject.parse(new FileReader(pathJSON.toString()));

        double[] thetas = rayleighCorrectionAux.parseJSON1DimArray(parse, "theta");
        ArrayList<double[][][]> ray_coeff_matrix = rayleighCorrectionAux.parseJSON3DimArray(parse, "ray_coeff_matrix");

        double[][][] doubles = ray_coeff_matrix.get(0);

        double doubles1 = SpikeInterpolation.interpolate2D(doubles[0], thetas, thetas, 3, 75);

        SpikeInterpolation.useApacheMath(thetas, thetas, doubles[0], 3, 75);

        SpikeInterpolation.useLibJAI(doubles[0], 3, 75);
    }
}