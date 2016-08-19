package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.util.math.RsMathUtils;

/**
 * Radiance/reflectance conversion for SLSTR
 *
 * @author olafd
 */
public class SlstrRadReflConverter implements RadReflConverter {

    private String conversionMode;

    public SlstrRadReflConverter(String conversionMode) {
        this.conversionMode = conversionMode;
    }

    @Override
    public float[] convert(float[] spectralInputValue, float[] sza, float[] solarFlux) {
        return new float[0];
    }

    @Override
    public float convert(float spectralInputValue, float sza, float solarFlux) {
        return 0;
    }

    float getSolarFlux(int allSpectralBandsIndex) {
        final int channel = Integer.parseInt(Sensor.SLSTR_500m.getRadBandNames()[allSpectralBandsIndex].substring(1, 2));
        // channel is between 1 and 6
        return Sensor.SLSTR_500m.getSolarFluxesDefault()[channel - 1];
    }

}
