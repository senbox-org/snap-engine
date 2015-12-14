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
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.BandMathsOp;
import org.esa.snap.core.util.RectangleExtender;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.math.MathUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OperatorMetadata(alias = "FillAerosol", internal = true)
public class FillAerosolOp extends MerisBasisOp {

    private RectangleExtender rectCalculator;
    private Map<Band, Band> sourceBands;
    private Map<Band, Band> defaultBands;
    private Product validProduct;
    private double[][] weights;
    private Rectangle sourceProductRect;
    
    @SourceProduct(alias="input")
    private Product sourceProduct;
    @SourceProduct(alias="default")
    private Product defaultProduct;
    @SourceProduct(alias="mask", optional = true)
    private Product maskProduct;
    @TargetProduct
    private Product targetProduct;
    
    private Configuration config;

    /**
     * Configuration Elements (can be set from XML)
     */
    private static class Configuration {
        private int pixelWidth;
        private boolean frs = true;
        private String maskBand;
        private List<BandDesc> bands;

        public Configuration() {
            bands = new ArrayList<>();
        }
    }

    public static class BandDesc {
        String name;
        String inputBand;
        String validExp;
        String defaultBand;
    }

    public FillAerosolOp() {
        config = new Configuration();
    }

    @Override
    public void initialize() throws OperatorException {
        targetProduct = createCompatibleProduct(sourceProduct, "fill_aerosol", "MER_L2");
        sourceBands = new HashMap<>(config.bands.size());
        defaultBands = new HashMap<>(config.bands.size());
        
        Map<String, Object> parameters = new HashMap<>();
        BandMathsOp.BandDescriptor[] bandDescriptors = new BandMathsOp.BandDescriptor[config.bands.size()];
        int i = 0;
        for (BandDesc bandDesc : config.bands) {
            Band srcBand = sourceProduct.getBand(bandDesc.inputBand);
            Band targetBand = targetProduct.addBand(bandDesc.name, ProductData.TYPE_FLOAT32);
            targetBand.setNoDataValue(-1);
            targetBand.setNoDataValueUsed(true);
            
            sourceBands.put(targetBand, srcBand);
            Band defaultBand = defaultProduct.getBand(bandDesc.defaultBand);
            defaultBands.put(targetBand, defaultBand);
            
            BandMathsOp.BandDescriptor bandDescriptor = new BandMathsOp.BandDescriptor();
    		bandDescriptor.name = bandDesc.name;
    		bandDescriptor.expression = bandDesc.validExp;
    		bandDescriptor.type = ProductData.TYPESTRING_INT8;
    		bandDescriptors[i] = bandDescriptor;
            
    		i++;
        }
		
        parameters.put("targetBands", bandDescriptors);
        validProduct = GPF.createProduct("BandMaths", parameters, sourceProduct);
		
		sourceProductRect = new Rectangle(sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        if (config.frs) {
            rectCalculator = new RectangleExtender(sourceProductRect, (config.pixelWidth+1)*4, (config.pixelWidth+1)*4);
		} else {
			rectCalculator = new RectangleExtender(sourceProductRect, config.pixelWidth, config.pixelWidth);
		}
        computeWeightMatrix();
    }
    
    private void computeWeightMatrix() {
    	weights = new double[config.pixelWidth][config.pixelWidth];
		for (int y = 0; y < config.pixelWidth; y++) {
			for (int x = 0; x < config.pixelWidth; x++) {
				final double w = Math.max(1.0 - Math.sqrt((x * x + y * y))/(config.pixelWidth),0.0);
				weights[x][y] = w;
			}
		}
		weights[0][0] = 0;
	}
    
    private float computeInterpolatedValue(final int x, final int y, Rectangle sourceRect, float[] srcValues, boolean[] valid, float defaultValue) {
		double weigthSum = 0;
		double weigthSumTotal = 0;
        double tauSum = 0;
        
        final int iyStart = Math.max(y - config.pixelWidth + 1,sourceRect.y);
        final int iyEnd = Math.min(y + config.pixelWidth - 1,sourceRect.y+sourceRect.height);
        final int ixStart = Math.max(x - config.pixelWidth + 1,sourceRect.x);
        final int ixEnd = Math.min(x + config.pixelWidth - 1,sourceRect.x+sourceRect.width);
        
        for (int iy = iyStart; iy < iyEnd; iy++) {
            final int yDist = Math.abs(iy - y);
            int index = convertToIndex(ixStart, iy, sourceRect);
            for (int ix = ixStart; ix < ixEnd; ix++, index++) {
            	final int xDist = Math.abs(ix - x);
                final double weight = weights[xDist][yDist];
                if (weight != 0) {
                	weigthSumTotal += weight;
                	if (valid[index]) {
						weigthSum += weight;
						tauSum += srcValues[index] * weight;
                    }
                }
            }
        }
        float mean;
        if (weigthSum > 0) {
			final double tauTemp = tauSum/weigthSum;
			final double ww = weigthSum/weigthSumTotal;
			// l3weighting gibt die Kr�mmung der Kurve an; l3weightiung=8 is a first guess
			final int l3weighting = 8;
			final double wwn = 1 - Math.pow(ww-1, l3weighting);
			final double tau = wwn * tauTemp + (1.0 - wwn)* defaultValue;
			mean = (float) tau;
        } else {
        	mean = defaultValue;
        }
        return mean;
	}
    
    private static int convertToIndex(int x, int y, Rectangle rectangle) {
        return (y - rectangle.y) * rectangle.width + (x - rectangle.x);
    }
    
    private float[] getScaledArrayFromTile(Tile tile) {
        ProductData valueDataBuffer = tile.getRawSamples();
        float[] scaledValues = new float[valueDataBuffer.getNumElems()];
        RasterDataNode srcRasterDataNode = tile.getRasterDataNode();
		boolean scaled = srcRasterDataNode.isScalingApplied();
		for (int i = 0; i < scaledValues.length; i++) {
			float value = valueDataBuffer.getElemFloatAt(i);
			if (scaled) {
				value = (float) srcRasterDataNode.scale(value);
			}
			scaledValues[i] = value;
		}
		
		return scaledValues;
    }
    
    private float[] getScaledArrayFromTileFRS(Tile tile) {
        ProductData valueDataBuffer = tile.getRawSamples();
        final int frsWidth = tile.getRectangle().width;
        final int frsHeight = tile.getRectangle().height;
        final int width = MathUtils.ceilInt(frsWidth / 4.0);
        final int height = MathUtils.ceilInt(frsHeight / 4.0);
        
        float[] scaledValues = new float[width * height];
        RasterDataNode srcRasterDataNode = tile.getRasterDataNode();
		boolean scaled = srcRasterDataNode.isScalingApplied();
		int scaledIndex = 0;
		for (int y = 0; y < frsHeight; y+=4) {
			int frsIndex = y * frsWidth;
			for (int x = 0; x < frsWidth; x+=4) {
				float value = valueDataBuffer.getElemFloatAt(frsIndex);
				if (scaled) {
					value = (float) srcRasterDataNode.scale(value);
				}
				scaledValues[scaledIndex] = value;
				frsIndex += 4;
				scaledIndex++;
			}
		}
		return scaledValues;
    }
    
    private boolean[] getArrayFromTileFRS(Tile tile) {
        ProductData dataBuffer = tile.getRawSamples();
        final int frsWidth = tile.getRectangle().width;
        final int frsHeight = tile.getRectangle().height;
        final int width = MathUtils.ceilInt(frsWidth / 4.0);
        final int height = MathUtils.ceilInt(frsHeight / 4.0);
        
        boolean[] values = new boolean[width * height];
		int scaledIndex = 0;
		for (int y = 0; y < frsHeight; y+=4) {
			int frsIndex = y * frsWidth;
			for (int x = 0; x < frsWidth; x+=4) {
				final boolean value = dataBuffer.getElemBooleanAt(frsIndex);
				values[scaledIndex] = value;
				frsIndex += 4;
				scaledIndex++;
			}
		}
		return values;
    }
    
    private boolean isMaskSetInRegion(int x, int y, int maxX, int maxY, Tile mask) {
    	final int ixEnd = Math.min(x+4, maxX);
    	final int iyEnd = Math.min(y+4, maxY);
    	for (int iy = y; iy < iyEnd; iy++) {
    		for (int ix = x; ix < ixEnd; ix++) {
    			if (mask.getSampleBoolean(ix, iy)) {
    				return true;
    			}
    		}
		}
    	return false;
    }
    
    private void setValueInRegion(int x, int y, int maxX, int maxY, float v, SimpleTile simpleTile) {
    	final int ixEnd = Math.min(x+4, maxX);
    	final int iyEnd = Math.min(y+4, maxY);
    	for (int iy = y; iy < iyEnd; iy++) {
    		for (int ix = x; ix < ixEnd; ix++) {
    			simpleTile.setSample(ix, iy, v);
    		}
		}
    }
    
    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

    	Rectangle targetRect = targetTile.getRectangle();
        Rectangle sourceRect = rectCalculator.extend(targetRect);
        pm.beginTask("Processing frame...", sourceRect.height + 1);
        try {
        	Tile maskTile = null;
            boolean useMask = false;
            if (maskProduct != null && StringUtils.isNotNullAndNotEmpty(config.maskBand)) {
            	maskTile = getSourceTile(maskProduct.getBand(config.maskBand), sourceRect);
            	useMask = true;
            }
            Tile defaultTile = getSourceTile(defaultBands.get(band), sourceRect);
            Tile validDataTile = getSourceTile(validProduct.getBand(band.getName()), sourceRect);
            Tile dataTile = getSourceTile(sourceBands.get(band), sourceRect);
            
            if (!config.frs) {
            	float[] scaledData = getScaledArrayFromTile(dataTile);
                boolean[] validData = (boolean[]) validDataTile.getRawSamples().getElems();
                
				for (int y = targetRect.y; y < targetRect.y + targetRect.height; y++) {
					for (int x = targetRect.x; x < targetRect.x
							+ targetRect.width; x++) {
						if (!useMask || maskTile.getSampleBoolean(x, y)) {
							if (validDataTile.getSampleBoolean(x, y)) {
								targetTile.setSample(x, y, dataTile
										.getSampleFloat(x, y));
							} else {
								final float defaultValue = defaultTile
										.getSampleFloat(x, y);
								float v = computeInterpolatedValue(x, y,
										sourceRect, scaledData, validData,
										defaultValue);
								targetTile.setSample(x, y, v);
							}
						} else {
							targetTile.setSample(x, y, -1);
						}
					}
					pm.worked(1);
				}
			} else {
				float[] scaledData = getScaledArrayFromTileFRS(dataTile);
				boolean[] validData = getArrayFromTileFRS(validDataTile);
	            Rectangle sourceRectFRS = new Rectangle(MathUtils.ceilInt(sourceRect.x/4.0), MathUtils.ceilInt(sourceRect.y/4.0),
	            		MathUtils.ceilInt(sourceRect.width/4.0), MathUtils.ceilInt(sourceRect.height/4.0));
	            
	            Rectangle intermediateRectangle = new Rectangle(targetRect.x - 4, targetRect.y - 4, targetRect.width + 8, targetRect.height + 8);
	            Rectangle productRect = sourceProductRect;
	            intermediateRectangle = intermediateRectangle.intersection(productRect);
	            SimpleTile intermediateRaster = new SimpleTile(intermediateRectangle);
	                
	            final int maxX = intermediateRectangle.x + intermediateRectangle.width;
	            final int maxY = intermediateRectangle.y + intermediateRectangle.height;
	            
				for (int y = intermediateRectangle.y; y < maxY; y += 4) {
					for (int x = intermediateRectangle.x; x < maxX; x += 4) {
						if (!useMask || isMaskSetInRegion(x, y, maxX, maxY, maskTile)) {
							if (validDataTile.getSampleBoolean(x, y)) {
								setValueInRegion(x, y, maxX, maxY, dataTile
										.getSampleFloat(x, y), intermediateRaster);
							} else {
								final float defaultValue = defaultTile
										.getSampleFloat(x, y);
								final int x4 = (x+1)/4;
								final int y4 = (y+1)/4;
								float v = computeInterpolatedValue(x4, y4,
										sourceRectFRS, scaledData, validData,
										defaultValue);
								setValueInRegion(x, y, maxX, maxY, v, intermediateRaster);
							}
						} else {
							setValueInRegion(x, y, maxX, maxY, -1, intermediateRaster);
						}
					}
					pm.worked(1);
				}
				
				//now smooth this
                final int maxtX = targetRect.x + targetRect.width;
                final int maxtY = targetRect.y + targetRect.height;
                for (int y = targetRect.y; y < maxtY; y++) {
                    for (int x = targetRect.x; x < maxtX; x ++) {
                        if (intermediateRaster.getSample(x, y) == -1) {
                            targetTile.setSample(x, y, -1);
                        } else {
                            float avg = 0;
                            int count = 0;
                            final int iyStart = Math.max(y - 1, intermediateRectangle.y);
                            final int iyEnd = Math.min(y + 2, intermediateRectangle.y
                                    + intermediateRectangle.height);
                            final int ixStart = Math.max(x - 1, intermediateRectangle.x);
                            final int ixEnd = Math.min(x + 2, intermediateRectangle.x
                                    + intermediateRectangle.width);
                            for (int iy = iyStart; iy < iyEnd; iy++) {
                                for (int ix = ixStart; ix < ixEnd; ix++) {
                                    float value = intermediateRaster.getSample(
                                            ix, iy);
                                    if (value != -1) {
                                        avg += value;
                                        count++;
                                    }
                                }
                            }
                            if (count > 0) {
                                float value = avg / count;
                                targetTile.setSample(x, y, value);
                            }
                        }
                    }
                }
			}
        } finally {
            pm.done();
        }
    }
    
    private static class SimpleTile {

        private final ProductData productData;
        private final Rectangle rectangle;

        private SimpleTile(Rectangle rectangle) {
            this.rectangle = rectangle;
            productData = ProductData.createInstance(ProductData.TYPE_FLOAT32, rectangle.width * rectangle.height);
        }

        public float getSample(int x, int y) {
            final int index = computeIndex(x, y);
            return productData.getElemFloatAt(index);
        }

        public void setSample(int x, int y, float v) {
            final int index = computeIndex(x, y);
            productData.setElemFloatAt(index, v);
        }
        
        private int computeIndex(int x, int y) {
            return (y - rectangle.y) * rectangle.width + (x - rectangle.x);
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(FillAerosolOp.class);
        }
    }
}