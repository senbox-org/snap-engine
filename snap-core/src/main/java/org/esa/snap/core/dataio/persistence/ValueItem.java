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

import org.esa.snap.core.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.splitByWholeSeparator;

public abstract class ValueItem<E> extends Item {
    private final E value;

    protected ValueItem(String name, E value) {
        super(name);

        this.value = value;
    }

    public E getValue() {
        return value;
    }

    public String getValueString() {
        return String.valueOf(value);
    }

    public String[] getValueStrings() {
        if (value != null && value.getClass().isArray()) {
            Object[] v = (Object[]) value;
            return Arrays.stream(v).map(String::valueOf).toArray(String[]::new);
        }
        String str = getValueString();
        final String delimiter = "\", \"";
        if (str.contains(delimiter)) {
            str = str.startsWith("\"") ? str.substring(1) : str;
            str = str.endsWith("\"") ? str.substring(0, str.length() - 1) : str;
            return splitByWholeSeparator(str, delimiter);
        }
        return new String[]{str};
    }

    public Double getValueDouble() {
        return getaDouble(value);
    }

    public Double[] getValueDoubles() {
        if (value != null && value.getClass().isArray()) {
            if ("double".equals(value.getClass().getComponentType().getName())) {
                final double[] doubles = (double[]) this.value;
                final Double[] Doubles = new Double[doubles.length];
                for (int i = 0; i < doubles.length; i++) {
                    Doubles[i] = doubles[i];
                }
                return Doubles;
            } else {
                Object[] v = (Object[]) value;
                return Arrays.stream(v).map(ValueItem::getaDouble).toArray(Double[]::new);
            }
        }
        if (value instanceof List) {
            final List<?> list = (List<?>) value;
            final Stream<Double> stream = list.stream().map(ValueItem::getaDouble);
            return stream.toArray(Double[]::new);
        }
        if (value != null && value instanceof String) {
            final String str = (String) this.value;
            final String[] strings = StringUtils.csvToArray(str);
            return Arrays.stream(strings).map(ValueItem::getaDouble).toArray(Double[]::new);
        }
        return new Double[]{getaDouble(value)};
    }

    public Float getValueFloat() {
        return getaFloat(value);
    }

    public Float[] getValueFloats() {
        final Double[] doubles = getValueDoubles();
        if (doubles != null) {
            return Arrays.stream(doubles).map(aDouble -> aDouble != null ? aDouble.floatValue() : null).toArray(Float[]::new);
        }
        return new Float[]{getaFloat(value)};
    }

    public Long getValueLong() {
        return getaLong(value);
    }

    public Long[] getValueLongs() {
        if (value != null && value.getClass().isArray()) {
            Object[] v = (Object[]) value;
            return Arrays.stream(v).map(ValueItem::getaLong).toArray(Long[]::new);
        }
        if (value instanceof List) {
            final List<?> list = (List<?>) value;
            final Stream<Long> stream = list.stream().map(ValueItem::getaLong);
            return stream.toArray(Long[]::new);
        }
        if (value != null && value instanceof String) {
            final String str = (String) this.value;
            final String[] strings = StringUtils.csvToArray(str);
            return Arrays.stream(strings).map(ValueItem::getaLong).toArray(Long[]::new);
        }
        return new Long[]{getaLong(value)};
    }

    public Integer getValueInt() {
        return getAnInteger(value);
    }

    public Integer[] getValueInts() {
        if (value != null && value.getClass().isArray()) {
            Object[] v = (Object[]) value;
            return Arrays.stream(v).map(ValueItem::getAnInteger).toArray(Integer[]::new);
        }
        return Arrays.stream(getValueLongs()).map(aLong -> aLong != null ? aLong.intValue() : null).toArray(Integer[]::new);
    }

    public Short getValueShort() {
        return getaShort(value);
    }

    public Short[] getValueShorts() {
        if (value != null && value.getClass().isArray()) {
            Object[] v = (Object[]) value;
            return Arrays.stream(v).map(ValueItem::getaShort).toArray(Short[]::new);
        }
        return Arrays.stream(getValueLongs()).map(aLong -> aLong != null ? aLong.shortValue() : null).toArray(Short[]::new);
    }

    public Byte getValueByte() {
        return getaByte(value);
    }

    public Byte[] getValueBytes() {
        if (value != null && value.getClass().isArray()) {
            Object[] v = (Object[]) value;
            return Arrays.stream(v).map(ValueItem::getaByte).toArray(Byte[]::new);
        }
        return Arrays.stream(getValueLongs()).map(aLong -> aLong != null ? aLong.byteValue() : null).toArray(Byte[]::new);
    }

    public Boolean getValueBoolean() {
        return getaBoolean(value);
    }

    public Boolean[] getValueBooleans() {
        if (value != null && value.getClass().isArray()) {
            Object[] v = (Object[]) value;
            return Arrays.stream(v).map(ValueItem::getaBoolean).toArray(Boolean[]::new);
        }
        if (value != null && value instanceof String) {
            final String str = (String) this.value;
            final String[] strings = StringUtils.csvToArray(str);
            return Arrays.stream(strings).map(ValueItem::getaBoolean).toArray(Boolean[]::new);
        }
        return new Boolean[]{getaBoolean(value)};
    }

    public E get() {
        return value;
    }

    static Double getaDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            final String str = (String) value;
            if ("null".equalsIgnoreCase(str.trim())) {
                return null;
            }
            return Double.parseDouble(str);
        }
        return Double.NaN;
    }

    static Float getaFloat(Object value) {
        final Double d = getaDouble(value);
        if (d != null) {
            return d.floatValue();
        }
        return null;
    }

    static Long getaLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            final String str = (String) value;
            if ("null".equalsIgnoreCase(str.trim())) {
                return null;
            }
            return Long.parseLong(str.trim());
        }
        return null;
    }

    static Integer getAnInteger(Object value) {
        final Long aLong = getaLong(value);
        if (aLong != null) {
            return aLong.intValue();
        }
        return null;
    }

    static Short getaShort(Object value) {
        final Long aLong = getaLong(value);
        if (aLong != null) {
            return aLong.shortValue();
        }
        return null;
    }

    static Byte getaByte(Object value) {
        final Long aLong = getaLong(value);
        if (aLong != null) {
            return aLong.byteValue();
        }
        return null;
    }

    static Boolean getaBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            final String str = (String) value;
            if ("null".equalsIgnoreCase(str.trim())) {
                return null;
            } else {
                return Boolean.parseBoolean(str);
            }
        }
        return null;
    }
}
