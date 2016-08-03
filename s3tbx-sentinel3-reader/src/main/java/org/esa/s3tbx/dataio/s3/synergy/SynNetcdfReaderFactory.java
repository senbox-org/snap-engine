package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;

import java.io.File;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class SynNetcdfReaderFactory {

    public static S3NetcdfReader createSynNetcdfReader(File file) throws IOException {
        final String fileName = file.getName();
        if(fileName.startsWith("OLC_RADIANCE_") || fileName.startsWith("MISREGIST") ||
                fileName.startsWith("SLST_NAD") || fileName.equals("GEOLOCATION_REF.nc")) {
            return new SynOlcRadReader();
        } else {
            return new S3NetcdfReader();
        }
    }

}
