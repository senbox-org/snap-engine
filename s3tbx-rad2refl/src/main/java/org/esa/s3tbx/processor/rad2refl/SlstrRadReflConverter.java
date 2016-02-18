package org.esa.s3tbx.processor.rad2refl;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.pointop.Sample;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 17.02.2016
 * Time: 13:15
 *
 * @author olafd
 */
public class SlstrRadReflConverter implements RadReflConverter {

    private String conversionMode;

    public SlstrRadReflConverter(String conversionMode) {
        this.conversionMode = conversionMode;
    }

    // TODO: 17.02.2016
    @Override
    public float[] convert(Product sourceProduct, Sample[] sourceSamples) {
        return new float[0];
    }

    public String getSpectralBandAutogroupingString() {
        return conversionMode.equals("RAD_TO_REFL") ? Sensor.SLSTR.getReflAutogroupingString() : Sensor.SLSTR.getRadAutogroupingString();
    }

}
