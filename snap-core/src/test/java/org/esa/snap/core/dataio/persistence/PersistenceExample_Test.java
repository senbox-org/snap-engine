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

package org.esa.snap.core.dataio.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistenceExample_Test {

    public static final int RASTER_WIDTH = 200;
    public static final int RASTER_HEIGHT = 200;
    private Product product;
    private Persistence persistence;

    @Before
    public void setUp() throws Exception {
        product = new Product("p", "t", RASTER_WIDTH, RASTER_HEIGHT);
        product.addMask("mask1", "(X * Y + X) > 111", "decr1", Color.RED, 0.6);
        product.addMask("mask2", "(X * Y + X) > 222", "decr2", Color.GREEN, 0.5);
        product.addMask("mask3", "(X * Y + X) > 333", "decr3", Color.BLUE, 0.4);
        product.addMask("mask4", "(X * Y + X) > 444", "decr4", Color.GRAY, 0.3);

        final ArrayList<PersistenceSpi> spiList = new ArrayList<>();
        spiList.add(new TestMaskPersistenceSpi());

        PersistenceSpiRegistry registry = mock(PersistenceSpiRegistry.class);
        when(registry.getPersistenceSpis()).then(invocation -> spiList.listIterator());
        persistence = new Persistence(registry);
    }

    @Test
    public void test_Encode_XML() {
        // Writer initializing (Dimap or Zarr)
        // in this case we are simulating Dimap ... a JDOM use case
        final JdomLanguageSupport languageSupport = new JdomLanguageSupport();

        // the language dependent node where the created nodes shall be added
        // in this case a JDOM node
        final Element root = new Element("nodeWhereTheGeneratedElementsShouldBeAdded");

        // iterate over the masks to be persisted
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        final Mask[] masks = maskGroup.toArray(new Mask[0]);
        for (Mask mask : masks) {
            // fetch a markup language independent persistence converter for this mask from registry
            final PersistenceEncoder<Object> converter = persistence.getEncoder(mask);
            // encode the object to markup independent language model
            final Item item = converter.encode(mask);
            // convert them to the Markup you need
            final Element mlObj = languageSupport.translateToLanguageObject(item);
            root.addContent(mlObj);
        }

        //verification
        final XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        final String out = xmlOutputter.outputString(root);
        assertThat(out).isEqualToIgnoringNewLines(
                "<nodeWhereTheGeneratedElementsShouldBeAdded>\n" +
                "  <mask>\n" +
                "    <___persistence_id___>TestMaskPersistenceConverter:2</___persistence_id___>\n" +
                "    <name>mask1</name>\n" +
                "    <expression>(X * Y + X) &gt; 111</expression>\n" +
                "    <description>decr1</description>\n" +
                "    <color_rgb>255, 0, 0</color_rgb>\n" +
                "    <image_transparency>0.6</image_transparency>\n" +
                "  </mask>\n" +
                "  <mask>\n" +
                "    <___persistence_id___>TestMaskPersistenceConverter:2</___persistence_id___>\n" +
                "    <name>mask2</name>\n" +
                "    <expression>(X * Y + X) &gt; 222</expression>\n" +
                "    <description>decr2</description>\n" +
                "    <color_rgb>0, 255, 0</color_rgb>\n" +
                "    <image_transparency>0.5</image_transparency>\n" +
                "  </mask>\n" +
                "  <mask>\n" +
                "    <___persistence_id___>TestMaskPersistenceConverter:2</___persistence_id___>\n" +
                "    <name>mask3</name>\n" +
                "    <expression>(X * Y + X) &gt; 333</expression>\n" +
                "    <description>decr3</description>\n" +
                "    <color_rgb>0, 0, 255</color_rgb>\n" +
                "    <image_transparency>0.4</image_transparency>\n" +
                "  </mask>\n" +
                "  <mask>\n" +
                "    <___persistence_id___>TestMaskPersistenceConverter:2</___persistence_id___>\n" +
                "    <name>mask4</name>\n" +
                "    <expression>(X * Y + X) &gt; 444</expression>\n" +
                "    <description>decr4</description>\n" +
                "    <color_rgb>128, 128, 128</color_rgb>\n" +
                "    <image_transparency>0.3</image_transparency>\n" +
                "  </mask>\n" +
                "</nodeWhereTheGeneratedElementsShouldBeAdded>"
        );
    }

    @Test
    public void test_Decode_XML() throws JDOMException, IOException {
        // Reader initializing (Dimap or Zarr)
        // in this case we are simulating Dimap ... a JDOM use case
        final JdomLanguageSupport languageSupport = new JdomLanguageSupport();

        //preparation
        final Product product = new Product("B", "T", RASTER_WIDTH, RASTER_HEIGHT);
        assertThat(product.getMaskGroup().getNodeCount()).isEqualTo(0);
        final String xmlSnipped = "<root-element>" +
                                  "  <mask>" +
                                  "    <___persistence_id___>TestMaskPersistenceConverter:2</___persistence_id___>" +
                                  "    <name>mask1</name>" +
                                  "    <expression>(X * Y + X) &gt; 111</expression>" +
                                  "    <description>decr1</description>" +
                                  "    <color_rgb>255, 0, 0</color_rgb>" +
                                  "    <image_transparency>0.6</image_transparency>" +
                                  "  </mask>" +
                                  "  <mask>" +
                                  "    <___persistence_id___>TestMaskPersistenceConverter:2</___persistence_id___>" +
                                  "    <name>mask2</name>" +
                                  "    <expression>(X * Y + X) &gt; 222</expression>" +
                                  "    <description>decr2</description>" +
                                  "    <color_rgb>0, 255, 0</color_rgb>" +
                                  "    <image_transparency>0.5</image_transparency>" +
                                  "  </mask>" +
                                  "  <mask>" +
                                  "    <___persistence_id___>TestMaskPersistenceConverter:2</___persistence_id___>" +
                                  "    <name>mask3</name>" +
                                  "    <expression>(X * Y + X) &gt; 333</expression>" +
                                  "    <description>decr3</description>" +
                                  "    <color_rgb>0, 0, 255</color_rgb>" +
                                  "    <image_transparency>0.4</image_transparency>" +
                                  "  </mask>" +
                                  "  <mask>" +
                                  "    <___persistence_id___>TestMaskPersistenceConverter:2</___persistence_id___>" +
                                  "    <name>mask4</name>" +
                                  "    <expression>(X * Y + X) &gt; 444</expression>" +
                                  "    <description>decr4</description>" +
                                  "    <color_rgb>128, 128, 128</color_rgb>" +
                                  "    <image_transparency>0.3</image_transparency>" +
                                  "  </mask>" +
                                  "</root-element>";

        final SAXBuilder builder = new SAXBuilder();
        final Document document = builder.build(new StringReader(xmlSnipped));
        final Element root = document.getRootElement();
        final List<Element> mlObjects = root.getChildren();

        //execution
        for (Element mlObj : mlObjects) {
            // convert the markup to markup independent language model
            final Item item = languageSupport.translateToItem(mlObj);
            // fetch a markup language independent persistence converter for this item representing a mask from registry
            final PersistenceDecoder<Mask> converter = persistence.getDecoder(item);
            // decode the item to a mask and add it to the product
            final Mask mask = converter.decode(item, product);
        }

        //verification
        assertThat(product.getMaskGroup().getNodeCount()).isEqualTo(4);
    }

    @Test
    public void test_Encode_JSON() throws JsonProcessingException {
        // Writer initializing (Dimap or Zarr)
        // in this case we are simulating e.g. ZNAP ... a JSON use case
        final JsonLanguageSupport languageSupport = new JsonLanguageSupport();

        // the language dependent node where the created nodes shall be added
        // in this case a JSON node is a map
        final Map<String, Object> root = new LinkedHashMap<>();
        final List<Map<String, Object>> mlObjects = new ArrayList<>();
        root.put("masks", mlObjects);

        // iterate over the masks to be persisted
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        final Mask[] masks = maskGroup.toArray(new Mask[0]);
        for (Mask mask : masks) {
            // fetch a markup language independent persistence converter for this mask from registry
            final PersistenceEncoder<Object> converter = persistence.getEncoder(mask);
            // encode the object to markup independent language model
            final Item item = converter.encode(mask);
            // convert them to the Markup you need
            final Map<String, Object> mlObj = languageSupport.translateToLanguageObject(item);
            mlObjects.add(mlObj);
        }

        //verification
        PrettyPrinter prettyPrinter = new DefaultPrettyPrinter()
                .withArrayIndenter(DefaultPrettyPrinter.FixedSpaceIndenter.instance);
        final ObjectWriter writer = new ObjectMapper().writer(prettyPrinter);
        final String out = writer.writeValueAsString(root);

        assertThat(out).isEqualToIgnoringNewLines(
                "{\n" +
                "  \"masks\" : [ {\n" +  // node where the generated json should be added
                "    \"mask\" : {\n" +
                "      \"___persistence_id___\" : \"TestMaskPersistenceConverter:2\",\n" +
                "      \"name\" : \"mask1\",\n" +
                "      \"expression\" : \"(X * Y + X) > 111\",\n" +
                "      \"description\" : \"decr1\",\n" +
                "      \"color_rgb\" : [ 255, 0, 0 ],\n" +
                "      \"image_transparency\" : 0.6\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
                "      \"___persistence_id___\" : \"TestMaskPersistenceConverter:2\",\n" +
                "      \"name\" : \"mask2\",\n" +
                "      \"expression\" : \"(X * Y + X) > 222\",\n" +
                "      \"description\" : \"decr2\",\n" +
                "      \"color_rgb\" : [ 0, 255, 0 ],\n" +
                "      \"image_transparency\" : 0.5\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
                "      \"___persistence_id___\" : \"TestMaskPersistenceConverter:2\",\n" +
                "      \"name\" : \"mask3\",\n" +
                "      \"expression\" : \"(X * Y + X) > 333\",\n" +
                "      \"description\" : \"decr3\",\n" +
                "      \"color_rgb\" : [ 0, 0, 255 ],\n" +
                "      \"image_transparency\" : 0.4\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
                "      \"___persistence_id___\" : \"TestMaskPersistenceConverter:2\",\n" +
                "      \"name\" : \"mask4\",\n" +
                "      \"expression\" : \"(X * Y + X) > 444\",\n" +
                "      \"description\" : \"decr4\",\n" +
                "      \"color_rgb\" : [ 128, 128, 128 ],\n" +
                "      \"image_transparency\" : 0.3\n" +
                "    }\n" +
                "  } ]\n" +
                "}"
        );
    }

    @Test
    public void test_Decode_JSON() throws IOException {
        // Reader initializing (Dimap or Zarr)
        // in this case we are simulating Zarr ... a JSON use case
        final JsonLanguageSupport languageSupport = new JsonLanguageSupport();

        //preparation
        final Product product = new Product("B", "T", RASTER_WIDTH, RASTER_HEIGHT);
        assertThat(product.getMaskGroup().getNodeCount()).isEqualTo(0);
        final String xmlSnipped =
                "{\n" +
                "  \"masks\" : [ {\n" +  // node where the generated json should be added
                "    \"mask\" : {\n" +
                "      \"___persistence_id___\" : \"TestMaskPersistenceConverter:2\",\n" +
                "      \"name\" : \"mask1\",\n" +
                "      \"expression\" : \"(X * Y + X) > 111\",\n" +
                "      \"description\" : \"decr1\",\n" +
                "      \"color_rgb\" : [ 255, 0, 0 ],\n" +
                "      \"image_transparency\" : 0.6\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
                "      \"___persistence_id___\" : \"TestMaskPersistenceConverter:2\",\n" +
                "      \"name\" : \"mask2\",\n" +
                "      \"expression\" : \"(X * Y + X) > 222\",\n" +
                "      \"description\" : \"decr2\",\n" +
                "      \"color_rgb\" : [ 0, 255, 0 ],\n" +
                "      \"image_transparency\" : 0.5\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
                "      \"___persistence_id___\" : \"TestMaskPersistenceConverter:2\",\n" +
                "      \"name\" : \"mask3\",\n" +
                "      \"expression\" : \"(X * Y + X) > 333\",\n" +
                "      \"description\" : \"decr3\",\n" +
                "      \"color_rgb\" : [ 0, 0, 255 ],\n" +
                "      \"image_transparency\" : 0.4\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
                "      \"___persistence_id___\" : \"TestMaskPersistenceConverter:2\",\n" +
                "      \"name\" : \"mask4\",\n" +
                "      \"expression\" : \"(X * Y + X) > 444\",\n" +
                "      \"description\" : \"decr4\",\n" +
                "      \"color_rgb\" : [ 128, 128, 128 ],\n" +
                "      \"image_transparency\" : 0.3\n" +
                "    }\n" +
                "  } ]\n" +
                "}";

        final ObjectMapper objectMapper = new ObjectMapper();
        final HashMap<String, List<Map<String, Object>>> content = objectMapper.reader().readValue(new StringReader(xmlSnipped), HashMap.class);

        //execution
        for (Map<String, Object> mlObj : content.get("masks")) {
            // convert the markup to markup independent language model
            final Item item = languageSupport.translateToItem(mlObj);
            // fetch a markup language independent translator for this item representing a mask from registry
            final PersistenceDecoder<Mask> converter = persistence.getDecoder(item);
            // decode the item to a mask and add it to the product
            final Mask mask = converter.decode(item, product);
        }

        //verification
        assertThat(product.getMaskGroup().getNodeCount()).isEqualTo(4);
    }

    @Test
    public void testHistoricalDecoding() throws JDOMException, IOException {
        // Reader initializing (Dimap or Zarr)
        // in this case we are simulating Dimap ... a JDOM use case
        final JdomLanguageSupport languageSupport = new JdomLanguageSupport();

        //preparation
        final Product product = new Product("B", "T", RASTER_WIDTH, RASTER_HEIGHT);
        assertThat(product.getMaskGroup().getNodeCount()).isEqualTo(0);
        final String xmlSnipped =
                "<root-element>" +
                "  <mask>" +
                //                                                                                       no persistence ID
                "    <name>mask1</name>" +
                "    <expr>(X * Y + X) &gt; 111</expr>" +                                             // old expression tag
                "    <description>decr1</description>" +
                "    <color_rgb>255, 0, 0</color_rgb>" +
                "    <image_transparency>0.6</image_transparency>" +
                "  </mask>" +
                "  <mask>" +
                "    <___persistence_id___>MyMaskPersistenceConverter:1</___persistence_id___>" +     // old persistence ID
                "    <name>mask2</name>" +
                "    <expr>(X * Y + X) &gt; 222</expr>" +                                             // old expression tag
                "    <description>decr2</description>" +
                "    <color_rgb>0, 255, 0</color_rgb>" +
                "    <image_transparency>0.5</image_transparency>" +
                "  </mask>" +
                "  <mask>" +
                "    <___persistence_id___>TestMaskPersistenceConverter:2</___persistence_id___>" +   // new persistence ID
                "    <name>mask3</name>" +
                "    <expression>(X * Y + X) &gt; 333</expression>" +                                  // new expression tag
                "    <description>decr3</description>" +
                "    <color_rgb>0, 0, 255</color_rgb>" +
                "    <image_transparency>0.4</image_transparency>" +
                "  </mask>" +
                "</root-element>";

        final SAXBuilder builder = new SAXBuilder();
        final Document document = builder.build(new StringReader(xmlSnipped));
        final Element root = document.getRootElement();
        final List<Element> mlObjects = root.getChildren();

        //execution
        for (Element mlObj : mlObjects) {
            // convert the markup to markup independent language model
            final Item item = languageSupport.translateToItem(mlObj);
            // fetch a markup language independent persistence converter for this item representing a mask from registry
            final PersistenceDecoder<Mask> converter = persistence.getDecoder(item);
            // decode the item to a mask and add it to the product
            final Mask mask = converter.decode(item, product);
        }

        //verification
        assertThat(product.getMaskGroup().getNodeCount()).isEqualTo(3);
    }
}
