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

package org.esa.snap.core.datamodel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.JdomLanguageSupport;
import org.esa.snap.core.dataio.persistence.JsonLanguageSupport;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.util.math.FXYSum;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * @author Marco Peters
 * @since 6.0
 */
public class GcpGeoCodingPersistenceConverterTest {

    private GcpGeoCodingPersistenceConverter _converter;
    private GcpGeoCoding _gcpGeoCoding;
    private Product _testProduct;
    private FXYGeoCoding _originalGC;

    @Before
    public void setUp() throws Exception {
        _converter = new GcpGeoCodingPersistenceConverter();

        int width = 10;
        int height = 10;

        Placemark[] gcps = {
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p1", "p1", "", new PixelPos(0.5f, 0.5f), new GeoPos(10, -10), null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p2", "p2", "", new PixelPos(width - 0.5f, 0.5f), new GeoPos(10, 10), null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p3", "p3", "", new PixelPos(width - 0.5f, height - 0.5f), new GeoPos(-10, 10), null),
                Placemark.createPointPlacemark(GcpDescriptor.getInstance(), "p4", "p4", "", new PixelPos(0.5f, height - 0.5f), new GeoPos(-10, -10), null),
        };
        final Datum datum = Datum.WGS_84;
        _gcpGeoCoding = new GcpGeoCoding(GcpGeoCoding.Method.POLYNOMIAL1, gcps, width, height, datum);

        final int pixelOffsetX = 1;
        final int pixelOffsetY = 2;
        final int pixelSizeX = 3;
        final int pixelSizeY = 4;
        final FXYSum.Linear xFunction = new FXYSum.Linear(new double[]{1, 2, 3});
        final FXYSum.Linear yFunction = new FXYSum.Linear(new double[]{2, 3, 4});
        final FXYSum.Linear latFunction = new FXYSum.Linear(new double[]{3, 4, 5});
        final FXYSum.Linear lonFunction = new FXYSum.Linear(new double[]{4, 5, 6});
        _originalGC = new FXYGeoCoding(pixelOffsetX, pixelOffsetY, pixelSizeX, pixelSizeY,
                xFunction, yFunction, latFunction, lonFunction, datum);
        _gcpGeoCoding.setOriginalGeoCoding(_originalGC);

        _testProduct = new Product("PName", "PType", width, height);
        for (Placemark placemark : gcps) {
            _testProduct.getGcpGroup().add(placemark);
        }
    }

    @Test
    public void testEncodeDecode_XML() {
        //preparation
        final JdomLanguageSupport xmlSupport = new JdomLanguageSupport();

        PixelPos pixelPos = _gcpGeoCoding.getPixelPos(new GeoPos(4.0, 2.0), null);
        assertEquals(5.9, pixelPos.x, 1e-8);
        assertEquals(3.2, pixelPos.y, 1e-8);

        assertSame(_originalGC, _gcpGeoCoding.getOriginalGeoCoding());

        //execution
        final Item encoded = _converter.encode(_gcpGeoCoding);
        final Element element = xmlSupport.translateToLanguageObject(encoded);
        final XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        final Item item = xmlSupport.translateToItem(element);
        final GcpGeoCoding decodedGC = _converter.decode(item, _testProduct);

        //verification
        final String xml = xmlOutputter.outputString(element);
        assertThat(xml).isEqualToIgnoringWhitespace(getExpectedXML());

        assertThat(decodedGC).isNotNull();
        assertThat(decodedGC).isNotSameAs(_gcpGeoCoding);
        pixelPos = decodedGC.getPixelPos(new GeoPos(4.0, 2.0), null);
        assertEquals(5.9, pixelPos.x, 1e-8);
        assertEquals(3.2, pixelPos.y, 1e-8);

        final GeoCoding originalGC = decodedGC.getOriginalGeoCoding();
        assertThat(originalGC).isInstanceOf(FXYGeoCoding.class);
        final FXYGeoCoding fxyGeoCoding = (FXYGeoCoding) originalGC;
        assertThat(fxyGeoCoding).isNotSameAs(_originalGC);
        assertThat(fxyGeoCoding.getPixelOffsetX()).isEqualTo(_originalGC.getPixelOffsetX());
        assertThat(fxyGeoCoding.getPixelOffsetY()).isEqualTo(_originalGC.getPixelOffsetY());
        assertThat(fxyGeoCoding.getPixelSizeX()).isEqualTo(_originalGC.getPixelSizeX());
        assertThat(fxyGeoCoding.getPixelSizeY()).isEqualTo(_originalGC.getPixelSizeY());

        equalFunctions(fxyGeoCoding.getPixelXFunction(), _originalGC.getPixelXFunction());
        equalFunctions(fxyGeoCoding.getPixelYFunction(), _originalGC.getPixelYFunction());
        equalFunctions(fxyGeoCoding.getLatFunction(), _originalGC.getLatFunction());
        equalFunctions(fxyGeoCoding.getLonFunction(), _originalGC.getLonFunction());

        final Datum datum = fxyGeoCoding.getDatum();
        final Datum _datum = _originalGC.getDatum();
        assertThat(datum).isNotNull().isNotSameAs(_datum);
        assertEquals(datum.getName(), _datum.getName());
    }

