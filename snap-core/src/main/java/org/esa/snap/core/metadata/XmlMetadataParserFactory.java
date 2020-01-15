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

package org.esa.snap.core.metadata;

import java.util.HashMap;
import java.util.Map;

/**
 * This factory class returns instances of {@code XmlMetadataParser}s that have been previously
 * registered with it.
 */
public class XmlMetadataParserFactory {

    private static Map<Class, XmlMetadataParser> parserMap = new HashMap<>();

    /**
     * Registers a parser instance, attached to the given metadata class, to this factory.
     * The metadata class should be an extension of {@code GenericXmlMetadata}
     *
     * @param clazz  The metadata class.
     * @param parser The parser instance.
     * @param <T>    Generic type for metadata class.
     */
    public static <T extends GenericXmlMetadata> void registerParser(Class clazz, XmlMetadataParser<T> parser) {
        if (!parserMap.containsKey(clazz)) {
            parserMap.put(clazz, parser);
        }
    }

    /**
     * Returns a parser instance for the given metadata class. If no parser was previously registered for
     * the class, it will throw an exception.
     *
     * @param clazz The metadata class.
     * @param <T>   Generic type for the metadata class.
     * @return The parser instance.
     * @throws InstantiationException Exception is thrown if no parser was registered for the input class.
     */
    public static <T extends GenericXmlMetadata> XmlMetadataParser<T> getParser(Class clazz) throws InstantiationException {
        XmlMetadataParser<T> parser;
        if (parserMap.containsKey(clazz)) {
            //noinspection unchecked
            parser = parserMap.get(clazz);
        } else {
            throw new InstantiationException("No parser registered for metadata class " + clazz.getName());
        }
        return parser;
    }
}
