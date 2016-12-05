package org.esa.s3tbx.processor.flh_mci.visat;

enum Presets {
    NONE("None", "", "", "", "", "", ""),
    MERIS_L1B_MCI("MERIS L1b MCI", "radiance_8", "radiance_10", "radiance_9", "MCI", "MCI_slope",
                  "NOT l1_flags.LAND_OCEAN AND NOT l1_flags.BRIGHT AND NOT l1_flags.INVALID"),
    MERIS_L2_FLH("MERIS L2 FLH", "reflec_7", "reflec_9", "reflec_8", "FLH", "FLH_slope",
                 "l2_flags.WATER"),
    MERIS_L2_MCI("MERIS L2 MCI", "reflec_8", "reflec_10", "reflec_9", "MCI", "MCI_slope",
                 "l2_flags.WATER"),
    OLCI_L1B_MCI("OLCI L1b MCI", "Oa10_radiance", "Oa12_radiance", "Oa11_radiance", "MCI", "MCI_slope",
                  "(!quality_flags.land || quality_flags.fresh_inland_water) && ! quality_flags.invalid"),
    OLCI_L2_FLH("OLCI L2 FLH", "Oa08_reflectance", "Oa11_reflectance", "Oa10_reflectance", "FLH", "FLH_slope",
                 "WQSF_lsb.WATER || WQSF_lsb.INLAND_WATER"),
    OLCI_L2_MCI("OLCI L2 MCI", "Oa10_reflectance", "Oa12_reflectance", "Oa11_reflectance", "MCI", "MCI_slope",
                 "WQSF_lsb.WATER || WQSF_lsb.INLAND_WATER");

    private final String label;
    private final String lowerBaselineBandName;
    private final String upperBaselineBandName;
    private final String signalBandName;
    private final String lineHeightBandName;
    private final String slopeBandName;
    private final String maskExpression;

    private Presets(String label, String upperBaselineBandName, String lowerBaselineBandName,
                    String signalBandName, String lineHeightBandName, String slopeBandName, String maskExpression) {
        this.label = label;
        this.upperBaselineBandName = upperBaselineBandName;
        this.lowerBaselineBandName = lowerBaselineBandName;
        this.signalBandName = signalBandName;
        this.lineHeightBandName = lineHeightBandName;
        this.slopeBandName = slopeBandName;
        this.maskExpression = maskExpression;
    }

    @Override
    public String toString() {
        return label;
    }

    String getLowerBaselineBandName() {
        return lowerBaselineBandName;
    }

    String getUpperBaselineBandName() {
        return upperBaselineBandName;
    }

    String getSignalBandName() {
        return signalBandName;
    }

    String getLineHeightBandName() {
        return lineHeightBandName;
    }

    String getSlopeBandName() {
        return slopeBandName;
    }

    String getMaskExpression() {
        return maskExpression;
    }
}
