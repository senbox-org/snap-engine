package org.esa.beam.dataio.s3.olci;

import org.esa.beam.dataio.s3.util.S3MultiLevelOpImage;
import org.esa.beam.dataio.s3.util.S3NetcdfReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.List;

/**
 * @author Tonio Fincke
 */
public class OlciInstrumentDataReader extends S3NetcdfReader {

    private final static String detector_index_name = "detector_index";
    private Variable detectorIndexVariable;

    public OlciInstrumentDataReader(String pathToFile) throws IOException {
        super(pathToFile);
    }

    @Override
    protected void addBands(Product product) {
        final NetcdfFile netcdfFile = getNetcdfFile();
        detectorIndexVariable = netcdfFile.findVariable(detector_index_name);
        addVariableAsBand(product, detectorIndexVariable, detector_index_name, false);
        addVariableMetadata(detectorIndexVariable, product);
        final List<Variable> variables = netcdfFile.getVariables();
        for (final Variable variable : variables) {
            final int bandsDimensionIndex = variable.findDimensionIndex("bands");
            final int detectorsDimensionIndex = variable.findDimensionIndex("detectors");
            if (bandsDimensionIndex != -1 && detectorsDimensionIndex != -1) {
                final int numBands = variable.getDimension(bandsDimensionIndex).getLength();
                for (int i = 1; i <= numBands; i++) {
                    addVariableAsBand(product, variable, variable.getFullName() + "_band_" + i, true);
                }
            } else if (variable.getDimensions().size() == 1 && detectorsDimensionIndex != -1) {
                addVariableAsBand(product, variable, variable.getFullName(), true);
            }
            addVariableMetadata(variable, product);
        }
    }

    @Override
    protected RenderedImage createSourceImage(Band band) {
        if (band.getName().equals(detector_index_name)) {
            return super.createSourceImage(band);
        }
        final String bandName = band.getName();
        String variableName = bandName;
        Variable variable;
        int dimensionIndex = -1;
        String dimensionName = "";
        if (bandName.contains("_band")) {
            final int suffixIndex = variableName.indexOf("_band");
            variableName = bandName.substring(0, suffixIndex);
            variable = getNetcdfFile().findVariable(variableName);
            dimensionName = "bands";
            dimensionIndex = Integer.parseInt(bandName.substring(suffixIndex + 6)) - 1;
        } else {
            variable = getNetcdfFile().findVariable(variableName);
        }
        return new S3MultiLevelOpImage(band, variable, dimensionName, dimensionIndex,
                                       detectorIndexVariable, "detectors", dimensionName);
    }

    @Override
    protected void addVariableMetadata(Variable variable, Product product) {
        super.addVariableMetadata(variable, product);
        if (variable.getRank() == 2 && variable.getDimension(0).getFullName().equals("bands")) {
            try {
                final String variableName = variable.getFullName();
                final MetadataElement variableElement =
                        product.getMetadataRoot().getElement("Variable_Attributes").getElement(variableName);
                final float[][] contentMatrix = (float[][]) variable.read().copyToNDJavaArray();
                final int length = contentMatrix.length;

                for (int i = 0; i < length; i++) {
                    final MetadataElement xElement = new MetadataElement(getMetadataElementName(variableName) +
                                                                                 " for band " + (i + 1));
                    final ProductData content = ProductData.createInstance(contentMatrix[i]);
                    final MetadataAttribute covarianceAttribute =
                            new MetadataAttribute(getMetadataAttributeName(variableName), content, true);
                    xElement.addAttribute(covarianceAttribute);
                    variableElement.addElement(xElement);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String getMetadataElementName(String attributeName) {
        switch (attributeName) {
            case "relative_spectral_covariance":
                return "Covariances";
            case "lambda0":
                return "Central wavelengths";
            case "FWHM":
                return "Bandwidths";
            case "solar_flux":
                return "Solar fluxes";
        }
        return "";
    }

    private String getMetadataAttributeName(String attributeName) {
        switch (attributeName) {
            case "relative_spectral_covariance":
                return "Covariance";
            case "lambda0":
                return "Central wavelength";
            case "FWHM":
                return "Bandwidths";
            case "solar_flux":
                return "Solar flux";
        }
        return "";
    }

}
