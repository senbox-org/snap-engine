package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.util.S3MultiLevelOpImage;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataAttribute;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.List;

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
        List<Dimension> variableDimensions = variable.getDimensions();
        if (variableDimensions.size() == 1) {
            MetadataElement variableElement =
                    product.getMetadataRoot().getElement("Variable_Attributes").getElement(variable.getFullName());
            try {
                Object data = variable.read().copyTo1DJavaArray();
                ProductData variableData;
                if (data instanceof float[]) {
                    variableData = ProductData.createInstance((float[]) data);
                } else {
                    variableData = ProductData.createInstance((short[]) data);
                }
                final MetadataAttribute variableAttribute =
                        new MetadataAttribute("value", variableData, true);
                variableAttribute.setUnit(variable.getUnitsString());
                variableAttribute.setDescription(variable.getDescription());
                variableElement.addAttribute(variableAttribute);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
