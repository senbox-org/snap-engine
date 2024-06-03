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
package org.esa.stac.reader;

import org.esa.snap.core.dataio.*;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.dataio.geotiff.GeoTiffProductReader;
import org.esa.stac.internal.StacItem;

import java.io.IOException;

public class STACReader extends GeoTiffProductReader {

    private ProductReaderPlugIn readerPlugIn;

    public STACReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        this.readerPlugIn = readerPlugIn;

    }
    public STACReader() {
        super(new STACReaderPlugIn());
        this.readerPlugIn = new STACReaderPlugIn();
    }

    @Override
    public ProductReaderPlugIn getReaderPlugIn() {
        return this.readerPlugIn;
    }


    @Override
    public Product readProductNodes(Object input, ProductSubsetDef subsetDef) throws IOException, IllegalFileFormatException {
        StacItem item = null;

        try{
            item = new StacItem(input);
        }catch(ParseException e){
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
    protected Product readProductNodesImpl() throws IOException{

        StacItem item = null;

        try{
            item = new StacItem(input);
        }catch(ParseException e){
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
