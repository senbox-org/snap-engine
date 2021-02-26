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
import org.esa.snap.core.dataio.persistable.Item;
import org.esa.snap.core.dataio.persistable.MarkupLanguageSupport;
import org.esa.snap.core.dataio.persistable.Property;
import org.jdom.Attribute;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XmlSupport implements MarkupLanguageSupport<Element> {
    static final String ___UNMODIFIED_NAME___ = "___unmodified_name___";
    final List<Element> elements = new ArrayList<>();

    @Override
    public Container<Element> createRootContainer(String name) {
        final XmlContainer container = (XmlContainer) createContainer(name);
        elements.add(container.element);
        return container;
    }

    @Override
    public List<Element> getCreated() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public List<Item> convert(Element... o) {
        final ArrayList<Item> items = new ArrayList<>();
        for (Element element : o) {
            if (element.getChildren().size() == 0) {
                items.add(new XmlProperty(element));
            } else {
                items.add(new XmlContainer(element));
            }
        }
        return items;
    }

    @Override
    public Property<Element> createProperty(String name, Object value) {
        final Element element = createElementWitValidName(name);
        element.setText(value.toString());
        return new XmlProperty(element);
    }

    @Override
    public Container<Element> createContainer(String name) {
        final Element element = createElementWitValidName(name);
        return new XmlContainer(element);
    }

    static Element createElementWitValidName(String name) {
        final String validXmlName = ensureValidName(name);
        final Element element = new Element(validXmlName);
        if (!validXmlName.equals(name)) {
            element.setAttribute(___UNMODIFIED_NAME___, name);
        }
        return element;
    }

    static String ensureValidName(String name) {
        if (name != null) {
            return name.trim().replace(" ", "_");
        }
        return name;
    }

    static String fetchNameFrom(Element element) {
        final Attribute unmodifiedName = element.getAttribute(___UNMODIFIED_NAME___);
        if (unmodifiedName != null) {
            return unmodifiedName.getValue();
        }
        return element.getName();
    }

}
