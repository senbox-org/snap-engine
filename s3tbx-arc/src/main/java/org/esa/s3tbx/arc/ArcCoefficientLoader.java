/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s3tbx.arc;

import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.math.VectorLookupTable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Convenience class for SST coefficient file handling. This class loads and verifies SST coefficient sets containd in
 * files conforming to the specifications denoted in the BEAM documentation.
 * <p/>
 * A short description of the format:<br> <ul> <li>A coefficient file is a standard java properties file consisting of
 * key/value pairs and comments.</li> <li>A comment line begins with a "#" and is ignored.</li> <li>Each coefficient
 * file should contain a short description, denoted as: <code>description = my coefficient description</code>.</li>
 * <li>The coefficient file contains any number of so called map keys. These keys define a pixel range across the
 * scanline where a specific set of coefficients shall be used.</li> <li>For every map range there must be a
 * corresponding set of coefficients. </li> </ul>
 * <p/>
 * Example:
 * <p/>
 * <code> # Testfile for SST coefficient sets<br> #<br> # The syntax is always<br> #   key=value<br> #<br> # Allowed
 * keys are:<br> #   description - a short description of the coefficient set<br> #   map.x        - defines a range of
 * scanline pixels as coefficient set x<br> #   a.x            - coefficient set for map x<br> #   b.x to d.x   - same
 * as a.x<br> <br> description=test coefficient set for nadir SST<br> <br> map.0=0, 234<br> map.1=235, 511<br> <br>
 * a.0=1.0, 2.0, 3.0<br> b.0=1.0, 2.0, 3.0, 4.0, 5.0<br> <br> a.1=1.0, 2.0, 3.0<br> b.1=1.0, 2.0, 3.0, 4.0, 5.0<br> <br>
 * </code>
 */
public class ArcCoefficientLoader {

    private static final String _nameKey = "name";
    private static final String _descriptionKey = "description";
    private static final String _secnadkey = "secnad";
    private static final String _secfwdkey = "secfwd";
    private static final String _wvbandkey = "wvband";
    private static final String _coeffskey = "coeffs";
    private static final char[] _separators = new char[]{','};

    private Properties _props;
    private static final int _numCoeffs = 7;

    private Logger _logger;

    /**
     * Constructs the object with default values
     */
    public ArcCoefficientLoader() {
        _props = new Properties();
        _logger = Logger.getLogger(ArcConstants.LOGGER_NAME);
    }

    /**
     * Loads a coefficient file passed in as URL and verifies the content for consistency
     *
     * @return a validated coeffcient set contained in the file
     */
    public ArcCoefficients load(URL coeffFile) throws IOException {
        Guardian.assertNotNull("coeffFile", coeffFile);

        _logger.fine("Reading coefficient file: '" + coeffFile.getPath() + "'");

        InputStream inStream = coeffFile.openStream();
        _props.clear();
        try {
            _props.load(inStream);
        } finally {
            inStream.close();
        }

        final String name = _props.getProperty(_nameKey);
        if (name != null) {
            _logger.fine("... coefficients name: '" + name + "'");
        } else {
            throw new OperatorException("illegal coefficient file format: name is required");
        }

        final String description = _props.getProperty(_descriptionKey);
        if (description != null) {
            _logger.fine("... coefficients description: '" + description + "'");
        } else {
            _logger.fine("... coefficients have no description");
        }

        String value;
        value = _props.getProperty(_secnadkey);
        final double[] secnad = loadCoeffArray(value);

        value = _props.getProperty(_secfwdkey);
        final double[] secfwd = loadCoeffArray(value);

        value = _props.getProperty(_wvbandkey);
        final double[] wvband = loadCoeffArray(value);

        final double[][] dimensions = {wvband, secfwd, secnad};

        value = _props.getProperty(_coeffskey);
        final double[] lutdata = loadCoeffArray(value);

        final VectorLookupTable lut = new VectorLookupTable(7, lutdata, dimensions);
        ArcCoefficients coeffs = new ArcCoefficients(name, description, lut);

        _logger.fine("... success");

        return coeffs;
    }
    /**
     * Retrieves the description of the coefficient file passed in
     */
    public String getDescription(URL coeffFile) throws IOException {
        Guardian.assertNotNull("coeffFile", coeffFile);
        String desc = "";

        File file = null;
        try {
            file = new File(coeffFile.toURI());
        } catch (URISyntaxException e) {
            // ignore
        }

        if (file != null && file.exists() && file.isFile()) {
            InputStream inStream = new FileInputStream(file);
            _props.load(inStream);
            inStream.close();
            desc = _props.getProperty(_descriptionKey);
        }

        return desc;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////
    private static double[] loadCoeffArray(String value) {
        String[] valStrings = StringUtils.split(value, _separators, true);

        // convert the string array to float values
        double[] valDouble = new double[valStrings.length];

        for (int n = 0; n < valStrings.length; n++) {
            valDouble[n] = Double.parseDouble(valStrings[n]);
        }
        return valDouble;
    }
}
