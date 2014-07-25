package org.esa.beam.dataio.s3.synergy;

import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class NcFile {

    private final NetcdfFile netcdfFile;

    static NcFile open(File file) throws IOException {
        return new NcFile(NetcdfFile.open(file.getPath()));
    }

    static NcFile openResource(String name) throws IOException, URISyntaxException {
        final URL url = NcFile.class.getResource(name);
        final File file = new File(url.toURI());
        return new NcFile(NetcdfFile.open(file.getPath()));
    }

    private NcFile(NetcdfFile netcdfFile) {
        this.netcdfFile = netcdfFile;
    }

    List<Variable> getVariables(String regex) {
        List<Variable> variables = new ArrayList<Variable>();
        for (final Variable variable : netcdfFile.getVariables()) {
            if (variable.getName().matches(regex)) {
                variables.add(variable);
            }
        }
        return variables;
    }

    double[] read(String name) throws IOException {
        return getDoubles(netcdfFile, name);
    }

    void close() {
        try {
            netcdfFile.close();
        } catch (IOException ignored) {
        }
    }

    private double[] getDoubles(NetcdfFile ncFile, String name) throws IOException {
        final Variable variable = ncFile.findVariable(name);

        if (variable != null) {
            final double scaleFactor = getAttributeDouble(variable, "scale_factor", 1.0);
            final double addOffset = getAttributeDouble(variable, "add_offset", 0.0);
            final double fillValue = getAttributeDouble(variable, "_FillValue", Double.NaN);
            final Array array = variable.read();

            final double[] data = new double[(int) variable.getSize()];
            for (int i = 0; i < data.length; i++) {
                final double value = array.getDouble(i);
                if (Double.isNaN(value) || value == fillValue) {
                    data[i] = Double.NaN;
                } else {
                    data[i] = addOffset + value * scaleFactor;
                }
            }
            return data;
        }

        return null;
    }

    private double getAttributeDouble(Variable variable, String attributeName, double defaultValue) {
        final Attribute attribute = variable.findAttribute(attributeName);
        if (attribute == null) {
            return defaultValue;
        }
        return attribute.getNumericValue().doubleValue();
    }


}
