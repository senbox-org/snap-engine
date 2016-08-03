package org.esa.s3tbx.dataio.s3.util;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * @author Tonio Fincke
 */
class TieMeteoReader extends S3NetcdfReader {

    @Override
    protected String[] getSeparatingDimensions() {
        return new String[]{"wind_vectors", "tie_pressure_levels"};
    }

    @Override
    public String[] getSuffixesForSeparatingDimensions() {
        return new String[]{"vector", "pressure_level"};
    }

    @Override
    protected void addVariableMetadata(Variable variable, Product product) {
        super.addVariableMetadata(variable, product);
        if (variable.getFullName().equals("atmospheric_temperature_profile")) {
            final MetadataElement atmosphericTemperatureProfileElement =
                    product.getMetadataRoot().getElement("Variable_Attributes").getElement("atmospheric_temperature_profile");
            try {
                final Variable referencePressureLevelVariable =
                        getNetcdfFile().findVariable("reference_pressure_level");
                final ProductData referencePressureLevelData =
                        ProductData.createInstance((float[]) referencePressureLevelVariable.read().copyTo1DJavaArray());
                final MetadataAttribute referencePressureLevelAttribute =
                        new MetadataAttribute("reference_pressure_level", referencePressureLevelData, true);
                referencePressureLevelAttribute.setUnit(referencePressureLevelVariable.getUnitsString());
                referencePressureLevelAttribute.setDescription(referencePressureLevelVariable.getDescription());
                atmosphericTemperatureProfileElement.addAttribute(referencePressureLevelAttribute);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
