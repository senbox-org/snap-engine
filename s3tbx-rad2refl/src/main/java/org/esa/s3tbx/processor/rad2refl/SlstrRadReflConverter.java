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
    public float[] convert(Product sourceProduct, Sample[] sourceSamples) {

        final float szaNadir = sourceSamples[Sensor.SLSTR_500m.getNumSpectralBands()].getFloat(); // in degrees
        final float szaOblique = sourceSamples[Sensor.SLSTR_500m.getNumSpectralBands() + 1].getFloat(); // in degrees
        float[] spectralInputValues = new float[Sensor.SLSTR_500m.getNumSpectralBands()];
        float[] spectralOutputValues = new float[Sensor.SLSTR_500m.getNumSpectralBands()];
        float[] solarFluxes = new float[Sensor.SLSTR_500m.getNumSpectralBands()];

        for (int i = 0; i < Sensor.SLSTR_500m.getNumSpectralBands(); i++) {
            float sza;
            if (Sensor.SLSTR_500m.getRadBandNames()[i].endsWith("_o")) {
                sza = szaOblique;
            } else {
                sza = szaNadir;
            }
            spectralInputValues[i] = sourceSamples[i].getFloat();
//            solarFluxes[i] = sourceSamples[Sensor.SLSTR.getNumSpectralBands() + 1 + i].getFloat();
            solarFluxes[i] = getSolarFlux(i); // todo: we have to wait until we have solar fluxes in test data or real data
            if (conversionMode.equals("RAD_TO_REFL")) {
                spectralOutputValues[i] = RsMathUtils.radianceToReflectance(spectralInputValues[i], sza, solarFluxes[i]);
            } else {
                spectralOutputValues[i] = RsMathUtils.reflectanceToRadiance(spectralInputValues[i], sza, solarFluxes[i]);
            }
        }

        return spectralOutputValues;
    }

    public String getSpectralBandAutogroupingString() {
        return conversionMode.equals("RAD_TO_REFL") ? Sensor.SLSTR_500m.getReflAutogroupingString() :
                Sensor.SLSTR_500m.getRadAutogroupingString();
    }

    float getSolarFlux(int allSpectralBandsIndex) {
        final int channel = Integer.parseInt(Sensor.SLSTR_500m.getRadBandNames()[allSpectralBandsIndex].substring(1, 2));
        // channel is between 1 and 6
        return Sensor.SLSTR_500m.getSolarFluxesDefault()[channel - 1];
    }

}
