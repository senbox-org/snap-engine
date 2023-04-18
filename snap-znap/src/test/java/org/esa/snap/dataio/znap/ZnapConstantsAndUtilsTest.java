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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.jsonToMetadata;
import static org.esa.snap.dataio.znap.ZnapConstantsAndUtils.metadataToJson;

public class ZnapConstantsAndUtilsTest {

    @Test
    public void metadataTransformation_toJsonStringAndBack() throws IOException {
        final MetadataElement nested = new MetadataElement("nested");
        nested.setDescription("nested Desc");
        nested.addAttribute(new MetadataAttribute("doub", ProductData.createInstance(new double[]{24.3}), true));
        nested.addAttribute(new MetadataAttribute("floa", ProductData.createInstance(new float[]{2.4f, 3.5f}), true));
        nested.addAttribute(new MetadataAttribute("long", ProductData.createInstance(new long[]{12345678901L}), true));
        nested.addAttribute(new MetadataAttribute("int", ProductData.createInstance(new int[]{12345678}), false));
        nested.addAttribute(new MetadataAttribute("shor", ProductData.createInstance(new short[]{12345}), true));
        nested.addAttribute(new MetadataAttribute("byte", ProductData.createInstance(new byte[]{123}), false));
        nested.addAttribute(new MetadataAttribute("stri", ProductData.createInstance("String att value"), true));

        final MetadataElement m1 = new MetadataElement("M1");
        m1.setDescription("D1");
        m1.addAttribute(new MetadataAttribute("any", ProductData.createInstance(ProductData.TYPE_UTC, new int[]{244, 233, 6}), true));
        m1.addElement(nested);

        final MetadataElement single = new MetadataElement("single");
        single.setDescription("single desc");
        final MetadataAttribute single_att = new MetadataAttribute("single att", new ProductData.Float(new float[]{-124.5F, 8.9F}), false);
        single_att.setUnit("singleUnit");
        single.addAttribute(single_att);

        final MetadataElement[] elements = {m1, single};

        final String json = metadataToJson(elements);
        final MetadataElement[] readElements = jsonToMetadata(json);

        assertThat(readElements.length).isEqualTo(elements.length);
        for (int i = 0; i < elements.length; i++) {
            MetadataElement e1 = readElements[i];
            MetadataElement e2 = elements[i];
            equalMetadataElements(e1, e2);
        }
    }

    private void equalMetadataElements(MetadataElement e1, MetadataElement e2) {
        assertThat(e1.getName()).isEqualTo(e2.getName());
        assertThat(e1.getDescription()).isEqualTo(e2.getDescription());
        assertThat(e1.getNumElements()).isEqualTo(e2.getNumElements());
        final MetadataElement[] elements1 = e1.getElements();
        final MetadataElement[] elements2 = e2.getElements();
        for (int i = 0; i < elements2.length; i++) {
            MetadataElement v1 = elements1[i];
            MetadataElement v2 = elements2[i];
            equalMetadataElements(v1, v2);
        }
        assertThat(e1.getNumAttributes()).isEqualTo(e2.getNumAttributes());
        final MetadataAttribute[] attributes1 = e1.getAttributes();
        final MetadataAttribute[] attributes2 = e2.getAttributes();
        for (int i = 0; i < attributes2.length; i++) {
            MetadataAttribute a1 = attributes1[i];
            MetadataAttribute a2 = attributes2[i];
            assertThat(a1.getName()).isEqualTo(a2.getName());
            assertThat(a1.getDescription()).isEqualTo(a2.getDescription());
            assertThat(a1.getUnit()).isEqualTo(a2.getUnit());
            assertThat(a1.getDataType()).isEqualTo(a2.getDataType());
            assertThat(a1.getNumDataElems()).isEqualTo(a2.getNumDataElems());
            assertThat(a1.getData().getElems()).isEqualTo(a2.getData().getElems());
        }
    }

    @Test
    public void testThatMetadataCanBeConvertedToListAndReturn() throws JsonProcessingException {
        //preparation
        final MetadataElement elem = new MetadataElement("elem");
        elem.addAttribute(new MetadataAttribute("Aname", ProductData.createInstance(new int[]{1, 2, 3, 4}), false));
        final MetadataElement elem2 = new MetadataElement("elem2");
        elem2.addAttribute(new MetadataAttribute("desc", new ProductData.ASCII("A short description"), true));
        final MetadataElement[] elements = {elem, elem2};

        //execution
        final List<Map<String, Object>> list = ZnapConstantsAndUtils.metadataToList(elements);
        final MetadataElement[] convertedElems = ZnapConstantsAndUtils.listToMetadata(list);

        //verification
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(ZnapConstantsAndUtils.metadataModule);
        final ObjectWriter ppWriter = objectMapper.writerWithDefaultPrettyPrinter();
        final String elemsJSON = ppWriter.writeValueAsString(elements);
        final String converted = ppWriter.writeValueAsString(convertedElems);
        assertThat(converted).isEqualTo(elemsJSON);
    }
}