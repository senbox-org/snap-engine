package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.pointop.Sample;

/**
 * Interface for Radiance/reflectance conversion. To be implemented sensor-dependent.
 *
 * @author olafd
 */
public interface RadReflConverter {

    /**
     * Provides Conversion from radiance to reflectance or backwards.
     *
     * @param sourceProduct - the source product
     * @param sourceSamples - the source data samples per pixel
     *
     * @return the converted values for the spectral bands of the sensor
     */
    float[] convert(Product sourceProduct, Sample[] sourceSamples);
}
