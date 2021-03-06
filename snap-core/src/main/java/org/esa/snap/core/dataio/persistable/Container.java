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

import java.util.ArrayList;
import java.util.List;

public class Container extends Item implements AttributeContainer {

    private final ArrayList<Attribute> attributes = new ArrayList<>();
    private final ArrayList<Property> properties = new ArrayList<>();
    private final ArrayList<Container> containers = new ArrayList<>();

    public Container(String name) {
        super(name);
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    public List<Attribute> getAttributes() {
        return attributes;
    }

    public Attribute getAttribute(String name) {
        for (Attribute attr : attributes) {
            if (attr.getName().equals(name)) {
                return attr;
            }
        }
        return null;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public Property getProperty(String name) {
        for (Property prop : properties) {
            if (prop.getName().equals(name)) {
                return prop;
            }
        }
        return null;
    }

    public List<Container> getContainer() {
        return containers;
    }
    public Container getContainer(String name) {
        for (Container prop : containers) {
            if (prop.getName().equals(name)) {
                return prop;
            }
        }
        return null;
    }
}
