package org.esa.s3tbx.dataio.s3.meris;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReaderPlugIn;
import org.esa.snap.framework.dataio.ProductReader;

/**
 * @author Tonio Fincke
 */
public class MerisLevel2ProductPlugIn extends Sentinel3ProductReaderPlugIn {

    private static final String format_name = "MER_L2_S3";

    public MerisLevel2ProductPlugIn() {
        super(format_name, "MERIS Level 2 in Sentinel-3 product format",
              "ENV_ME_2_RR(G|P)____.*______ACR_R_NT____.SEN3", "xfdumanifest", "L1c_Manifest", ".xml");
    }

    @Override
    public ProductReader createReaderInstance() {
        return new MerisLevel2ProductReader(this);
    }
}
