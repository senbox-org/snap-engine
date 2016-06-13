/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry.smilecorr;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author muhammad.bc.
 */
public class SmileCorretionOpTest {
    @Test
    public void testIsNumber() throws Exception {
        SmileCorretionOp corretionOp = new SmileCorretionOp();
        assertTrue(corretionOp.isWholeNumber("23"));
        assertTrue(corretionOp.isWholeNumber("235665"));
        assertTrue(corretionOp.isWholeNumber("257"));
        assertFalse(corretionOp.isWholeNumber("hi"));
        assertFalse(corretionOp.isWholeNumber("§0983"));
        assertFalse(corretionOp.isWholeNumber("0983.!"));
        assertFalse(corretionOp.isWholeNumber("0983.098345903485"));
        assertFalse(corretionOp.isWholeNumber("0983.5345"));


    }
}