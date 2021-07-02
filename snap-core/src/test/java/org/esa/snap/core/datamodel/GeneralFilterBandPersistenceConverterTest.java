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

import static org.assertj.core.api.Assertions.*;
import static org.esa.snap.core.datamodel.GeneralFilterBandPersistenceConverter.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.esa.snap.core.dataio.persistence.Container;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.JdomLanguageSupport;
import org.esa.snap.core.dataio.persistence.JsonLanguageSupport;
import org.esa.snap.core.dataio.persistence.PersistenceDecoder;
//import org.jdom.Element;
import org.junit.Before;
import org.junit.Test;

//import java.util.ArrayList;
//import java.util.List;

public class GeneralFilterBandPersistenceConverterTest {

    private static final String EXPECTED_JSON = "{\n" +
                                                "  \"Spectral_Band_Info\" : {\n" +
                                                "    \"___persistence_id___\" : \"GenFilterBand:1\",\n" +
                                                "    \"BAND_INDEX\" : \"1\",\n" +
                                                "    \"BAND_NAME\" : \"filteredBand\",\n" +
                                                "    \"BAND_DESCRIPTION\" : \"somehow explainig\",\n" +
                                                "    \"DATA_TYPE\" : \"float32\",\n" +
                                                "    \"PHYSICAL_UNIT\" : \"someUnit\",\n" +
                                                "    \"SOLAR_FLUX\" : \"0.0\",\n" +
                                                "    \"BAND_WAVELEN\" : \"0.0\",\n" +
                                                "    \"BANDWIDTH\" : \"0.0\",\n" +
                                                "    \"SCALING_FACTOR\" : \"1.0\",\n" +
                                                "    \"SCALING_OFFSET\" : \"0.0\",\n" +
                                                "    \"LOG10_SCALED\" : \"false\",\n" +
                                                "    \"NO_DATA_VALUE_USED\" : \"true\",\n" +
                                                "    \"NO_DATA_VALUE\" : \"NaN\",\n" +
                                                "    \"Filter_Band_Info\" : {\n" +
                                                "      \"bandType\" : \"GeneralFilterBand\",\n" +
                                                "      \"FILTER_SOURCE\" : \"anyBand\",\n" +
                                                "      \"FILTER_OP_TYPE\" : \"MAX\",\n" +
                                                "      \"ITERATION_COUNT\" : 3,\n" +
                                                "      \"Filter_Kernel\" : {\n" +
                                                "        \"KERNEL_WIDTH\" : \"2\",\n" +
                                                "        \"KERNEL_HEIGHT\" : \"2\",\n" +
                                                "        \"KERNEL_X_ORIGIN\" : \"0\",\n" +
                                                "        \"KERNEL_Y_ORIGIN\" : \"0\",\n" +
                                                "        \"KERNEL_FACTOR\" : \"1.0\",\n" +
                                                "        \"KERNEL_DATA\" : \"0,0,0,0\"\n" +
                                                "      }\n" +
                                                "    }\n" +
                                                "  }\n" +
                                                "}";

//    static final String VERSION_1_0 = "1.0";
//    static final String VERSION_1_1 = "1.1";
//    static final String VERSION_1_2 = "1.2";

    private GeneralFilterBandPersistenceConverter _generalFilterBandPersistenceConverter;
    private static final double EPS = 1e-6;
    private Product _product;
    private Band _source;
    //    private JdomLanguageSupport _jdomLanguageSupport;
    private JsonLanguageSupport _jsonLanguageSupport;

