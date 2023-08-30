/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.getasse30;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;


public class GETASSE30ReaderPlugInTest {

    private GETASSE30ReaderPlugIn _plugIn;

    @Before
    public void setUp() {
        _plugIn = new GETASSE30ReaderPlugIn();
    }

    @After
    public void tearDown() {
        _plugIn = null;
    }

    @Test
    public void testValidInputs() {
        testValidInput("./GETASSE30/00N015W.getasse30");
        testValidInput("./GETASSE30/00N015W.GETASSE30");
        testValidInput("./GETASSE30/00N015W.GETaSSe30");
    }

    private void testValidInput(final String s) {
        assertSame(_plugIn.getDecodeQualification(s), DecodeQualification.INTENDED);
        assertSame(_plugIn.getDecodeQualification(new File(s)), DecodeQualification.INTENDED);
    }

    @Test
    public void testInvalidInputs() {
        testInvalidInput("10n143w.GETASSE30.zip");
        testInvalidInput("./GETASSE30/00N015W.getasse30.zip");
        testInvalidInput("./GETASSE30/00N015W.GETASSE30.zip");
        testInvalidInput("./GETASSE30/readme.txt");
        testInvalidInput("./GETASSE30/readme.txt.zip");
        testInvalidInput("./GETASSE30/readme");
        testInvalidInput("./GETASSE30/");
        testInvalidInput("./");
        testInvalidInput(".");
        testInvalidInput("");
        testInvalidInput("./GETASSE30/.hgt");
        testInvalidInput("./GETASSE30/.hgt.zip");
        testInvalidInput("./GETASSE30/.hgt");
        testInvalidInput("./GETASSE30/.hgt.zip");
    }

    private void testInvalidInput(final String s) {
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(s));
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(new File(s)));
    }

    @Test
    public void testThatOtherTypesCannotBeDecoded() throws MalformedURLException {
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(null));
        final URL url = new File("./GETASSE30/readme.txt").toURL();
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(url));
        final Object object = new Object();
        assertEquals(DecodeQualification.UNABLE, _plugIn.getDecodeQualification(object));
    }

    @Test
    public void testCreateReaderInstance() {
        final ProductReader reader = _plugIn.createReaderInstance();
        assertTrue(reader instanceof GETASSE30Reader);
    }

    @Test
    public void testGetInputTypes() {
        final Class[] inputTypes = _plugIn.getInputTypes();
        assertNotNull(inputTypes);
        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    @Test
    public void testGetFormatNames() {
        final String[] formatNames = _plugIn.getFormatNames();
        assertNotNull(formatNames);
        assertEquals(1, formatNames.length);
        assertEquals("GETASSE30", formatNames[0]);
    }

    @Test
    public void testGetDefaultFileExtensions() {
        final String[] defaultFileExtensions = _plugIn.getDefaultFileExtensions();
        assertNotNull(defaultFileExtensions);
        assertEquals(1, defaultFileExtensions.length);
        assertEquals(".getasse30", defaultFileExtensions[0]);
    }
}
