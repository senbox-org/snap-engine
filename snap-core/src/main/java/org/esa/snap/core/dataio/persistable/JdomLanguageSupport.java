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

import org.esa.snap.core.util.StringUtils;
import org.jdom.Element;

import java.util.List;

public class JdomLanguageSupport implements MarkupLanguageSupport<Element> {

    @Override
    public Element toLanguageObject(Item item) {
        if (item.isProperty()) {
            final Property p = (Property) item;
            return createPropertyElem(p);
        } else if (item.isContainer()) {
            final Container c = (Container) item;
            return createContainerElem(c);
        } else {
            throw new IllegalArgumentException("Item must be type of Container or type of Property");
        }
    }

    @Override
    public Item convertToItem(Element languageObject) {
        if (languageObject.getChildren().size() == 0) {
            return createProperty(languageObject);
        }
        return createContainer(languageObject);
    }

    private Property createProperty(Element element) {
        final String name = fetchNameFrom(element);
        final String value = element.getValue();
        final Property item;
        final Property<String> property = new Property<>(name, value);
        addAttributes(property, element.getAttributes());
        item = property;
        return item;
    }

    private Container createContainer(Element element) {
        final String name = fetchNameFrom(element);
        final Container container = new Container(name);
        addAttributes(container, element.getAttributes());
        addChildren(container, element.getChildren());
        return container;
    }

    private void addChildren(Container container, List<Element> children) {
        for (Element child : children) {
            if (child.getChildren().size() == 0) {
                container.getProperties().add(createProperty(child));
            } else {
                container.getContainer().add(createContainer(child));
            }
        }
    }

    private void addAttributes(AttributeContainer attCon, List<org.jdom.Attribute> attributes) {
        for (org.jdom.Attribute jdomA : attributes) {
            final String name = jdomA.getName();
            if (ATTR___THE_UNCHANGED_NAME.equals(name)) {
                continue;
            }
            final Attribute attribute = new Attribute(name, jdomA.getValue());
            attCon.getAttributes().add(attribute);
        }
    }

    private Element createContainerElem(Container c) {
        final Element element = createElementWitValidName(c.getName());
        addJdomAttributes(element, c.getAttributes());
        addChildElems(element, c.getContainer());
        addPropertieyElems(element, c.getProperties());
        return element;
    }

    private Element createPropertyElem(Property p) {
        final Element element = createElementWitValidName(p.getName());
        final Object value = p.getValue();
        if (value == null || !value.getClass().isArray()) {
            element.setText(String.valueOf(value));
        } else {
            if (value.getClass().equals(String[].class)) {
                element.setText("\"" + StringUtils.arrayToString(value, "\", \"") + "\"");
            } else {
                element.setText(StringUtils.arrayToString(value, ", "));
            }
        }
        addJdomAttributes(element, p.getAttributes());
        return element;
    }

    private void addJdomAttributes(Element element, List<Attribute> attributes) {
        for (Attribute attribute : attributes) {
            element.setAttribute(attribute.getName(), attribute.getValueString());
        }
    }

    private void addPropertieyElems(Element element, List<Property> properties) {
        for (Property property : properties) {
            element.addContent(createPropertyElem(property));
        }
    }

    private void addChildElems(Element element, List<Container> cList) {
        for (Container container : cList) {
            element.addContent(createContainerElem(container));
        }
    }

    private final static String ATTR___THE_UNCHANGED_NAME = "ATTR___THE_UNCHANGED_NAME";

    static Element createElementWitValidName(String name) {
        final String validXmlName = ensureValidName(name);
        final Element element = new Element(validXmlName);
        if (!validXmlName.equals(name)) {
            element.setAttribute(ATTR___THE_UNCHANGED_NAME, name);
        }
        return element;
    }

    static String fetchNameFrom(Element element) {
        final org.jdom.Attribute unmodifiedName = element.getAttribute(ATTR___THE_UNCHANGED_NAME);
        if (unmodifiedName != null) {
            return unmodifiedName.getValue();
        }
        return element.getName();
    }


    static String ensureValidName(String name) {
        if (name != null) {
            return name.trim().replace(" ", "_");
        }
        return name;
    }
}
