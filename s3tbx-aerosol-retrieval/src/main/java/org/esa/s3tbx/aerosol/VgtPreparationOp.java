/*
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol;

import org.esa.s3tbx.idepix.algorithms.vgt.VgtOp;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;

/**
 * Create Vgt input product for Globalbedo aerosol retrieval and BBDR processor
 *
 * @author akheckel
 */
@OperatorMetadata(alias = "AerosolRetrieval.VgtPreparation",
        description = "Create Vgt product for input to Globalbedo aerosol retrieval and BBDR processor",
        authors = "Andreas Heckel, Olaf Danne, Marco Zuehlke",
        version = "1.0",
        internal = true,
        copyright = "(C) 2010, 2016 by University Swansea (a.heckel@swansea.ac.uk) and Brockmann Consult")
public class VgtPreparationOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {

        InstrumentConsts instrC = InstrumentConsts.getInstance();
        final boolean needPixelClassif = (!sourceProduct.containsBand(instrC.getIdepixFlagBandName()));
        final boolean needElevation = (!sourceProduct.containsBand(instrC.getElevationBandName()));
        final boolean needSurfacePres = (!sourceProduct.containsBand(instrC.getSurfPressureName("VGT")));

        //general SzaSubset to less
        // doesn't work so easy with this projected data set! :(

        //Map<String,Object> szaSubParam = new HashMap<String, Object>(3);
        //szaSubParam.put("szaBandName", "SZA");
        //szaSubParam.put("hasSolarElevation", false);
        //szaSubParam.put("szaLimit", 69.99);
        //Product szaSubProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SzaSubsetOp.class), szaSubParam, sourceProduct);
        Product szaSubProduct = sourceProduct;

        // subset might have set ptype to null, thus:
        if (szaSubProduct.getDescription() == null) szaSubProduct.setDescription("vgt product");

        // setup target product primarily as copy of sourceProduct
        final int rasterWidth = szaSubProduct.getSceneRasterWidth();
        final int rasterHeight = szaSubProduct.getSceneRasterHeight();
        targetProduct = new Product(szaSubProduct.getName(),
                                    szaSubProduct.getProductType(),
                                    rasterWidth, rasterHeight);
        targetProduct.setStartTime(szaSubProduct.getStartTime());
        targetProduct.setEndTime(szaSubProduct.getEndTime());
        targetProduct.setPointingFactory(szaSubProduct.getPointingFactory());
        ProductUtils.copyTiePointGrids(szaSubProduct, targetProduct);
        ProductUtils.copyGeoCoding(szaSubProduct, targetProduct);
        ProductUtils.copyFlagBands(szaSubProduct, targetProduct, true);

        // create pixel calssification if missing in sourceProduct
        // and add flag band to targetProduct
        Product idepixProduct;
        if (needPixelClassif) {
            VgtOp vgtOp = new VgtOp();
            vgtOp.setParameterDefaultValues();
            vgtOp.setParameter("copyToaReflectances", true); // todo: check if needed
            vgtOp.setParameter("cloudBufferWidth", 3);
            vgtOp.setSourceProduct(szaSubProduct);
            idepixProduct = vgtOp.getTargetProduct();
            ProductUtils.copyFlagBands(idepixProduct, targetProduct, true);
        }

        // create elevation product if band is missing in sourceProduct
        Product elevProduct = null;
        if (needElevation) {
            elevProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(CreateElevationBandOp.class), GPF.NO_PARAMS, szaSubProduct);
        }

        // create surface pressure estimate product if band is missing in sourceProduct
        VirtualBand surfPresBand = null;
        if (needSurfacePres) {
            String presExpr = "(1013.25 * exp(-elevation/8400))";
            surfPresBand = new VirtualBand(instrC.getSurfPressureName("VGT"),
                                           ProductData.TYPE_FLOAT32,
                                           rasterWidth, rasterHeight, presExpr);
            surfPresBand.setDescription("estimated sea level pressure (p0=1013.25hPa, hScale=8.4km)");
            surfPresBand.setNoDataValue(0);
            surfPresBand.setNoDataValueUsed(true);
            surfPresBand.setUnit("hPa");
        }

        // copy all bands from sourceProduct
        for (Band srcBand : szaSubProduct.getBands()) {
            String srcName = srcBand.getName();
            if (!srcBand.isFlagBand()) {
                ProductUtils.copyBand(srcName, szaSubProduct, targetProduct, true);
            }
        }

        // add elevation band if needed
        if (needElevation) {
            Guardian.assertNotNull("elevProduct", elevProduct);
            Band srcBand = elevProduct.getBand(instrC.getElevationBandName());
            Guardian.assertNotNull("elevation band", srcBand);
            ProductUtils.copyBand(srcBand.getName(), elevProduct, targetProduct, true);
        }

        // add vitrual surface pressure band if needed
        if (needSurfacePres) {
            targetProduct.addBand(surfPresBand);
        }

    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(VgtPreparationOp.class);
        }
    }
}
