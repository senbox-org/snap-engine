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

import org.esa.snap.core.datamodel.Product;

public interface PersistenceDecoder<T> {

    String KEY_PERSISTENCE_ID = "___persistence_id___";

    HistoricalDecoder[] getHistoricalDecoders();

    String getID();

    T decode(Item item, Product product);

    default boolean canDecode(Item item) {
        if (isCurrentVersion(item)) {
            return true;
        }
        final HistoricalDecoder[] decoders = getHistoricalDecoders();
        for (int i = decoders.length - 1; i >= 0; i--) { // reverse order !
            HistoricalDecoder decoder = decoders[i];
            if (decoder.canDecode(item)) {
                return true;
            }
        }
        return false;
    }

    default boolean isCurrentVersion(Item item) {
        if (item == null || !item.isContainer()) {
            return false;
        }
        final Property<?> property = ((Container) item).getProperty(KEY_PERSISTENCE_ID);
        if (property == null) {
            return false;
        }
        return getID().equals(property.getValueString());
    }
}
