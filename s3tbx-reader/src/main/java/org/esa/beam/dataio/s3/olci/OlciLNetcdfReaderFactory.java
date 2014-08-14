package org.esa.beam.dataio.s3.olci;

import org.esa.beam.dataio.s3.util.S3NetcdfReader;

import java.io.File;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class OlciLNetcdfReaderFactory {

    static S3NetcdfReader createOlciNetcdfReader(File file) throws IOException {
        final String fileName = file.getName();
        if(fileName.startsWith("tie_") && fileName.endsWith(".nc")) {
            return new OlciTieMeteoReader(file.getAbsolutePath());
        } else {
            return new S3NetcdfReader(file.getAbsolutePath());
        }
    }

}
