package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.util.MetTxReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;

import java.io.File;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class SlstrNetcdfReaderFactory {

    static S3NetcdfReader createSlstrNetcdfReader(File file, Manifest manifest) throws IOException {
        String productType = manifest.getProductType();
        final String fileName = file.getName();
        if(fileName.equals("FRP_in.nc")) {
            return new SlstrFRPReader();
        } else if(productType.startsWith("SL_2_WST")) {
            return new SlstrL2WSTL2PReader();
        } else if(fileName.equals("LST_ancillary_ds.nc")) {
            return new SlstrLSTAncillaryDsReader();
        } else if(fileName.equals("met_tx.nc")) {
            return new MetTxReader();
        } else {
            return new S3NetcdfReader();
        }
    }

}
