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
    public float convert(Product sourceProduct, Sample[] sourceSamples, int bandIndex) {
        final float szaNadir = sourceSamples[Sensor.SLSTR_500m.getNumSpectralBands()].getFloat(); // in degrees
        final float szaOblique = sourceSamples[Sensor.SLSTR_500m.getNumSpectralBands() + 1].getFloat(); // in degrees

        float sza;
        if (Sensor.SLSTR_500m.getRadBandNames()[bandIndex].endsWith("_o")) {
            sza = szaOblique;
        } else {
            sza = szaNadir;
        }
        final float spectralInputValue = sourceSamples[bandIndex].getFloat();
//            solarFluxes[i] = sourceSamples[Sensor.SLSTR.getNumSpectralBands() + 1 + i].getFloat();
        final float solarFlux = getSolarFlux(bandIndex); // todo: we have to wait until we have solar fluxes in test data or real data
        if (conversionMode.equals("RAD_TO_REFL")) {
            return RsMathUtils.radianceToReflectance(spectralInputValue, sza, solarFlux);
        } else {
            return RsMathUtils.reflectanceToRadiance(spectralInputValue, sza, solarFlux);
        }
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
