/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.snap.stac.reader;

import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.dataio.geotiff.GeoTiffProductReader;
import org.esa.snap.stac.StacItem;

import java.io.File;
import java.io.IOException;

public class StacReader extends GeoTiffProductReader {

    private final ProductReaderPlugIn readerPlugIn;

    public StacReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        this.readerPlugIn = readerPlugIn;

    }

    public StacReader() {
        super(new StacReaderPlugIn());
        this.readerPlugIn = new StacReaderPlugIn();
    }

    @Override
    public ProductReaderPlugIn getReaderPlugIn() {
        return this.readerPlugIn;
    }

    @Override
    public Product readProductNodes(Object input, ProductSubsetDef subsetDef) throws IOException {
        StacItem item = null;

        try {
            if (input instanceof String) {
                item = new StacItem((String) input);
            } else if (input instanceof File) {
                item = new StacItem((File) input);
            } else {
                throw new IllegalFileFormatException("Product input must be a URL for File.");
            }
        } catch (ParseException e) {
            throw new IOException("Product is an invalid STAC item.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        StacItemToProduct converter = new StacItemToProduct(item);

        try {
            // Create product but do not stream band data. Just read in metadata and create empty bands.
            return converter.createProduct(false, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {

        StacItem item = null;

        try {
            if (input instanceof String) {
                item = new StacItem((String) input);
            } else if (input instanceof File) {
                item = new StacItem((File) input);
            } else {
                throw new IllegalFileFormatException("Product input must be a URL for File.");
            }
        } catch (ParseException e) {
            throw new IOException("Product is an invalid STAC item.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        StacItemToProduct converter = new StacItemToProduct(item);

        try {
            return converter.createProduct(false, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
