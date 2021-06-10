/*
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
 */

package org.esa.snap.dataio.znap.snap;

import org.esa.snap.core.dataio.geometry.WriterBasedVectorDataNodeWriter;
import org.esa.snap.core.datamodel.PinDescriptor;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.Product;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class VectorDataNodeWriterTest {

    private Product product;

    @Before
    public void setUp() throws Exception {
        product = new Product("testProduct", "testType", 100, 150);
    }

    @Test
    public void testWriteOutPins() throws IOException {
        //preparation
        final PinDescriptor pinDescriptor = PinDescriptor.getInstance();
        product.getPinGroup().add(Placemark.createPointPlacemark(
                pinDescriptor, "Pin1", "Label1", "Text1", new PixelPos(2, 3), null, null));
        product.getPinGroup().add(Placemark.createPointPlacemark(
                pinDescriptor, "Pin2", "Label2", "Text2", new PixelPos(5, 6), null, null));
        final StringWriter stringWriter = new StringWriter();

        //execution
        final WriterBasedVectorDataNodeWriter vectorDataNodeWriter = new WriterBasedVectorDataNodeWriter();
        vectorDataNodeWriter.write(product.getPinGroup().getVectorDataNode(), stringWriter);

        //verification
        final String t = "\t";
        final String[] expected = {
                "#defaultCSS=symbol:pin; fill:#0000ff; fill-opacity:0.7; stroke:#ffffff; stroke-opacity:1.0; stroke-width:0.5",
                "org.esa.snap.Pin", t, "geometry:Point", t, "style_css:String", t, "label:String", t, "text:String", t, "pixelPos:Point", t, "geoPos:Point", t, "dateTime:Date",
                "Pin1", t, "POINT (2 3)", t, "[null]", t, "Label1", t, "Text1", t, "POINT (2 3)", t, "[null]", t, "[null]",
                "Pin2", t, "POINT (5 6)", t, "[null]", t, "Label2", t, "Text2", t, "POINT (5 6)", t, "[null]", t, "[null]",
        };
        assertThat(stringWriter.toString(), is(stringContainsInOrder(expected)));
    }
}