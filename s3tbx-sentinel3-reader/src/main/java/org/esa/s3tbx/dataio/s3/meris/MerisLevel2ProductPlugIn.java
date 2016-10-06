package org.esa.s3tbx.dataio.s3.meris;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;

/**
 * @author Tonio Fincke
 */
public class MerisLevel2ProductPlugIn extends Sentinel3ProductReaderPlugIn {

    private static final String format_name = "MER_L2_S3";

    static {
        registerRGBProfiles();
    }

    public MerisLevel2ProductPlugIn() {
        super(format_name, "MERIS Level 2 in Sentinel-3 product format",
              "ENV_ME_2_(F|R)R(G|P).*.SEN3", "xfdumanifest", "L1c_Manifest", ".xml");
    }

    @Override
    public ProductReader createReaderInstance() {
        return new MerisLevel2ProductReader(this);
    }

    private static void registerRGBProfiles() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("MERIS L2 - Tristimulus",
                                               new String[]{
                                                       "log(0.05 + 0.35 * M02_rho_TOA + 0.60 * M05_rho_TOA + M06_rho_TOA + 0.13 * M07_rho_TOA)",
                                                       "log(0.05 + 0.21 * M03_rho_TOA + 0.50 * M04_rho_TOA + M05_rho_TOA + 0.38 * M06_rho_TOA)",
                                                       "log(0.05 + 0.21 * M01_rho_TOA + 1.75 * M02_rho_TOA + 0.47 * M03_rho_TOA + 0.16 * M04_rho_TOA)"
                                               },
                                               new String[]{
                                                       "*ME_2_*",
                                                       "ENV_ME_2_*",
                                                       "",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("MERIS4 L2 - 13,5,1",
                                               new String[]{
                                                       "M13_rho_TOA",
                                                       "M05_rho_TOA",
                                                       "M01_rho_TOA"
                                               }
        ));
    }
}
