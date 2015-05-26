package org.esa.s3tbx.dataio.s3.meris;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.Product;

import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class MerisLevel2ProductReader extends Sentinel3ProductReader {

    public MerisLevel2ProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final String dirName = getInputFileParentDirectory().getName();
        if (dirName.matches("ENV_ME_2_RR(G|P)____.*______ACR_R_NT____.SEN3")) {
            setFactory(new MerisLevel2ProductFactory(this));
        }
        return createProduct();
    }
}
