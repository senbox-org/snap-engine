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

import java.util.LinkedHashMap;

public class Container extends Item implements AttributeContainer {

    private final LinkedHashMap<String, Attribute<?>> attributes = new LinkedHashMap<>();
    private final LinkedHashMap<String, Property<?>> properties = new LinkedHashMap<>();
    private final LinkedHashMap<String, Container> containers = new LinkedHashMap<>();

    public Container(String name) {
        super(name);
    }

    @Override
    public boolean isContainer() {
        return true;
    }

    @Override
    public Container asContainer() {
        return this;
    }

    public Attribute<?>[] getAttributes() {
        return attributes.values().toArray(new Attribute[0]);
    }

    public Attribute<?> getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Attribute<?> removeAttribute(String name) {
        return attributes.remove(name);
    }

    public void set(Attribute<?> attribute) {
        if (attribute == null) {
            return;
        }
        final String name = attribute.getName();
        attributes.remove(name);
        attributes.put(name, attribute);
    }

    public Property<?>[] getProperties() {
        return properties.values().toArray(new Property[0]);
    }

    public Property<?> getProperty(String name) {
        return properties.get(name);
    }

    public Property<?> removeProperty(String name) {
        return properties.remove(name);
    }

    public void add(Property<?> property) {
        if (property == null) {
            return;
        }
        final String name = property.getName();
        if (properties.containsKey(name)) {
            throw new IllegalArgumentException("Already contains a property with the name '" + name + "'.");
        }
        properties.put(name, property);
    }

    public Container[] getContainers() {
        return containers.values().toArray(new Container[0]);
    }

    public Container getContainer(String name) {
        return containers.get(name);
    }

    public Container removeContainer(String name) {
        return containers.remove(name);
    }

    public void add(Container container) {
        if (container == null) {
            return;
        }
        final String name = container.getName();
        if (containers.containsKey(name)) {
            throw new IllegalArgumentException("Already contains a container with the name '" + name + "'.");
        }
        containers.put(name, container);
    }
}
