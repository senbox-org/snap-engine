package org.esa.s3tbx.olci.radiometry.operator;

import org.esa.s3tbx.olci.radiometry.smilecorr.BandToCompute;

/**
 * @author muhammad.bc.
 */
public class GaseousAbsorptionOp {

    public String[] bandToCompute(String bandName){
        BandToCompute bandToCompute = BandToCompute.valueOf(bandName);
        String[] gasBandToCompute = bandToCompute.getGasBandToCompute();
        return null;
    }
    public double calExponential(double h2O) {
        return calExponential(h2O, 1, 1);
    }

    public double calExponential(double h2O, double nO2) {
        return calExponential(h2O, nO2, 1);
    }

    public double calExponential(double h2O, double nO2, double o3) {
        final double calValue = h2O * nO2 * o3;
        return Math.exp(calValue);
    }

    public double ozone_O3() {
        return 0;
    }

    public double h_2O() {
        return 0;
    }

    public double n_02() {
        return 0;
    }
}