    @Before
    public void setUp() throws Exception {
        _generalFilterBandPersistenceConverter = new GeneralFilterBandPersistenceConverter();
        _product = new Product("p", "doesntMatter", 2, 2);
        _source = _product.addBand("anyBand", ProductData.TYPE_UINT16);
//        _jdomLanguageSupport = new JdomLanguageSupport();
        _jsonLanguageSupport = new JsonLanguageSupport();
    }

//    @Test
//    public void testCreateObjectFromHistoricDimapXml_Version_1_0() {
//        Element xmlElement = createHistoricDimapXmlElement(VERSION_1_0);
//        final Item item = _jdomLanguageSupport.translateToItem(xmlElement);
//        assertCreateRightGeneralFilterBand(item);
//    }
//
//    @Test
//    public void testCreateObjectFromHistoricDimapXml_Version_1_1() {
//        final Element xmlElement = createHistoricDimapXmlElement(VERSION_1_1);
//        final Item item = _jdomLanguageSupport.translateToItem(xmlElement);
//        assertCreateRightGeneralFilterBand(item);
//    }
//
//    @Test
//    public void testCreateObjectFromHistoricDimapXml_Version_1_2() {
//        final Element xmlElement = createHistoricDimapXmlElement(VERSION_1_2);
//        final Item item = _jdomLanguageSupport.translateToItem(xmlElement);
//        assertCreateRightGeneralFilterBand(item);
//    }

    @Test
    public void testEncode() throws JsonProcessingException {
        //preparation
        final GeneralFilterBand gfb = new GeneralFilterBand("filteredBand", _source, GeneralFilterBand.OpType.MAX,
                                                            new Kernel(2, 2, new double[2 * 2]), 3);
        gfb.setDescription("somehow explainig");
        gfb.setUnit("someUnit");
        _product.addBand(gfb);

        //execution
        final Item item = _generalFilterBandPersistenceConverter.encode(gfb);

        //verification
        assertEqualsExpectedJson(item);

        assertThat(item).isNotNull();
        assertThat(item.isContainer()).isTrue();
        final Container root = item.asContainer();
        assertThat(root.getName()).isEqualTo(ROOT_NAME_SPECTRAL_BAND_INFO);
        assertThat(root.getProperties().length).isEqualTo(14);

        assertThat(root.getProperty(PersistenceDecoder.KEY_PERSISTENCE_ID)).isNotNull();
        assertThat(root.getProperty(PersistenceDecoder.KEY_PERSISTENCE_ID).getValueString())
                .isEqualTo(_generalFilterBandPersistenceConverter.getID());

        assertThat(root.getProperty(PROP_NAME_BAND_INDEX)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_BAND_INDEX).getValueInt())
                .isEqualTo(gfb.getProduct().getBandIndex(gfb.getName()));

