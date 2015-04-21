package org.esa.s3tbx.dataio.s3.meris;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;

/**
 * @author Tonio Fincke
 */
public class MerisLevel1ProductFactory extends MerisProductFactory {

    private final static String validExpression = "!quality_flags.invalid";

    public MerisLevel1ProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        if (targetNode.getName().matches("M[0-1][0-9].*")) {
            if (targetNode instanceof Band) {
                final Band targetBand = (Band) targetNode;
                String name = targetBand.getName();
                targetBand.setSpectralBandIndex(getBandindex(name));
                targetBand.setSpectralWavelength(getWavelength(name));
                targetBand.setSpectralBandwidth(getBandwidth(name));
            }
        }
        targetNode.setValidPixelExpression(getValidExpression());
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("M*_radiance:atmospheric_temperature_profile:lambda0:FWHM:solar_flux");
    }

    @Override
    protected String getValidExpression() {
        return validExpression;
    }

}
