package org.esa.beam.dataio.s3.slstr;

import org.esa.beam.dataio.s3.util.S3NetcdfReader;

import java.io.File;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class SlstrNetcdfReaderFactory {

    static S3NetcdfReader createSlstrNetcdfReader(File file) throws IOException {
        final String fileName = file.getName();
        if(fileName.equals("FRP_in.nc")) {
            return new SlstrFRPReader(file.getAbsolutePath());
        } else if(fileName.equals("LST_ancillary_ds.nc")) {
            return new SlstrLSTAncillaryDsReader(file.getAbsolutePath());
        } else {
            return new S3NetcdfReader(file.getAbsolutePath());
        }
    }

}
