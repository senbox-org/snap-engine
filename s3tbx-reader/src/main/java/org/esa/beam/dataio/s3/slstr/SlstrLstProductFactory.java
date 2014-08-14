package org.esa.beam.dataio.s3.slstr;/*
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

import org.esa.beam.dataio.s3.Manifest;
import org.esa.beam.dataio.s3.Sentinel3ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCodingFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SlstrLstProductFactory extends SlstrL2ProductFactory {

    public SlstrLstProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        final File directory = getInputFileParentDirectory();
        final String[] fileNames = directory.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".nc") && !name.equals("time_in.nc");
            }
        });
        //todo read from manifest as soon as it contains all files
//        final List<String> fileNames = new ArrayList<String>();
//        fileNames.addAll(manifest.getFileNames(new String[0]));
        // TODO - time data are provided in a 64-bit variable, so we currently don't use them
        return Arrays.asList(fileNames);
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {}

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {}

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        final Band latBand = targetProduct.getBand("latitude_in");
        final Band lonBand = targetProduct.getBand("longitude_in");
        if (latBand != null && lonBand != null) {
            targetProduct.setGeoCoding(
                    GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, "!confidence_in_duplicate", 5));
        }
        if (targetProduct.getGeoCoding() == null) {
            if (targetProduct.getTiePointGrid("latitude_tx") != null && targetProduct.getTiePointGrid(
                    "longitude_tx") != null) {
                targetProduct.setGeoCoding(new TiePointGeoCoding(targetProduct.getTiePointGrid("latitude_tx"),
                                                                 targetProduct.getTiePointGrid("longitude_tx")));
            }
        }
    }

    @Override
    protected Integer getStartOffset(String gridIndex) {
        return 0;
    }

    @Override
    protected Integer getTrackOffset(String gridIndex) {
        return 0;
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        //todo use sensible values as soon as they are provided
        int subSamplingX = 1;
        int subSamplingY = 1;
        return copyBandAsTiePointGrid(sourceBand, targetProduct, subSamplingX, subSamplingY, 0.0f, 0.0f);
    }
}
