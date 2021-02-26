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

package org.esa.snap.core.dataio.persistable.json;

import org.esa.snap.core.dataio.persistable.Container;
import org.esa.snap.core.dataio.persistable.Item;
import org.esa.snap.core.dataio.persistable.MarkupLanguageSupport;
import org.esa.snap.core.dataio.persistable.Property;
import org.jdom.Element;

import java.util.*;

public class JsonSupport implements MarkupLanguageSupport<Map<String, Object>> {
    final List<Element> elements = new ArrayList<>();

    @Override
    public Container<Map<String, Object>> createRootContainer(String name) {
        return null;
    }

    @Override
    public List<Map<String, Object>> getCreated() {
        return null;
    }

    @Override
    public List<Item> convert(Map<String, Object>... o) {
        return null;
    }

    @Override
    public Property<Map<String, Object>> createProperty(String name, Object value) {
        return null;
    }

    @Override
    public Container<Map<String, Object>> createContainer(String name) {
        return null;
    }

    static class MapKV {
        final String name;
        final Map map;

        public MapKV(String name) {
            this(name, new LinkedHashMap());
        }

        public MapKV(String name, Map map) {
            this.name = name;
            this.map = map;
        }
    }

    static class ListKV {
        final String name;
        final List list;

        public ListKV(String name) {
            this(name, new ArrayList());
        }

        public ListKV(String name, List list) {
            this.name = name;
            this.list = list;
        }
    }

    static class ObjKV {
        final String name;
        final Object o;

        public ObjKV(String name, Object o) {
            this.name = name;
            this.o = o;
        }
    }


}