    @Test
    public void testEncodeDecode_JSON() throws JsonProcessingException {
        //preparation
        final JsonLanguageSupport jsonSupport = new JsonLanguageSupport();

        PixelPos pixelPos = _gcpGeoCoding.getPixelPos(new GeoPos(4.0, 2.0), null);
        assertEquals(5.9, pixelPos.x, 1e-8);
        assertEquals(3.2, pixelPos.y, 1e-8);

        assertSame(_originalGC, _gcpGeoCoding.getOriginalGeoCoding());

        //execution
        final Item encoded = _converter.encode(_gcpGeoCoding);
        final Map<String, Object> element = jsonSupport.translateToLanguageObject(encoded);
        final Item item = jsonSupport.translateToItem(element);
        final GcpGeoCoding decodedGC = _converter.decode(item, _testProduct);

        //verification
        final ObjectWriter jsonWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();
        final String json = jsonWriter.writeValueAsString(element);
        assertThat(json).isEqualToIgnoringWhitespace(getExpectedJson());

        assertThat(decodedGC).isNotNull();
        assertThat(decodedGC).isNotSameAs(_gcpGeoCoding);
        pixelPos = decodedGC.getPixelPos(new GeoPos(4.0, 2.0), null);
        assertEquals(5.9, pixelPos.x, 1e-8);
        assertEquals(3.2, pixelPos.y, 1e-8);

        final GeoCoding originalGC = decodedGC.getOriginalGeoCoding();
        assertThat(originalGC).isInstanceOf(FXYGeoCoding.class);
        final FXYGeoCoding fxyGeoCoding = (FXYGeoCoding) originalGC;
        assertThat(fxyGeoCoding).isNotSameAs(_originalGC);
        assertThat(fxyGeoCoding.getPixelOffsetX()).isEqualTo(_originalGC.getPixelOffsetX());
        assertThat(fxyGeoCoding.getPixelOffsetY()).isEqualTo(_originalGC.getPixelOffsetY());
        assertThat(fxyGeoCoding.getPixelSizeX()).isEqualTo(_originalGC.getPixelSizeX());
        assertThat(fxyGeoCoding.getPixelSizeY()).isEqualTo(_originalGC.getPixelSizeY());

        equalFunctions(fxyGeoCoding.getPixelXFunction(), _originalGC.getPixelXFunction());
        equalFunctions(fxyGeoCoding.getPixelYFunction(), _originalGC.getPixelYFunction());
        equalFunctions(fxyGeoCoding.getLatFunction(), _originalGC.getLatFunction());
        equalFunctions(fxyGeoCoding.getLonFunction(), _originalGC.getLonFunction());

        final Datum datum = fxyGeoCoding.getDatum();
        final Datum _datum = _originalGC.getDatum();
        assertThat(datum).isNotNull().isNotSameAs(_datum);
        assertEquals(datum.getName(), _datum.getName());
    }

    private void equalFunctions(FXYSum function, FXYSum _function) {
        assertThat(function.getOrder()).isEqualTo(_function.getOrder());
        assertThat(function.getCoefficients()).isEqualTo(_function.getCoefficients());
    }

