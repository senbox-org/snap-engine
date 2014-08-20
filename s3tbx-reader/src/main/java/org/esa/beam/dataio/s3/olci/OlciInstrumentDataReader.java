package org.esa.beam.dataio.s3.olci;

import org.esa.beam.dataio.netcdf.util.DataTypeUtils;
import org.esa.beam.dataio.s3.util.S3MultiLevelOpImage;
import org.esa.beam.dataio.s3.util.S3NetcdfReader;
import org.esa.beam.dataio.s3.util.S3ReferencingVariableOpImage;
import org.esa.beam.dataio.s3.util.S3VariableOpImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.IndexCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.ResolutionLevel;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
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
        addVariableAsBand(product, detectorIndexVariable, detector_index_name);
        addVariableMetadata(detectorIndexVariable, product);
        final List<Variable> variables = netcdfFile.getVariables();
        for (final Variable variable : variables) {
            final int bandsDimensionIndex = variable.findDimensionIndex("bands");
            if (bandsDimensionIndex != -1 && variable.findDimensionIndex("detectors") != -1) {
                final int numBands = variable.getDimension(bandsDimensionIndex).getLength();
                for(int i = 1; i <= numBands; i++) {
                    addVariableAsBand(product, variable, variable.getFullName() + "_band_" + i);
                }
            }
            addVariableMetadata(variable, product);
        }
    }

    @Override
    protected RenderedImage createSourceImage(Band band) {
        if(band.getName().equals(detector_index_name)) {
            return super.createSourceImage(band);
        }
        final String bandName = band.getName();
        String variableName = bandName;
        Variable variable;
        int dimensionIndex = -1;
        String dimensionName = "";
        if(bandName.contains("_band")) {
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
        if(variable.getRank() == 2 && variable.getDimension(0).getFullName().equals("bands") &&
                variable.getDimension(1).getFullName().equals("bands")) {
            try {
                final MetadataElement variableElement =
                        product.getMetadataRoot().getElement("Variable_Attributes").getElement(variable.getFullName());
                final float[][] covarianceMatrix = (float[][]) variable.read().copyToNDJavaArray();
                final int length = covarianceMatrix.length;
                for(int i = 0; i < length; i++) {
                    final MetadataElement xElement = new MetadataElement("Covariances for band " + (i + 1));
                    final ProductData covariances = ProductData.createInstance(covarianceMatrix[i]);
                    final MetadataAttribute covarianceAttribute = new MetadataAttribute("Covariance",
                                                                                        covariances, true);
                    xElement.addAttribute(covarianceAttribute);
                    variableElement.addElement(xElement);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
