package org.esa.snap.change.detection;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Mask;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.SampleCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.pointop.PixelOperator;
import org.esa.snap.core.gpf.pointop.ProductConfigurer;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;
import org.esa.snap.core.gpf.pointop.TargetSampleConfigurer;
import org.esa.snap.core.gpf.pointop.WritableSample;

/**
 * The <code>ChangeVectorAnalysis</code> is change vector analysis operation between two dual
 * bands at two differents dates.
 * Return two products magnitude and direction of change
 * @author Douziech Florian
 */

@OperatorMetadata(alias = "ChangeVectorAnalysis", category = "Raster/Change Detection", version = "1.0", internal = false, description = "The 'Change Vector Analysis' between two dual bands at two differents dates.", authors = "Douziech Florian", copyright = "2021")
public class ChangeVectorAnalysis extends PixelOperator {

    private boolean hasValidPixelExpression;

    @SourceProducts(count = 2, description = "The sources product.")
    private Product[] sourceProducts;

    @SourceProduct(description = "A product to be updated.", optional = true)
    Product updateProduct;

    @TargetProduct
    private Product targetProduct;
    // any 2 bands from the first date
    @Parameter(label = "Band 1 at the first date", rasterDataNodeType = Band.class)
    private String sourceBand1;
    @Parameter(label = "Band 2 at the first date", rasterDataNodeType = Band.class)
    private String sourceBand2;
    // any 2 bands from the second date
    @Parameter(label = "Band 3 at the second date", rasterDataNodeType = Band.class)
    private String sourceBand3;
    @Parameter(label = "Band 2 at the second date", rasterDataNodeType = Band.class)
    private String sourceBand4;

    /**
     * Configures all source samples that this operator requires for the computation
     * of target samples. Source sample are defined by using the provided
     * {@link SourceSampleConfigurer}.
     * <p/>
     * <p/>
     * The method is called by {@link #initialize()}.
     *
     * @param sampleConfigurator The configurator that defines the layout of a
     *                           pixel.
     * @throws OperatorException If the source samples cannot be configured.
     */
    @Override
    protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurator) throws OperatorException {
        sampleConfigurator.defineSample(0, sourceBand1);
        sampleConfigurator.defineSample(1, sourceBand2);
        sampleConfigurator.defineSample(2, sourceBand1);
        sampleConfigurator.defineSample(3, sourceBand2);
    }

    /**
     * Configures all target samples computed by this operator. Target samples are
     * defined by using the provided {@link TargetSampleConfigurer}.
     * <p/>
     * <p/>
     * The method is called by {@link #initialize()}.
     *
     * @param sampleConfigurator The configurer that defines the layout of a pixel.
     * @throws OperatorException If the target samples cannot be configured.
     */
    @Override
    protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurator) throws OperatorException {
        sampleConfigurator.defineSample(0, "magnitude");
        sampleConfigurator.defineSample(1, "direction");
    }

    /**
     * Configures the target product via the given {@link ProductConfigurer}. Called
     * by {@link #initialize()}.
     * <p/>
     * Client implementations of this method usually add product components to the
     * given target product, such as {@link Band bands} to be computed by this
     * operator, {@link VirtualBand virtual bands}, {@link Mask masks} or
     * {@link SampleCoding sample codings}.
     * <p/>
     * The default implementation retrieves the (first) source product and copies to
     * the target product
     * <ul>
     * <li>the start and stop time by calling
     * {@link ProductConfigurer#copyTimeCoding()},</li>
     * <li>all tie-point grids by calling
     * {@link ProductConfigurer#copyTiePointGrids(String...)},</li>
     * <li>the geo-coding by calling {@link ProductConfigurer#copyGeoCoding()}.</li>
     * </ul>
     * <p/>
     * Clients that require a similar behaviour in their operator shall first call
     * the {@code super} method in their implementation.
     *
     * @param productConfigurator The target product configurer.
     * @throws OperatorException If the target product cannot be configured.
     * @see Product#addBand(Band)
     * @see Product#addBand(String, String)
     * @see Product#addTiePointGrid(TiePointGrid)
     * @see Product#getMaskGroup()
     */
    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurator) {
        super.configureTargetProduct(productConfigurator);
        Product target_product = productConfigurator.getTargetProduct();
        target_product.addBand("magnitude", ProductData.TYPE_FLOAT32);
        target_product.addBand("direction", ProductData.TYPE_FLOAT32);
    }

    private void computeChangeVectorAnalysis(Sample[] sourceSamples, WritableSample[] targetSamples) {
        double band1_t1 = sourceSamples[0].getDouble();
        double band2_t1 = sourceSamples[1].getDouble();
        double band1_t2 = sourceSamples[2].getDouble();
        double band2_t2 = sourceSamples[3].getDouble();

        // compute diffences on each axis
        double diff1 = band1_t2 - band1_t1;
        double diff2 = band2_t2 - band2_t1;
        if (Double.isNaN(diff1) || Double.isNaN(diff2)) {
            targetSamples[0].set(Float.NaN);
            targetSamples[1].set(Float.NaN);
            return;
        }

        // compute magnitude
        double change_magnitude = Math.sqrt(diff1 * diff1 + diff2 * diff2);
        double thresholdFix = 0;
        if (change_magnitude <= thresholdFix) {
            targetSamples[0].set(0);
            targetSamples[1].set(0);
            return;
        }
        
        // compute direction
        double change_direction = Math.atan2(diff1, diff2) * Math.PI / 180.0;
        if (change_direction < 0)
            change_direction += 360;
        
        targetSamples[0].set(change_magnitude);
        targetSamples[1].set(change_direction);
    }

    /**
     * Computes the target samples from the given source samples.
     * <p/>
     * The number of source/target samples is the maximum defined sample index plus
     * one. Source/target samples are defined by using the respective sample
     * configurator in the {@link #configureSourceSamples(SourceSampleConfigurer)
     * configureSourceSamples} and
     * {@link #configureTargetSamples(TargetSampleConfigurer)
     * configureTargetSamples} methods. Attempts to read from source samples or
     * write to target samples at undefined sample indices will cause undefined
     * behaviour.
     *
     * @param x             The current pixel's X coordinate.
     * @param y             The current pixel's Y coordinate.
     * @param sourceSamples The source samples (= source pixel).
     * @param targetSamples The target samples (= target pixel).
     */
    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        computeChangeVectorAnalysis(sourceSamples, targetSamples);
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ChangeVectorAnalysis.class);
        }
    }
}
