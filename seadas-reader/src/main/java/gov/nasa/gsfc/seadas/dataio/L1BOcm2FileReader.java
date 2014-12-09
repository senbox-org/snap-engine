/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package gov.nasa.gsfc.seadas.dataio;

import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCodingFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.Variable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 2/8/13
 * Time: 1:03 PM
 */
public class L1BOcm2FileReader extends SeadasFileReader {

    L1BOcm2FileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    private void fixBandNames() {
        String navGroup = "Geophysical_Data";
        List<Variable> variables = ncFile.findGroup(navGroup).getVariables();
        String varName;
        for (Variable variable : variables) {
            varName = variable.getShortName().replace("L", "Lt");
            variable.setName(varName);
        }
    }

    @Override
    public Product createProduct() throws ProductIOException {

        int sceneWidth = getIntAttribute("Pixels_per_Scan_Line");
        int sceneHeight = getIntAttribute("Number_of_Scan_Lines");

        fixBandNames();

        String productName = getStringAttribute("Product_Name");

        mustFlipX = mustFlipY = getDefaultFlip();
        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
        product.setDescription(productName);

        ProductData.UTC utcStart = getUTCAttribute("Start_Time");
        if (utcStart != null) {
            if (mustFlipY) {
                product.setEndTime(utcStart);
            } else {
                product.setStartTime(utcStart);
            }
        }
        ProductData.UTC utcEnd = getUTCAttribute("End_Time");
        if (utcEnd != null) {
            if (mustFlipY) {
                product.setStartTime(utcEnd);
            } else {
                product.setEndTime(utcEnd);
            }
        }

        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);
        addInputParamMetadata(product);
        addBandMetadata(product);
        addScientificMetadata(product);

        variableMap = addBands(product, ncFile.getVariables());

        addGeocoding(product);

        addFlagsAndMasks(product);
        product.setAutoGrouping("Lt");

        product.setPreferredTileSize(256, 256);
        return product;
    }

    public void addGeocoding(final Product product) throws ProductIOException {
        final String longitude = "longitude";
        final String latitude = "latitude";

        Band latBand = product.getBand(latitude);
        Band lonBand = product.getBand(longitude);

        latBand.setNoDataValue(-999.);
        lonBand.setNoDataValue(-999.);
        latBand.setNoDataValueUsed(true);
        lonBand.setNoDataValueUsed(true);

        product.setGeoCoding(GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, null, 5));
    }
}

