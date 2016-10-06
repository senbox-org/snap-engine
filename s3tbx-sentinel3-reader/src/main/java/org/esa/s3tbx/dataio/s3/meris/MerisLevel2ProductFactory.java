package org.esa.s3tbx.dataio.s3.meris;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Tonio Fincke
 */
public class MerisLevel2ProductFactory extends MerisProductFactory {

    public MerisLevel2ProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected Band addBand(Band sourceBand, Product targetProduct) {
        final String sourceBandName = sourceBand.getName();
        if (sourceBandName.startsWith("IWV")) {
            if (sourceBand.getProduct().getName().startsWith("l")) {
                sourceBand.setName("L_" + sourceBandName);
            } else if (sourceBand.getProduct().getName().startsWith("w")) { // masterProduct.getName().startsWith("w")
                sourceBand.setName("W_" + sourceBandName);
            }
        }
        return super.addBand(sourceBand, targetProduct);
    }

    @Override
    protected void configureTargetNode(Band sourceBand, RasterDataNode targetNode) {
        if (targetNode.getName().matches("M[0-1][0-9].*")) {
            if (targetNode instanceof Band) {
                final Band targetBand = (Band) targetNode;
                String partialName = targetBand.getName().substring(0, 3);
                targetBand.setSpectralBandIndex(getBandindex(partialName));
                targetBand.setSpectralWavelength(getWavelength(partialName));
                targetBand.setSpectralBandwidth(getBandwidth(partialName));
            }
        }
        // convert log10 scaled variables int concentrations and also their error bands
        // the unit string follows the CF conventions.
        // See: http://www.unidata.ucar.edu/software/udunits/udunits-2.0.4/udunits2lib.html#Syntax
        if (targetNode.getName().startsWith("ADG443_NN") ||
            targetNode.getName().startsWith("CHL_NN") ||
            targetNode.getName().startsWith("CHL_OC4ME") ||
            targetNode.getName().startsWith("KD490_M07") ||
            targetNode.getName().startsWith("TSM_NN")) {
            if (targetNode instanceof Band) {
                final Band targetBand = (Band) targetNode;
                String unit = targetBand.getUnit();
                Pattern pattern = Pattern.compile("lg\\s*\\(\\s*re:?\\s*(.*)\\)");
                final Matcher m = pattern.matcher(unit);
                if (m.matches()) {
                    targetBand.setLog10Scaled(true);
                    targetBand.setUnit(m.group(1));
                    String description = targetBand.getDescription();
                    description = description.replace("log10 scaled ", "");
                    targetBand.setDescription(description);
                } else {
                    getLogger().log(Level.WARNING, "Unit extraction not working for band " + targetNode.getName());
                }

            }
        }
        targetNode.setValidPixelExpression(getValidExpression());
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        targetProduct.setAutoGrouping("M*_rho_toa:M*_rho_toa_err:M*_rho_top:M*_rho_top_err:M*_rho_w:M*_rho_w_err:" +
                                      "atmospheric_temperature_profile:lambda0:FWHM:solar_flux");
    }

}
