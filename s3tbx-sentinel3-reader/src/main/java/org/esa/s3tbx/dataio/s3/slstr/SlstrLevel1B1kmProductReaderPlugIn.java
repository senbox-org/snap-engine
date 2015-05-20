package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReaderPlugIn;

/**
 * @author Tonio Fincke
 */
public class SlstrLevel1B1kmProductReaderPlugIn extends Sentinel3ProductReaderPlugIn {

    private static final String format_name = "S3 SLSTR L1B (1km)";

    public SlstrLevel1B1kmProductReaderPlugIn() {
        super(format_name, "Sentinel-3 SLSTR L1B products in 1km resolution",
              "(S3.?_SL_1_RBT_.*(.SEN3)?)", "xfdumanifest", "L1c_Manifest", ".xml");
    }

}