        assertThat(root.getProperty(PROP_NAME_BAND_NAME)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_BAND_NAME).getValueString()).isEqualTo(gfb.getName());

        assertThat(root.getProperty(PROP_NAME_BAND_DESCRIPTION)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_BAND_DESCRIPTION).getValueString()).isEqualTo(gfb.getDescription());

        assertThat(root.getProperty(PROP_NAME_DATA_TYPE)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_DATA_TYPE).getValueString())
                .isEqualTo(ProductData.getTypeString(gfb.getDataType()));

        assertThat(root.getProperty(PROP_NAME_PHYSICAL_UNIT)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_PHYSICAL_UNIT).getValueString()).isEqualTo(gfb.getUnit());

        assertThat(root.getProperty(PROP_NAME_SOLAR_FLUX)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_SOLAR_FLUX).getValueFloat()).isEqualTo(gfb.getSolarFlux());

        assertThat(root.getProperty(PROP_NAME_BAND_WAVELEN)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_BAND_WAVELEN).getValueFloat()).isEqualTo(gfb.getSpectralWavelength());

        assertThat(root.getProperty(PROP_NAME_BANDWIDTH)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_BANDWIDTH).getValueFloat()).isEqualTo(gfb.getSpectralBandwidth());

        assertThat(root.getProperty(PROP_NAME_SCALING_FACTOR)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_SCALING_FACTOR).getValueDouble()).isEqualTo(gfb.getScalingFactor());

        assertThat(root.getProperty(PROP_NAME_SCALING_OFFSET)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_SCALING_OFFSET).getValueDouble()).isEqualTo(gfb.getScalingOffset());

        assertThat(root.getProperty(PROP_NAME_LOG_10_SCALED)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_LOG_10_SCALED).getValueBoolean()).isEqualTo(gfb.isLog10Scaled());

        assertThat(root.getProperty(PROP_NAME_NO_DATA_VALUE_USED)).isNotNull();
        assertThat(root.getProperty(PROP_NAME_NO_DATA_VALUE_USED).getValueBoolean()).isEqualTo(gfb.isNoDataValueUsed());

        assertThat(root.getProperty(PROP_NAME_NO_DATA_VALUE)).isNotNull();
        assertThat("" + root.getProperty(PROP_NAME_NO_DATA_VALUE).getValueDouble()).isEqualTo("" + gfb.getNoDataValue());

        assertThat(root.getContainers().length).isEqualTo(1);
        final Container filterBandInfo = root.getContainer(NAME_FILTER_BAND_INFO);
        assertThat(filterBandInfo).isNotNull();
        assertThat(filterBandInfo.getProperties().length).isEqualTo(4);
        assertThat(filterBandInfo.getProperty(PROP_NAME_BAND_TYPE)).isNotNull();
        assertThat(filterBandInfo.getProperty(PROP_NAME_BAND_TYPE).getValueString()).isEqualTo(VALUE_GENERAL_FILTER_BAND_TYPE);
        assertThat(filterBandInfo.getProperty(PROP_NAME_FILTER_SOURCE)).isNotNull();
        assertThat(filterBandInfo.getProperty(PROP_NAME_FILTER_SOURCE).getValueString()).isEqualTo(gfb.getSource().getName());
        assertThat(filterBandInfo.getProperty(PROP_NAME_FILTER_OP_TYPE)).isNotNull();
        assertThat(filterBandInfo.getProperty(PROP_NAME_FILTER_OP_TYPE).getValueString()).isEqualTo(gfb.getOpType().toString());
        assertThat(filterBandInfo.getProperty(PROP_NAME_FILTER_ITERATION_COUNT)).isNotNull();
        assertThat(filterBandInfo.getProperty(PROP_NAME_FILTER_ITERATION_COUNT).getValueInt()).isEqualTo(gfb.getIterationCount());

        assertThat(filterBandInfo.getContainers().length).isEqualTo(1);
        final Container filterKernel = filterBandInfo.getContainer(NAME_FILTER_KERNEL);
        assertThat(filterKernel).isNotNull();
        assertThat(filterKernel.getProperties().length).isEqualTo(6);
        final Kernel kernel = gfb.getStructuringElement();
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_WIDTH)).isNotNull();
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_WIDTH).getValueInt()).isEqualTo(kernel.getWidth());
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_HEIGHT)).isNotNull();
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_HEIGHT).getValueInt()).isEqualTo(kernel.getHeight());
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_X_ORIGIN)).isNotNull();
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_X_ORIGIN).getValueInt()).isEqualTo(kernel.getXOrigin());
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_Y_ORIGIN)).isNotNull();
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_Y_ORIGIN).getValueInt()).isEqualTo(kernel.getYOrigin());
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_FACTOR)).isNotNull();
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_FACTOR).getValueDouble()).isEqualTo(kernel.getFactor());
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_DATA)).isNotNull();
        assertThat(filterKernel.getProperty(PROP_NAME_KERNEL_DATA).getValueString())
                .isEqualTo("0,0,0,0");
    }

    private void assertEqualsExpectedJson(Item item) throws JsonProcessingException {
        final Object o = _jsonLanguageSupport.translateToLanguageObject(item);
        final ObjectMapper objectMapper = new ObjectMapper();
        final String s = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        assertThat(s).isEqualToIgnoringWhitespace(EXPECTED_JSON);
    }

    @Test
    public void testDecode() throws JsonProcessingException {
        //preparation
        final GeneralFilterBand gfbExpected = new GeneralFilterBand("filteredBand", _source, GeneralFilterBand.OpType.MAX,
                                                                    new Kernel(2, 2, new double[2 * 2]), 3);
        gfbExpected.setDescription("somehow explainig");
        gfbExpected.setUnit("someUnit");
        _product.addBand(gfbExpected);
        final Item item = _generalFilterBandPersistenceConverter.encode(gfbExpected);
        _product.removeBand(gfbExpected);

        //execution
        final GeneralFilterBand gfb = _generalFilterBandPersistenceConverter.decode(item, _product);

        //verification
        _product.addBand(gfb);
        assertEqualsExpectedJson(_generalFilterBandPersistenceConverter.encode(gfb));

        assertThat(gfb).isNotNull();
        assertThat(gfb.getName()).isEqualTo(gfbExpected.getName());
        assertThat(gfb.getSource()).isSameAs(gfbExpected.getSource());
        assertThat(gfb.getOpType()).isEqualTo(gfbExpected.getOpType());
        assertThat(gfb.getIterationCount()).isEqualTo(gfbExpected.getIterationCount());
        assertThat(gfb.getDescription()).isEqualTo(gfbExpected.getDescription());
        assertThat(gfb.getUnit()).isEqualTo(gfbExpected.getUnit());
        assertThat(gfb.getSolarFlux()).isEqualTo(gfbExpected.getSolarFlux());
        assertThat(gfb.getSpectralWavelength()).isEqualTo(gfbExpected.getSpectralWavelength());
        assertThat(gfb.getSpectralBandwidth()).isEqualTo(gfbExpected.getSpectralBandwidth());
        assertThat(gfb.getScalingFactor()).isEqualTo(gfbExpected.getScalingFactor());
        assertThat(gfb.getScalingOffset()).isEqualTo(gfbExpected.getScalingOffset());
        assertThat(gfb.isLog10Scaled()).isEqualTo(gfbExpected.isLog10Scaled());
        assertThat(gfb.isNoDataValueUsed()).isEqualTo(gfbExpected.isNoDataValueUsed());
        assertThat("" + gfb.getNoDataValue()).isEqualTo("" + gfbExpected.getNoDataValue());

        assertThat(gfb.getDataType()).isEqualTo(gfbExpected.getDataType());

        final Kernel gfbKernel = gfb.getStructuringElement();
        final Kernel expKernel = gfbExpected.getStructuringElement();

        assertThat(gfbKernel).isNotNull();
        assertThat(expKernel).isNotNull();

        assertThat(gfbKernel.getHeight()).isEqualTo(expKernel.getHeight());
        assertThat(gfbKernel.getWidth()).isEqualTo(expKernel.getWidth());
        assertThat(gfbKernel.getXOrigin()).isEqualTo(expKernel.getXOrigin());
        assertThat(gfbKernel.getYOrigin()).isEqualTo(expKernel.getYOrigin());
        assertThat(gfbKernel.getFactor()).isEqualTo(expKernel.getFactor());
        assertThat(gfbKernel.getKernelData(null)).isEqualTo(expKernel.getKernelData(null));
    }

