package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;

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
        } else if(fileName.equals("L2P.nc")) {
            return new SlstrL2WSTL2PReader(file.getAbsolutePath());
        } else if(fileName.equals("LST_ancillary_ds.nc")) {
            return new SlstrLSTAncillaryDsReader(file.getAbsolutePath());
        } else if(fileName.equals("met_tx.nc")) {
            return new MetTxReader(file.getAbsolutePath());
        } else {
            return new S3NetcdfReader(file.getAbsolutePath());
        }
    }

}
