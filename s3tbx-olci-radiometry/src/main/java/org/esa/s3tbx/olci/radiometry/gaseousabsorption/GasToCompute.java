package org.esa.s3tbx.olci.radiometry.gaseousabsorption;

/**
 * @author muhammad.bc.
 */
public enum GasToCompute {
    Oa01_radiance(new String[]{"NO2"}),
    Oa02_radiance(new String[]{"NO2"}),
    Oa03_radiance(new String[]{"NO2", "H2O", "O3"}),
    Oa04_radiance(new String[]{"O3"}),
    Oa05_radiance(new String[]{"H2O", "O3"}),
    Oa06_radiance(new String[]{"O3"}),
    Oa07_radiance(new String[]{"O3"}),
    Oa08_radiance(new String[]{"H2O", "O3"}),
    Oa09_radiance(new String[]{"O3"}),
    Oa10_radiance(new String[]{"H2O", "O3"}),
    Oa11_radiance(new String[]{"H2O", "O3"}),
    Oa12_radiance(new String[]{"O3"}),
    Oa13_radiance(new String[]{"O2", "O3"}),
    Oa14_radiance(new String[]{"O2", "O3"}),
    Oa15_radiance(new String[]{"O2", "O3"}),
    Oa16_radiance(new String[]{"O2", "O3", "H2O"}),
    Oa17_radiance(new String[]{"H2O"}),
    Oa18_radiance(new String[]{"H2O"}),
    Oa19_radiance(new String[]{"H2O"}),
    Oa20_radiance(new String[]{"H2O"}),
    Oa21_radiance(new String[]{"H2O"});

    private final String[] gasToCompute;

    GasToCompute(String[] gasToCompute) {
        this.gasToCompute = gasToCompute;
    }

    public String[] getGasBandToCompute() {
        return gasToCompute;
    }

}
