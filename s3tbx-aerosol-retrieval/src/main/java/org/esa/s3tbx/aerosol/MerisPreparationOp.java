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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol;

import org.esa.s3tbx.aerosol.util.AerosolUtils;
import org.esa.s3tbx.idepix.algorithms.meris.MerisOp;
import org.esa.s3tbx.processor.rad2refl.Rad2ReflOp;
import org.esa.s3tbx.processor.rad2refl.Sensor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Create Meris input product for Globalbedo aerosol retrieval and BBDR processor
 * <p>
 * TODO: check what rad2refl does with the masks and which masks need to be copied.
 * if the sourceProd contains already idepix, what happens with the masks in rad2refl?
 *
 * @author akheckel
 */
@OperatorMetadata(alias = "AerosolRetrieval.MerisPreparation",
        description = "Create Meris product for input to Globalbedo aerosol retrieval and BBDR processor",
        authors = "Andreas Heckel, Olaf Danne, Marco Zuehlke",
        version = "1.0",
        internal = true,
        copyright = "(C) 2010, 2016 by University Swansea (a.heckel@swansea.ac.uk) and Brockmann Consult")
public class MerisPreparationOp extends Operator {

    public static final String ALTITUDE_BAND_NAME = "altitude";
    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "true",
            label = "Perform equalization",
            description = "Perform removal of detector-to-detector systematic radiometric differences in MERIS L1b data products.")
    private boolean doEqualization;

    @Parameter(defaultValue = "true",
            label = " Use land-water flag from L1b product instead")
    private boolean gaUseL1bLandWaterFlag;

    @Parameter(defaultValue = "false")     // cloud/snow flag refinement. Was not part of GA FPS processing.
    private boolean gaRefineClassificationNearCoastlines;

    @Parameter(defaultValue = "false", label = " Use the LC cloud buffer algorithm")
    private boolean gaLcCloudBuffer;

    @Parameter(defaultValue = "false", label = " Use the LC cloud buffer algorithm")
    private boolean gaComputeCloudShadow;

    @Parameter(defaultValue = "true", label = "Compute cloud buffer")
    private boolean gaComputeCloudBuffer;

    @Parameter(defaultValue = "false", label = "Copy cloud top pressure")
    private boolean gaCopyCTP;

    @Override
    public void initialize() throws OperatorException {
        InstrumentConsts instrC = InstrumentConsts.getInstance();
        final boolean needPixelClassif = (!sourceProduct.containsBand(instrC.getIdepixFlagBandName()));
        final boolean needElevation = (!sourceProduct.containsBand(instrC.getElevationBandName()));
        final boolean needSurfacePres = (!sourceProduct.containsBand(instrC.getSurfPressureName("MERIS")));

        //general SzaSubset to less 70 degree
        Product szaSubProduct;
        Rectangle szaRegion = AerosolUtils.getSzaRegion(sourceProduct.getRasterDataNode("sun_zenith"), false, 69.99);
        if (szaRegion.x == 0 && szaRegion.y == 0 &&
                szaRegion.width == sourceProduct.getSceneRasterWidth() &&
                szaRegion.height == sourceProduct.getSceneRasterHeight()) {
            szaSubProduct = sourceProduct;
        } else if (szaRegion.width < 2 || szaRegion.height < 2) {
            targetProduct = AerosolRetrievalMasterOp.EMPTY_PRODUCT;
            return;
        } else {
            Map<String, Object> subsetParam = new HashMap<>(3);
            subsetParam.put("region", szaRegion);
            Dimension targetTS = ImageManager.getPreferredTileSize(sourceProduct);
            RenderingHints rhTarget = new RenderingHints(GPF.KEY_TILE_SIZE, targetTS);
            szaSubProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SubsetOp.class), subsetParam, sourceProduct, rhTarget);
            ProductUtils.copyMetadata(sourceProduct, szaSubProduct);
        }

        // convert radiance bands to reflectance
        Map<String, Object> relfParam = new HashMap<>(3);
        relfParam.put("doRadToRefl", true);
        relfParam.put("doEqualization", doEqualization);
        relfParam.put("sensor", Sensor.MERIS);
        Product reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(Rad2ReflOp.class), relfParam, szaSubProduct);

        // subset might have set ptype to null, thus:
        if (szaSubProduct.getDescription() == null) {
            szaSubProduct.setDescription("MERIS Radiance product");
        }

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
        ProductUtils.copyFlagBands(szaSubProduct, targetProduct, true);
        ProductUtils.copyGeoCoding(szaSubProduct, targetProduct);

        // create pixel calssification if missing in sourceProduct
        // and add flag band to targetProduct
        Product idepixProduct;
        if (needPixelClassif) {
            MerisOp merisOp = new MerisOp();
            merisOp.setParameterDefaultValues();
            merisOp.setParameter("computeCloudShadow", gaComputeCloudShadow);
            merisOp.setParameter("computeCloudBuffer", gaComputeCloudBuffer);
            merisOp.setSourceProduct(szaSubProduct);
            idepixProduct = merisOp.getTargetProduct();

            ProductUtils.copyFlagBands(idepixProduct, targetProduct, true);
        }

        // TODO
        // if we should to haze correction / removal
        // do it now use sza product and idepix as input. merge them.
        // feed teh output into MerisRadiometryCorrectionOp a 2nd time
        // use the resulting reflectances for further processing
        // TODO

        // create elevation product if band is missing in sourceProduct
        Product elevProduct = null;
        if (needElevation && !szaSubProduct.containsBand(ALTITUDE_BAND_NAME)) {
            elevProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(CreateElevationBandOp.class), GPF.NO_PARAMS, szaSubProduct);
        }

        // create surface pressure estimate product if band is missing in sourceProduct
        VirtualBand surfPresBand = null;
        if (needSurfacePres) {
            String presExpr = "(1013.25 * exp(-elevation/8400))";
            surfPresBand = new VirtualBand(instrC.getSurfPressureName("MERIS"),
                                           ProductData.TYPE_FLOAT32,
                                           rasterWidth, rasterHeight, presExpr);
            surfPresBand.setDescription("estimated sea level pressure (p0=1013.25hPa, hScale=8.4km)");
            surfPresBand.setNoDataValue(0);
            surfPresBand.setNoDataValueUsed(true);
            surfPresBand.setUnit("hPa");
        }

        // copy all non-radiance bands from sourceProduct and
        // copy reflectance bands from reflProduct
        for (Band srcBand : szaSubProduct.getBands()) {
            String srcName = srcBand.getName();
            if (!srcBand.isFlagBand()) {
                if (srcName.startsWith("radiance")) {
//                    String reflName = "reflec_" + srcName.split("_")[1];
//                    String tarName = "reflectance_" + srcName.split("_")[1];
//                    ProductUtils.copyBand(reflName, reflProduct, tarName, targetProduct, true);
                    String reflName = "reflectance_" + srcName.split("_")[1];
                    ProductUtils.copyBand(reflName, reflProduct, targetProduct, true);
                } else if (!targetProduct.containsBand(srcName)) {
                    ProductUtils.copyBand(srcName, szaSubProduct, targetProduct, true);
                }
            }
        }

        // add elevation band if needed
        if (needElevation) {
            if (elevProduct != null) {
                ProductUtils.copyBand(instrC.getElevationBandName(), elevProduct, targetProduct, true);
            } else if (szaSubProduct.containsBand(ALTITUDE_BAND_NAME)) {
                ProductUtils.copyBand(ALTITUDE_BAND_NAME, szaSubProduct, instrC.getElevationBandName(), targetProduct, true);
            }
        }

        // add vitrual surface pressure band if needed
        if (needSurfacePres) {
            targetProduct.addBand(surfPresBand);
        }
        ProductUtils.copyPreferredTileSize(szaSubProduct, targetProduct);

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
            super(MerisPreparationOp.class);
        }
    }
}
