/*
 *
 * Copyright (c) 2021.  Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.dataio.persistence;

public class Item {

    private final String name;

    public Item(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name must be not null and not empty");
        }
        this.name = name;
    }

    public final String getName() {
        return name;
    }

    public boolean isAttribute() {
        return false;
    }

    public boolean isProperty() {
        return false;
    }

    public boolean isContainer() {
        return false;
    }
}
