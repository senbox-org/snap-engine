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
//        if(fileName.equals("tie_meteo.nc")) {
//            return new TieMeteoReader(file.getAbsolutePath());
//        } else if (fileName.equals("instrument_data.nc")) {
//            return new InstrumentDataReader(file.getAbsolutePath());
//        } else {
            return new S3NetcdfReader(file.getAbsolutePath());
//        }
    }

}
