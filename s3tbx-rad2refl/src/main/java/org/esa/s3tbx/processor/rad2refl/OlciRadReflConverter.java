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
    public float convert(Product sourceProduct, Sample[] sourceSamples, int bandIndex) {
        final float sza = sourceSamples[Sensor.OLCI.getNumSpectralBands()].getFloat(); // in degrees

        final int detectorIndex = sourceSamples[Sensor.OLCI.getSolarFluxBandNames().length +
                Sensor.OLCI.getNumSpectralBands() + 1].getInt();
        if (detectorIndex >= 0) {
            final float spectralInputValue = sourceSamples[bandIndex].getFloat();
            final float solarFlux = sourceSamples[Sensor.OLCI.getNumSpectralBands() + 1 + bandIndex].getFloat();
            if (conversionMode.equals("RAD_TO_REFL")) {
                return RsMathUtils.radianceToReflectance(spectralInputValue, sza, solarFlux);
            } else {
                return RsMathUtils.reflectanceToRadiance(spectralInputValue, sza, solarFlux);
            }
        }

        return -1.0f;
    }

    public String getSpectralBandAutogroupingString() {
        return conversionMode.equals("RAD_TO_REFL") ? Sensor.OLCI.getReflAutogroupingString() :
                Sensor.OLCI.getRadAutogroupingString();
    }

}
