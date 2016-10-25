/*
 * $Id: $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.s3tbx.meris.brr.operator;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by marcoz.
 *
 * @author marcoz
 */
public class BrrOpTest {
    
    @Test
    public void testRhoSpectralIndex() {

        assertTrue(BrrOp.isValidRhoSpectralIndex(0));
        assertFalse(BrrOp.isValidRhoSpectralIndex(10));
        assertTrue(BrrOp.isValidRhoSpectralIndex(11));
        assertFalse(BrrOp.isValidRhoSpectralIndex(14));

        assertFalse(BrrOp.isValidRhoSpectralIndex(-1));
        assertFalse(BrrOp.isValidRhoSpectralIndex(15));
    }
}
