/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.stac.internal;

import org.junit.Assert;
import org.junit.Test;

public class TestDownloadModifier {
    @Test
    public void testPlanetaryModifier() {
        String tifURL = "https://sentinel2l2a01.blob.core.windows.net/sentinel2-l2/10/T/CR/2022/08/28/S2A_MSIL2A_20220828T190931_N0400_R056_T10TCR_20220830T153754.SAFE/GRANULE/L2A_T10TCR_A037519_20220828T191548/IMG_DATA/R10m/T10TCR_20220828T190931_B08_10m.tif";
        String signed = EstablishedModifiers.planetaryComputer().signURL(tifURL);
        Assert.assertNotEquals(tifURL, signed);
        Assert.assertTrue(signed.contains(tifURL));
    }
}
