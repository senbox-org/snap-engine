package org.esa.s3tbx.dataio.s3.aatsr;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.RGBImageProfileManager;

/**
 * @author Sabine Embacher
 */
public class AatsrLevel1ProductReaderPlugIn extends Sentinel3ProductReaderPlugIn {

    public final static String DIRECTORY_NAME_PATTERN = "ENV_AT_1_RBT____" +
                                                        "[12]\\d{3}[01]\\d[0123]\\dT[012]\\d[012345]\\d[012345]\\d_" +
                                                        "[12]\\d{3}[01]\\d[0123]\\dT[012]\\d[012345]\\d[012345]\\d_" +
                                                        "[12]\\d{3}[01]\\d[0123]\\dT[012]\\d[012345]\\d[012345]\\d_" +
                                                        "\\d{4}_\\d{3}_\\d{3}______(DSI|TLS)_R_NT____\\.SEN3";

    private static final String format_name = "ATS_L1_S3";

    static {
        registerRGBProfiles();
    }

    public AatsrLevel1ProductReaderPlugIn() {
        super(format_name, "(A)ATSR Level 1 in Sentinel-3 product format",
              DIRECTORY_NAME_PATTERN, "xfdumanifest", "L1b_Manifest", ".xml");
    }

    @Override
    public ProductReader createReaderInstance() {
        return new AatsrLevel1ProductReader(this);
    }

    private static void registerRGBProfiles() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        // todo ideas+
//        manager.addProfile(new RGBImageProfile("MERIS4 L1b - Tristimulus",
//                                               new String[]{
//                                                       "log(1.0 + 0.35 * M02_radiance + 0.60 * M05_radiance + M06_radiance + 0.13 * M07_radiance)",
//                                                       "log(1.0 + 0.21 * M03_radiance + 0.50 * M04_radiance + M05_radiance + 0.38 * M06_radiance)",
//                                                       "log(1.0 + 0.21 * M01_radiance + 1.75 * M02_radiance + 0.47 * M03_radiance + 0.16 * M04_radiance)"
//                                               },
//                                               new String[]{
//                                                       "*ME_1*",
//                                                       "ENV_ME_1_*",
//                                                       "",
//                                               }
//        ));
//        manager.addProfile(new RGBImageProfile("MERIS4 L1b - 13,5,1",
//                                               new String[]{
//                                                       "M13_radiance",
//                                                       "M05_radiance",
//                                                       "M01_radiance"
//                                               }
//        ));
//        manager.addProfile(new RGBImageProfile("MERIS4 L1b - 13,4,2",
//                                               new String[]{
//                                                       "M13_radiance",
//                                                       "M04_radiance",
//                                                       "M02_radiance"
//                                               }
//        ));
    }
}
