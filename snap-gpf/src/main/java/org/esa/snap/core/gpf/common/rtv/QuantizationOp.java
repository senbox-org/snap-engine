/**
 * 
 */
package org.esa.snap.core.gpf.common.rtv;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.media.jai.util.Range;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.StringUtils;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.core.ProgressMonitor;

/**
 * The Quantization operator
 *
 * @author Lucian Barbulescu
 */
@OperatorMetadata(alias = "Quantization",
        category = "Raster",
        description = "Converts each pixel from a band based on a lookup table",
        authors = "SNAP Team",
        version = "1.0",
        copyright = "(c) 2024 by CS GROUP ROMANIA")
public class QuantizationOp extends Operator {

	private final String TARGET_BAND_NAME ="converted_band";

	@SourceProduct(alias = "source", description = "The product which contains the raster.")
    private Product sourceProduct;

	@TargetProduct
    private Product targetProduct = null;

	@Parameter(label = "Source band", description = "The band to convert.", 
			alias = "bandName", rasterDataNodeType = Band.class)
	private String bandName = null;

	@Parameter(label = "Intervals", description = "The intervals used for conversion, separated by semicolons.",
			alias = "intervalsMap", converter = MapIntegerRangeConverter.class)
	private Map<Integer, Range> intervalsMap = new HashMap<>();
	
    private Band bandToConvert;

	@Override
	public void initialize() throws OperatorException {
		if (sourceProduct == null) {
			throw new OperatorException("Please add a source product");
		}
		
		if (bandName != null) {
			bandToConvert = sourceProduct.getBand(bandName);
			if (bandToConvert == null) {
				throw new OperatorException("The band " + bandName + " is not present in the source product");
			}
		} else {
			final Band[] bands = sourceProduct.getBands();
			if (bands.length == 0) {
				throw new OperatorException("The source product has no bands");
			} else {
				throw new OperatorException("Please select the source band.");
			}
		}

        this.targetProduct = new Product(sourceProduct.getName()+"_rl", sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
		final Dimension tileSize = sourceProduct.getPreferredTileSize();
		this.targetProduct.setPreferredTileSize(tileSize);
		this.targetProduct.setSceneCRS(this.sourceProduct.getSceneCRS());
		this.targetProduct.setSceneGeoCoding(this.sourceProduct.getSceneGeoCoding());
        this.targetProduct.setStartTime(this.sourceProduct.getStartTime());
        this.targetProduct.setEndTime(this.sourceProduct.getEndTime());
        
		ProductUtils.copyGeoCoding(this.sourceProduct, this.targetProduct);

		Band newBand = new Band(TARGET_BAND_NAME, ProductData.TYPE_INT32, bandToConvert.getRasterWidth(), bandToConvert.getRasterHeight());
		
		newBand.setGeoCoding(bandToConvert.getGeoCoding());
		newBand.setScalingFactor(bandToConvert.getScalingFactor());
		newBand.setScalingOffset(bandToConvert.getScalingOffset());
		newBand.setNoDataValueUsed(true);
		newBand.setNoDataValue(0);
		
		this.targetProduct.addBand(newBand);

	}

	@Override
	public void doExecute(ProgressMonitor pm) throws OperatorException {
		pm.beginTask("Quantization", 1);
		try {
			executeOp();
		} catch (Throwable e) {
			throw new OperatorException(e);
		} finally {
			pm.done();
		}
	}

	private void executeOp(){
		if (bandToConvert == null  ){
			throw new OperatorException("Please select the source band.");
		}

		Band targetBand = this.targetProduct.getBand(TARGET_BAND_NAME);
		targetBand.setGeoCoding(bandToConvert.getGeoCoding());
	}

	@Override
	public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

    	final Rectangle tileRectangle = targetTile.getRectangle();
    	final Tile sourceTile = getSourceTile(bandToConvert, tileRectangle);
        for (int y = tileRectangle.y; y < tileRectangle.y + tileRectangle.height; y++) {
            for (int x = tileRectangle.x; x < tileRectangle.x + tileRectangle.width; x++) {
            	final double value = sourceTile.getSampleDouble(x, y);
                Optional<Map.Entry<Integer, Range> > rangeEntryOptional = intervalsMap.entrySet().stream().filter(r -> r.getValue().contains(value)).findFirst();
                if (rangeEntryOptional.isPresent()) {
                	Map.Entry<Integer, Range> rangeEntry = rangeEntryOptional.get();
                    targetTile.setSample(x, y, rangeEntry.getKey());
                } else {
                	// Ignore pixel
                    targetTile.setSample(x, y, 0);
                }
            }
        }
	}

	/**
	 * Set a new value for the sourceProduct.
	 *
	 * @param sourceProduct the sourceProduct to set
	 */
	public void setSourceProduct(Product sourceProduct) {
		this.sourceProduct = sourceProduct;
	}

	/**
	 * Set a new value for the bandName.
	 *
	 * @param bandName the bandName to set
	 */
	public void setBandName(String bandName) {
		this.bandName = bandName;
	}

	/**
	 * Set a new value for the intervalsMap.
	 *
	 * @param intervalsMap the intervalsMap to set
	 */
	public void setIntervalsMap(Map<Integer, Range> intervalsMap) {
		this.intervalsMap = intervalsMap;
	}

	public static class Spi extends OperatorSpi {

        public Spi() {
            super(QuantizationOp.class);
        }
    }	
	
	public static class MapIntegerRangeConverter implements Converter<Map<Integer, Range> > {

		@Override
		public Class<? extends Map<Integer, Range>> getValueType() {
			return (Class<? extends Map<Integer, Range>>)(new HashMap<Integer, Range>()).getClass();
		}

		@Override
		public Map<Integer, Range> parse(String text) throws ConversionException {
	        if (text != null) {
	            text = text.trim();
	        }
	        if (text == null) {
	            throw new ConversionException("Invalid map parameter");
	        }
			
			final Map<Integer, Range> result = new HashMap<>();
			try {
				String[] arrIntervals = text.split(";");
				for (String interval: arrIntervals){
					final String[] s = StringUtils.csvToArray(interval);
					if (s.length != 3) {
						throw new ConversionException("Invalid entry: " + interval);
					}

					result.put(Integer.valueOf(s[0]), new Range(Double.class, Double.valueOf(s[1]), false, Double.valueOf(s[2]), true));
				}
			} catch (Exception ex) {
				throw new ConversionException("Invalid map parameter", ex);
			}

			return result;
		}

		@Override
		public String format(Map<Integer, Range> value) {
			if (value == null) {
				return null;
			}
			
			final StringBuilder result = new StringBuilder();
			value.entrySet().stream().forEach(e -> {
				result.append(e.getKey()).append(",").append(e.getValue().getMinValue()).append(",").append(e.getValue().getMaxValue()).append(";");
			});
			return result.toString();
		}
		
	}
	
}
