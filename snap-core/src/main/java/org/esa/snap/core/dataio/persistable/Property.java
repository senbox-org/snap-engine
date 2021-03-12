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

package org.esa.snap.core.dataio.persistable;

import java.util.LinkedHashMap;

public class Property<E> extends ValueItem<E> implements AttributeContainer {

    private final LinkedHashMap<String, Attribute<?>> attributes = new LinkedHashMap<>();

    public Property(String name, E value) {
        super(name, value);
    }

    @Override
    public boolean isProperty() {
        return true;
    }

    @Override
    public Attribute<?>[] getAttributes() {
        return attributes.values().toArray(new Attribute[0]);
    }

    public Attribute<?> getAttribute(String name) {
        return attributes.get(name);
    }

    public void add(Attribute<?> attribute) {
        if (attribute == null) {
            return;
        }
        final String name = attribute.getName();
        if (attributes.containsKey(name)) {
            throw new IllegalArgumentException("Already contains an attribute with the name '" + name + "'.");
        }
        attributes.put(name, attribute);
    }
}
