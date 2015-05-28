package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.util.S3MultiLevelOpImage;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
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
class MetTxReader extends S3NetcdfReader {

    MetTxReader(String pathToFile) throws IOException {
        super(pathToFile);
    }

    @Override
    protected String[] getSeparatingDimensions() {
//        return new String[]{"n_bound", "t_single", "t_series", "p_atmos"};
        return new String[]{"n_bound", "t_series", "p_atmos"};
    }

    @Override
    protected String[] getSuffixesForSeparatingDimensions() {
//        return new String[]{"bound", "time_single", "time_series", "pressure_level"};
        return new String[]{"bound", "time_series", "pressure_level"};
    }

    @Override
    protected void addVariableAsBand(Product product, Variable variable, String variableName, boolean synthetic) {
        String[] suffixesForSeparatingDimensions = getSuffixesForSeparatingDimensions();
        for (String suffixForSeparatingDimension : suffixesForSeparatingDimensions) {
            if (variableName.contains(suffixForSeparatingDimension)) {
                variableName = variableName + "_tx";
                break;
            }
        }
        super.addVariableAsBand(product, variable, variableName, synthetic);
    }

    @Override
    protected int getDimensionIndexFromBandName(String bandName) {
        final int end = bandName.lastIndexOf("_");
        final int start = bandName.substring(0, end).lastIndexOf("_");
        return Integer.parseInt(bandName.substring(start + 1, end)) - 1;
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
