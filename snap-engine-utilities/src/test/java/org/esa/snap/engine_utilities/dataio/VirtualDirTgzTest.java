/*
 *
 *  Copyright (c) 2022.
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

package org.esa.snap.engine_utilities.dataio;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VirtualDirTgzTest {
    @Test
    public void testIsTgz() {
        assertFalse(VirtualDirTgz.isTgz("xxxxx.nc.gz"));
        assertFalse(VirtualDirTgz.isTgz("xxxxx.ppt.gz"));
        assertFalse(VirtualDirTgz.isTgz("xxxxx.gz"));
        assertFalse(VirtualDirTgz.isTgz("xxxxx.tar"));
        assertFalse(VirtualDirTgz.isTgz("xxxxx.geotiff"));

        assertTrue(VirtualDirTgz.isTgz("xxxxx.tgz"));
        assertTrue(VirtualDirTgz.isTgz("xxxxx.tGz"));
        assertTrue(VirtualDirTgz.isTgz("xxxxx.tar.gz"));
        assertTrue(VirtualDirTgz.isTgz("xxxxx.TAR.gz"));
    }
}
