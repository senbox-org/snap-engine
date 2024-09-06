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

import com.bc.ceres.annotation.STTM;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class TestThatJdomAndJsonLanguageSupportWorksEqualy {

    private MarkupLanguageSupport<Element> jdomSupport;
    private MarkupLanguageSupport<Map<String, Object>> jsonSupport;

    @Before
    public void setUp() throws Exception {
        jdomSupport = new JdomLanguageSupport();
        jsonSupport = new JsonLanguageSupport();
    }

    @Test
    @STTM("SNAP-3481")
    public void testSingleValueProperties() throws JsonProcessingException {
        //preparation
        final List<Item> items1 = Arrays.asList(
                new Property<>("pSt", "a string value"),
                new Property<>("pBD1", new BigDecimal("111111111111111111111111111111.111111")),
                new Property<>("pBD2", new BigDecimal("111111111111111111111111111111.111111").add(new BigDecimal("7777777.77"))),
                new Property<>("pDo1", Double.MAX_VALUE),
                new Property<>("pDo2", Double.MIN_VALUE),
                new Property<>("pDo3", Double.NaN),
                new Property<>("pDo4", Double.NEGATIVE_INFINITY),
                new Property<>("pDo5", Double.POSITIVE_INFINITY),
                new Property<>("pFl", Float.MAX_VALUE),
                new Property<>("pLo", Long.MAX_VALUE),
                new Property<>("pIn", Integer.MAX_VALUE),
                new Property<>("pSh", Short.MAX_VALUE),
                new Property<>("pBy1", Byte.MAX_VALUE),
                new Property<Byte>("pBy2", null),
                new Property<>("pBool1", true),
                new Property<>("pBool2", false),
                new Property<Boolean>("pBool3", null)
        );

        //execution
        final List<Element> jdomElements = toLanguageObjects(items1, jdomSupport);
        final List<Map<String, Object>> jsonElements = toLanguageObjects(items1, jsonSupport);
        final List<Item> itemsFromXML = convertToItems(jdomElements, jdomSupport);
        final List<Item> itemsFromJson = convertToItems(jsonElements, jsonSupport);

        //verification
        assertThat(xmlOut(jdomElements)).contains(
                "<pSt>a string value</pSt>",
                "<pBD1>111111111111111111111111111111.111111</pBD1>",
                "<pBD2>111111111111111111111118888888.881111</pBD2>",
                "<pDo1>1.7976931348623157E308</pDo1>",
                "<pDo2>4.9E-324</pDo2>",
                "<pDo3>NaN</pDo3>",
                "<pDo4>-Infinity</pDo4>",
                "<pDo5>Infinity</pDo5>",
                "<pFl>3.4028235E38</pFl>",
                "<pLo>9223372036854775807</pLo>",
                "<pIn>2147483647</pIn>",
                "<pSh>32767</pSh>",
                "<pBy1>127</pBy1>",
                "<pBy2>null</pBy2>",
                "<pBool1>true</pBool1>",
                "<pBool2>false</pBool2>",
                "<pBool3>null</pBool3>"
        );

        assertThat(jsonOut(jsonElements)).contains(
                "\"pSt\" : \"a string value\"",
                "\"pBD1\" : 111111111111111111111111111111.111111",
                "\"pBD2\" : 111111111111111111111118888888.881111",
                "\"pDo1\" : 1.7976931348623157E308",
                "\"pDo2\" : 4.9E-324",
                "\"pDo3\" : \"NaN\"",
                "\"pDo4\" : \"-Infinity\"",
                "\"pDo5\" : \"Infinity\"",
                "\"pFl\" : 3.4028235E38",
                "\"pLo\" : 9223372036854775807",
                "\"pIn\" : 2147483647",
                "\"pSh\" : 32767",
                "\"pBy1\" : 127",
                "\"pBy2\" : null",
                "\"pBool1\" : true",
                "\"pBool2\" : false",
                "\"pBool3\" : null"
        );

        assertThat(itemsFromXML.size()).isEqualTo(items1.size());
        assertThat(itemsFromJson.size()).isEqualTo(items1.size());

        final List<TrippleForTest> tripples = Arrays.asList(
                new TrippleForTest<>("pSt", 1, (GetterFactory<ValueItem<?>, String>) item -> item::getValueString),
                new TrippleForTest<>("pBD", 2, (GetterFactory<ValueItem<?>, String>) item -> item::getValueString),
                new TrippleForTest<>("pDo", 5, (GetterFactory<ValueItem<?>, Double>) item -> item::getValueDouble),
                new TrippleForTest<>("pFl", 1, (GetterFactory<ValueItem<?>, Float>) item -> item::getValueFloat),
                new TrippleForTest<>("pLo", 1, (GetterFactory<ValueItem<?>, Long>) item -> item::getValueLong),
                new TrippleForTest<>("pIn", 1, (GetterFactory<ValueItem<?>, Integer>) item -> item::getValueInt),
                new TrippleForTest<>("pSh", 1, (GetterFactory<ValueItem<?>, Short>) item -> item::getValueShort),
                new TrippleForTest<>("pBy", 2, (GetterFactory<ValueItem<?>, Byte>) item -> item::getValueByte),
                new TrippleForTest<>("pBool", 3, (GetterFactory<ValueItem<?>, Boolean>) item -> item::getValueBoolean)
        );

        final Map<String, Item> map1 = getPropertyMap(items1);
        final Map[] mapsToBeTested = new Map[]{
                getPropertyMap(itemsFromXML),
                getPropertyMap(itemsFromJson)
        };
        for (TrippleForTest tripple : tripples) {
            int start = tripple.count == 1 ? 0 : 1;
            int end = start + tripple.count;
            final String pref = tripple.namePrefix;
            for (int i = start; i < end; i++) {
                String name = i < 1 ? pref : pref + i;
                for (int j = 0; j < mapsToBeTested.length; j++) {
                    Map map2 = mapsToBeTested[j];
                    final Getter<?> getter1 = tripple.getterFactory.create(map1.get(name));
                    final Getter<?> getter2 = tripple.getterFactory.create(map2.get(name));
                    assertThat(getter1.get())
                            .withFailMessage("Items from " + (j == 0 ? "xml" : "json") + ":  property with name '" + name + "'")
                            .isEqualTo(getter2.get());
                }
            }
        }
    }

    @Test
    @STTM("SNAP-3481")
    @Ignore
    public void testArrayValueProperties() throws JsonProcessingException {
        //preparation
        final List<Item> items1 = Arrays.asList(
                new Property<>("pStrings", new String[]{"A", "[", ",", "b"}),
                new Property<>("pNumArr", new Number[]{16.2, -24.78344e-16, null, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN, 1234567890123456L}),
                new Property<>("pDoubles", new Double[]{16.2, -24.78344e-16, null, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN, Double.parseDouble("1234567890123456")}),
                new Property<>("pFloats", new Float[]{6.2f, -4.78344e-16f, null, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN, Float.parseFloat("1234567890123456")}),
                new Property<>("pLongs", new Long[]{Long.MAX_VALUE, null, 1L, 2L, 3L, 4L}),
                new Property<>("pInts", new Integer[]{Integer.MAX_VALUE, null, 1, 2, 3, 4}),
                new Property<>("pShorts", new Short[]{Short.MAX_VALUE, null, 1, 2, 3, 4}),
                new Property<>("pBytes", new Byte[]{Byte.MAX_VALUE, null, 1, 2, 3, 4}),
                new Property<>("pBooleans", new Boolean[]{true, null, false})
        );

        //execution
        final List<Element> jdomElements = toLanguageObjects(items1, jdomSupport);
        final List<Map<String, Object>> jsonElements = toLanguageObjects(items1, jsonSupport);
        final List<Item> itemsFromXML = convertToItems(jdomElements, jdomSupport);
        final List<Item> itemsFromJson = convertToItems(jsonElements, jsonSupport);

        //verification
        assertThat(xmlOut(jdomElements)).contains(
                "<pStrings>\"A\", \"[\", \",\", \"b\"</pStrings>",
                "<pNumArr>16.2, -2.478344E-15, null, Infinity, -Infinity, NaN, 1234567890123456</pNumArr>",
                "<pDoubles>16.2, -2.478344E-15, null, Infinity, -Infinity, NaN, 1.234567890123456E15</pDoubles>",
                "<pFloats>6.2, -4.78344E-16, null, Infinity, -Infinity, NaN, 1.23456795E15</pFloats>",
                "<pLongs>9223372036854775807, null, 1, 2, 3, 4</pLongs>",
                "<pInts>2147483647, null, 1, 2, 3, 4</pInts>",
                "<pShorts>32767, null, 1, 2, 3, 4</pShorts>",
                "<pBytes>127, null, 1, 2, 3, 4</pBytes>",
                "<pBooleans>true, null, false</pBooleans>"
        );

        assertThat(jsonOut(jsonElements)).contains(
                "\"pStrings\" : [ \"A\", \"[\", \",\", \"b\" ]",
                "\"pNumArr\" : [ 16.2, -2.478344E-15, null, \"Infinity\", \"-Infinity\", \"NaN\", 1234567890123456 ]",
                "\"pDoubles\" : [ 16.2, -2.478344E-15, null, \"Infinity\", \"-Infinity\", \"NaN\", 1.234567890123456E15 ]",
                "\"pFloats\" : [ 6.2, -4.78344E-16, null, \"Infinity\", \"-Infinity\", \"NaN\", 1.23456795E15 ]",
                "\"pLongs\" : [ 9223372036854775807, null, 1, 2, 3, 4 ]",
                "\"pInts\" : [ 2147483647, null, 1, 2, 3, 4 ]",
                "\"pShorts\" : [ 32767, null, 1, 2, 3, 4 ]",
                "\"pBytes\" : [ 127, null, 1, 2, 3, 4 ]",
                "\"pBooleans\" : [ true, null, false ]"
        );

        assertThat(itemsFromXML.size()).isEqualTo(items1.size());
        assertThat(itemsFromJson.size()).isEqualTo(items1.size());

        final TrippleForTest[] tripples = new TrippleForTest[]{
                new TrippleForTest("pStrings", 1, (GetterFactory<ValueItem, String[]>) item -> item::getValueStrings),
                new TrippleForTest("pNumArr", 1, (GetterFactory<ValueItem, Double[]>) item -> item::getValueDoubles),
                new TrippleForTest("pNumArr", 1, (GetterFactory<ValueItem, Float[]>) item -> item::getValueFloats),
                new TrippleForTest("pDoubles", 1, (GetterFactory<ValueItem, Double[]>) item -> item::getValueDoubles),
                new TrippleForTest("pFloats", 1, (GetterFactory<ValueItem, Float[]>) item -> item::getValueFloats),
                new TrippleForTest("pLongs", 1, (GetterFactory<ValueItem, Long[]>) item -> item::getValueLongs),
                new TrippleForTest("pInts", 1, (GetterFactory<ValueItem, Integer[]>) item -> item::getValueInts),
                new TrippleForTest("pShorts", 1, (GetterFactory<ValueItem, Short[]>) item -> item::getValueShorts),
                new TrippleForTest("pBytes", 1, (GetterFactory<ValueItem, Byte[]>) item -> item::getValueBytes),
                new TrippleForTest("pBooleans", 1, (GetterFactory<ValueItem, Boolean[]>) item -> item::getValueBooleans),
        };

        final Map<String, Item> map1 = getPropertyMap(items1);
        final Map[] toBeTested = {
                getPropertyMap(itemsFromXML),
                getPropertyMap(itemsFromJson)
        };
        for (TrippleForTest tripple : tripples) {
            int start = tripple.count == 1 ? 0 : 1;
            int end = start + tripple.count;
            final String pref = tripple.namePrefix;
            for (int i = start; i < end; i++) {
                String name = i < 1 ? pref : pref + i;
                for (int j = 0; j < toBeTested.length; j++) {
                    Map map2 = toBeTested[j];
                    final Getter<?> getter1 = tripple.getterFactory.create(map1.get(name));
                    final Getter<?> getter2 = tripple.getterFactory.create(map2.get(name));
                    assertThat(getter1.get()).as(name)
                            .withFailMessage("Items from " + (j == 0 ? "xml" : "json") + ":  property with name '" + name + "'")
                            .isEqualTo(getter2.get());
                }
            }
        }
    }

    @Test
    @STTM("SNAP-3481")
    public void testContainer() throws JsonProcessingException {
        //preparation
        final Container c1 = new Container("an invalid name");
        c1.add(new Property<>("some", "name"));
        c1.add(new Property<>("int", 42));
        final Container c2 = new Container("c2");
        final Container c3 = new Container("c3");
        c3.set(new Attribute<>("name", "att"));
        c3.add(new Property<>("propC3", new Integer[]{16, 176, 42, 8}));
        c2.add(c3);
        c2.add(new Property<>("propC2", 3230523.41331));
        final List<Item> items1 = Arrays.asList(c1, c2);

        //execution
        final List<Element> jdomElems1 = toLanguageObjects(items1, jdomSupport);
        final List<Item> itemsFromXML = convertToItems(jdomElems1, jdomSupport);
        final List<Element> jdomElems2 = toLanguageObjects(itemsFromXML, jdomSupport);

        final List<Map<String, Object>> jsonElems1 = toLanguageObjects(items1, jsonSupport);
        final List<Item> itemsFromJson = convertToItems(jsonElems1, jsonSupport);
        final List<Map<String, Object>> jsonElems2 = toLanguageObjects(itemsFromJson, jsonSupport);

        //verification
        final String xmlOut1 = xmlOut(jdomElems1);
        final String xmlOut2 = xmlOut(jdomElems2);
        assertThat(xmlOut1).isEqualToIgnoringNewLines(
                "<root>" +
                        "  <an_invalid_name ATTR___THE_UNCHANGED_NAME=\"an invalid name\">" +
                        "    <some>name</some>" +
                        "    <int>42</int>" +
                        "  </an_invalid_name>" +
                        "  <c2>" +
                        "    <c3 name=\"att\">" +
                        "      <propC3>16, 176, 42, 8</propC3>" +
                        "    </c3>" +
                        "    <propC2>3230523.41331</propC2>" +
                        "  </c2>" +
                        "</root>"
        );
        assertThat(xmlOut2).isEqualTo(xmlOut1);

        final String jsonOut1 = jsonOut(jsonElems1);
        final String jsonOut2 = jsonOut(jsonElems2);
        assertThat(jsonOut1).isEqualToIgnoringNewLines(
                "{\n" +
                        "  \"root\" : [ {\n" +
                        "    \"an invalid name\" : {\n" + // this name is invalid in XML case only
                        "      \"some\" : \"name\",\n" +
                        "      \"int\" : 42\n" +
                        "    }\n" +
                        "  }, {\n" +
                        "    \"c2\" : {\n" +
                        "      \"propC2\" : 3230523.41331,\n" +
                        "      \"c3\" : {\n" +
                        "        \"_$ATT$_name\" : \"att\",\n" +
                        "        \"propC3\" : [ 16, 176, 42, 8 ]\n" +
                        "      }\n" +
                        "    }\n" +
                        "  } ]\n" +
                        "}"
        );
        assertThat(jsonOut2).isEqualTo(jsonOut1);

        assertThat(itemsFromXML.size()).isEqualTo(items1.size());
        assertThat(itemsFromJson.size()).isEqualTo(items1.size());
    }

    private <E> List<Item> convertToItems(List<E> jdomElements, MarkupLanguageSupport<E> support) {
        final List<Item> items = new ArrayList<>();
        for (E element : jdomElements) {
            items.add(support.translateToItem(element));
        }
        return items;
    }

    private <E> List<E> toLanguageObjects(List<Item> items1, MarkupLanguageSupport<E> support) {
        final List<E> languageObjects = new ArrayList<>();
        for (Item item : items1) {
            languageObjects.add(support.translateToLanguageObject(item));
        }
        return languageObjects;
    }

    private String xmlOut(List<Element> elements1) {
        final Element root = new Element("root");
        root.addContent(elements1);
        final XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        return xmlOutputter.outputString(root);
    }

    private String jsonOut(List<Map<String, Object>> objects) throws JsonProcessingException {
        final Map root = new LinkedHashMap();
        root.put("root", objects);
        PrettyPrinter prettyPrinter = new DefaultPrettyPrinter()
                .withArrayIndenter(DefaultPrettyPrinter.FixedSpaceIndenter.instance);
        final ObjectWriter writer = new ObjectMapper().writer(prettyPrinter);
        return writer.writeValueAsString(root);
    }

    private static class TrippleForTest<P, T> {
        final String namePrefix;
        final int count;
        final GetterFactory<P, T> getterFactory;

        private TrippleForTest(String namePrefix, int count, GetterFactory<P, T> getterFactory) {
            this.namePrefix = namePrefix;
            this.count = count;
            this.getterFactory = getterFactory;
        }
    }

    private interface GetterFactory<P, T> {
        Getter<T> create(P item);
    }

    private interface Getter<T> {
        T get();
    }

    private Map<String, Item> getPropertyMap(List<Item> properties) {
        final HashMap<String, Item> map1 = new HashMap<>();
        for (Item item : properties) {
            map1.put(item.getName(), item);
        }
        return map1;
    }
}
