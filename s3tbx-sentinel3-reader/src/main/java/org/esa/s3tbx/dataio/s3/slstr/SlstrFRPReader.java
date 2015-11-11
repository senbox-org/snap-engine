package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

/**
 * @author Tonio Fincke
 */
class SlstrFRPReader extends S3NetcdfReader {

    private final NetcdfFile netcdfFile;

    SlstrFRPReader(String pathToFile) throws IOException {
        super(pathToFile);
        netcdfFile = NetcdfFileOpener.open(pathToFile);
    }

    //todo continue here as soon as valid test products are available

//    protected void addBands(Product product) {
//        final Variable iVariable = netcdfFile.findVariable("i");
//        final Variable jVariable = netcdfFile.findVariable("j");
//        final List<Variable> variables = netcdfFile.getVariables();
//        for (final Variable variable : variables) {
//            if (variable.findDimensionIndex("rows") != -1 && variable.findDimensionIndex("columns") != -1) {
//                addVariableAsBand(product, variable, variable.getFullName());
//            }
//            addVariableMetadata(variable, product);
//        }
//    }

//    protected RenderedImage createSourceImage(Band band) {
//        final int bufferType = ImageManager.getDataBufferType(band.getDataType());
//        final int sourceWidth = band.getRasterWidth();
//        final int sourceHeight = band.getRasterHeight();
//        final java.awt.Dimension tileSize = band.getProduct().getPreferredTileSize();
//        final String bandName = band.getName();
//        String variableName = bandName;
//        Variable variable;
//        int dimensionIndex = -1;
//        String dimensionName = "";
//        if(bandName.contains("_channel")) {
//            variableName = bandName.substring(0, variableName.indexOf("_channel"));
//            variable = netcdfFile.findVariable(variableName);
//            dimensionName = "channel";
//            dimensionIndex = Integer.parseInt(bandName.substring(bandName.length() - 1)) - 1;
//        } else {
//            variable = netcdfFile.findVariable(variableName);
//        }
//        return new SlstrVariableOpImage(variable, bufferType, sourceWidth, sourceHeight, tileSize,
//                                        ResolutionLevel.MAXRES, dimensionName, dimensionIndex);
//    }

}
