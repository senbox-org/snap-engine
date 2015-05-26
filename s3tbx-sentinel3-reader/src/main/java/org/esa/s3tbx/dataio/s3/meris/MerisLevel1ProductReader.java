package org.esa.s3tbx.dataio.s3.meris;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.framework.datamodel.Product;

import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class MerisLevel1ProductReader extends Sentinel3ProductReader {

    public MerisLevel1ProductReader(MerisLevel1ProductPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final String dirName = getInputFileParentDirectory().getName();
        if (dirName.matches("ENV_ME_1_RR(G|P)____.*______ACR_R_NT____.SEN3")) {
            setFactory(new MerisLevel1ProductFactory(this));
        }
        return createProduct();
    }

}
