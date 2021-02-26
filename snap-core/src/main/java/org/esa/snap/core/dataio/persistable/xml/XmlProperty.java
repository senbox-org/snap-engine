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

import org.esa.snap.core.dataio.persistable.Property;
import org.jdom.Element;

class XmlProperty implements Property<Element> {
    final Element element;

    XmlProperty(Element element) {
        this.element = element;
    }

    @Override
    public Object getValue() {
        return element.getValue();
    }

    @Override
    public String getValueString() {
        return element.getValue();
    }

    @Override
    public Double getValueDouble() {
        final String value = element.getValue();
        if (value != null && !value.trim().isEmpty()) {
            return Double.valueOf(value);
        }
        return null;
    }

    @Override
    public Float getValueFloat() {
        final String value = element.getValue();
        if (value != null && !value.trim().isEmpty()) {
            return Float.valueOf(value);
        }
        return null;
    }

    @Override
    public Long getValueLong() {
        final String value = element.getValue();
        if (value != null && !value.trim().isEmpty()) {
            return Long.valueOf(value);
        }
        return null;
    }

    @Override
    public Integer getValueInt() {
        final String value = element.getValue();
        if (value != null && !value.trim().isEmpty()) {
            return Long.valueOf(value).intValue();
        }
        return null;
    }

    @Override
    public Short getValueShort() {
        final String value = element.getValue();
        if (value != null && !value.trim().isEmpty()) {
            return Long.valueOf(value).shortValue();
        }
        return null;
    }

    @Override
    public Byte getValueByte() {
        final String value = element.getValue();
        if (value != null && !value.trim().isEmpty()) {
            return Long.valueOf(value).byteValue();
        }
        return null;
    }

    @Override
    public Element get() {
        return element;
    }

    @Override
    public String getName() {
        return XmlSupport.fetchNameFrom(element);
    }
}
