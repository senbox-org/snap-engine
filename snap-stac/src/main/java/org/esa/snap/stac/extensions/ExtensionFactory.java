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
package org.esa.snap.stac.extensions;

public class ExtensionFactory {

    public static void getExtensions() {

    }

    public static String getSchema(final String ext) {
        switch (ext) {
            case EO.eo:
                return EO.schema;
            case Proj.proj:
                return Proj.schema;
            case Raster.raster:
                return Raster.schema;
            case SAR.sar:
                return SAR.schema;
            case Sat.sat:
                return Sat.schema;
            case View.view:
                return View.schema;
        }
        return ext;
    }
}
