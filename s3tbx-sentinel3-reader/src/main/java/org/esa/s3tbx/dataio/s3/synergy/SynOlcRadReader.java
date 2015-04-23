package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;

import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class SynOlcRadReader extends S3NetcdfReader {

    public SynOlcRadReader(String pathToFile) throws IOException {
        super(pathToFile);
    }

    @Override
    protected String[][] getRowColumnNamePairs() {
        return new String[][]{{"N_LINE_OLC", "N_DET_CAM"}, {"N_SCAN_SLST_NAD_1km_L1C", "N_PIX_SLST_NAD_1km_L1C"},
                {"N_SCAN_SLST_NAD_05km_L1C", "N_PIX_SLST_NAD_05km_L1C"}, {"OLC_MISREG_ALT_DIM", "N_DET_CAM"}};
    }

    @Override
    protected String[] getSeparatingThirdDimensions() {
        return new String[]{"N_CAM"};
    }

    @Override
    protected String[] getSuffixesForSeparatingThirdDimensions() {
        return new String[]{"CAM"};
    }
}
