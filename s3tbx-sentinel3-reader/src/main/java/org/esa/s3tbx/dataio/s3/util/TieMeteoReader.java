package org.esa.s3tbx.dataio.s3.util;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import ucar.nc2.Variable;

import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class TieMeteoReader extends S3NetcdfReader {

    public TieMeteoReader(String pathToFile) throws IOException {
        super(pathToFile);
    }

    @Override
    protected String[] getSeparatingDimensions() {
//        return new String[]{"wind_vectors"};
        //todo use this later - currently it slows the reader down during product opening
        return new String[]{"wind_vectors", "tie_pressure_levels"};
    }

    @Override
    protected String[] getSuffixesForSeparatingDimensions() {
//        return new String[]{"vector"};
        //todo use this later - currently it slows the reader down during product opening
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
