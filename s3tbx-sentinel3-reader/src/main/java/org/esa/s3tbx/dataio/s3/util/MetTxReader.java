package org.esa.s3tbx.dataio.s3.util;

import org.esa.snap.core.datamodel.Product;
import ucar.nc2.Variable;

/**
 * @author Tonio Fincke
 */
public class MetTxReader extends S3NetcdfReader {

    @Override
    protected String[] getSeparatingDimensions() {
        return new String[]{"n_bound", "t_series", "p_atmos"};
    }

    @Override
    public String[] getSuffixesForSeparatingDimensions() {
        return new String[]{"bound", "time", "pressure_level"};
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
}