//    private void assertCreateRightGeneralFilterBand(Item item) {
//        final GeneralFilterBand gfb = _generalFilterBandPersistenceConverter.decode(item, _product);
//        assertThat(gfb).isNotNull();
//        _product.addBand(gfb);
//
//        assertThat(_product.getBandIndex(gfb.getName())).isEqualTo(1);
//        assertThat(gfb.getSpectralBandIndex()).isEqualTo(-1);
//        assertThat(gfb.getName()).isEqualTo("filtered_coffee");
//        assertThat(gfb.getDescription()).isEqualTo("with milk & sugar");
//        assertThat(gfb.getDataType()).isEqualTo(ProductData.TYPE_FLOAT32);
//        assertThat(gfb.getUnit()).isEqualTo("l");
//        assertThat(gfb.getSolarFlux()).isEqualTo(0.0f);
//        assertThat(gfb.getSpectralWavelength()).isEqualTo(0.0f);
//        assertThat(gfb.getSpectralBandwidth()).isEqualTo(0.0f);
//        assertThat(gfb.getScalingFactor()).isEqualTo(1.0);
//        assertThat(gfb.getScalingOffset()).isEqualTo(0.0);
//        assertThat(gfb.isLog10Scaled()).isFalse();
//        assertThat(gfb.getSource().getName()).isEqualTo(_source.getName());
//        assertThat(gfb.getStructuringElement().getWidth()).isEqualTo(5);
//        assertThat(gfb.getStructuringElement().getHeight()).isEqualTo(5);
//        assertThat(gfb.getOpType()).isEqualTo(GeneralFilterBand.OpType.MEAN);
//    }

