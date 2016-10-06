package org.esa.s3tbx.dataio.s3.meris;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;

/**
 * @author Tonio Fincke
 */
public class MerisLevel1ProductPlugIn extends Sentinel3ProductReaderPlugIn {

    private static final String format_name = "MER_L1_S3";
    static {
        registerRGBProfiles();
    }

    public MerisLevel1ProductPlugIn() {
        super(format_name, "MERIS Level 1 in Sentinel-3 product format",
              "ENV_ME_1_(F|R)R(G|P).*.SEN3", "xfdumanifest", "L1c_Manifest", ".xml");
    }

    @Override
    public ProductReader createReaderInstance() {
        return new MerisLevel1ProductReader(this);
    }

    private static void registerRGBProfiles() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("MERIS4 L1b - Tristimulus",
                                               new String[]{
                                                       "log(1.0 + 0.35 * M02_radiance + 0.60 * M05_radiance + M06_radiance + 0.13 * M07_radiance)",
                                                       "log(1.0 + 0.21 * M03_radiance + 0.50 * M04_radiance + M05_radiance + 0.38 * M06_radiance)",
                                                       "log(1.0 + 0.21 * M01_radiance + 1.75 * M02_radiance + 0.47 * M03_radiance + 0.16 * M04_radiance)"
                                               },
                                               new String[]{
                                                       "*ME_1*",
                                                       "ENV_ME_1_*",
                                                       "",
                                               }
        ));
        manager.addProfile(new RGBImageProfile("MERIS4 L1b - 13,5,1",
                                               new String[]{
                                                       "M13_radiance",
                                                       "M05_radiance",
                                                       "M01_radiance"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("MERIS4 L1b - 13,4,2",
                                               new String[]{
                                                       "M13_radiance",
                                                       "M04_radiance",
                                                       "M02_radiance"
                                               }
        ));
    }
}
