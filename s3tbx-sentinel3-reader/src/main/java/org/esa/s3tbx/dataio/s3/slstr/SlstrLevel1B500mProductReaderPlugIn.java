package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReaderPlugIn;

/**
 * @author Tonio Fincke
 */
public class SlstrLevel1B500mProductReaderPlugIn extends Sentinel3ProductReaderPlugIn {

    private static final String format_name = "Sen3_SLSTRL1B_500m";

    public SlstrLevel1B500mProductReaderPlugIn() {
        super(format_name, "Sentinel-3 SLSTR L1B products in 500 m resolution",
              "(S3.?_SL_1_RBT_.*(.SEN3)?)", "xfdumanifest", "L1c_Manifest", ".xml");
    }

}
