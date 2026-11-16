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

package com.bc.ceres.core.runtime.internal;

import org.junit.Test;

import java.nio.file.Path;

import static org.junit.Assert.*;

public class JarFilenameFilterTest {

    @Test
    public void testNPE() {
        try {
            JarFilenameFilter.isJarName(null);
            fail();
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void testAccept() {
        JarFilenameFilter filter = new JarFilenameFilter();
        Path dir = Path.of(".");
        assertTrue(filter.test(dir.resolve("xstream.jar")));
        assertTrue(filter.test(dir.resolve("xstream.JAR")));
        assertTrue(filter.test(dir.resolve("xstream.zip")));
        assertTrue(filter.test(dir.resolve("xstream.ZIP")));
        assertTrue(filter.test(dir.resolve("lib/xstream.jar")));
        assertTrue(filter.test(dir.resolve("lib/xstream.JAR")));
        assertTrue(filter.test(dir.resolve("lib/xstream.zip")));
        assertTrue(filter.test(dir.resolve("lib/xstream.ZIP")));
        assertFalse(filter.test(dir.resolve("xstream")));
        assertFalse(filter.test(dir.resolve("xstream.txt")));
        assertFalse(filter.test(dir.resolve("xstream.JaR")));
        assertFalse(filter.test(dir.resolve(".jar")));
        assertFalse(filter.test(dir.resolve("")));
    }

    @Test
    public void testIsJarName() {
        assertTrue(JarFilenameFilter.isJarName("xstream.jar"));
        assertTrue(JarFilenameFilter.isJarName("xstream.JAR"));
        assertTrue(JarFilenameFilter.isJarName("xstream.zip"));
        assertTrue(JarFilenameFilter.isJarName("xstream.ZIP"));
        assertTrue(JarFilenameFilter.isJarName("lib/xstream.jar"));
        assertTrue(JarFilenameFilter.isJarName("lib/xstream.JAR"));
        assertTrue(JarFilenameFilter.isJarName("lib/xstream.zip"));
        assertTrue(JarFilenameFilter.isJarName("lib/xstream.ZIP"));
        assertFalse(JarFilenameFilter.isJarName("xstream"));
        assertFalse(JarFilenameFilter.isJarName("xstream.txt"));
        assertFalse(JarFilenameFilter.isJarName("xstream.JaR"));
        assertFalse(JarFilenameFilter.isJarName(".jar"));
        assertFalse(JarFilenameFilter.isJarName(""));
    }
}
