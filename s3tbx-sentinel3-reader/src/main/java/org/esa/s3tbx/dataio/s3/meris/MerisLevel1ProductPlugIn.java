package org.esa.s3tbx.dataio.s3.meris;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReaderPlugIn;
import org.esa.snap.framework.dataio.ProductReader;

/**
 * @author Tonio Fincke
 */
public class MerisLevel1ProductPlugIn extends Sentinel3ProductReaderPlugIn {

    private static final String format_name = "MER_L1_S3";

    public MerisLevel1ProductPlugIn() {
        super(format_name, "MERIS Level 1 in Sentinel-3 product format",
              "ENV_ME_1_(F|R)R(G|P).*NT____.SEN3", "xfdumanifest", "L1c_Manifest", ".xml");
    }

    @Override
    public ProductReader createReaderInstance() {
        return new MerisLevel1ProductReader(this);
    }

}
