package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.util.math.RsMathUtils;

/**
 * Radiance/reflectance conversion for OLCI
 *
 * @author olafd
 */
public class OlciRadReflConverter implements RadReflConverter {

    private String conversionMode;

    public OlciRadReflConverter(String conversionMode) {
        this.conversionMode = conversionMode;
    }

    @Override
    public float[] convert(Product sourceProduct, Sample[] sourceSamples) {
        final float sza = sourceSamples[Sensor.OLCI.getNumSpectralBands()].getFloat(); // in degrees
        float[] spectralInputValues = new float[Sensor.OLCI.getNumSpectralBands()];
        float[] spectralOutputValues = new float[Sensor.OLCI.getNumSpectralBands()];
        float[] solarFluxes = new float[Sensor.OLCI.getNumSpectralBands()];

        final int detectorIndex = sourceSamples[Sensor.MERIS.getNumSpectralBands() + 1].getInt();
        if (detectorIndex >= 0) {
            for (int i = 0; i < Sensor.OLCI.getNumSpectralBands(); i++) {
                spectralInputValues[i] = sourceSamples[i].getFloat();
                solarFluxes[i] = sourceSamples[Sensor.OLCI.getNumSpectralBands() + 1 + i].getFloat();
                if (conversionMode.equals("RAD_TO_REFL")) {
                    spectralOutputValues[i] = radianceToReflectance(spectralInputValues[i], sza, solarFluxes[i]);
                } else {
                    spectralOutputValues[i] = reflectanceToRadiance(spectralInputValues[i], sza, solarFluxes[i]);
                }
            }
        }

        return spectralOutputValues;
    }

    public String getSpectralBandAutogroupingString() {
        return conversionMode.equals("RAD_TO_REFL") ? Sensor.OLCI.getReflAutogroupingString() : Sensor.OLCI.getRadAutogroupingString();
    }

    public float radianceToReflectance(float rad, float sza, float solarFlux) {
        return RsMathUtils.radianceToReflectance(rad, sza, solarFlux);
    }

    public float reflectanceToRadiance(float refl, float sza, float solarFlux) {
        return RsMathUtils.reflectanceToRadiance(refl, sza, solarFlux);
    }

}
