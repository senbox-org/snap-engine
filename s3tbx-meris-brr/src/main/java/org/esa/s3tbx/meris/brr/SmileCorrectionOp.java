/*
 * $Id: SmileCorrectionOp.java,v 1.2 2007/04/26 11:53:53 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.s3tbx.meris.brr;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.s3tbx.meris.l2auxdata.Constants;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataProvider;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.util.Map;

@OperatorMetadata(alias = "Meris.SmileCorrection", internal = true)
public class SmileCorrectionOp extends MerisBasisOp implements Constants {

    private L2AuxData auxData;

    private Band isLandBand;
    private Band[] rhoCorectedBands;

    @SourceProduct(alias="l1b")
    private Product l1bProduct;
    @SourceProduct(alias="gascor")
    private Product gascorProduct;
    @SourceProduct(alias="land")
    private Product landProduct;
    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {
        try {
            auxData = L2AuxDataProvider.getInstance().getAuxdata(l1bProduct);
        } catch (Exception e) {
            throw new OperatorException("could not load L2Auxdata", e);
        }
        
        createTargetProduct();
    }

    private void createTargetProduct() throws OperatorException {
        targetProduct = createCompatibleProduct(gascorProduct, "MER", "MER_L2");
        rhoCorectedBands = new Band[EnvisatConstants.MERIS_L1B_NUM_SPECTRAL_BANDS];
        for (int i = 0; i < rhoCorectedBands.length; i++) {
            Band inBand = gascorProduct.getBandAt(i);

            rhoCorectedBands[i] = targetProduct.addBand(inBand.getName(), ProductData.TYPE_FLOAT32);
            ProductUtils.copySpectralBandProperties(inBand, rhoCorectedBands[i]);
            rhoCorectedBands[i].setNoDataValueUsed(true);
            rhoCorectedBands[i].setNoDataValue(BAD_VALUE);
        }
        isLandBand = createBooleanExpressionBand(LandClassificationOp.LAND_FLAGS + ".F_LANDCONS", landProduct);
        if (l1bProduct.getPreferredTileSize() != null) {
            targetProduct.setPreferredTileSize(l1bProduct.getPreferredTileSize());
        }
    }
    
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Processing frame...", rectangle.height);
        try {
        	Tile detectorIndex = getSourceTile(l1bProduct.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME), rectangle);
        	Tile[] rho = new Tile[rhoCorectedBands.length];
            for (int i = 0; i < rhoCorectedBands.length; i++) {
                rho[i] = getSourceTile(gascorProduct.getBand(rhoCorectedBands[i].getName()), rectangle);
            }
            Tile isLandCons = getSourceTile(isLandBand, rectangle);
            
            Tile[] rhoCorrected = new Tile[rhoCorectedBands.length];
            for (int i = 0; i < rhoCorectedBands.length; i++) {
                rhoCorrected[i] = targetTiles.get(rhoCorectedBands[i]);
            }

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
				for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
					if (rho[0].getSampleFloat(x, y) == BAD_VALUE) {
						for (int bandId = 0; bandId < L1_BAND_NUM; bandId++) {
							rhoCorrected[bandId].setSample(x, y, BAD_VALUE);
						}
					} else {
						L2AuxData.SmileParams params; 
						if (isLandCons.getSampleBoolean(x, y)) {
							params = auxData.land_smile_params;
						} else {
							params = auxData.water_smile_params;
						}
						for (int bandId = 0; bandId < L1_BAND_NUM; bandId++) {
				            if (params.enabled[bandId]) {
				                /* DPM #2.1.6-3 */
				                final int bandMin = params.derivative_band_id[bandId][0];
				                final int bandMax = params.derivative_band_id[bandId][1];
				                final int detector = detectorIndex.getSampleInt(x, y);
				                final double derive = (rho[bandMax].getSampleFloat(x, y) - rho[bandMin].getSampleFloat(x, y))
				                        / (auxData.central_wavelength[bandMax][detector] - auxData.central_wavelength[bandMin][detector]);
				                /* DPM #2.1.6-4 */
				                final double simleCorrectValue = rho[bandId].getSampleFloat(x, y)
				                        + derive
				                        * (auxData.theoretical_wavelength[bandId] - auxData.central_wavelength[bandId][detector]);
				                rhoCorrected[bandId].setSample(x, y, (float)simleCorrectValue);
				            } else {
				                /* DPM #2.1.6-5 */
				            	rhoCorrected[bandId].setSample(x, y, rho[bandId].getSampleFloat(x, y));
				            }
				        }
					}
				}
				pm.worked(1);
            }
		} finally {
            pm.done();
        }
    }

    public static Band createBooleanExpressionBand(String expression, Product sourceProduct) {
        BandMathsOp.BandDescriptor bandDescriptor = new BandMathsOp.BandDescriptor();
        bandDescriptor.name = "band1";
        bandDescriptor.expression = expression;
        bandDescriptor.type = ProductData.TYPESTRING_INT8;

        BandMathsOp bandMathsOp = new BandMathsOp();
        bandMathsOp.setParameterDefaultValues();
        bandMathsOp.setSourceProduct(sourceProduct);
        bandMathsOp.setTargetBandDescriptors(bandDescriptor);
        return bandMathsOp.getTargetProduct().getBandAt(0);
    }



    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SmileCorrectionOp.class);
        }
    }
}
