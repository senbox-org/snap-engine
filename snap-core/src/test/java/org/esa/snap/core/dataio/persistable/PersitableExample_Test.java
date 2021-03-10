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

package org.esa.snap.core.dataio.persistable;

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
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class PersitableExample_Test {

    private Product product;
    private PersistableRegistry registry;

    @Before
    public void setUp() throws Exception {
        product = new Product("p", "t", 200, 200);
        product.addMask("mask1", "(X * Y + X) > 111", "decr1", Color.RED, 0.6);
        product.addMask("mask2", "(X * Y + X) > 222", "decr2", Color.GREEN, 0.5);
        product.addMask("mask3", "(X * Y + X) > 333", "decr3", Color.BLUE, 0.4);
        product.addMask("mask4", "(X * Y + X) > 444", "decr4", Color.GRAY, 0.3);

        final PersistableFactory factory = new PersistableFactory() {
            @Override
            public <ML, O> Persistable<ML, O> create(MarkupLanguageSupport<ML> support) {
                return (Persistable<ML, O>) new MyMaskPersistable<ML>(support);
            }
        };
        registry = new PersistableRegistry();
        registry.register(Mask.class, factory);
    }

    @Test
    public void testPersistable_Encode_XML() {
        // Writer initializing (Dimap or Zarr)
        // in this case we are simulating Dimap ... a JDOM use case
        final JdomLanguageSupport languageSupport = new JdomLanguageSupport();
        // the language dependent node where the created nodes shall be added
        // in this case a JDOM node
        final Element root = new Element("nodeWhereTheGeneratedElementsShouldBeAdded");

        // detect if masks are to be persisted
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        if (maskGroup.getNodeCount() > 0) {
            // fetch a markup language dependent persistable for Masks from registry
            Persistable<Element, Mask> p = registry.createPersistableFor(Mask.class, languageSupport);
            // ensure there exist a registered persistable for masks
            if (p != null) {
                final Mask[] masks = maskGroup.toArray(new Mask[0]);
                // execution ... let the persistable create the language dependent elements
                for (Mask mask : masks) {
                    root.addContent(p.encode(mask));
                }
            }
        }

        //verification
        final XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        final String out = xmlOutputter.outputString(root);
        assertThat(out).isEqualToIgnoringNewLines(
                "<nodeWhereTheGeneratedElementsShouldBeAdded>\n" +
                "  <mask>\n" +
                "    <name>mask1</name>\n" +
                "    <expression>(X * Y + X) &gt; 111</expression>\n" +
                "    <description>decr1</description>\n" +
                "    <color_rgb>255, 0, 0</color_rgb>\n" +
                "    <image_transparency>0.6</image_transparency>\n" +
                "  </mask>\n" +
                "  <mask>\n" +
                "    <name>mask2</name>\n" +
                "    <expression>(X * Y + X) &gt; 222</expression>\n" +
                "    <description>decr2</description>\n" +
                "    <color_rgb>0, 255, 0</color_rgb>\n" +
                "    <image_transparency>0.5</image_transparency>\n" +
                "  </mask>\n" +
                "  <mask>\n" +
                "    <name>mask3</name>\n" +
                "    <expression>(X * Y + X) &gt; 333</expression>\n" +
                "    <description>decr3</description>\n" +
                "    <color_rgb>0, 0, 255</color_rgb>\n" +
                "    <image_transparency>0.4</image_transparency>\n" +
                "  </mask>\n" +
                "  <mask>\n" +
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
    public void testPersistable_Decode_XML() throws JDOMException, IOException, ParserConfigurationException, SAXException {
        //preparation
        final Product product = new Product("B", "T", 50, 50);
        assertThat(product.getMaskGroup().getNodeCount()).isEqualTo(0);
        final String xmlSnipped = "<root-element>" +
                                  "  <mask>" +
                                  "    <name>mask1</name>" +
                                  "    <expression>(X * Y + X) &gt; 111</expression>" +
                                  "    <description>decr1</description>" +
                                  "    <color_rgb>255, 0, 0</color_rgb>" +
                                  "    <image_transparency>0.6</image_transparency>" +
                                  "  </mask>" +
                                  "  <mask>" +
                                  "    <name>mask2</name>" +
                                  "    <expression>(X * Y + X) &gt; 222</expression>" +
                                  "    <description>decr2</description>" +
                                  "    <color_rgb>0, 255, 0</color_rgb>" +
                                  "    <image_transparency>0.5</image_transparency>" +
                                  "  </mask>" +
                                  "  <mask>" +
                                  "    <name>mask3</name>" +
                                  "    <expression>(X * Y + X) &gt; 333</expression>" +
                                  "    <description>decr3</description>" +
                                  "    <color_rgb>0, 0, 255</color_rgb>" +
                                  "    <image_transparency>0.4</image_transparency>" +
                                  "  </mask>" +
                                  "  <mask>" +
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
        final List<Element> content = root.getChildren();

        //execution
        final Persistable<Element, Mask> persistable = registry.createPersistableFor(Mask.class, new JdomLanguageSupport());
        for (Element element : content) {
            persistable.decode(element, product, product.getSceneRasterSize());
        }

        //verification
        assertThat(product.getMaskGroup().getNodeCount()).isEqualTo(4);
    }

    @Test
    public void testPersistable_Encode_JSON() throws JsonProcessingException {
        // Writer initializing (Dimap or Zarr)
        // in this case we are simulating e.g. SNAP-ZARR ... a JSON use case
        final JsonLanguageSupport languageSupport = new JsonLanguageSupport();
        // the language dependent node where the created nodes shall be added
        // in this case a JSON node is a map
        final Map<String, Object> root = new LinkedHashMap<>();
        final List<Map<String, Object>> mlEntryList = new ArrayList<>();
        root.put("masks", mlEntryList);


        // detect if masks are to be persisted
        final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        if (maskGroup.getNodeCount() > 0) {
            // fetch a markup language dependent persistable for Masks from registry
            Persistable<Map<String, Object>, Mask> p = registry.createPersistableFor(Mask.class, languageSupport);
            // ensure there exist a registered persistable for masks
            if (p != null) {
                final Mask[] masks = maskGroup.toArray(new Mask[0]);
                // execution ... let the persistable create the language dependent elements
                for (Mask mask : masks) {
                    mlEntryList.add(p.encode(mask));
                }
            }
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
                "      \"name\" : \"mask1\",\n" +
                "      \"expression\" : \"(X * Y + X) > 111\",\n" +
                "      \"description\" : \"decr1\",\n" +
                "      \"color_rgb\" : [ 255, 0, 0 ],\n" +
                "      \"image_transparency\" : 0.6\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
                "      \"name\" : \"mask2\",\n" +
                "      \"expression\" : \"(X * Y + X) > 222\",\n" +
                "      \"description\" : \"decr2\",\n" +
                "      \"color_rgb\" : [ 0, 255, 0 ],\n" +
                "      \"image_transparency\" : 0.5\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
                "      \"name\" : \"mask3\",\n" +
                "      \"expression\" : \"(X * Y + X) > 333\",\n" +
                "      \"description\" : \"decr3\",\n" +
                "      \"color_rgb\" : [ 0, 0, 255 ],\n" +
                "      \"image_transparency\" : 0.4\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
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
    public void testPersistable_Decode_JSON() throws JDOMException, IOException, ParserConfigurationException, SAXException {
        //preparation
        final Product product = new Product("B", "T", 50, 50);
        assertThat(product.getMaskGroup().getNodeCount()).isEqualTo(0);
        final String xmlSnipped =
                "{\n" +
                "  \"masks\" : [ {\n" +  // node where the generated json should be added
                "    \"mask\" : {\n" +
                "      \"name\" : \"mask1\",\n" +
                "      \"expression\" : \"(X * Y + X) > 111\",\n" +
                "      \"description\" : \"decr1\",\n" +
                "      \"color_rgb\" : [ 255, 0, 0 ],\n" +
                "      \"image_transparency\" : 0.6\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
                "      \"name\" : \"mask2\",\n" +
                "      \"expression\" : \"(X * Y + X) > 222\",\n" +
                "      \"description\" : \"decr2\",\n" +
                "      \"color_rgb\" : [ 0, 255, 0 ],\n" +
                "      \"image_transparency\" : 0.5\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
                "      \"name\" : \"mask3\",\n" +
                "      \"expression\" : \"(X * Y + X) > 333\",\n" +
                "      \"description\" : \"decr3\",\n" +
                "      \"color_rgb\" : [ 0, 0, 255 ],\n" +
                "      \"image_transparency\" : 0.4\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"mask\" : {\n" +
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
        final Persistable<Map<String, Object>, Mask> persistable = registry.createPersistableFor(Mask.class, new JsonLanguageSupport());
        for (Map<String, Object> toDecode : content.get("masks")) {
            persistable.decode(toDecode, product, product.getSceneRasterSize());
        }

        //verification
        assertThat(product.getMaskGroup().getNodeCount()).isEqualTo(4);
    }

    private static class PersistableRegistry {

        private final Map<Type, List<PersistableFactory>> registry = new HashMap<>();

        public void register(Type type, PersistableFactory factory) {
            if (!registry.containsKey(type)) {
                registry.put(type, new ArrayList<>());
            }
            registry.get(type).add(factory);
        }

        public <ML, T> Persistable<ML, T> createPersistableFor(Class<? extends T> aClass, MarkupLanguageSupport<ML> support) {
            final List<PersistableFactory> persistableFactories = registry.get(aClass);
            final PersistableFactory persistableFactory = persistableFactories.get(0);
            return persistableFactory.create(support);
        }
    }

    private interface PersistableFactory {
        <ML, O> Persistable<ML, O> create(MarkupLanguageSupport<ML> support);
    }

    private static class MyMaskPersistable<ML> extends Persistable<ML, Mask> {
        public MyMaskPersistable(MarkupLanguageSupport<ML> support) {
            super(support);
        }

        @Override
        protected Mask decodeItem(Item item, Product product, Dimension regionRasterSize) {
            if (!item.isContainer()) {
                throw new IllegalArgumentException("Container expected.");
            }
            final String itemName = item.getName();
            if (!"mask".equals(itemName)) {
                throw new IllegalArgumentException("Item with name 'mask' expected but was '" + itemName + "'.");
            }
            final Container container = (Container) item;
            final String[] expectedProperties = {
                    "name",
                    "expression",
                    "description",
                    "color_rgb",
                    "image_transparency"
            };
            final Property[] props = new Property[expectedProperties.length];
            for (int i = 0; i < props.length; i++) {
                final String propName = expectedProperties[i];
                final Property property = container.getProperty(propName);
                if (property == null) {
                    throw new IllegalArgumentException("Property '" + propName + "' expected.");
                }
                props[i] = property;
            }
            final String maskName = props[0].getValueString();
            final String expression = props[1].getValueString();
            final String description = props[2].getValueString();
            final Integer[] rgb = props[3].getValueInts();
            final Double transparency = props[4].getValueDouble();

            return product.addMask(
                    maskName,
                    expression,
                    description,
                    new Color(rgb[0], rgb[1], rgb[2]),
                    transparency);
        }

        @Override
        protected Item encodeObject(Mask mask) {
            final Container cont = new Container("mask");
            final List<Property> props = cont.getProperties();
            props.add(new Property<>("name", mask.getName()));
            props.add(new Property<>("expression", Mask.BandMathsType.getExpression(mask)));
            props.add(new Property<>("description", mask.getDescription()));
            final Color cl = mask.getImageColor();
            final int[] rgb = new int[]{cl.getRed(), cl.getGreen(), cl.getBlue()};
            props.add(new Property<>("color_rgb", rgb));
            props.add(new Property<>("image_transparency", mask.getImageTransparency()));
            return cont;
        }
    }
}
