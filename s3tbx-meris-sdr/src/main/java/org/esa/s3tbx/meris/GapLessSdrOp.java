/*
 * $Id: GapLessSdrOp.java,v 1.1 2007/03/27 12:52:22 marcoz Exp $
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
package org.esa.s3tbx.meris;

import com.bc.ceres.core.ProgressMonitor;
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

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@OperatorMetadata(alias = "Meris.GapLessSdr", internal = true)
public class GapLessSdrOp extends MerisBasisOp {

    private Map<Band, Band> sdrBands;
    private Map<Band, Band> toarBands;
    private Band invalidBand;
    
    @SourceProduct(alias="sdr")
    private Product sdrProduct;
    @SourceProduct(alias="toar")
    private Product toarProduct;
    @TargetProduct
    private Product targetProduct;

//    private BandMathsOp.BandDescriptor bandDescriptor = new BandMathsOp.BandDescriptor();

    @Override
    public void initialize() throws OperatorException {
        targetProduct = createCompatibleProduct(sdrProduct, "MER_SDR", "MER_SDR");

        sdrBands = new HashMap<>();
        toarBands = new HashMap<>();
        String[] bandNames = sdrProduct.getBandNames();
        for (final String bandName : bandNames) {
            if (bandName.startsWith("sdr_") && !bandName.endsWith("flags")) {
                Band targetBand = ProductUtils.copyBand(bandName, sdrProduct, targetProduct, false);
                Band sdrBand = sdrProduct.getBand(bandName);
                sdrBands.put(targetBand, sdrBand);
                
                final String toarBandName = bandName.replaceFirst("sdr", "toar");
    			Band toarBand = toarProduct.getBand(toarBandName);
                toarBands.put(targetBand, toarBand);
            }
        }
        invalidBand = createBooleanExpressionBand("l2_flags_p1.F_INVALID", toarProduct);
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
    	Rectangle rectangle = targetTile.getRectangle();
        pm.beginTask("Processing frame...", rectangle.height + 1);
        try {
        	Tile sdrTile = getSourceTile(sdrBands.get(band), rectangle);
        	Tile toarTile = getSourceTile(toarBands.get(band), rectangle);
        	Tile invalid = getSourceTile(invalidBand, rectangle);
        	
        	pm.worked(1);

        	for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
				for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
					final float sdr = sdrTile.getSampleFloat(x, y);
					if (invalid.getSampleBoolean(x, y) || (sdr != -1 && sdr != 0)) {
						targetTile.setSample(x, y, sdr);
					} else {
						targetTile.setSample(x, y, toarTile.getSampleFloat(x, y));

					}
				}
				pm.worked(1);
			}
        } finally {
            pm.done();
        }
    }

    private static Band createBooleanExpressionBand(String expression, Product sourceProduct) {
        final BandMathsOp.BandDescriptor bandDescriptor = new BandMathsOp.BandDescriptor();
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
            super(GapLessSdrOp.class);
        }
    }
}