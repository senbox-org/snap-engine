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
     * @param sourceProduct - the source product
     * @param sourceSamples - the source data samples per pixel
     * @param bandIndex     - the band index
     * @return the converted value for the spectral band with given band index
     */
    float convert(Product sourceProduct, Sample[] sourceSamples, int bandIndex);

    float convert(float spectralInputValue, float sza, float solarFlux);
}
