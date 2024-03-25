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

package com.bc.ceres.binio;

import com.bc.ceres.binio.util.ByteArrayIOHandler;
import com.bc.ceres.test.LongTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static com.bc.ceres.binio.TypeBuilder.*;
import static org.junit.Assert.assertEquals;

@RunWith(LongTestRunner.class)
public class ReadWriteTest {

    @Before
    public void setUp() throws Exception {
        new File("test.dat").delete();
    }

    @After
    public void tearDown() {
        new File("test.dat").delete();
    }

    @Test
    public void testFixCompound() throws IOException {
        CompoundType type = COMPOUND("Complex", MEMBER("x", DOUBLE), MEMBER("y", DOUBLE));

        ByteArrayIOHandler byteArrayIOHandler = new ByteArrayIOHandler();
        TracingIOHandler tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        DataContext context = new DataFormat(type).createContext(tracingIOHandler);
        CompoundData complex = context.getData();
        complex.setDouble("x", 23.04);
        complex.setDouble("y", 10.12);
        complex.flush();
        assertEquals("R(0,16)W(0,16)", tracingIOHandler.getTrace());
        tracingIOHandler.reset();
        assertEquals(23.04, complex.getDouble("x"), 1e-10);
        assertEquals(10.12, complex.getDouble("y"), 1e-10);
        assertEquals("", tracingIOHandler.getTrace());

        final byte[] byteData = byteArrayIOHandler.toByteArray();
        byteArrayIOHandler = new ByteArrayIOHandler(byteData);
        tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        context = new DataFormat(type).createContext(tracingIOHandler);
        complex = context.getData();
        assertEquals(23.04, complex.getDouble("x"), 1e-10);
        assertEquals(10.12, complex.getDouble("y"), 1e-10);
        assertEquals("R(0,16)", tracingIOHandler.getTrace());
    }

    @Test
    public void testCompoundWithFixSequenceOfFixCompounds() throws IOException {
        CompoundType type =
                COMPOUND("Data",
                        MEMBER("Complex_List",
                                SEQUENCE(COMPOUND("Complex",
                                        MEMBER("x", DOUBLE),
                                        MEMBER("y", DOUBLE)), 5)));

        ByteArrayIOHandler byteArrayIOHandler = new ByteArrayIOHandler();
        TracingIOHandler tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        DataContext context = new DataFormat(type).createContext(tracingIOHandler);
        CompoundData data = context.getData();
        SequenceData seq = data.getSequence("Complex_List");
        for (int i = 0; i < 5; i++) {
            CompoundData complex = seq.getCompound(i);
            complex.setDouble("x", i + 23.04);
            complex.setDouble("y", i + 10.12);
            complex.flush();
        }
        assertEquals("R(0,16)W(0,16)R(16,16)W(16,16)R(32,16)W(32,16)R(48,16)W(48,16)R(64,16)W(64,16)", tracingIOHandler.getTrace());

        tracingIOHandler.reset();
        for (int i = 0; i < 5; i++) {
            CompoundData complex = seq.getCompound(i);
            assertEquals(i + 23.04, complex.getDouble("x"), 1e-10);
            assertEquals(i + 10.12, complex.getDouble("y"), 1e-10);
        }
        assertEquals("R(0,16)R(16,16)R(32,16)R(48,16)R(64,16)", tracingIOHandler.getTrace());

        final byte[] byteData = byteArrayIOHandler.toByteArray();
        byteArrayIOHandler = new ByteArrayIOHandler(byteData);
        tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        context = new DataFormat(type).createContext(tracingIOHandler);
        data = context.getData();
        seq = data.getSequence("Complex_List");
        for (int i = 0; i < 5; i++) {
            CompoundData complex = seq.getCompound(i);
            assertEquals(i + 23.04, complex.getDouble("x"), 1e-10);
            assertEquals(i + 10.12, complex.getDouble("y"), 1e-10);
        }

        assertEquals("R(0,16)R(16,16)R(32,16)R(48,16)R(64,16)", tracingIOHandler.getTrace());
    }

    @Test
    public void testWriteVarSequence() throws IOException {
        CompoundType type =
                COMPOUND("Data",
                        MEMBER("Counter", INT),
                        MEMBER("Complex_List",
                                VAR_SEQUENCE(COMPOUND("Complex",
                                        MEMBER("x", DOUBLE),
                                        MEMBER("y", DOUBLE)), "Counter")));

        ByteArrayIOHandler byteArrayIOHandler = new ByteArrayIOHandler();
        TracingIOHandler tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        DataContext context = new DataFormat(type).createContext(tracingIOHandler);
        CompoundData data = context.getData();
        data.setInt("Counter", -1);
        SequenceData seq = data.getSequence("Complex_List");
        assertEquals(0, seq.getElementCount());
        assertEquals(0, seq.getSize());
        data.flush();
        assertEquals("R(0,4)W(0,4)", tracingIOHandler.getTrace());
    }
}
