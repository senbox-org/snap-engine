package org.esa.beam.meris.radiometry;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Test;

public class MerisRadiometryCorrectionOpTest {


    @Test
    public void testWithFSGBandsAtBeginning() throws Exception {
        Product fsg = TestHelper.createL1bProduct("FSG");
        moveFSGBandsAtTheBeginning(fsg);
        MerisRadiometryCorrectionOp correctionOp = new MerisRadiometryCorrectionOp();
        correctionOp.setParameterDefaultValues();
        correctionOp.setSourceProduct(fsg);
        Product targetProduct = correctionOp.getTargetProduct();

        // trigger computation
        targetProduct.getBandAt(0).getGeophysicalImage().getData();

    }

    void moveFSGBandsAtTheBeginning(Product fsg) {
        Band corrLon = fsg.getBandGroup().get("corr_longitude");
        fsg.getBandGroup().remove(corrLon);
        Band corrLat = fsg.getBandGroup().get("corr_latitude");
        fsg.getBandGroup().remove(corrLat);
        fsg.getBandGroup().add(0, corrLat);
        fsg.getBandGroup().add(0, corrLon);
    }
}