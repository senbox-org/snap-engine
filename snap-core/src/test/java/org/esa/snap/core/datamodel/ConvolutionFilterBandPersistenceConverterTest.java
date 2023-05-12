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
import org.apache.commons.jxpath.xml.JDOMParser;
import org.esa.snap.core.dataio.dimap.JDomHelper;
import org.esa.snap.core.dataio.dimap.spi.DimapHistoricalDecoder;
import org.esa.snap.core.dataio.persistence.HistoricalDecoder;
import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.dataio.persistence.JdomLanguageSupport;
import org.esa.snap.core.dataio.persistence.JsonLanguageSupport;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.esa.snap.core.datamodel.ConvolutionFilterBandPersistenceConverter.*;

public class ConvolutionFilterBandPersistenceConverterTest {

    private final JdomLanguageSupport _jdomLanguageSupport = new JdomLanguageSupport();
    private final JsonLanguageSupport _jsonLanguageSupport = new JsonLanguageSupport();
    private ConvolutionFilterBandPersistenceConverter _persistenceConverter;
    private Product _product;
    private Band _source;

    @Before
    public void setUp() throws Exception {
        _persistenceConverter = new ConvolutionFilterBandPersistenceConverter();
        _product = new Product("p", "doesntMatter", 2, 2);
        _source = _product.addBand("anyBand", ProductData.TYPE_UINT16);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getID() {
        assertThat(_persistenceConverter.getID()).isEqualTo("ConvFilterBand:1");
    }

    @Test
    public void encode() throws JsonProcessingException {
        //preparation
        final ConvolutionFilterBand cfb = createFilterBand();
        _product.addBand(cfb);

        //execution
        final Item item = _persistenceConverter.encode(cfb);

        //verification
        final Element xmlElement = _jdomLanguageSupport.translateToLanguageObject(item);

        /**
         * Turn this "false" to "true", if you want to see the whole XML in a junit error message.
         * This is should only be done if you are having trouble understanding the test and want to
         * see the XML for analysis purposes.
         * Attention!! Do not push the "true" into the git repository.
         */
        boolean wantToSeeTheXmlInFailedAssertEqualMessage = false;
        if (wantToSeeTheXmlInFailedAssertEqualMessage) {
            final XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
            assertThat(xmlOutputter.outputString(xmlElement)).isEqualTo("");
        }

        /**
         * Turn this "false" to "true", if you want to see the whole JSON in a junit error message.
         * This is should only be done if you are having trouble understanding the test and want to
         * see the JSON for analysis purposes.
         * Attention!! Do not push the "true" into the git repository.
         */
        final boolean wantToSeeTheJsonInFailedAssertEqualMessage = false;
        if (wantToSeeTheJsonInFailedAssertEqualMessage) {
            final Object jsonObjects = _jsonLanguageSupport.translateToLanguageObject(item);
            final ObjectWriter jsonOutputer = new ObjectMapper().writerWithDefaultPrettyPrinter();
            assertThat(jsonOutputer.writeValueAsString(jsonObjects)).isEqualTo("");
        }


        assertThat(xmlElement).isNotNull();
        assertThat(xmlElement.getName()).isEqualTo(ROOT_NAME_SPECTRAL_BAND_INFO);
        assertThat(xmlElement.getChildren().size()).isEqualTo(15);
        assertThat(xmlElement.getChild(PROP_NAME_BAND_INDEX)).isNotNull();
        assertThat(xmlElement.getChild(PROP_NAME_BAND_NAME)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_BAND_NAME)).isEqualTo(cfb.getName());
        assertThat(xmlElement.getChild(PROP_NAME_BAND_DESCRIPTION)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_BAND_DESCRIPTION)).isEqualTo(cfb.getDescription());
        assertThat(xmlElement.getChild(PROP_NAME_DATA_TYPE)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_DATA_TYPE)).isEqualTo(ProductData.getTypeString(cfb.getDataType()));
        assertThat(xmlElement.getChild(PROP_NAME_PHYSICAL_UNIT)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_PHYSICAL_UNIT)).isEqualTo(cfb.getUnit());
        assertThat(xmlElement.getChild(PROP_NAME_SOLAR_FLUX)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_SOLAR_FLUX)).isEqualTo("" + cfb.getSolarFlux());
        assertThat(xmlElement.getChild(PROP_NAME_BAND_WAVELEN)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_BAND_WAVELEN)).isEqualTo("" + cfb.getSpectralWavelength());
        assertThat(xmlElement.getChild(PROP_NAME_BANDWIDTH)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_BANDWIDTH)).isEqualTo("" + cfb.getSpectralBandwidth());
        assertThat(xmlElement.getChild(PROP_NAME_SCALING_FACTOR)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_SCALING_FACTOR)).isEqualTo("" + cfb.getScalingFactor());
        assertThat(xmlElement.getChild(PROP_NAME_SCALING_OFFSET)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_SCALING_OFFSET)).isEqualTo("" + cfb.getScalingOffset());
        assertThat(xmlElement.getChild(PROP_NAME_LOG_10_SCALED)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_LOG_10_SCALED)).isEqualToIgnoringCase("" + cfb.isLog10Scaled());
        assertThat(xmlElement.getChild(PROP_NAME_NO_DATA_VALUE_USED)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_NO_DATA_VALUE_USED)).isEqualTo("" + cfb.isNoDataValueUsed());
        assertThat(xmlElement.getChild(PROP_NAME_NO_DATA_VALUE)).isNotNull();
        assertThat(xmlElement.getChildTextTrim(PROP_NAME_NO_DATA_VALUE)).isEqualTo("" + cfb.getNoDataValue());

        final Element filterInfo = xmlElement.getChild(NAME_FILTER_BAND_INFO);
        assertThat(filterInfo).isNotNull();
        assertThat(filterInfo.getChildren().size()).isEqualTo(4);
        assertThat(filterInfo.getChildTextTrim(PROP_NAME_BAND_TYPE)).isEqualTo(VALUE_CONVOLUTION_FILTER_BAND);
        assertThat(filterInfo.getChild(PROP_NAME_FILTER_SOURCE)).isNotNull();
        assertThat(filterInfo.getChildTextTrim(PROP_NAME_FILTER_SOURCE)).isEqualTo(cfb.getSource().getName());
        assertThat(filterInfo.getChildTextTrim(PROP_NAME_FILTER_ITERATION_COUNT)).isEqualTo("" + cfb.getIterationCount());

        final Kernel kernel = cfb.getKernel();
        final Element kernelInfo = filterInfo.getChild(NAME_FILTER_KERNEL);
        assertThat(kernelInfo).isNotNull();
        assertThat(kernelInfo.getChildren().size()).isEqualTo(6);
        assertThat(kernelInfo.getChild(PROP_NAME_KERNEL_WIDTH)).isNotNull();
        assertThat(kernelInfo.getChildTextTrim(PROP_NAME_KERNEL_WIDTH)).isEqualTo("" + kernel.getWidth());
        assertThat(kernelInfo.getChild(PROP_NAME_KERNEL_HEIGHT)).isNotNull();
        assertThat(kernelInfo.getChildTextTrim(PROP_NAME_KERNEL_HEIGHT)).isEqualTo("" + kernel.getHeight());
        assertThat(kernelInfo.getChild(PROP_NAME_KERNEL_FACTOR)).isNotNull();
        assertThat(kernelInfo.getChildTextTrim(PROP_NAME_KERNEL_FACTOR)).isEqualTo("" + kernel.getFactor());
        assertThat(kernelInfo.getChild(PROP_NAME_KERNEL_DATA)).isNotNull();
        assertThat(kernelInfo.getChildTextTrim(PROP_NAME_KERNEL_DATA)).isEqualTo(ConvolutionFilterBandPersistenceConverter.toCsv(kernel.getKernelData(null)));
    }

    @Test
    public void decode() {
        //preparation
        final ConvolutionFilterBand cfb = createFilterBand();
        _product.addBand(cfb);
        final Item item = _persistenceConverter.encode(cfb);
        _product.removeBand(cfb);

        //execution
        final ConvolutionFilterBand decodedFilterBand = _persistenceConverter.decode(item, _product);

        //verification
        assertThat(decodedFilterBand).isNotNull();
        _product.addBand(decodedFilterBand);

        assertThat(decodedFilterBand.getSpectralBandIndex()).isEqualTo(-1);
        assertThat(_product.getBandIndex(decodedFilterBand.getName())).isEqualTo(1);
        assertThat(decodedFilterBand.getName()).isEqualTo("filteredBand");
        assertThat(decodedFilterBand.getDescription()).isEqualTo("somehow explainig");
        assertThat(decodedFilterBand.getDataType()).isEqualTo(ProductData.TYPE_FLOAT32);
        assertThat(decodedFilterBand.getUnit()).isEqualTo("someUnit");
        assertThat(decodedFilterBand.getSolarFlux()).isEqualTo(0.0f);
        assertThat(decodedFilterBand.getSpectralWavelength()).isEqualTo(0.0f);
        assertThat(decodedFilterBand.getSpectralBandwidth()).isEqualTo(0.0f);
        assertThat(decodedFilterBand.getScalingFactor()).isEqualTo(1.0);
        assertThat(decodedFilterBand.getScalingOffset()).isEqualTo(0.0);
        assertThat(decodedFilterBand.isLog10Scaled()).isFalse();
        assertThat(decodedFilterBand.isNoDataValueUsed()).isTrue();
        assertThat(decodedFilterBand.getSource().getName()).isEqualTo(_source.getName());
        assertThat(decodedFilterBand.getKernel().getWidth()).isEqualTo(3);
        assertThat(decodedFilterBand.getKernel().getHeight()).isEqualTo(3);
        assertThat(decodedFilterBand.getKernel().getFactor()).isEqualTo(1.7);
        assertThat(decodedFilterBand.getKernel().getKernelData(null))
                .isEqualTo(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0});
    }

    @Test
    public void canDecodeHistoricalDimapOutput() throws IOException, JDOMException {
        //preparation
        final byte[] historicalBytes = historicDimapOutput.getBytes(StandardCharsets.UTF_8);
        final InputStream inputStream = new ByteArrayInputStream(historicalBytes);
        final Document xml = JDomHelper.parse(inputStream);
        final Item item = _jdomLanguageSupport.translateToItem(xml.getRootElement());

        //execution
        assertThat(_persistenceConverter.canDecode(item)).isTrue();
        final ConvolutionFilterBand histFilterBand = _persistenceConverter.decode(item, _product);
        _product.addBand(histFilterBand);

        //verification
        final Item itemFromHistoricalFB = _persistenceConverter.encode(histFilterBand);
        final Element element = _jdomLanguageSupport.translateToLanguageObject(itemFromHistoricalFB);
        final String xmlFromHistorical = new XMLOutputter(Format.getPrettyFormat()).outputString(element);
        _product.removeBand(histFilterBand);

        final ConvolutionFilterBand newFilterBand = createFilterBand();
        _product.addBand(newFilterBand);
        final Item itemExpected = _persistenceConverter.encode(newFilterBand);
        final Element elementExpected = _jdomLanguageSupport.translateToLanguageObject(itemExpected);
        final String expected = new XMLOutputter(Format.getPrettyFormat()).outputString(elementExpected);

        assertThat(xmlFromHistorical).isEqualTo(expected);
    }

    @Test
    public void getHistoricalDecoders() {
        final HistoricalDecoder[] historicalDecoders = _persistenceConverter.getHistoricalDecoders();
        assertThat(historicalDecoders.length).isEqualTo(1);
        assertThat(historicalDecoders[0]).isInstanceOf(DimapHistoricalDecoder.class);
    }

    private ConvolutionFilterBand createFilterBand() {
        final double[] kernelData = new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0};
        final ConvolutionFilterBand cfb = new ConvolutionFilterBand("filteredBand", _source,
                new Kernel(3, 3, 1.7, kernelData), 1);
        cfb.setDescription("somehow explainig");
        cfb.setUnit("someUnit");
        return cfb;
    }

    private final String historicDimapOutput =
            "<Spectral_Band_Info>\n" +
                    "  <BAND_INDEX>1</BAND_INDEX>\n" +
                    "  <BAND_NAME>filteredBand</BAND_NAME>\n" +
                    "  <BAND_DESCRIPTION>somehow explainig</BAND_DESCRIPTION>\n" +
                    "  <DATA_TYPE>float32</DATA_TYPE>\n" +
                    "  <PHYSICAL_UNIT>someUnit</PHYSICAL_UNIT>\n" +
                    "  <SOLAR_FLUX>0.0</SOLAR_FLUX>\n" +
                    "  <BAND_WAVELEN>0.0</BAND_WAVELEN>\n" +
                    "  <BANDWIDTH>0.0</BANDWIDTH>\n" +
                    "  <SCALING_FACTOR>1.0</SCALING_FACTOR>\n" +
                    "  <SCALING_OFFSET>0.0</SCALING_OFFSET>\n" +
                    "  <LOG10_SCALED>false</LOG10_SCALED>\n" +
                    "  <NO_DATA_VALUE_USED>true</NO_DATA_VALUE_USED>\n" +
                    "  <NO_DATA_VALUE>NaN</NO_DATA_VALUE>\n" +
                    "  <Filter_Band_Info bandType=\"ConvolutionFilterBand\">\n" +
                    "    <FILTER_SOURCE>anyBand</FILTER_SOURCE>\n" +
                    "    <Filter_Kernel>\n" +
                    "      <KERNEL_WIDTH>3</KERNEL_WIDTH>\n" +
                    "      <KERNEL_HEIGHT>3</KERNEL_HEIGHT>\n" +
                    "      <KERNEL_X_ORIGIN>1</KERNEL_X_ORIGIN>\n" +
                    "      <KERNEL_Y_ORIGIN>1</KERNEL_Y_ORIGIN>\n" +
                    "      <KERNEL_FACTOR>1.7</KERNEL_FACTOR>\n" +
                    "      <KERNEL_DATA>1,2,3,4,5,6,7,8,9</KERNEL_DATA>\n" +
                    "    </Filter_Kernel>\n" +
                    "  </Filter_Band_Info>\n" +
                    "</Spectral_Band_Info>";
}