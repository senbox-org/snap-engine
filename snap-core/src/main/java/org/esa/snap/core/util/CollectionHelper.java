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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class for simplifying lambda expression usage on collections and arrays.
 *
 * @author Cosmin Cara
 */
public final class CollectionHelper {

    /**
     * Returns the first element of the collection that satisfies the given condition, or <code>null</code> if no such
     * element exists. If the condition is <code>null</code>, the first element of the collection is returned.
     *
     * @param collection The collection to be searched
     * @param condition  The condition to be applied
     * @param <T>        The type of the collection elements
     * @return The first element of the collection matching the condition, or <code>null</code>
     */
    public static <T> T firstOrDefault(Collection<T> collection, Predicate<T> condition) {
        T result = null;
        if (collection != null) {
            if (condition != null) {
                List<T> subset = where(collection, condition);
                if (subset != null && subset.size() > 0)
                    result = subset.get(0);
            } else {
                result = collection.iterator().next();
            }
        }
        return result;
    }

    /**
     * Returns the first element of the array that satisfies the given condition, or <code>null</code> if no such
     * element exists. If the condition is <code>null</code>, the first element of the array is returned.
     *
     * @param array     The array to be searched
     * @param condition The condition to be applied
     * @param <T>       The type of the array elements
     * @return The first element of the array matching the condition, or <code>null</code>
     */
    public static <T> T firstOrDefault(T[] array, Predicate<T> condition) {
        T result = null;
        if (array != null) {
            if (condition != null) {
                List<T> subset = where(array, condition);
                if (subset != null && subset.size() > 0)
                    result = subset.get(0);
            } else {
                result = array[0];
            }
        }
        return result;
    }

    /**
     * Selects the list of collection elements that satisfy the given filter.
     * If the filter is <code>null</code>, all the collection elements are returned.
     *
     * @param collection The collection to be filtered
     * @param filter     The filter to be applied
     * @param <T>        The type of collection elements
     * @return A list of elements satisfying the filter
     */
    public static <T> List<T> where(Collection<T> collection, Predicate<T> filter) {
        List<T> result = null;
        if (collection != null) {
            if (filter != null) {
                result = where(collection.stream(), filter);
            } else {
                result = (collection instanceof List ? (List<T>) collection : new ArrayList<>(collection));
            }
        }
        return result;
    }

    /**
     * Selects the list of array elements that satisfy the given filter.
     * If the filter is <code>null</code>, all the array elements are returned.
     *
     * @param array  The array to be filtered
     * @param filter The filter to be applied
     * @param <T>    The type of array elements
     * @return A list of elements satisfying the filter
     */
    public static <T> List<T> where(T[] array, Predicate<T> filter) {
        List<T> result = null;
        if (array != null) {
            if (filter != null) {
                result = where(Arrays.stream(array), filter);
            } else {
                result = Arrays.asList(array);
            }
        }
        return result;
    }

    private static <T> List<T> where(Stream<T> stream, Predicate<T> filter) {
        return stream.filter(filter).collect(Collectors.toList());
    }
}
