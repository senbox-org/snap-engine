package org.esa.s3tbx.dataio.s3.slstr;

/* Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.BasicPixelGeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.io.IOException;

public class SlstrWstProductFactory extends SlstrSstProductFactory {

    public SlstrWstProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        final String[] bandNames = targetProduct.getBandNames();
        Band latBand = null;
        Band lonBand = null;
        boolean otherBandAlreadyFound = false;
        for (String bandName : bandNames) {
            if (bandName.endsWith("lat")) {
                latBand = targetProduct.getBand(bandName);
                if (otherBandAlreadyFound) {
                    break;
                } else {
                    otherBandAlreadyFound = true;
                }
            } else if (bandName.endsWith("lon")) {
                lonBand = targetProduct.getBand(bandName);
                if (otherBandAlreadyFound) {
                    break;
                } else {
                    otherBandAlreadyFound = true;
                }
            }
        }
        if (latBand != null && lonBand != null) {
            final BasicPixelGeoCoding geoCoding = GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, null, 5);
            targetProduct.setSceneGeoCoding(geoCoding);
        }
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("brightness_temperature:nedt");
    }

    @Override
    protected void setUncertaintyBands(Product product) {
        super.setUncertaintyBands(product);
        // sst_theoretical_error is the old name
        String[] possibleUncBandNames = new String[]{"sst_theoretical_uncertainty", "sst_theoretical_error"};
        if(product.containsBand("sea_surface_temperature")) {
            final Band seaSurfaceTemperatureBand = product.getBand("sea_surface_temperature");
            for (String bandName : possibleUncBandNames) {
                if (product.containsBand(bandName)) {
                    final Band band = product.getBand(bandName);
                    seaSurfaceTemperatureBand.addAncillaryVariable(band, "uncertainty");
                    addUncertaintyImageInfo(band);
                    break;
                }
            }
        }
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
    }


}
