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

package org.esa.snap.core.dataio.dimap;

import org.esa.snap.GlobalTestConfig;
import org.esa.snap.GlobalTestTools;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

public class DimapDiscontinuity_ReadAndWriteTest {

    private Product product;
    private Path ioDir;
    private DimapProductWriterPlugIn writerPlugin;
    private DimapProductReaderPlugIn readerPlugIn;
    private File outDir;

    @Before
    public void setUp() throws Exception {
        product = new Product("name", "type", 20, 30);
        product.addBand("aBand", ProductData.TYPE_INT16);
        product.addTiePointGrid(new TiePointGrid("dis_no", 10, 15, .5f, .5f, 2, 2, new float[10 * 15], TiePointGrid.DISCONT_NONE));
        product.addTiePointGrid(new TiePointGrid("dis180", 10, 15, .5f, .5f, 2, 2, new float[10 * 15], TiePointGrid.DISCONT_AT_180));
        product.addTiePointGrid(new TiePointGrid("dis360", 10, 15, .5f, .5f, 2, 2, new float[10 * 15], TiePointGrid.DISCONT_AT_360));
        writerPlugin = new DimapProductWriterPlugIn();
        readerPlugIn = new DimapProductReaderPlugIn();
        outDir = GlobalTestConfig.getSnapTestDataOutputDirectory();
    }

    @After
    public void tearDown() throws Exception {
        GlobalTestTools.deleteTestDataOutputDirectory();
    }

    @Test
    public void testThatDiscontinuityPropertyOfTiePointGridsAreCyclicDi() throws IOException {
        assertThat(product.getTiePointGrid("dis_no").getDiscontinuity(), is(TiePointGrid.DISCONT_NONE));
        assertThat(product.getTiePointGrid("dis180").getDiscontinuity(), is(TiePointGrid.DISCONT_AT_180));
        assertThat(product.getTiePointGrid("dis360").getDiscontinuity(), is(TiePointGrid.DISCONT_AT_360));

        final File productFile = new File(outDir, "prod.dim");
        writerPlugin.createWriterInstance().writeProductNodes(product, productFile);

        final List<Path> collect = Files.list(outDir.toPath()).collect(Collectors.toList());
        assertThat(collect.size(), is(2));

        final Product product = readerPlugIn.createReaderInstance().readProductNodes(productFile, null);
        assertThat(product.getTiePointGrid("dis_no").getDiscontinuity(), is(TiePointGrid.DISCONT_NONE));
        assertThat(product.getTiePointGrid("dis180").getDiscontinuity(), is(TiePointGrid.DISCONT_AT_180));
        assertThat(product.getTiePointGrid("dis360").getDiscontinuity(), is(TiePointGrid.DISCONT_AT_360));
    }

}