    private String getExpectedXML() {
        return "<GcpGeoCoding>\n" +
                "  <Coordinate_Reference_System>\n" +
                "    <Horizontal_CS>\n" +
                "      <Geographic_CS>\n" +
                "        <Horizontal_Datum>\n" +
                "          <Ellipsoid>\n" +
                "            <Ellipsoid_Parameters>\n" +
                "              <ELLIPSOID_MAJ_AXIS>\n" +
                "                <unit>M</unit>\n" +
                "                <value>6378137.0</value>\n" +
                "              </ELLIPSOID_MAJ_AXIS>\n" +
                "              <ELLIPSOID_MIN_AXIS>\n" +
                "                <unit>M</unit>\n" +
                "                <value>6356752.3</value>\n" +
                "              </ELLIPSOID_MIN_AXIS>\n" +
                "            </Ellipsoid_Parameters>\n" +
                "            <ELLIPSOID_NAME>WGS-84</ELLIPSOID_NAME>\n" +
                "          </Ellipsoid>\n" +
                "          <HORIZONTAL_DATUM_NAME>WGS-84</HORIZONTAL_DATUM_NAME>\n" +
                "        </Horizontal_Datum>\n" +
                "      </Geographic_CS>\n" +
                "      <HORIZONTAL_CS_TYPE>GEOGRAPHIC</HORIZONTAL_CS_TYPE>\n" +
                "    </Horizontal_CS>\n" +
                "  </Coordinate_Reference_System>\n" +
                "  <Geoposition>\n" +
                "    <Geoposition_Points>\n" +
                "      <Original_Geocoding>\n" +
                "        <FXYGeoCoding>\n" +
                "          <Coordinate_Reference_System>\n" +
                "            <Horizontal_CS>\n" +
                "              <Geographic_CS>\n" +
                "                <Horizontal_Datum>\n" +
                "                  <Ellipsoid>\n" +
                "                    <Ellipsoid_Parameters>\n" +
                "                      <ELLIPSOID_MAJ_AXIS>\n" +
                "                        <unit>M</unit>\n" +
                "                        <value>6378137.0</value>\n" +
                "                      </ELLIPSOID_MAJ_AXIS>\n" +
                "                      <ELLIPSOID_MIN_AXIS>\n" +
                "                        <unit>M</unit>\n" +
                "                        <value>6356752.3</value>\n" +
                "                      </ELLIPSOID_MIN_AXIS>\n" +
                "                    </Ellipsoid_Parameters>\n" +
                "                    <ELLIPSOID_NAME>WGS-84</ELLIPSOID_NAME>\n" +
                "                  </Ellipsoid>\n" +
                "                  <HORIZONTAL_DATUM_NAME>WGS-84</HORIZONTAL_DATUM_NAME>\n" +
                "                </Horizontal_Datum>\n" +
                "              </Geographic_CS>\n" +
                "              <HORIZONTAL_CS_TYPE>GEOGRAPHIC</HORIZONTAL_CS_TYPE>\n" +
                "            </Horizontal_CS>\n" +
                "          </Coordinate_Reference_System>\n" +
                "          <Geoposition>\n" +
                "            <Geoposition_Insert>\n" +
                "              <ULXMAP>1.0</ULXMAP>\n" +
                "              <ULYMAP>2.0</ULYMAP>\n" +
                "              <XDIM>3.0</XDIM>\n" +
                "              <YDIM>4.0</YDIM>\n" +
                "            </Geoposition_Insert>\n" +
                "            <Simplified_Location_Model>\n" +
                "              <Direct_Location_Model order=\"1\">\n" +
                "                <lc_List>4.0, 5.0, 6.0</lc_List>\n" +
                "                <pc_List>3.0, 4.0, 5.0</pc_List>\n" +
                "              </Direct_Location_Model>\n" +
                "              <Reverse_Location_Model order=\"1\">\n" +
                "                <ic_List>1.0, 2.0, 3.0</ic_List>\n" +
                "                <jc_List>2.0, 3.0, 4.0</jc_List>\n" +
                "              </Reverse_Location_Model>\n" +
                "            </Simplified_Location_Model>\n" +
                "          </Geoposition>\n" +
                "          <___persistence_id___>FXYGC:1</___persistence_id___>\n" +
                "        </FXYGeoCoding>\n" +
                "      </Original_Geocoding>\n" +
                "      <INTERPOLATION_METHOD>POLYNOMIAL1</INTERPOLATION_METHOD>\n" +
                "    </Geoposition_Points>\n" +
                "  </Geoposition>\n" +
                "  <___persistence_id___>GcpGC:1</___persistence_id___>\n" +
                "</GcpGeoCoding>";
    }

