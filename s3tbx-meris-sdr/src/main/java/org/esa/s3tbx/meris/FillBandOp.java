/*
 * $Id: FillAerosolOp.java,v 1.1 2007/05/14 12:26:01 marcoz Exp $
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

import java.util.*;

@OperatorMetadata(alias = "FillBand", internal = true)
public class FillBandOp extends MerisBasisOp {
	
	private Map<Band, Float> defaultMap;
	/**
     * Configuration Elements (can be set from XML)
     */
	private static class Configuration {
		private List<BandDesc> bands;
		
		public Configuration() {
			bands = new ArrayList<>();
		}
	}
	public static class BandDesc {
		String name;
		float defaultValue;
	}
	private Configuration config;
	
	@SourceProduct(alias="input")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
	
	
	public FillBandOp() {
		config = new Configuration();
	}

	@Override
    public void initialize() throws OperatorException {
        
		targetProduct = createCompatibleProduct(sourceProduct, "fille_band", "FILL");
		defaultMap = new HashMap<>(config.bands.size());
		for (BandDesc bandDesc: config.bands) {
			Band targetBand = targetProduct.addBand(bandDesc.name, ProductData.TYPE_FLOAT32);
			defaultMap.put(targetBand, bandDesc.defaultValue);
		}
	}
	
	@Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
	    ProductData rawSampleData = targetTile.getRawSamples();
        float[] outValues = (float[]) rawSampleData.getElems();
	    final float defaultValue = defaultMap.get(band);
	    
	    Arrays.fill(outValues, defaultValue);
	    targetTile.setRawSamples(rawSampleData);
    }
	
	public static class Spi extends OperatorSpi {
        public Spi() {
            super(FillBandOp.class);
        }
    }
}