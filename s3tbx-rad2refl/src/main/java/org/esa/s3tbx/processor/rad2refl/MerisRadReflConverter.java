package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.util.math.RsMathUtils;

import java.util.Arrays;

/**
 * Radiance/reflectance conversion for MERIS
 *
 * @author olafd
 */
public class MerisRadReflConverter implements RadReflConverter {

    private String conversionMode;

    public MerisRadReflConverter(String conversionMode) {
        this.conversionMode = conversionMode;
    }

    @Override
    public float[] convert(float[] spectralInputValue, float[] sza, float[] solarFlux) {
        float[] reflectance = new float[spectralInputValue.length];
        Arrays.fill(reflectance, Float.NaN);
        for (int i = 0; i < spectralInputValue.length; i++) {
            if (conversionMode.equals("RAD_TO_REFL")) {
                reflectance[i] = RsMathUtils.radianceToReflectance(spectralInputValue[i], sza[i], solarFlux[i]);
            } else {
                reflectance[i] = RsMathUtils.reflectanceToRadiance(spectralInputValue[i], sza[i], solarFlux[i]);
            }
        }
        return reflectance;
    }

    @Override
    public float convert(float spectralInputValue, float sza, float solarFlux) {
        if (conversionMode.equals("RAD_TO_REFL")) {
            return RsMathUtils.radianceToReflectance(spectralInputValue, sza, solarFlux);
        } else {
            return RsMathUtils.reflectanceToRadiance(spectralInputValue, sza, solarFlux);
        }
    }

}
