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

public class TarVirtualDirTest {
    @Test
    public void testIsTgz() {
        assertFalse(TarVirtualDir.isTgz("xxxxx.nc.gz"));
        assertFalse(TarVirtualDir.isTgz("xxxxx.ppt.gz"));
        assertFalse(TarVirtualDir.isTgz("xxxxx.gz"));
        assertFalse(TarVirtualDir.isTgz("xxxxx.tar"));
        assertFalse(TarVirtualDir.isTgz("xxxxx.geotiff"));

        assertTrue(TarVirtualDir.isTgz("xxxxx.tgz"));
        assertTrue(TarVirtualDir.isTgz("xxxxx.tGz"));
        assertTrue(TarVirtualDir.isTgz("xxxxx.tar.gz"));
        assertTrue(TarVirtualDir.isTgz("xxxxx.TAR.gz"));
    }
}
