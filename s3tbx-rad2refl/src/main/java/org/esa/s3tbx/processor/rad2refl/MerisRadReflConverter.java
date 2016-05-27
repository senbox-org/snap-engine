package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.pointop.Sample;
import org.esa.snap.core.util.math.Array;
import org.esa.snap.core.util.math.RsMathUtils;

import java.io.IOException;
import java.util.Arrays;

/**
 * Radiance/reflectance conversion for MERIS
 *
 * @author olafd
 */
public class MerisRadReflConverter implements RadReflConverter {

    private String conversionMode;

    private Rad2ReflAuxdata rad2ReflAuxdata;

    public MerisRadReflConverter(Product sourceProduct, String conversionMode) {
        this.conversionMode = conversionMode;

        try {
            rad2ReflAuxdata = Rad2ReflAuxdata.loadAuxdata(sourceProduct.getProductType());
        } catch (IOException e) {
            throw new OperatorException("Cannot load Radiance-to-Reflectance auxdata for MERIS: " + e.getMessage());
        }
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

}
