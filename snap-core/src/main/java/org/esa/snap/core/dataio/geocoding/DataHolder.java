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

package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.persistence.Item;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.ProductNodeEvent;
import org.esa.snap.core.datamodel.ProductNodeListenerAdapter;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.util.HashMap;

/**
 * The lazy instantiating DataHolder class supports ProductReader implementers in not having to create large double
 * arrays multiple times if they are needed multiple times for the instantiation of a GeoRaster. <br><br>
 *
 * For an example of its use, see: {@link ComponentGeoCodingPersistenceConverter#decode(Item, Product) decode}
 * of ComponentGeoCodingPersistenceConverter.
 */
public class DataHolder {
    /**
     * The private constructor prevents external instantiating
     */
    private DataHolder() {
    }

    /**
     * Internal static holder class
     */
    private static class Holder {
        private static final DataHolder INSTANCE = new DataHolder();

        private Holder() {
        }
    }

    /**
     * Static factory method for lazy instantiating
     */
    public static DataHolder getInstance() {
        return Holder.INSTANCE;
    }

    private final HashMap<Product, HashMap<RasterDataNode, double[]>> dataReference = new HashMap<>();

    public HashMap<RasterDataNode, double[]> getDataMap(Product product) {
        if (!dataReference.containsKey(product)) {
            product.addProductNodeListener(new ProductNodeListenerAdapter() {
                @Override
                public void nodeStartDisposal(ProductNodeEvent event) {
                    freeDataFor(event.getSourceNode());
                }
            });
            dataReference.put(product, new HashMap<>());
        }
        return dataReference.get(product);
    }

    public void freeDataFor(ProductNode sourceNode) {
        final HashMap<RasterDataNode, double[]> dataMap = dataReference.remove(sourceNode);
        if (dataMap != null) {
            dataMap.clear();
        }
    }

}