    private String getExpectedJson() {
        return "{\n" +
                "  \"GcpGeoCoding\" : {\n" +
                "    \"___persistence_id___\" : \"GcpGC:1\",\n" +
                "    \"Coordinate_Reference_System\" : {\n" +
                "      \"Horizontal_CS\" : {\n" +
                "        \"HORIZONTAL_CS_TYPE\" : \"GEOGRAPHIC\",\n" +
                "        \"Geographic_CS\" : {\n" +
                "          \"Horizontal_Datum\" : {\n" +
                "            \"HORIZONTAL_DATUM_NAME\" : \"WGS-84\",\n" +
                "            \"Ellipsoid\" : {\n" +
                "              \"ELLIPSOID_NAME\" : \"WGS-84\",\n" +
                "              \"Ellipsoid_Parameters\" : {\n" +
                "                \"ELLIPSOID_MAJ_AXIS\" : {\n" +
                "                  \"unit\" : \"M\",\n" +
                "                  \"value\" : \"6378137.0\"\n" +
                "                },\n" +
                "                \"ELLIPSOID_MIN_AXIS\" : {\n" +
                "                  \"unit\" : \"M\",\n" +
                "                  \"value\" : \"6356752.3\"\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"Geoposition\" : {\n" +
                "      \"Geoposition_Points\" : {\n" +
                "        \"INTERPOLATION_METHOD\" : \"POLYNOMIAL1\",\n" +
                "        \"Original_Geocoding\" : {\n" +
                "          \"FXYGeoCoding\" : {\n" +
                "            \"___persistence_id___\" : \"FXYGC:1\",\n" +
                "            \"Coordinate_Reference_System\" : {\n" +
                "              \"Horizontal_CS\" : {\n" +
                "                \"HORIZONTAL_CS_TYPE\" : \"GEOGRAPHIC\",\n" +
                "                \"Geographic_CS\" : {\n" +
                "                  \"Horizontal_Datum\" : {\n" +
                "                    \"HORIZONTAL_DATUM_NAME\" : \"WGS-84\",\n" +
                "                    \"Ellipsoid\" : {\n" +
                "                      \"ELLIPSOID_NAME\" : \"WGS-84\",\n" +
                "                      \"Ellipsoid_Parameters\" : {\n" +
                "                        \"ELLIPSOID_MAJ_AXIS\" : {\n" +
                "                          \"unit\" : \"M\",\n" +
                "                          \"value\" : \"6378137.0\"\n" +
                "                        },\n" +
                "                        \"ELLIPSOID_MIN_AXIS\" : {\n" +
                "                          \"unit\" : \"M\",\n" +
                "                          \"value\" : \"6356752.3\"\n" +
                "                        }\n" +
                "                      }\n" +
                "                    }\n" +
                "                  }\n" +
                "                }\n" +
                "              }\n" +
                "            },\n" +
                "            \"Geoposition\" : {\n" +
                "              \"Geoposition_Insert\" : {\n" +
                "                \"ULXMAP\" : 1.0,\n" +
                "                \"ULYMAP\" : 2.0,\n" +
                "                \"XDIM\" : 3.0,\n" +
                "                \"YDIM\" : 4.0\n" +
                "              },\n" +
                "              \"Simplified_Location_Model\" : {\n" +
                "                \"Direct_Location_Model\" : {\n" +
                "                  \"_$ATT$_order\" : 1,\n" +
                "                  \"lc_List\" : [ 4.0, 5.0, 6.0 ],\n" +
                "                  \"pc_List\" : [ 3.0, 4.0, 5.0 ]\n" +
                "                },\n" +
                "                \"Reverse_Location_Model\" : {\n" +
                "                  \"_$ATT$_order\" : \"1\",\n" +
                "                  \"ic_List\" : [ 1.0, 2.0, 3.0 ],\n" +
                "                  \"jc_List\" : [ 2.0, 3.0, 4.0 ]\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }
}