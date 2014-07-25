package org.esa.beam.dataio.s3.olci;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.dataio.s3.AbstractProductFactory;
import org.esa.beam.dataio.s3.Manifest;
import org.esa.beam.dataio.s3.Sentinel3ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCodingFactory;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;

import java.io.IOException;
import java.util.List;

public class OlciLevel1ProductFactory extends OlciProductFactory {

    private final static String validExpression = "!quality_flags_invalid && !quality_flags_duplicated";

    public OlciLevel1ProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("Oa*_radiance:Oa*_radiance_err");
    }

    @Override
    protected String getValidExpression() {
        return validExpression;
    }

    @Override
    protected ProductNodeGroup<Mask> prepareMasksForCopying(ProductNodeGroup<Mask> maskGroup) {
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            final Mask mask = maskGroup.get(i);
            if (mask.getName().equals("quality_flags_invalid")) {
                mask.setName("quality_flags_cosmetic");
            } else if (mask.getName().equals("quality_flags_cosmetic")) {
                mask.setName("quality_flags_invalid");
            } else if (mask.getName().equals("quality_flags_duplicated")) {
                mask.setName("quality_flags_sun_glint_risk");
            } else if (mask.getName().equals("quality_flags_sun_glint_risk")) {
                mask.setName("quality_flags_duplicated");
            }
        }
        return maskGroup;
    }
}
