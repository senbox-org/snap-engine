/**
 * 
 */
package org.esa.snap.core.gpf.common.rtv;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PlainFeatureFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VectorDataNode;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.geotools.feature.DefaultFeatureCollection;
import org.jaitools.media.jai.vectorize.VectorizeDescriptor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.opengis.feature.simple.SimpleFeatureType;

import com.bc.ceres.core.ProgressMonitor;

/**
 * The raster to vector operator converts a raster to a vector.
 *
 * @author Lucian Barbulescu
 */
@OperatorMetadata(alias = "Raster-To-Vector",
        category = "Raster",
        description = "Converts a raster band to a vector",
        authors = "SNAP Team",
        version = "1.0",
        copyright = "(c) 2024 by CS GROUP ROMANIA")
public class RasterToVectorOp extends Operator {
	private final String TARGET_BAND_NAME ="vectors_band";

	@SourceProduct(alias = "source", description = "The product which contains the raster.")
    private Product sourceProduct;

	@TargetProduct
    private Product targetProduct = null;

	@Parameter(label = "Source band", description = "The band to convert.", 
			alias = "bandName", rasterDataNodeType = Band.class)
	private String bandName = null;

    private Band bandToConvert;
    
    private AffineTransformation jtsTransformation;
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
			
			if (bandToConvert.getDataType() >= ProductData.TYPE_FLOAT32) {
				throw new OperatorException("The band " + bandName + " must be of integer type");
			}
		} else {
			final Band[] bands = sourceProduct.getBands();
			if (bands.length == 0) {
				throw new OperatorException("The source product has no bands");
			} else {
				Optional<Band> band = Arrays.asList(bands).stream().filter(b -> b.getDataType() < ProductData.TYPE_FLOAT32).findFirst();
				if  (!band.isPresent()) {
					throw new OperatorException("The source product has no integer type bands");
				}else{
					throw new OperatorException("Please select the source band.");
				}
			}
		}

        this.targetProduct = new Product(sourceProduct.getName()+"_rtv", sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

		Band newBand = new Band(TARGET_BAND_NAME, ProductData.TYPE_INT32, bandToConvert.getRasterWidth(), bandToConvert.getRasterHeight());
		this.targetProduct.addBand(newBand);
	}

	@Override
	public void doExecute(ProgressMonitor pm) throws OperatorException {
		pm.beginTask("Raster To Vector", 1);
		try {
			executeOp();
		} catch (Throwable e) {
			throw new OperatorException(e);
		} finally {
			pm.done();
		}
	}

	private void executeOp(){
		if (this.bandToConvert == null ) {
			throw new OperatorException("Please select the source band.");
		}

		final Dimension tileSize = ImageManager.getPreferredTileSize(sourceProduct);
		this.targetProduct.setPreferredTileSize(tileSize);
		this.targetProduct.setSceneCRS(this.sourceProduct.getSceneCRS());
		this.targetProduct.setSceneGeoCoding(this.sourceProduct.getSceneGeoCoding());

		ProductUtils.copyGeoCoding(sourceProduct, targetProduct);

		Band targetBand = this.targetProduct.getBand(TARGET_BAND_NAME);
		targetBand.setGeoCoding(bandToConvert.getGeoCoding());

		SimpleFeatureType vectors = PlainFeatureFactory.createPlainFeatureType("vectors", Polygon.class, this.sourceProduct.getSceneCRS());

		VectorDataNode vdn = new VectorDataNode("shapes", vectors);

		this.targetProduct.getVectorDataGroup().add(vdn);

		final AffineTransform transf =  bandToConvert.getImageToModelTransform();
		this.jtsTransformation =
				new AffineTransformation(
						transf.getScaleX(),
						transf.getShearX(),
						transf.getTranslateX(),
						transf.getShearY(),
						transf.getScaleY(),
						transf.getTranslateY());

		// perform jai operation
		ParameterBlockJAI pb = new ParameterBlockJAI("Vectorize");
		pb.setSource("source0", bandToConvert.getSourceImage());
		pb.setParameter("band", 0);
		pb.setParameter("insideEdges", true);
		pb.setParameter("removeCollinear", true);
		final List<Number> noData = new ArrayList<>(1);
		if (bandToConvert.isNoDataValueUsed()) {
			noData.add(bandToConvert.getNoDataValue());
		} else {
			noData.add(0);
		}
		pb.setParameter("outsideValues", noData);

		final RenderedOp dest = JAI.create("Vectorize", pb);

		@SuppressWarnings("unchecked")
		final Collection<Geometry> geometryCollection =
				(Collection<Geometry>) dest.getProperty(VectorizeDescriptor.VECTOR_PROPERTY_NAME);

		final DefaultFeatureCollection featureCollection = vdn.getFeatureCollection();
		int counter = 0;
		for (final Geometry shape : geometryCollection) {
			shape.apply(jtsTransformation);
			featureCollection.add(PlainFeatureFactory.createPlainFeature(vdn.getFeatureType(), String.valueOf(counter++), shape, null));
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

	public static class Spi extends OperatorSpi {

        public Spi() {
            super(RasterToVectorOp.class);
        }
    }	
}
