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

package org.esa.snap.core.dataio.geocoding.inverse;

class Result {

    int x;
    int y;
    double delta;

    Result() {
        delta = Double.MAX_VALUE;
    }

    final boolean update(final int x, final int y, final double delta) {
        final boolean b = delta < this.delta;
        if (b) {
            this.x = x;
            this.y = y;
            this.delta = delta;
        }
        return b;
    }
}
