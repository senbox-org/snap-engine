package org.esa.s3tbx.dataio.s3.util;

import org.esa.s3tbx.dataio.s3.olci.OlciInstrumentDataReader;

import java.io.File;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class S3NetcdfReaderFactory {

    public static S3NetcdfReader createS3NetcdfReader(File file) throws IOException {
        final String fileName = file.getName();
        if(fileName.equals("tie_meteo.nc")) {
            return new TieMeteoReader(file.getAbsolutePath());
        } else if (fileName.equals("instrument_data.nc")) {
            return new InstrumentDataReader(file.getAbsolutePath());
        } else {
            return new S3NetcdfReader(file.getAbsolutePath());
        }
    }

}
