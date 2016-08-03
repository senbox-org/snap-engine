package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;

/**
 * @author Tonio Fincke
 */
class SlstrL2WSTL2PReader extends S3NetcdfReader {

    @Override
    protected String[] getSeparatingDimensions() {
        return new String[]{"channel"};
    }

    @Override
    public String[] getSuffixesForSeparatingDimensions() {
        return new String[]{"channel"};
    }

    protected String[][] getRowColumnNamePairs() {
        return new String[][]{{"nj", "ni"}};
    }
}