//    private Element createHistoricDimapXmlElement(final String version) {
//        final List<Element> contentList = new ArrayList<Element>(16);
//        contentList.add(createElement(DimapProductConstants.TAG_BAND_INDEX, "1"));
//        contentList.add(createElement(DimapProductConstants.TAG_BAND_NAME, "filtered_coffee"));
//        contentList.add(createElement(DimapProductConstants.TAG_BAND_DESCRIPTION, "with milk & sugar"));
//        contentList.add(createElement(DimapProductConstants.TAG_DATA_TYPE, "float32"));
//        contentList.add(createElement(DimapProductConstants.TAG_PHYSICAL_UNIT, "l"));
//        contentList.add(createElement(DimapProductConstants.TAG_SOLAR_FLUX, "0.0"));
//        contentList.add(createElement(DimapProductConstants.TAG_BAND_WAVELEN, "0.0"));
//        contentList.add(createElement(DimapProductConstants.TAG_BANDWIDTH, "0.0"));
//        contentList.add(createElement(DimapProductConstants.TAG_SCALING_FACTOR, "1.0"));
//        contentList.add(createElement(DimapProductConstants.TAG_SCALING_OFFSET, "0.0"));
//        contentList.add(createElement(DimapProductConstants.TAG_SCALING_LOG_10, "false"));
//        contentList.add(createElement(DimapProductConstants.TAG_NO_DATA_VALUE_USED, "true"));
//        contentList.add(createElement(DimapProductConstants.TAG_NO_DATA_VALUE,
//                                      String.valueOf(_source.getGeophysicalNoDataValue())));
//        final List<Element> filterBandInfoList = new ArrayList<Element>(5);
//        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SOURCE, "anyBand"));
//        filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_OP_TYPE, "MEAN"));
//        final Element filterBandInfo = new Element(DimapProductConstants.TAG_FILTER_BAND_INFO);
//        filterBandInfo.setAttribute(PROP_NAME_BAND_TYPE, VALUE_GENERAL_FILTER_BAND_TYPE);
//        if (VERSION_1_0.equals(version)) {
//            // Version 1.0
//            filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SUB_WINDOW_WIDTH, "5"));
//            filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SUB_WINDOW_HEIGHT, "5"));
//        } else if (VERSION_1_1.equals(version)) {
//            // Version 1.1
//            filterBandInfo.setAttribute("version", version);
//            filterBandInfoList.add(createElement(DimapProductConstants.TAG_FILTER_SUB_WINDOW_SIZE, "5"));
//        } else {
//            // Version 1.2
//            filterBandInfo.setAttribute("version", version);
//            final List<Element> filterKernelList = new ArrayList<Element>(5);
//            filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_WIDTH, "5"));
//            filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_HEIGHT, "5"));
//            filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_X_ORIGIN, "2"));
//            filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_Y_ORIGIN, "2"));
//            filterKernelList.add(createElement(DimapProductConstants.TAG_KERNEL_DATA, "" +
//                                                                                      "0,0,0,0,0," +
//                                                                                      "0,0,0,0,0," +
//                                                                                      "0,0,0,0,0," +
//                                                                                      "0,0,0,0,0," +
//                                                                                      "0,0,0,0,0"
//            ));
//            final Element kernelElement = new Element(DimapProductConstants.TAG_FILTER_KERNEL);
//            kernelElement.addContent(filterKernelList);
//            filterBandInfoList.add(kernelElement);
//        }
//        filterBandInfo.addContent(filterBandInfoList);
//        contentList.add(filterBandInfo);
//
//        final Element root = new Element(DimapProductConstants.TAG_SPECTRAL_BAND_INFO);
//        root.setContent(contentList);
//        return root;
//    }

//    private static Element createElement(String tagName, String text) {
//        final Element elem = new Element(tagName);
//        elem.setText(text);
//        return elem;
//    }

//    private static Element createElement(String tagName, boolean[] se) {
//        final Element elem = new Element(tagName);
//        StringBuilder text = new StringBuilder();
//        for (boolean b : se) {
//            if (text.length() > 0) {
//                text.append(", ");
//            }
//            text.append(b ? "1" : "0");
//        }
//        elem.setText(text.toString());
//        return elem;
//    }
}