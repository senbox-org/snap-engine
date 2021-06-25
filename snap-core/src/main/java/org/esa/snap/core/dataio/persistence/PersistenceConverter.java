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

public abstract class PersistenceConverter<T> implements PersistenceEncoder<T>, PersistenceDecoder<T> {

    /**
     * Dont override this method. Instead implement {@link #decodeImpl(Item, Product)}.
     */
    @Override
    public T decode(Item item, Product product){
        if (!isCurrentVersion(item)) {
            item = convertToCurrentVersion(item, product);
        }
        return decodeImpl(item, product);
    };

    protected abstract T decodeImpl(Item item, Product product);

    protected Item convertToCurrentVersion(Item item, Product product) {
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

    protected final Container createRootContainer(String name) {
        final Container codingMain = new Container(name);
        codingMain.add(new Property<>(KEY_PERSISTENCE_ID, getID()));
        return codingMain;
    }

    /**
     * Override if necessary.
     */
    @Override
    public HistoricalDecoder[] getHistoricalDecoders() {
        return new HistoricalDecoder[0];
    }
}
