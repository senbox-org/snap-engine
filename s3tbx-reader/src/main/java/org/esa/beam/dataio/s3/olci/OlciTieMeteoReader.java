package org.esa.beam.dataio.s3.olci;

import org.esa.beam.dataio.s3.util.S3NetcdfReader;
import org.esa.beam.dataio.s3.util.S3VariableOpImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.List;

/**
 * @author Tonio Fincke
 */
class OlciTieMeteoReader extends S3NetcdfReader {

    public OlciTieMeteoReader(String pathToFile) throws IOException {
        super(pathToFile);
    }

    @Override
    protected String[] getSeparatingThirdDimensions() {
        return new String[]{"wind_vectors", "tie_pressure_levels"};
    }

    @Override
    protected String[] getSuffixesForSeparatingThirdDimensions() {
        return new String[]{"vector", "pressure_level"};
    }

    @Override
    protected String getNameOfRowDimension() {
        return "tie_rows";
    }

    @Override
    protected String getNameOfColumnDimension() {
        return "tie_columns";
    }

    @Override
    protected RenderedImage createImage(Variable variable, int bufferType, int sourceWidth, int sourceHeight,
                                        java.awt.Dimension tileSize, String dimensionName, int dimensionIndex) {
        return new OlciVariableOpImage(variable, bufferType, sourceWidth, sourceHeight, tileSize,
                                     ResolutionLevel.MAXRES, dimensionName, dimensionIndex);
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
