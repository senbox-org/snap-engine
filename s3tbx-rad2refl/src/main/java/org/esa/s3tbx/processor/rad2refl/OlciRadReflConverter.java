package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.util.math.RsMathUtils;

import java.util.Arrays;

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
    public float[] convert(float[] radiances, float[] sza, float[] samplesFlux) {
        float[] fRet = new float[radiances.length];
        Arrays.fill(fRet, Float.NaN);
        for (int i = 0; i < radiances.length; i++) {
            fRet[i] = RsMathUtils.radianceToReflectance(radiances[i], sza[i], samplesFlux[i]);
        }
        return fRet;
    }

    @Override
    public float convert(float radiance, float sza, float flux) {
        return RsMathUtils.radianceToReflectance(radiance, sza, flux);
    }
}
