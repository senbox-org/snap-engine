package org.esa.beam.dataio.s3.olci;

import org.esa.beam.dataio.s3.util.S3NetcdfReader;
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
    protected void addBands(Product product) {
        final NetcdfFile netcdfFile = getNetcdfFile();
        final List<Variable> variables = netcdfFile.getVariables();
        for (final Variable variable : variables) {
            final String variableName = variable.getFullName();
            if (variable.findDimensionIndex("tie_rows") != -1 && variable.findDimensionIndex("tie_columns") != -1) {
                if (variable.findDimensionIndex("wind_vectors") != -1) {
                    final Dimension Dimension = variable.getDimension(variable.findDimensionIndex("wind_vectors"));
                    for (int i = 0; i < Dimension.getLength(); i++) {
                        createBand(product, variable, variableName + "_vector" + (i + 1));
                    }
                } else if (variable.findDimensionIndex("tie_pressure_levels") != -1) {
                    final Dimension Dimension = variable.getDimension(variable.findDimensionIndex("tie_pressure_levels"));
                    for (int i = 0; i < Dimension.getLength(); i++) {
                        createBand(product, variable, variableName + "_pressure_level" + i);
                    }
                } else {
                    createBand(product, variable, variableName);
                }
            }
            addVariableMetadata(variable, product);
        }

    }

    protected RenderedImage createSourceImage(Band band) {
        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
        final int sourceWidth = band.getSceneRasterWidth();
        final int sourceHeight = band.getSceneRasterHeight();
        final java.awt.Dimension tileSize = band.getProduct().getPreferredTileSize();
        final String bandName = band.getName();
        String variableName = bandName;
        Variable variable;
        int dimensionIndex = -1;
        String dimensionName = "";
        if (bandName.contains("_vector")) {
            variableName = bandName.substring(0, variableName.indexOf("_vector"));
            variable = getNetcdfFile().findVariable(variableName);
            dimensionName = "wind_vectors";
            dimensionIndex = Integer.parseInt(bandName.substring(bandName.length() - 1)) - 1;
        } else if (bandName.contains("_pressure_level")) {
            variableName = bandName.substring(0, variableName.indexOf("_pressure_level"));
            variable = getNetcdfFile().findVariable(variableName);
            dimensionName = "tie_pressure_levels";
            dimensionIndex = Integer.parseInt(bandName.substring(bandName.length() - 1)) - 1;
        } else {
            variable = getNetcdfFile().findVariable(variableName);
        }
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

    @Override
    protected int getWidth() {
        final Dimension widthDimension = getNetcdfFile().findDimension("tie_columns");
        if (widthDimension != null) {
            return widthDimension.getLength();
        }
        return 0;
    }

    @Override
    protected int getHeight() {
        final Dimension heightDimension = getNetcdfFile().findDimension("tie_rows");
        if (heightDimension != null) {
            return heightDimension.getLength();
        }
        return 0;
    }

}
