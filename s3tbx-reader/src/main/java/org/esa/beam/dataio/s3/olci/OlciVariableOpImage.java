package org.esa.beam.dataio.s3.olci;

import org.esa.beam.dataio.s3.util.S3VariableOpImage;
import org.esa.beam.jai.ResolutionLevel;
import ucar.nc2.VariableIF;

import java.awt.Dimension;

/**
 * @author Tonio Fincke
 */
public class OlciVariableOpImage extends S3VariableOpImage {

    //todo remove this when data delivers rows and columns at other positions

    public OlciVariableOpImage(VariableIF variable, int dataBufferType, int sourceWidth, int sourceHeight, Dimension tileSize, ResolutionLevel level, String dimensionName, int dimensionIndex) {
        super(variable, dataBufferType, sourceWidth, sourceHeight, tileSize, level, dimensionName, dimensionIndex);
    }

    @Override
    protected int getIndexX(int rank) {
        return 1;
    }

    @Override
    protected int getIndexY(int rank) {
        return 0;
    }

}
