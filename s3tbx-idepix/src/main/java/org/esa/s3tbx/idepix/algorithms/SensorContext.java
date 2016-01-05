package org.esa.s3tbx.idepix.algorithms;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.pointop.SourceSampleConfigurer;

/**
 * OC-CCI sensor context interface
 * todo: duplicated from beam-waterradiance - put in a more general place?
 *
 * @author olafd
 */
public interface SensorContext {
    Sensor getSensor();

    int getNumSpectralInputBands();

    void configureSourceSamples(SourceSampleConfigurer sampleConfigurer, Product sourceProduct, String spectralBandPrefix);

    /**
     * Scales the input spectral data to be consistent with MERIS TOA reflectances  (dimensionless)
     * Scaling is performed "in place", if necessary
     *
     * @param inputs input data vector
     */
    void scaleInputSpectralDataToReflectance(double[] inputs, int offset);

    void init(Product sourceProduct);

    int getSrcRadOffset();
}
