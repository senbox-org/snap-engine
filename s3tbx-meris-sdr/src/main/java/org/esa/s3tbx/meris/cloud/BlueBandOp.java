/*
 * $Id: BlueBandOp.java,v 1.1 2007/03/27 12:52:22 marcoz Exp $
 *
 * Copyright (C) 2006 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.s3tbx.meris.cloud;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

@OperatorMetadata(alias = "Meris.BlueBand", internal = true)
public class BlueBandOp extends MerisBasisOp {

    public static final int FLAG_CLEAR = 1;
    public static final int FLAG_SNOW = 2;
    public static final int FLAG_DENSE_CLOUD = 4;
    public static final int FLAG_THIN_CLOUD = 8;
    public static final int FLAG_SNOW_INDEX = 16;
    public static final int FLAG_SNOW_PLAUSIBLE = 32;
    public static final int FLAG_BRIGHT_LAND = 64;

    public static final int CLEAR_BIT = 0;
    public static final int SNOW_BIT = 1;
    public static final int DENSE_CLOUD_BIT = 2;
    public static final int THIN_CLOUD_BIT = 3;
    public static final int SNOW_INDEX_BIT = 4;
    public static final int SNOW_PLAUSIBLE_BIT = 5;
    public static final int BRIGHT_LAND_BIT = 6;
    
    public static final String BLUE_FLAG_BAND = "blue_cloud";

    private static final float D_BBT = 0.25f;
    private static final float D_ASS = 0.4f;

    private static final float R1_BBT = -1.0f;
    private static final float R2_BBT = 0.01f;
    private static final float R3_BBT = 0.1f;
    private static final float R4_BBT = 0.95f;
    private static final float R5_BBT = 0.05f;
    private static final float R6_BBT = 0.6f;
    private static final float R7_BBT = 0.45f;

    private static final float R1_ASS = 0.95f;
    private static final float R2_ASS = 0.05f;
    private static final float R3_ASS = 0.6f;
    private static final float R4_ASS = 0.05f;
    private static final float R5_ASS = 0.5f;

    // for plausibility test
    private static final float LAT_ALWAYS_SNOW = 60.0f;
    private static final float LAT_TROPIC = 30.0f;
    private static final float ALT_MEDIAL = 1000.0f;
    private static final float ALT_TROPIC = 2000.0f;
    private static final int MIN_LAND_ALT = -50;

    // for bright sand test
    private static final float SLOPE2_LOW = 0.65f;
    private static final float SLOPE2_UPPER = 1.075f;

    private static final float TOAR_9_SAT = 0.99f;
   
    private Band landBand;
    public int month;
    
    @SourceProduct(alias="l1b")
    private Product l1bProduct;
    @SourceProduct(alias="toar")
    private Product brrProduct;
    @TargetProduct
    private Product targetProduct;
    

    @Override
    public void initialize() throws OperatorException {
        month = l1bProduct.getStartTime().getAsCalendar().get(Calendar.MONTH);

        targetProduct = createCompatibleProduct(l1bProduct, "MER_BLUEBAND_CLOUD", "MER_L2");
        
        // create and add the flags coding
        FlagCoding flagCoding = createFlagCoding();
        targetProduct.getFlagCodingGroup().add(flagCoding);

        // create and add the flags dataset
        Band cloudFlagBand = targetProduct.addBand(BLUE_FLAG_BAND, ProductData.TYPE_UINT8);
        cloudFlagBand.setDescription("blue band cloud flags");
        cloudFlagBand.setSampleCoding(flagCoding);
        
        landBand = createBooleanBandForExpression("$toar.l2_flags_p1.F_LANDCONS or (($toar.l2_flags_p1.F_LAND or $l1b.dem_alt > "
                + MIN_LAND_ALT + " )and $toar.l2_flags_p1.F_CLOUD)");
    }
    
    private Band createBooleanBandForExpression(String expression) throws OperatorException {
        Map<String, Object> parameters = new HashMap<>();
        BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[1];
        BandMathsOp.BandDescriptor bandDescriptor = new BandMathsOp.BandDescriptor();
        bandDescriptor.name = "bBand";
        bandDescriptor.expression = expression;
        bandDescriptor.type = ProductData.TYPESTRING_INT8;
        bandDescriptors[0] = bandDescriptor;
        parameters.put("targetBands", bandDescriptors);

        Map<String, Product> products = new HashMap<>();
        products.put(getSourceProductId(l1bProduct), l1bProduct);
        products.put(getSourceProductId(brrProduct), brrProduct);
        
        Product validLandProduct = GPF.createProduct("BandMaths",
                                                     parameters, products);
        return validLandProduct.getBand("bBand");
    }

    
    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
    	
    	Rectangle rect = targetTile.getRectangle();
        pm.beginTask("Processing frame...", rect.height);
        try {
            float[] toar1 = (float[]) getSourceTile(brrProduct.getBand("toar_1"), rect).getRawSamples().getElems();
			float[] toar7 = (float[]) getSourceTile(brrProduct.getBand("toar_7"), rect).getRawSamples().getElems();
			float[] toar9 = (float[]) getSourceTile(brrProduct.getBand("toar_9"), rect).getRawSamples().getElems();
			float[] toar10 = (float[]) getSourceTile(brrProduct.getBand("toar_10"), rect).getRawSamples().getElems();
			float[] toar11 = (float[]) getSourceTile(brrProduct.getBand("toar_11"), rect).getRawSamples().getElems();
			float[] toar13 = (float[]) getSourceTile(brrProduct.getBand("toar_13"), rect).getRawSamples().getElems();
			float[] toar14 = (float[]) getSourceTile(brrProduct.getBand("toar_14"), rect).getRawSamples().getElems();
			
			Tile latitude;
			Tile altitude;
			if (l1bProduct.getProductType().equals(
			        EnvisatConstants.MERIS_FSG_L1B_PRODUCT_TYPE_NAME)) {
			    latitude = getSourceTile(l1bProduct.getBand("corr_latitude"), rect);
			    altitude = getSourceTile(l1bProduct.getBand("altitude"), rect);
			} else {
			    latitude = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_LAT_DS_NAME), rect);
			    altitude = getSourceTile(l1bProduct.getTiePointGrid(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME), rect);
			}
			Tile sourceTile = getSourceTile(landBand, rect);
			ProductData rawSamples = sourceTile.getRawSamples();
//			boolean[] safeLand = (boolean[]) rawSamples.getElems(); // This cast does not work!?
			
			boolean[] safeLand = new boolean[rawSamples.getNumElems()];
			for (int i=0; i<rawSamples.getNumElems(); i++) {
				safeLand[i] = rawSamples.getElemBooleanAt(i);
			}
			
            ProductData rawSampleData = targetTile.getRawSamples();
            byte[] cloudFlagScanLine = (byte[]) rawSampleData.getElems();

            int i = 0;
            for (int y = rect.y; y < rect.y+rect.height; y++) {
            	for (int x = rect.x; x < rect.x+rect.width; x++, i++) {
            		final float po2 = toar11[i] / toar10[i];
            		final float alt = altitude.getSampleFloat(x, y);
                    boolean assuredLand = safeLand[i];
                    boolean isSnowPlausible = isSnowPlausible(latitude.getSampleFloat(x, y), alt, assuredLand);

                    boolean isBrightLand = isBrightLand(toar9[i], toar14[i]);

            		// blue band test
            		if (toar1[i] >= D_BBT) {
            			final float ndvi = (toar13[i] - toar7[i])
            					/ (toar13[i] + toar7[i]);
            			final float ndsi = (toar10[i] - toar13[i])
            					/ (toar10[i] + toar13[i]);
            			// snow cover
            			if (((ndvi <= R1_BBT * ndsi + R2_BBT) || // snow test 1
            					(ndsi >= R3_BBT))
            					&& (po2 <= R7_BBT)) {
            				cloudFlagScanLine[i] = FLAG_SNOW;
            			} else {
            				if ((toar13[i] <= R4_BBT * toar7[i] + R5_BBT) && // snow test 2
            						(toar13[i] <= R6_BBT) && (po2 <= R7_BBT)) {
            					cloudFlagScanLine[i] = FLAG_SNOW;
            				} else {
            					cloudFlagScanLine[i] = FLAG_DENSE_CLOUD;
            				}
            			}
            		} else {
            			// altitude of scattering surface
            			if ((alt < 1700 && po2 >= D_ASS)
            					|| (alt >= 1700 && po2 > 0.04 + (0.31746 + 0.00003814 * alt))) {
            				// snow cover
            				if ((toar13[i] <= R1_ASS * toar7[i] + R2_ASS) && // snow test 3
            						(toar13[i] <= R3_ASS)) {
            					if ((toar13[i] >= R4_ASS) && // snow test 4
            							(toar7[i] >= R5_ASS)) {
            						cloudFlagScanLine[i] = FLAG_SNOW;
            					} else {
            						cloudFlagScanLine[i] = FLAG_CLEAR;
            					}
            				} else {
            					cloudFlagScanLine[i] = FLAG_THIN_CLOUD;
            				}
            			} else {
            				cloudFlagScanLine[i] = FLAG_CLEAR;
            			}
            		}
            		double snowIndex = (toar14[i] - toar13[i]) / (toar14[i] + toar13[i]);
                    if(snowIndex < -0.01) {
                        cloudFlagScanLine[i] += FLAG_SNOW_INDEX;
                    }
                    if (isSnowPlausible) {
                        cloudFlagScanLine[i] += FLAG_SNOW_PLAUSIBLE;
                    }
                    if (isBrightLand) {
                        cloudFlagScanLine[i] += FLAG_BRIGHT_LAND;
                    }
            	}
                pm.worked(1);
            }
            targetTile.setRawSamples(rawSampleData);
        } finally {
            pm.done();
        }
    }

    private boolean isBrightLand(float toar_9, float toar_14) {
        final float bsRatio = toar_9 / toar_14;
        return ((bsRatio >= SLOPE2_LOW) && (bsRatio <= SLOPE2_UPPER)) || toar_9 > TOAR_9_SAT;
    }

    private boolean isSnowPlausible(float lat, float alt, boolean land) {
        if (!land) {
            return false;
        }
        if (lat > LAT_ALWAYS_SNOW || lat < -LAT_ALWAYS_SNOW) {
            return true;
        }
        if (lat <= LAT_ALWAYS_SNOW && lat >= LAT_TROPIC) {
            // northern hemisphere
            if (month >= 4 && month <= 10) {
                //summer
                if (alt > ALT_MEDIAL) {
                    return true;
                }
            } else {
                // winter
                return true;
            }
        } else if (lat >= -LAT_ALWAYS_SNOW && lat <= -LAT_TROPIC) {
            // southern hemisphere
            if (month >= 10 || month <= 4) {
                //summer
                if (alt > ALT_MEDIAL) {
                    return true;
                }
            } else {
                // winter
                return true;
            }
        } else if (lat < LAT_TROPIC && lat > -LAT_TROPIC && alt > ALT_TROPIC) {
            return true;
        }
        return false;
    }

    public static FlagCoding createFlagCoding() {
        MetadataAttribute cloudAttr;
        final FlagCoding flagCoding = new FlagCoding(BLUE_FLAG_BAND);
        flagCoding.setDescription("Blue Band - Cloud Flag Coding");

        cloudAttr = new MetadataAttribute("clear", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_CLEAR);
        flagCoding.addAttribute(cloudAttr);

        cloudAttr = new MetadataAttribute("snow", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_SNOW);
        flagCoding.addAttribute(cloudAttr);

        cloudAttr = new MetadataAttribute("dense_cloud", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_DENSE_CLOUD);
        flagCoding.addAttribute(cloudAttr);

        cloudAttr = new MetadataAttribute("thin_cloud", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_THIN_CLOUD);
        flagCoding.addAttribute(cloudAttr);

        cloudAttr = new MetadataAttribute("snow_index", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_SNOW_INDEX);
        flagCoding.addAttribute(cloudAttr);

        cloudAttr = new MetadataAttribute("snow_plausible", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_SNOW_PLAUSIBLE);
        flagCoding.addAttribute(cloudAttr);
        
        cloudAttr = new MetadataAttribute("bright_land", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_BRIGHT_LAND);
        flagCoding.addAttribute(cloudAttr);

        return flagCoding;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(BlueBandOp.class);
        }
    }
}