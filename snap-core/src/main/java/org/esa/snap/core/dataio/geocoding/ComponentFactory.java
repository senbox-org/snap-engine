/*
 *
 * Copyright (C) 2020 Brockmann Consult GmbH (info@brockmann-consult.de)
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
 *
 */

package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.geocoding.forward.*;
import org.esa.snap.core.dataio.geocoding.inverse.InversePlugin;
import org.esa.snap.core.dataio.geocoding.inverse.PixelGeoIndexInverse;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;

import java.util.HashMap;

public class ComponentFactory {

    private static final HashMap<String, ForwardPlugin> forwardPlugins = new HashMap<>();
    private static final HashMap<String, InversePlugin> inversePlugins = new HashMap<>();

    static {
        forwardPlugins.put(PixelForward.KEY, new PixelForward.Plugin());
        forwardPlugins.put(PixelInterpolatingForward.KEY, new PixelInterpolatingForward.Plugin());
        forwardPlugins.put(TiePointBilinearForward.KEY, new TiePointBilinearForward.Plugin());
        forwardPlugins.put(TiePointSplineForward.KEY, new TiePointSplineForward.Plugin());

        inversePlugins.put(PixelQuadTreeInverse.KEY, new PixelQuadTreeInverse.Plugin(false));
        inversePlugins.put(PixelQuadTreeInverse.KEY_INTERPOLATING, new PixelQuadTreeInverse.Plugin(true));
        inversePlugins.put(PixelGeoIndexInverse.KEY, new PixelGeoIndexInverse.Plugin(false));
        inversePlugins.put(PixelGeoIndexInverse.KEY_INTERPOLATING, new PixelGeoIndexInverse.Plugin(true));
        inversePlugins.put(TiePointInverse.KEY, new TiePointInverse.Plugin());
    }

    public static ForwardCoding getForward(String key) {
        final ForwardPlugin forwardPlugin = forwardPlugins.get(key);
        if (forwardPlugin == null) {
            throw new IllegalArgumentException("unknown forward coding: " + key);
        }

        return forwardPlugin.create();
    }

    public static InverseCoding getInverse(String key) {
        final InversePlugin inversePlugin = inversePlugins.get(key);
        if (inversePlugin == null) {
            throw new IllegalArgumentException("unknown inverse coding: " + key);
        }

        return inversePlugin.create();
    }
}
