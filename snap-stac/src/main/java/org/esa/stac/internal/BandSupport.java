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

import org.esa.stac.extensions.EO;
import org.esa.stac.extensions.Proj;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;

public class BandSupport {

    private BandSupport() {
    }

    public static Dimension getMaxDimension(final JSONObject propertiesJSON) {
        if (propertiesJSON.containsKey(Proj.shape)) {
            JSONArray shape = (JSONArray)propertiesJSON.get(Proj.shape);
            if(shape.size() > 1) {
                return new Dimension(JsonUtils.getInt(shape.get(1)), JsonUtils.getInt(shape.get(0)));
            }
        }
        if (propertiesJSON.containsKey(EO.bands)) {
            // find overall bands
            final JSONArray bandsArray = (JSONArray) propertiesJSON.get(EO.bands);
            int maxWidth = 0, maxHeight = 0;
            for (Object o : bandsArray) {
                final JSONObject bandJSON = (JSONObject) o;
                Dimension bandDim = getMaxDimension(bandJSON);
                if(bandDim != null) {
                    if (bandDim.width > maxWidth) {
                        maxWidth = bandDim.width;
                    }
                    if (bandDim.height > maxHeight) {
                        maxHeight = bandDim.height;
                    }
                }
            }
            if (maxWidth > 0 && maxHeight > 0) {
                return new Dimension(maxWidth, maxHeight);
            }
        }
        return null;
    }
}
