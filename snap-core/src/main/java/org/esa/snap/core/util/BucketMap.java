/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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
 */

package org.esa.snap.core.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by kraftek on 11/6/2015.
 */
public class BucketMap<K extends Number, V> implements Map<K, V> {

    private final HashMap<K[], V> map;

    public BucketMap() {
        this.map = new HashMap<>();
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null || !(key instanceof Number)) {
            throw new IllegalArgumentException();
        }
        boolean ret = false;
        Set<K[]> keySet = map.keySet();
        Number castKey = (Number)key;
        for (Number[] minMax : keySet) {
            if (minMax[0].longValue() <= ((Number) key).longValue() &&
                    minMax[1].longValue() >= ((Number) key).longValue()) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public V get(Object key) {
        V value = null;
        Number castKey = (Number)key;
        for (Number[] minMax : map.keySet()) {
            if (minMax[0].longValue() <= ((Number) key).longValue() &&
                    minMax[1].longValue() >= ((Number) key).longValue()) {
                value = map.get(minMax);
                break;
            }
        }
        return value;
    }

    @Override
    public V put(K key, V value) {
        return put(key, key, value);
    }

    public V put(K minKey, K maxKey, V value) {
        K[] key = createKey(minKey, maxKey);
        for (K[] numbers : map.keySet()) {
            if ((isGreaterOrEqual(numbers[0], minKey) && isLessOrEqual(minKey, numbers[1])) ||
                    (isGreaterOrEqual(numbers[0], maxKey) && isLessOrEqual(maxKey, numbers[1]))) {
                throw new IllegalArgumentException("Intervals have to be disjoint");
            }
        }
        return map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        throw new java.lang.UnsupportedOperationException();
    }

    public V remove(K minKey, K maxKey) {
        return map.remove(createKey(minKey, maxKey));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new java.lang.UnsupportedOperationException();
    }

    @Override
    public void clear() {
        this.map.clear();
    }

    @Override
    public Set<K> keySet() {
        throw new java.lang.UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        return this.map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new java.lang.UnsupportedOperationException();
    }

    private K[] createKey(K min, K max) {
        if (min == null || max == null) {
            throw new IllegalArgumentException();
        }
        K[] key = (K[])Array.newInstance(min.getClass(), 2);
        key[0] = min;
        key[1] = max;
        return key;
    }

    private boolean isLessOrEqual(K first, K second) {
        return first.longValue() <= second.longValue();
    }

    private boolean isLess(K first, K second) {
        return first.longValue() < second.longValue();
    }

    private boolean isGreaterOrEqual(K first, K second) {
        return first.longValue() >= second.longValue();
    }

    private boolean isGreater(K first, K second) {
        return first.longValue() > second.longValue();
    }
}
