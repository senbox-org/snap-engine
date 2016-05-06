package org.esa.s3tbx.olci.radiometry.smilecorr;

/**
 * @author muhammad.bc.
 */
public enum BandToCompute {
    Oa01_radiance(new String[]{"H2O", "NO3","O3"}),
    Oa02_radiance(new String[]{"", " "}),
    Oa03_radiance(new String[]{"", " "}),
    Oa04_radiance(new String[]{"", " "}),
    Oa05_radiance(new String[]{"", " "}),
    Oa06_radiance(new String[]{"", " "}),
    Oa07_radiance(new String[]{"", " "}),
    Oa08_radiance(new String[]{"", " "}),
    Oa09_radiance(new String[]{"", " "}),
    Oa10_radiance(new String[]{"", " "}),
    Oa11_radiance(new String[]{"", " "}),
    Oa12_radiance(new String[]{"", " "}),
    Oa13_radiance(new String[]{"", " "}),
    Oa14_radiance(new String[]{"", " "}),
    Oa15_radiance(new String[]{"", " "}),
    Oa16_radiance(new String[]{"", " "}),
    Oa17_radiance(new String[]{"", " "}),
    Oa18_radiance(new String[]{"", " "}),
    Oa19_radiance(new String[]{"", " "}),
    Oa20_radiance(new String[]{"", " "}),
    Oa21_radiance(new String[]{"", " "});

    private final String[] gasToCompute;

    BandToCompute(String[] gasToCompute) {
        this.gasToCompute = gasToCompute;
    }

    public String[] getGasBandToCompute() {
        return gasToCompute;
    }
}
