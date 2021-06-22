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

package org.esa.snap.core.datamodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.JsonLanguageSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class BasicPixelGeoCodingPersistenceConverterTest {

    private static final int S = 4;
    private static final int GW = 3;
    private static final int GH = 5;
    private static final int PW = (GW - 1) * S + 1;
    private static final int PH = (GH - 1) * S + 1;
    private static final float LAT_1 = 53.0f;
    private static final float LAT_2 = 50.0f;
    private static final float LON_1 = 10.0f;
    private static final float LON_2 = 15.0f;

    private Product productF;
    private Product productD;
    private BasicPixelGeoCodingPersistenceConverter converter;

    @Before
    public void setUp() {
        productF = createProduct(ProductData.TYPE_FLOAT32);
        productD = createProduct(ProductData.TYPE_FLOAT64);
        converter = new BasicPixelGeoCodingPersistenceConverter();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void ensureSetupResult() {
        assertThat(productF.getRasterDataNode("latBand").getDataType()).isEqualTo(ProductData.TYPE_FLOAT32);
        assertThat(productF.getRasterDataNode("lonBand").getDataType()).isEqualTo(ProductData.TYPE_FLOAT32);
        assertThat(productF.getSceneGeoCoding()).isInstanceOf(TiePointGeoCoding.class);

        assertThat(productD.getRasterDataNode("latBand").getDataType()).isEqualTo(ProductData.TYPE_FLOAT64);
        assertThat(productD.getRasterDataNode("lonBand").getDataType()).isEqualTo(ProductData.TYPE_FLOAT64);
        assertThat(productD.getSceneGeoCoding()).isInstanceOf(TiePointGeoCoding.class);
    }

    @Test
    public void encodeAndDecodePixelGeoCoding() {
        encodeAndDecodePixelGeoCoding(productF);
        encodeAndDecodePixelGeoCoding(productD);
    }

    @Test
    public void encodeAndDecodePixelGeoCoding_ProductWithoutSceneGeoCoding() {
        encodeAndDecodePixelGeoCoding_ProductWithoutSceneGeoCoding(productF);
        encodeAndDecodePixelGeoCoding_ProductWithoutSceneGeoCoding(productD);
    }

    @Test
    public void encodeAndDecodePixelGeoCoding2() {
        encodeAndDecodePixelGeoCoding2(productF);
        encodeAndDecodePixelGeoCoding2(productD);
    }

    @Test
    public void encodeAndDecodePixelGeoCoding2_ProductWithoutSceneGeoCoding() {
        encodeAndDecodePixelGeoCoding2_ProductWithoutSceneGeoCoding(productF);
        encodeAndDecodePixelGeoCoding2_ProductWithoutSceneGeoCoding(productD);
    }

    private void encodeAndDecodePixelGeoCoding(Product product) {
        final Band latBand = product.getBand("latBand");
        final Band lonBand = product.getBand("lonBand");
        final TiePointGeoCoding tiePointGeoCoding = (TiePointGeoCoding) product.getSceneGeoCoding();
        final PixelGeoCoding pixelGeoCoding = new PixelGeoCoding(latBand, lonBand, "flags.valid", 6, tiePointGeoCoding);
        assertThat(pixelGeoCoding).isNotNull();
        assertThat(pixelGeoCoding.getPixelPosEstimator()).isSameAs(tiePointGeoCoding);

        final Item encoded = converter.encode(pixelGeoCoding);
        final String json = toJson(encoded);
        assertThat(json).isEqualToIgnoringNewLines("{\n" +
                                                   "  \"PixelGeoCoding\" : {\n" +
                                                   "    \"___persistence_id___\" : \"BasicPixelGC:1\",\n" +
                                                   "    \"LATITUDE_BAND\" : \"latBand\",\n" +
                                                   "    \"LONGITUDE_BAND\" : \"lonBand\",\n" +
                                                   "    \"VALID_MASK_EXPRESSION\" : \"flags.valid\",\n" +
                                                   "    \"SEARCH_RADIUS\" : 6,\n" +
                                                   "    \"Pixel_Position_Estimator\" : {\n" +
                                                   "      \"TiePointGeoCoding\" : {\n" +
                                                   "        \"___persistence_id___\" : \"TiePointGC:1\",\n" +
                                                   "        \"Coordinate_Reference_System\" : {\n" +
                                                   "          \"Horizontal_CS\" : {\n" +
                                                   "            \"HORIZONTAL_CS_TYPE\" : \"GEOGRAPHIC\",\n" +
                                                   "            \"Geographic_CS\" : {\n" +
                                                   "              \"Horizontal_Datum\" : {\n" +
                                                   "                \"HORIZONTAL_DATUM_NAME\" : \"WGS-84\",\n" +
                                                   "                \"Ellipsoid\" : {\n" +
                                                   "                  \"ELLIPSOID_NAME\" : \"WGS-84\",\n" +
                                                   "                  \"Ellipsoid_Parameters\" : {\n" +
                                                   "                    \"ELLIPSOID_MAJ_AXIS\" : {\n" +
                                                   "                      \"unit\" : \"M\",\n" +
                                                   "                      \"value\" : \"6378137.0\"\n" +
                                                   "                    },\n" +
                                                   "                    \"ELLIPSOID_MIN_AXIS\" : {\n" +
                                                   "                      \"unit\" : \"M\",\n" +
                                                   "                      \"value\" : \"6356752.3\"\n" +
                                                   "                    }\n" +
                                                   "                  }\n" +
                                                   "                }\n" +
                                                   "              }\n" +
                                                   "            }\n" +
                                                   "          }\n" +
                                                   "        },\n" +
                                                   "        \"Geoposition\" : {\n" +
                                                   "          \"Geoposition_Points\" : {\n" +
                                                   "            \"TIE_POINT_GRID_NAME_LAT\" : \"latGrid\",\n" +
                                                   "            \"TIE_POINT_GRID_NAME_LON\" : \"lonGrid\"\n" +
                                                   "          }\n" +
                                                   "        }\n" +
                                                   "      }\n" +
                                                   "    }\n" +
                                                   "  }\n" +
                                                   "}");
        final BasicPixelGeoCoding decoded = converter.decode(encoded, product);
        assertThat(decoded).isInstanceOf(PixelGeoCoding.class);
        assertThat(decoded).isNotSameAs(pixelGeoCoding);
        assertThat(decoded.getLatBand().getProduct()).isSameAs(product);
        assertThat(decoded.getLatBand().getName()).isEqualTo("latBand");
        assertThat(decoded.getLonBand().getName()).isEqualTo("lonBand");
        assertThat(decoded.getSearchRadius()).isEqualTo(6);
        assertThat(decoded.getValidMask()).isEqualTo("flags.valid");
        final GeoCoding decodedEstimator = decoded.getPixelPosEstimator();
        assertThat(decodedEstimator).isInstanceOf(TiePointGeoCoding.class);
        TiePointGeoCoding decodedTPGC = (TiePointGeoCoding) decodedEstimator;
        assertThat(decodedTPGC).isNotSameAs(tiePointGeoCoding);
        assertThat(decodedTPGC.getLatGrid()).isSameAs(tiePointGeoCoding.getLatGrid());
        assertThat(decodedTPGC.getLonGrid()).isSameAs(tiePointGeoCoding.getLonGrid());
    }

    private void encodeAndDecodePixelGeoCoding2(Product product) {
        final Band latBand = product.getBand("latBand");
        final Band lonBand = product.getBand("lonBand");
        final TiePointGeoCoding tiePointGeoCoding = (TiePointGeoCoding) product.getSceneGeoCoding();
        final PixelGeoCoding2 pixelGeoCoding2 = new PixelGeoCoding2(latBand, lonBand, null, 6, tiePointGeoCoding);
        assertThat(pixelGeoCoding2).isNotNull();
        assertThat(pixelGeoCoding2.getPixelPosEstimator()).isSameAs(tiePointGeoCoding);

        final Item encoded = converter.encode(pixelGeoCoding2);
        final String json = toJson(encoded);
        assertThat(json).isEqualToIgnoringNewLines("{\n" +
                                                   "  \"PixelGeoCoding2\" : {\n" +
                                                   "    \"___persistence_id___\" : \"BasicPixelGC:1\",\n" +
                                                   "    \"LATITUDE_BAND\" : \"latBand\",\n" +
                                                   "    \"LONGITUDE_BAND\" : \"lonBand\",\n" +
//                                                   "    \"VALID_MASK_EXPRESSION\" : \"flags.valid\",\n" +
                                                   "    \"SEARCH_RADIUS\" : 6,\n" +
                                                   "    \"Pixel_Position_Estimator\" : {\n" +
                                                   "      \"TiePointGeoCoding\" : {\n" +
                                                   "        \"___persistence_id___\" : \"TiePointGC:1\",\n" +
                                                   "        \"Coordinate_Reference_System\" : {\n" +
                                                   "          \"Horizontal_CS\" : {\n" +
                                                   "            \"HORIZONTAL_CS_TYPE\" : \"GEOGRAPHIC\",\n" +
                                                   "            \"Geographic_CS\" : {\n" +
                                                   "              \"Horizontal_Datum\" : {\n" +
                                                   "                \"HORIZONTAL_DATUM_NAME\" : \"WGS-84\",\n" +
                                                   "                \"Ellipsoid\" : {\n" +
                                                   "                  \"ELLIPSOID_NAME\" : \"WGS-84\",\n" +
                                                   "                  \"Ellipsoid_Parameters\" : {\n" +
                                                   "                    \"ELLIPSOID_MAJ_AXIS\" : {\n" +
                                                   "                      \"unit\" : \"M\",\n" +
                                                   "                      \"value\" : \"6378137.0\"\n" +
                                                   "                    },\n" +
                                                   "                    \"ELLIPSOID_MIN_AXIS\" : {\n" +
                                                   "                      \"unit\" : \"M\",\n" +
                                                   "                      \"value\" : \"6356752.3\"\n" +
                                                   "                    }\n" +
                                                   "                  }\n" +
                                                   "                }\n" +
                                                   "              }\n" +
                                                   "            }\n" +
                                                   "          }\n" +
                                                   "        },\n" +
                                                   "        \"Geoposition\" : {\n" +
                                                   "          \"Geoposition_Points\" : {\n" +
                                                   "            \"TIE_POINT_GRID_NAME_LAT\" : \"latGrid\",\n" +
                                                   "            \"TIE_POINT_GRID_NAME_LON\" : \"lonGrid\"\n" +
                                                   "          }\n" +
                                                   "        }\n" +
                                                   "      }\n" +
                                                   "    }\n" +
                                                   "  }\n" +
                                                   "}");
        final BasicPixelGeoCoding decoded = converter.decode(encoded, product);
        assertThat(decoded).isInstanceOf(PixelGeoCoding2.class);
        assertThat(decoded).isNotSameAs(pixelGeoCoding2);
        assertThat(decoded.getLatBand().getProduct()).isSameAs(product);
        assertThat(decoded.getLatBand().getName()).isEqualTo("latBand");
        assertThat(decoded.getLonBand().getName()).isEqualTo("lonBand");
        assertThat(decoded.getSearchRadius()).isEqualTo(6);
        assertThat(decoded.getValidMask()).isEqualTo(null);
        final GeoCoding decodedEstimator = decoded.getPixelPosEstimator();
        assertThat(decodedEstimator).isInstanceOf(TiePointGeoCoding.class);
        TiePointGeoCoding decodedTPGC = (TiePointGeoCoding) decodedEstimator;
        assertThat(decodedTPGC).isNotSameAs(tiePointGeoCoding);
        assertThat(decodedTPGC.getLatGrid()).isSameAs(tiePointGeoCoding.getLatGrid());
        assertThat(decodedTPGC.getLonGrid()).isSameAs(tiePointGeoCoding.getLonGrid());
    }

    private void encodeAndDecodePixelGeoCoding_ProductWithoutSceneGeoCoding(Product product) {
        final Band latBand = product.getBand("latBand");
        final Band lonBand = product.getBand("lonBand");
        product.setSceneGeoCoding(null);
        final PixelGeoCoding pixelGeoCoding = new PixelGeoCoding(latBand, lonBand, "flags.valid", 6);
        assertThat(pixelGeoCoding).isNotNull();
        assertThat(pixelGeoCoding.getPixelPosEstimator()).isNull();

        final Item encoded = converter.encode(pixelGeoCoding);
        final String json = toJson(encoded);
        assertThat(json).isEqualToIgnoringNewLines("{\n" +
                                                   "  \"PixelGeoCoding\" : {\n" +
                                                   "    \"___persistence_id___\" : \"BasicPixelGC:1\",\n" +
                                                   "    \"LATITUDE_BAND\" : \"latBand\",\n" +
                                                   "    \"LONGITUDE_BAND\" : \"lonBand\",\n" +
                                                   "    \"VALID_MASK_EXPRESSION\" : \"flags.valid\",\n" +
                                                   "    \"SEARCH_RADIUS\" : 6\n" +
                                                   "  }\n" +
                                                   "}");
        final BasicPixelGeoCoding decoded = converter.decode(encoded, product);
        assertThat(decoded).isInstanceOf(PixelGeoCoding.class);
        assertThat(decoded.getLatBand().getProduct()).isSameAs(product);
        assertThat(decoded.getLatBand().getName()).isEqualTo("latBand");
        assertThat(decoded.getLonBand().getName()).isEqualTo("lonBand");
        assertThat(decoded.getSearchRadius()).isEqualTo(6);
        assertThat(decoded.getValidMask()).isEqualTo("flags.valid");
        assertThat(decoded.getPixelPosEstimator()).isNull();
    }

    private void encodeAndDecodePixelGeoCoding2_ProductWithoutSceneGeoCoding(Product product) {
        final Band latBand = product.getBand("latBand");
        final Band lonBand = product.getBand("lonBand");
        product.setSceneGeoCoding(null);
        final PixelGeoCoding2 pixelGeoCoding2 = new PixelGeoCoding2(latBand, lonBand, null, 6);
        assertThat(pixelGeoCoding2).isNotNull();
        assertThat(pixelGeoCoding2.getPixelPosEstimator()).isNull();

        final Item encoded = converter.encode(pixelGeoCoding2);
        final String json = toJson(encoded);
        assertThat(json).isEqualToIgnoringNewLines("{\n" +
                                                   "  \"PixelGeoCoding2\" : {\n" +
                                                   "    \"___persistence_id___\" : \"BasicPixelGC:1\",\n" +
                                                   "    \"LATITUDE_BAND\" : \"latBand\",\n" +
                                                   "    \"LONGITUDE_BAND\" : \"lonBand\",\n" +
//                                                   "    \"VALID_MASK_EXPRESSION\" : \"flags.valid\",\n" +
                                                   "    \"SEARCH_RADIUS\" : 6\n" +
                                                   "  }\n" +
                                                   "}");
        final BasicPixelGeoCoding decoded = converter.decode(encoded, product);
        assertThat(decoded).isInstanceOf(PixelGeoCoding2.class);
        assertThat(decoded.getLatBand().getProduct()).isSameAs(product);
        assertThat(decoded.getLatBand().getName()).isEqualTo("latBand");
        assertThat(decoded.getLonBand().getName()).isEqualTo("lonBand");
        assertThat(decoded.getSearchRadius()).isEqualTo(6);
        assertThat(decoded.getValidMask()).isEqualTo(null);
        assertThat(decoded.getPixelPosEstimator()).isNull();
    }

    private String toJson(Item encoded) {
        JsonLanguageSupport languageSupport = new JsonLanguageSupport();
        final Map<String, Object> stringObjectMap = languageSupport.translateToLanguageObject(encoded);
        final JsonMapper mapper = new JsonMapper();
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(stringObjectMap);
        } catch (JsonProcessingException e) {

        }
        return null;
    }

    private Product createProduct(int latLonBandDataType) {
        Product product = new Product("test", "test", PW, PH);

        TiePointGrid latGrid = new TiePointGrid("latGrid", GW, GH, 0.5, 0.5, S, S, createLatGridData());
        TiePointGrid lonGrid = new TiePointGrid("lonGrid", GW, GH, 0.5, 0.5, S, S, createLonGridData());

        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);

        Band latBand = product.addBand("latBand", latLonBandDataType);
        Band lonBand = product.addBand("lonBand", latLonBandDataType);

        latBand.setRasterData(createBandData(latGrid, latLonBandDataType));
        lonBand.setRasterData(createBandData(lonGrid, latLonBandDataType));
        final FlagCoding flagCoding = new FlagCoding("flags");
        flagCoding.addFlag("valid", 0x01, "valid pixel");

        product.getFlagCodingGroup().add(flagCoding);

        Band flagomatBand = product.addBand("flagomat", ProductData.TYPE_UINT8);
        byte[] flagomatData = new byte[PW * PH];
        Arrays.fill(flagomatData, (byte) 0x01);
        flagomatBand.setRasterData(ProductData.createInstance(ProductData.TYPE_UINT8, flagomatData));
        flagomatBand.setSampleCoding(flagCoding);

        product.setSceneGeoCoding(new TiePointGeoCoding(latGrid, lonGrid));

        return product;
    }

    private float[] createLatGridData() {
        return createGridData(LAT_1, LAT_2);
    }

    private float[] createLonGridData() {
        return createGridData(LON_1, LON_2);
    }

    private static float[] createGridData(float lon0, float lon1) {
        float[] floats = new float[GW * GH];

        for (int j = 0; j < GH; j++) {
            for (int i = 0; i < GW; i++) {
                float x = i / (GW - 1.0f);
                float y = j / (GH - 1.0f);
                floats[j * GW + i] = lon0 + (lon1 - lon0) * x * x + 0.1f * (lon1 - lon0) * y * y;
            }
        }

        return floats;
    }

    private static ProductData createBandData(TiePointGrid grid, int latLonBandDataType) {
        ProductData bandData = ProductData.createInstance(latLonBandDataType, PW * PH);
        for (int y = 0; y < PH; y++) {
            for (int x = 0; x < PW; x++) {
                bandData.setElemFloatAt(y * PW + x, grid.getPixelFloat(x, y));
            }
        }
        return bandData;
    }
}
