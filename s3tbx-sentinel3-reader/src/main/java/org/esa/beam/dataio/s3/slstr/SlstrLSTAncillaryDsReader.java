package org.esa.beam.dataio.s3.slstr;

import org.esa.beam.dataio.s3.util.S3NetcdfReader;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.IOException;

/**
 * @author Tonio Fincke
 */
public class SlstrLSTAncillaryDsReader extends S3NetcdfReader {

    SlstrLSTAncillaryDsReader(String pathToFile) throws IOException {
        super(pathToFile);
    }

    //todo continue when valid data is provided

    @Override
    protected void addVariableMetadata(Variable variable, Product product) {
        super.addVariableMetadata(variable, product);
        if(variable.getFullName().equals("biome")) {
            final NetcdfFile netcdfFile = getNetcdfFile();
            final Variable validationVariable = netcdfFile.findVariable("validation");
            if(validationVariable != null) {
                try {
                    final Attribute validationMeanings = validationVariable.findAttribute("flag_meanings");
                    final Attribute validationValues = validationVariable.findAttribute("flag_values");
                    final Array validationFlags = validationVariable.read();
                    final MetadataElement element =
                            product.getMetadataRoot().getElement("Variable_Attributes").getElement(variable.getFullName());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
