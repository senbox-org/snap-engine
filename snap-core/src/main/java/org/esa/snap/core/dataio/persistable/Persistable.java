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

package org.esa.snap.core.dataio.persistable;

import org.esa.snap.core.datamodel.Product;

import java.awt.Dimension;
import java.util.List;

public abstract class Persistable<ML, O> {

    protected final MarkupLanguageSupport<ML> languageSupport;

    protected Persistable(MarkupLanguageSupport<ML> languageSupport) {
        this.languageSupport = languageSupport;
    }

    public final List<ML> encode(List<O> objects){
        final List<Item> list = encodeObjects(objects);
        return languageSupport.toLanguageObjects(list);
    }

    protected abstract List<Item> encodeObjects(List<O> instances);

    public final List<O> decode(List<ML> languageElements, Product product, Dimension regionRasterSize){
        final List<Item> items = languageSupport.convertToItems(languageElements);
        return decodeItems(items, product, regionRasterSize);
    }

    protected abstract List<O> decodeItems(List<Item> items, Product product, Dimension regionRasterSize);
}
