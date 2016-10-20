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

import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.util.HashMap;
import java.util.Map;

/**
 * Main Operator producing AOT for GlobAlbedo
 */
@OperatorMetadata(alias = "AerosolRetrieval.Master",
        description = "Aerosol retrieval from optical sensors (MERIS, VGT) following USwansea algorithm as used in GlobAlbedo project.",
        authors = "Andreas Heckel, Olaf Danne, Marco Zuehlke",
        version = "1.0",
        internal = true,
        copyright = "(C) 2010, 2016 by University Swansea (a.heckel@swansea.ac.uk) and Brockmann Consult")
public class AerosolRetrievalMasterOp extends Operator {

    public static final Product EMPTY_PRODUCT = new Product("empty", "empty", 0, 0);

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "false")
    private boolean copyToaRadBands;
    @Parameter(defaultValue = "true")
    private boolean copyToaReflBands;
    @Parameter(defaultValue = "false")
    private boolean noFilling;
    @Parameter(defaultValue = "false")
    private boolean noUpscaling;
    @Parameter(defaultValue = "1")
    private int soilSpecId;
    @Parameter(defaultValue = "5")
    private int vegSpecId;
    @Parameter(defaultValue = "9")
    private int scale;
    @Parameter(defaultValue = "0.3")
    private float ndviThr;

    @Parameter(defaultValue = "true",
            label = "Perform equalization",
            description = "Perform removal of detector-to-detector systematic radiometric differences in MERIS L1b data products.")
    private boolean doEqualization;

    @Parameter(defaultValue = "false", label = " If set, we shall use AOT climatology (no retrieval)")
    private boolean useAotClimatology;

    @Parameter(defaultValue = "false")     // cloud/snow flag refinement. Was not part of GA FPS processing.
    private boolean gaRefineClassificationNearCoastlines;

    @Parameter(defaultValue = "false")     // in line with GA FPS processing, BEAM 4.9.0.1. Better set to true??
    private boolean gaUseL1bLandWaterFlag;

    @Parameter(defaultValue = "false", label = " Use the LC cloud buffer algorithm")
    private boolean gaComputeCloudShadow;

    @Parameter(defaultValue = "true", label = "Compute cloud buffer")
    private boolean gaComputeCloudBuffer;

    @Parameter(defaultValue = "false", label = "Copy cloud top pressure")
    private boolean gaCopyCTP;

    private String instrument;

    @Override
    public void initialize() throws OperatorException {
        if (sourceProduct.getSceneRasterWidth() < 9 || sourceProduct.getSceneRasterHeight() < 9) {
            setTargetProduct(EMPTY_PRODUCT);
            return;
        }
        Dimension targetTS = ImageManager.getPreferredTileSize(sourceProduct);
        Dimension aotTS = new Dimension(targetTS.width / 9, targetTS.height / 9);
        RenderingHints rhTarget = new RenderingHints(GPF.KEY_TILE_SIZE, targetTS);
        RenderingHints rhAot = new RenderingHints(GPF.KEY_TILE_SIZE, aotTS);

        final String productType = sourceProduct.getProductType();
        final boolean isMerisProduct = EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(productType).matches() ||
                IdepixConstants.MERIS_CCL1P_TYPE_PATTERN.matcher(productType).matches();
        final boolean isVgtProduct = productType.startsWith("VGT PRODUCT FORMAT V1.");

        if (!isMerisProduct && !isVgtProduct) {
            throw new OperatorException("Product " + sourceProduct.getName() + " is neither MERIS nor VGT L1 product.");
        }

        Product reflProduct;
        if (isMerisProduct) {
            instrument = "MERIS";
            Map<String, Object> params = new HashMap<>(4);
            params.put("gaUseL1bLandWaterFlag", gaUseL1bLandWaterFlag);
            params.put("gaRefineClassificationNearCoastlines", gaRefineClassificationNearCoastlines);
            params.put("doEqualization", doEqualization);
            params.put("gaComputeCloudShadow", gaComputeCloudShadow);
            params.put("gaComputeCloudBuffer", gaComputeCloudBuffer);
            params.put("gaCopyCTP", gaCopyCTP);
            reflProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(MerisPreparationOp.class), params, sourceProduct);
        } else {
            instrument = "VGT";
            VgtPreparationOp vgtPrepOp = new VgtPreparationOp();
            vgtPrepOp.setParameterDefaultValues();
            vgtPrepOp.setSourceProduct(sourceProduct);
            reflProduct = vgtPrepOp.getTargetProduct();
        }
        if (reflProduct == EMPTY_PRODUCT) {
            setTargetProduct(EMPTY_PRODUCT);
            return;
        }

        // if bbdr seaice, just add 2 bands:
        // - aot
        // - aot_err
        // to reflProduct, and set this as targetProduct, and return.
        // we will set the climatological value (or zero AOT) in BBDR Op
        if (useAotClimatology) {
            reflProduct.addBand("aot", ProductData.TYPE_FLOAT32);
            reflProduct.addBand("aot_err", ProductData.TYPE_FLOAT32);
            setTargetProduct(reflProduct);
            return;
        }


        Map<String, Object> aotParams = new HashMap<>(4);
        aotParams.put("soilSpecId", soilSpecId);
        aotParams.put("vegSpecId", vegSpecId);
        aotParams.put("scale", scale);
        aotParams.put("ndviThreshold", ndviThr);

        Product aotDownsclProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(AerosolOp.class), aotParams, reflProduct, rhAot);

        Product fillAotProduct = aotDownsclProduct;
        if (!noFilling) {
            Map<String, Product> fillSourceProds = new HashMap<>(2);
            fillSourceProds.put("aotProduct", aotDownsclProduct);
            fillAotProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(GapFillingOp.class), GPF.NO_PARAMS, fillSourceProds);
        }

        targetProduct = fillAotProduct;
        if (!noUpscaling) {
            Map<String, Product> upsclProducts = new HashMap<>(2);
            upsclProducts.put("lowresProduct", fillAotProduct);
            upsclProducts.put("hiresProduct", reflProduct);
            Map<String, Object> sclParams = new HashMap<>(1);
            sclParams.put("scale", scale);
            Product aotHiresProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(UpscaleOp.class), sclParams, upsclProducts, rhTarget);

            targetProduct = mergeToTargetProduct(reflProduct, aotHiresProduct);
            ProductUtils.copyPreferredTileSize(reflProduct, targetProduct);
        }
        setTargetProduct(targetProduct);
    }

    private Product mergeToTargetProduct(Product reflProduct, Product aotHiresProduct) {
        String pname = reflProduct.getName() + "_AOT";
        String ptype = reflProduct.getProductType() + " GlobAlbedo AOT";
        int rasterWidth = reflProduct.getSceneRasterWidth();
        int rasterHeight = reflProduct.getSceneRasterHeight();
        Product tarP = new Product(pname, ptype, rasterWidth, rasterHeight);
        tarP.setStartTime(reflProduct.getStartTime());
        tarP.setEndTime(reflProduct.getEndTime());
        tarP.setPointingFactory(reflProduct.getPointingFactory());
        ProductUtils.copyMetadata(aotHiresProduct, tarP);
        ProductUtils.copyTiePointGrids(reflProduct, tarP);
        copyTiePointGridsIfBands(reflProduct, tarP);
        ProductUtils.copyGeoCoding(reflProduct, tarP);
        ProductUtils.copyFlagBands(reflProduct, tarP, true);
        ProductUtils.copyFlagBands(aotHiresProduct, tarP, true);
        String sourceBandName;
        if (copyToaRadBands) {
            for (Band sourceBand : reflProduct.getBands()) {
                sourceBandName = sourceBand.getName();
                if (sourceBand.getSpectralWavelength() > 0) {
                    ProductUtils.copyBand(sourceBandName, reflProduct, tarP, true);
                }
            }
        }
        for (Band sourceBand : reflProduct.getBands()) {
            sourceBandName = sourceBand.getName();

            boolean copyBand = (copyToaReflBands && !tarP.containsBand(sourceBandName) && sourceBand.getSpectralWavelength() > 0);
            copyBand = copyBand || (instrument.equals("VGT") && InstrumentConsts.getInstance().isVgtAuxBand(sourceBand));
            copyBand = copyBand || (sourceBandName.equals("elevation"));
            copyBand = copyBand || (gaCopyCTP && sourceBandName.equals("cloud_top_press"));

            if (copyBand && !tarP.containsBand(sourceBandName)) {
                ProductUtils.copyBand(sourceBandName, reflProduct, tarP, true);
            }
        }
        for (Band sourceBand : aotHiresProduct.getBands()) {
            sourceBandName = sourceBand.getName();
            if (!sourceBand.isFlagBand() && !tarP.containsBand(sourceBandName)) {
                ProductUtils.copyBand(sourceBandName, aotHiresProduct, tarP, true);
            }
        }
        return tarP;
    }

    private void copyTiePointGridsIfBands(Product reflProduct, Product tarP) {
        // i.e. if we use netcdf product as L1b input
        for (Band sourceBand : reflProduct.getBands()) {
            String sourceBandName = sourceBand.getName();
            if ((Float.isNaN(sourceBand.getSpectralWavelength()) || sourceBand.getSpectralWavelength() <= 0) &&
                    !tarP.containsBand(sourceBandName) && !tarP.containsTiePointGrid(sourceBandName)) {
                ProductUtils.copyBand(sourceBandName, reflProduct, tarP, true);
            }
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
            super(AerosolRetrievalMasterOp.class);
        }
    }
}
