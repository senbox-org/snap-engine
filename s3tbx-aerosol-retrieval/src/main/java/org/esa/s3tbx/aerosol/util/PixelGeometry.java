/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol.util;

/**
 *
 * @author akheckel
 */
public class PixelGeometry {
    public final float sza;
    public final float vza;
    public final float razi;

    public PixelGeometry(double sza, double saa, double vza, double vaa) {
        this.sza = (float) sza;
        this.vza = (float) vza;
        this.razi = getRelativeAzi((float)saa, (float)vaa);
    }

    private float getRelativeAzi(float saa, float vaa) {
        float relAzi = Math.abs(saa - vaa);
        relAzi = (relAzi > 180.0f) ? 180 - (360 - relAzi) : 180 - relAzi;
        return relAzi;
    }

}
