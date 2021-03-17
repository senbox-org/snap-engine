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

public interface PersistenceConverter<T> extends PersistenceDecoder<T>, PersistenceEncoder<T> {

    String KEY_PERSISTENCE_ID = "___persistence_id___";

    HistoricalDecoder[] getHistoricalDecoders();

    default Item convertToCurrentVersion(Item item, Product product) {
        final HistoricalDecoder[] decoders = getHistoricalDecoders();
        int startIndex = -1;
        for (int i = decoders.length - 1; i >= 0; i--) { // reverse order !
            if (decoders[i].canDecode(item)) {
                startIndex = i;
                break;
            }
        }
        if (startIndex == -1) {
            throw new IllegalStateException("Unable to decode item.");
        }
        for (int i = startIndex; i < decoders.length; i++) {
            item = decoders[i].decode(item, product);
        }
        return item;
    }

}
