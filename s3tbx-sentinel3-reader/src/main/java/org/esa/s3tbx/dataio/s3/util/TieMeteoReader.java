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

    private final boolean isFullResolutionProduct;

    public TieMeteoReader(String pathToFile, boolean isFullResolutionProduct) throws IOException {
        super(pathToFile);
        this.isFullResolutionProduct = isFullResolutionProduct;
    }

    @Override
    protected String[] getSeparatingDimensions() {
        if (isFullResolutionProduct) {
            return new String[]{"wind_vectors"};
        }
        return new String[]{"wind_vectors", "tie_pressure_levels"};
    }

    @Override
    protected String[] getSuffixesForSeparatingDimensions() {
        if (isFullResolutionProduct) {
            return new String[]{"vector"};
        }
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
