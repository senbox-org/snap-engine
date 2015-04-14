package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;

import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class SlstrL2WSTL2PReader extends S3NetcdfReader {

    public SlstrL2WSTL2PReader(String pathToFile) throws IOException {
        super(pathToFile);
    }

    @Override
    protected String[] getSeparatingThirdDimensions() {
        return new String[]{"channel"};
    }

    @Override
    protected String[] getSuffixesForSeparatingThirdDimensions() {
        return new String[]{"channel"};
    }
}
