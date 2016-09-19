package org.esa.s3tbx.olci.radiometry.gasabsorption;

/**
 * @author muhammad.bc.
 */
public enum GasToCompute {
    gaseous_absorp_01(new String[]{"NO2"}),
    gaseous_absorp_02(new String[]{"NO2"}),
    gaseous_absorp_03(new String[]{"NO2", "H2O", "O3"}),
    gaseous_absorp_04(new String[]{"O3"}),
    gaseous_absorp_05(new String[]{"H2O", "O3"}),
    gaseous_absorp_06(new String[]{"O3"}),
    gaseous_absorp_07(new String[]{"O3"}),
    gaseous_absorp_08(new String[]{"H2O", "O3"}),
    gaseous_absorp_09(new String[]{"O3"}),
    gaseous_absorp_10(new String[]{"H2O", "O3"}),
    gaseous_absorp_11(new String[]{"H2O", "O3"}),
    gaseous_absorp_12(new String[]{"O3"}),
    gaseous_absorp_13(new String[]{"O2", "O3"}),
    gaseous_absorp_14(new String[]{"O2", "O3"}),
    gaseous_absorp_15(new String[]{"O2", "O3"}),
    gaseous_absorp_16(new String[]{"O2", "O3", "H2O"}),
    gaseous_absorp_17(new String[]{"H2O"}),
    gaseous_absorp_18(new String[]{"H2O"}),
    gaseous_absorp_19(new String[]{"H2O"}),
    gaseous_absorp_20(new String[]{"H2O"}),
    gaseous_absorp_21(new String[]{"H2O"});

    private final String[] gasToCompute;

    GasToCompute(String[] gasToCompute) {
        this.gasToCompute = gasToCompute;
    }

    public String[] getGasBandToCompute() {
        return gasToCompute;
    }

}
