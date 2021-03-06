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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonLanguageSupport implements MarkupLanguageSupport<Map<String, Object>> {


    public static final String IS_PROPERTY = "_@%$_is_property";
    public static final String VALUE = "value";
    public static final String ATT_PREFIX = "_$ATT$_";

    @Override
    public List<Map<String, Object>> toLanguageObjects(List<Item> items) {
        final ArrayList<Map<String, Object>> maps = new ArrayList<>();
        final Map<String, Object> map = new LinkedHashMap<>();
        maps.add(map);
        for (Item item : items) {
            if (item.isProperty()) {
                addProperty(map, (Property) item);
            } else {
                addContainer(map, (Container) item);
            }
        }
        return maps;
    }

    @Override
    public List<Item> convertToItems(List<Map<String, Object>> lang) {
        final ArrayList<Item> items = new ArrayList<>();
        for (Map<String, Object> objects : lang) {
            for (Map.Entry<String, Object> entry : objects.entrySet()) {
                final String name = entry.getKey();
                final Object value = entry.getValue();
                if (isListOfMaps(value)) {
                    final List list = (List) value;
                    for (Object obj : list) {
                        items.add(createItem(name, obj));
                    }
                } else {
                    items.add(createItem(name, value));
                }
            }
        }
        return items;
    }

    private void addContainer(Map<String, Object> map, Container cont) {
        final LinkedHashMap<String, Object> newContainerMap = new LinkedHashMap<>();
        final String name = cont.getName();
        if (map.containsKey(name)) {
            final Object o = map.get(name);
            if (o instanceof List) {
                ((List) o).add(newContainerMap);
            } else {
                final ArrayList list = new ArrayList();
                list.add(o);
                list.add(newContainerMap);
                map.put(name, list);
            }
        } else {
            map.put(name, newContainerMap);
        }
        final List<Attribute> attributes = cont.getAttributes();
        for (Attribute attribute : attributes) {
            addAttribute(newContainerMap, attribute);
        }
        final List<Property> properties = cont.getProperties();
        for (Property property : properties) {
            addProperty(newContainerMap, property);
        }
        final List<Container> containers = cont.getContainer();
        for (Container container : containers) {
            addContainer(newContainerMap, container);
        }
    }

    private void addProperty(Map<String, Object> map, Property property) {
        final List<Attribute> attributes = property.getAttributes();
        if (attributes.size() == 0) {
            map.put(property.getName(), property.getValue());
        } else {
            final LinkedHashMap<String, Object> mapProp = new LinkedHashMap<>();
            map.put(property.getName(), mapProp);
            mapProp.put(IS_PROPERTY, true);
            for (Attribute attribute : attributes) {
                mapProp.put(ATT_PREFIX + attribute.getName(), attribute.getValue());
            }
            mapProp.put(VALUE, property.getValue());
        }
    }

    private void addAttribute(Map<String, Object> map, Attribute attribute) {
        map.put(ATT_PREFIX + attribute.getName(), attribute.getValue());
    }

    private Item createItem(String name, Object value) {
        if (value instanceof Map) {
            final Map<String, Object> map = (Map) value;
            if (map.containsKey(IS_PROPERTY)) {
                return createPropertyContainingAttributes(name, map);
            } else {
                return createContainer(name, map);
            }
        } else {
            return new Property<>(name, value);
        }
    }

    private boolean isListOfMaps(Object value) {
        if (value instanceof List) {
            final List list = (List) value;
            for (Object o : list) {
                if (!(o instanceof Map)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private Container createContainer(String name, Map<String, Object> map) {
        final Container container = new Container(name);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (key.startsWith(ATT_PREFIX)) {
                container.getAttributes().add(new Attribute(getAttName(key), value));
            } else if (value instanceof Map) {
                container.getContainer().add(createContainer(key, (Map) value));
            } else {
                container.getProperties().add(new Property(key, value));
            }
        }
        return container;
    }

    private Property createPropertyContainingAttributes(String name, Map<String, Object> map) {
        final Property property = new Property<>(name, map.get(VALUE));
        for (String key : map.keySet()) {
            if (key.startsWith(ATT_PREFIX)) {
                final String attName = getAttName(key);
                final Object attValue = map.get(key);
                property.getAttributes().add(new Attribute(attName, attValue));
            }
        }
        return property;
    }

    private String getAttName(String attName) {
        return attName.substring(ATT_PREFIX.length());
    }
}
