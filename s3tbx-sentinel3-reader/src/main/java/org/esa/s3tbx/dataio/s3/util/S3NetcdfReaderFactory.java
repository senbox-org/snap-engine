package org.esa.s3tbx.dataio.s3.util;

import java.io.File;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class S3NetcdfReaderFactory {

    public static S3NetcdfReader createS3NetcdfProduct(File file) throws IOException {
        final String fileName = file.getName();
        if (fileName.equals("tie_meteo.nc")) {
            return new TieMeteoReader();
        } else if (fileName.equals("instrument_data.nc")) {
            return new InstrumentDataReader();
        } else {
            return new S3NetcdfReader();
        }
    }

}
