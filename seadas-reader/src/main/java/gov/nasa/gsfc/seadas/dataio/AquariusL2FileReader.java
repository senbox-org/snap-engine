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

/**
 * Created by IntelliJ IDEA.
 * User: seadas
 * Date: 11/14/11
 * Time: 2:23 PM
  */
public class AquariusL2FileReader extends SeadasFileReader {

    AquariusL2FileReader(SeadasProductReader productReader) {
        super(productReader);
    }

    @Override
    public Product createProduct() throws ProductIOException {

        int sceneWidth = getIntAttribute("Number_of_Beams");
        int sceneHeight = getIntAttribute("Number_of_Blocks");
        String productName = getStringAttribute("Product_Name");


        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
        product.setDescription(productName);

        ProductData.UTC utcStart = getUTCAttribute("Start_Time");
        if (utcStart != null) {
            product.setStartTime(utcStart);

        }
        ProductData.UTC utcEnd = getUTCAttribute("End_Time");
        if (utcEnd != null) {
            product.setEndTime(utcEnd);
        }

        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);
        addBandMetadata(product);
//        addScientificMetadata(product);

        variableMap = addBands(product, ncFile.getVariables());

        addGeocoding(product);

//        todo: add flags
/*
    The radiometer flag field has the following dimensions (in FORTRAN order):
    (index, beam, block).


    Bits     Condition         Index Dimension                L3 Flagname
    ----     ---------         ---------------                -----------
     1       RFI moderate      (V,P,M,H) polarization         RFIYELLOW
     2       RFI severe        (V,P,M,H) polarization         RFIRED
     3       Rain              (V mod, V sev, H mod, H sev)   RAINYELLOW, RAINRED
     4       Land              (moderate, severe)             LANDYELLOW, LANDRED
     5       Ice               (moderate, severe)             ICEYELLOW, ICERED
     6       Wind/Foam         (moderate, severe)             WINDYELLOW, WINDRED
     7       Temp              (V mod, V sev, H mod, H sev)   TEMPYELLOW, TEMPRED
     8       Solar Flux D      (V mod, V sev, H mod, H sev)   FLUXDYELLOW, FLUXDRED
     9       Solar Flux R      (V mod, V sev, H mod, H sev)   FLUXRYELLOW, FLUXRRED
    10       Sun Glint mod     (V mod, V sev, H mod, H sev)   GLINTYELLOW, GLINTRED
    11       Moon              (V mod, V sev, H mod, H sev)   MOONYELLOW, MOONRED
    12       Galaxy            (V mod, V sev, H mod, H sev)   GALYELLOW,  GALRED
    13       Nav               (Roll, Pitch, Yaw, OOB)        NAV
    14       SA overflow       On                             SAOVERFLOW
    15       Roughness fail    On                             ROUGH

*/
//        addFlagsAndMasks(product);
        product.setAutoGrouping("Kpc:SSS:anc:dTB:rad:scat:sun");

        return product;
    }

    public void addGeocoding(final Product product) throws ProductIOException {
        final String longitude = "scat_beam_clon";
        final String latitude = "scat_beam_clat";
        Band latBand = null;
        Band lonBand = null;


        if (product.containsBand(latitude) && product.containsBand(longitude)) {
            latBand = product.getBand(latitude);
            lonBand = product.getBand(longitude);
        }
        if (latBand != null && lonBand != null) {
            product.setGeoCoding(GeoCodingFactory.createPixelGeoCoding(latBand, lonBand, null, 5));
        }
    }
}