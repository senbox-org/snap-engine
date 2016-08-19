package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.pointop.Sample;

/**
 * Interface for Radiance/reflectance conversion. To be implemented sensor-dependent.
 *
 * @author olafd
 */
public interface RadReflConverter {

    float[] convert(float[] spectralInputValue, float[] sza, float[] solarFlux);

    float convert(float spectralInputValue, float sza, float solarFlux);
}
