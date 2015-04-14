package org.esa.s3tbx.dataio.s3.olci;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.framework.datamodel.Product;

/**
 * @author Tonio Fincke
 */
public class OlciLevel2WProductFactory extends OlciProductFactory {

    public OlciLevel2WProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected String getValidExpression() {
        return "!WQSF_lsb.INVALID";
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("Oa*_reflectance:Oa*_reflectance_err:A865:ADG:CHL:IWV:KD490:PAR:T865:TSM:" +
                                              "atmospheric_temperature_profile:lambda0:FWHM:solar_flux");
    }

}
