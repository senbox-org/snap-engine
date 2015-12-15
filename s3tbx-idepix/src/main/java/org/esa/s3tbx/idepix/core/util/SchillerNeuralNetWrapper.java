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

package org.esa.s3tbx.idepix.core.util;

import org.esa.snap.core.nn.NNffbpAlphaTabFast;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * A wrapper around a neural net together with its input.
 * This wrapper support 'Schiller' nets.
 */
public class SchillerNeuralNetWrapper {

    private final NNffbpAlphaTabFast neuralNet;
    private final double[] nnIn;

    private SchillerNeuralNetWrapper(NNffbpAlphaTabFast neuralNet, int in) {
        this.neuralNet = neuralNet;
        this.nnIn = new double[in];
    }

    public NNffbpAlphaTabFast getNeuralNet() {
        return neuralNet;
    }

    public double[] getInputVector() {
        return nnIn;
    }

    public static ThreadLocal<SchillerNeuralNetWrapper> create(InputStream inputStream) {
        final String netAsString = readNeuralNetFromStream(inputStream);
        return new ThreadLocal<SchillerNeuralNetWrapper>() {
            @Override
            protected SchillerNeuralNetWrapper initialValue() {
                try {
                    NNffbpAlphaTabFast nNffbpAlphaTabFast = new NNffbpAlphaTabFast(netAsString);
                    int numIn = nNffbpAlphaTabFast.getInmin().length;
                    return new SchillerNeuralNetWrapper(nNffbpAlphaTabFast, numIn);
                } catch (IOException e) {
                    throw new OperatorException("Cannot initialize neural nets: " + e.getMessage());
                }
            }
        };
    }

    private static String readNeuralNetFromStream(InputStream neuralNetStream) {
        try (Reader reader = new InputStreamReader(neuralNetStream)) {
            return FileUtils.readText(reader);
        } catch (IOException ioe) {
            throw new OperatorException("Could not initialize neural net", ioe);
        }
    }
}
