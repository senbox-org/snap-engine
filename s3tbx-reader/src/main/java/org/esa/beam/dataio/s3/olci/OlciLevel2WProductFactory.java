package org.esa.beam.dataio.s3.olci;

import org.esa.beam.dataio.s3.Sentinel3ProductReader;
import org.esa.beam.framework.datamodel.Product;

/**
 * @author Tonio Fincke
 */
public class OlciLevel2WProductFactory extends OlciProductFactory {

    public OlciLevel2WProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected String getValidExpression() {
        return "!WQSF_lsb_INVALID";
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("Oa*_reflectance:Oa*_reflectance_err:A865:ADG:CHL:IWV:KD490:PAR:T865:TSM");
    }

}
