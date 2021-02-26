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

package org.esa.snap.core.dataio.persistable.xml;

import org.esa.snap.core.dataio.persistable.Container;
import org.esa.snap.core.dataio.persistable.Property;
import org.jdom.Element;

import java.util.*;

class XmlContainer implements Container<Element> {

    final Element element;
    private final Map<String, Property<Element>> properties = new LinkedHashMap<>();
    private final Map<String, Container<Element>> containers = new LinkedHashMap<>();

    XmlContainer(Element element) {
        this.element = element;
        initialize();
    }

    private void initialize() {
        final List<Element> children = element.getChildren();
        for (Element child : children) {
            final String name = XmlSupport.fetchNameFrom(child);
            final int numChildren = child.getChildren().size();
            if (numChildren == 0) {
                properties.put(name, new XmlProperty(child));
            } else {
                containers.put(name, new XmlContainer(child));
            }
        }
    }

    @Override
    public List<Property<Element>> getProperties() {
        return Collections.unmodifiableList(new ArrayList<>(properties.values()));
    }

    @Override
    public List<Container<Element>> getContainer() {
        return Collections.unmodifiableList(new ArrayList<>(containers.values()));
    }

    @Override
    public Property<Element> getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Container<Element> getContainer(String name) {
        return containers.get(name);
    }

    @Override
    public void set(Property<Element> property) {
        Element e = property.get();
        element.addContent(e);
        properties.put(property.getName(), property);
    }

    @Override
    public void set(Container<Element> container) {
        element.addContent(((XmlContainer) container).element);
        containers.put(container.getName(), container);
    }

    @Override
    public String getName() {
        return XmlSupport.fetchNameFrom(element);
    }
}
