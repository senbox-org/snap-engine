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
package org.esa.snap.stac.internal;

import org.json.simple.JSONObject;

public class EstablishedModifiers {

    public static DownloadModifier planetaryComputer() {
        String signingURL = "https://planetarycomputer.microsoft.com/api/sas/v1/sign?href=";
        return input -> {
            for (int tryCount = 0; tryCount < 20; tryCount++) {
                try {
                    JSONObject signedObject = StacComponent.getJSONFromURLStatic(signingURL + input);
                    return (String) signedObject.get("href");
                } catch (Exception e) {
                    try {
                        // Planetary can throw 429 too many requests errors.
                        // Exponential backoff to mitigate.
                        Thread.sleep((long) Math.pow(500, tryCount));
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            return null;
        };
    }

}
