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

import java.util.Iterator;

public class Persistence {

    private final PersistenceSpiRegistry spiRegistry;

    public Persistence() {
        this(PersistenceSpiRegistry.getInstance());
    }

    // package local for test purposes only
    Persistence(PersistenceSpiRegistry spiRegistry) {
        this.spiRegistry = spiRegistry;
    }

    public <T> PersistenceDecoder<T> getDecoder(Item item) {
        return getRegisteredConverter(item);
    }

    public <T> PersistenceEncoder<T> getEncoder(Object object) {
        return getRegisteredConverter(object);
    }

    private <T> PersistenceConverter<T> getRegisteredConverter(Object object) {
        final Iterator serviceProviders = spiRegistry.getPersistenceSpis();
        while (serviceProviders.hasNext()) {
            final PersistenceSpi persistenceSpi = (PersistenceSpi) serviceProviders.next();
            if (checkUsability(persistenceSpi, object)) {
                return persistenceSpi.createConverter();
            }
        }
        return null;
    }

    private static boolean checkUsability(PersistenceSpi persistenceSpi, Object object) {
        if (object instanceof Item) {
            return persistenceSpi.canDecode((Item) object);
        } else {
            return persistenceSpi.canEncode(object);
        }
    }
}
