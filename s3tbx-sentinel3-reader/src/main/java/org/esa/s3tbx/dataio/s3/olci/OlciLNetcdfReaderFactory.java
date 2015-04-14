package org.esa.s3tbx.dataio.s3.olci;

import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;

import java.io.File;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class OlciLNetcdfReaderFactory {

    static S3NetcdfReader createOlciNetcdfReader(File file) throws IOException {
        final String fileName = file.getName();
        if(fileName.equals("tie_meteo.nc")) {
            return new OlciTieMeteoReader(file.getAbsolutePath());
        } else if (fileName.equals("instrument_data.nc")) {
            return new OlciInstrumentDataReader(file.getAbsolutePath());
        } else {
            return new S3NetcdfReader(file.getAbsolutePath());
        }
    }

}
