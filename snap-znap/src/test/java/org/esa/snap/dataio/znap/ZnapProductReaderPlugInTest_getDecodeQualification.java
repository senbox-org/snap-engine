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

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.bc.zarr.ZarrConstants;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.util.io.TreeDeleter;
import org.junit.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ZnapProductReaderPlugInTest_getDecodeQualification {

    private ZnapProductReaderPlugIn plugIn;
    private Path productRoot;
    private Path zarrRootHeader;
    private Path aRasterDataDir;
    private Path zarrHeaderFile;
    private Path testPath;

    @Before
    public void setUp() throws Exception {
        plugIn = new ZnapProductReaderPlugIn();
        testPath = Files.createTempDirectory("temporary-snap-development-test-path");
        Files.createDirectories(testPath);
        productRoot = testPath.resolve("snap_zarr_product_root_dir.znap");
        zarrRootHeader = productRoot.resolve(ZarrConstants.FILENAME_DOT_ZGROUP);
        aRasterDataDir = productRoot.resolve("a_raster_data_dir");
        zarrHeaderFile = aRasterDataDir.resolve(ZarrConstants.FILENAME_DOT_ZARRAY);
    }

    @After
    public void tearDown() throws IOException {
        TreeDeleter.deleteDir(testPath);
    }

    @Test
    public void decodeQualification_INTENDED_perfectMatch_inputIsPathObject() throws IOException {
        Files.createDirectories(aRasterDataDir);
        Files.createFile(zarrRootHeader);
        Files.createFile(zarrHeaderFile);

        final Object input = this.productRoot;
        assertThat(plugIn.getDecodeQualification(input),
                   is(equalTo(DecodeQualification.INTENDED)));
    }

    @Test
    public void decodeQualification_INTENDED_perfectMatch_inputIsFileObject() throws IOException {
        Files.createDirectories(aRasterDataDir);
        Files.createFile(zarrRootHeader);
        Files.createFile(zarrHeaderFile);

        final Object input = this.productRoot.toFile();
        assertThat(plugIn.getDecodeQualification(input),
                   is(equalTo(DecodeQualification.INTENDED)));
    }

    @Test
    public void decodeQualification_INTENDED_perfectMatch_inputStringObject() throws IOException {
        Files.createDirectories(aRasterDataDir);
        Files.createFile(zarrRootHeader);
        Files.createFile(zarrHeaderFile);

        final Object input = this.productRoot.toString();
        assertThat(plugIn.getDecodeQualification(input),
                   is(equalTo(DecodeQualification.INTENDED)));
    }

    @Test
    public void decodeQualification_UNABLE_inputObjectIsNullOrCanNotBeConvertedToPath() throws IOException {
        Files.createDirectories(aRasterDataDir);
        Files.createFile(zarrRootHeader);
        Files.createFile(zarrHeaderFile);


        assertThat(plugIn.getDecodeQualification(null),
                   is(equalTo(DecodeQualification.UNABLE)));

        assertThat(plugIn.getDecodeQualification(productRoot.toUri()),
                   is(equalTo(DecodeQualification.UNABLE)));
    }

    @Test
    public void decodeQualification_UNABLE_productRootDoesNotExist() {
        // No file or directory as expected

        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(productRoot);

        assertThat(decodeQualification, is(equalTo(DecodeQualification.UNABLE)));
    }

    @Test
    public void decodeQualification_UNABLE_productRootPathIsFileInsteadOfDirectory() throws IOException {
        Files.createFile(productRoot);

        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(productRoot);

        assertThat(decodeQualification, is(equalTo(DecodeQualification.UNABLE)));
    }

    @Test
    public void decodeQualification_UNABLE_NoSnapHeaderFile() throws IOException {
        Files.createDirectories(productRoot);
        Files.createDirectories(aRasterDataDir);
        Files.createFile(zarrHeaderFile);

        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(productRoot);

        assertThat(decodeQualification, is(equalTo(DecodeQualification.UNABLE)));
    }

    @Test
    public void decodeQualification_UNABLE_SnapHeaderFileExistButIsADirectory() throws IOException {
        Files.createDirectories(productRoot);
        Files.createDirectories(zarrRootHeader);
        Files.createDirectories(aRasterDataDir);
        Files.createFile(zarrHeaderFile);

        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(productRoot);

        assertThat(decodeQualification, is(equalTo(DecodeQualification.UNABLE)));
    }

    @Test
    public void decodeQualification_UNABLE_NoZarrHeaderFile() throws IOException {
        Files.createDirectories(productRoot);
        Files.createFile(zarrRootHeader);
        Files.createDirectories(aRasterDataDir);

        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(productRoot);

        assertThat(decodeQualification, is(equalTo(DecodeQualification.UNABLE)));
    }

    @Test
    public void decodeQualification_UNABLE_ZarrHeaderFileExistButIsADirectory() throws IOException {
        Files.createDirectories(productRoot);
        Files.createFile(zarrRootHeader);
        Files.createDirectories(aRasterDataDir);
        Files.createDirectories(zarrHeaderFile);

        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(productRoot);

        assertThat(decodeQualification, is(equalTo(DecodeQualification.UNABLE)));
    }
}