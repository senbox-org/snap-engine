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

import com.bc.jnn.Jnn;
import com.bc.jnn.JnnException;
import com.bc.jnn.JnnNet;
import org.esa.snap.core.gpf.OperatorException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A wrapper around a neural net together with its input and output vector.
 * This wrapper support 'NNA' nets.
 *
 */
public class NeuralNetWrapper {

    private final JnnNet neuralNet;
    private final double[] nnIn;
    private final double[] nnOut;

    private NeuralNetWrapper(JnnNet neuralNet, int in, int out) {
        this.neuralNet = neuralNet;
        this.nnIn = new double[in];
        this.nnOut = new double[out];
    }

    public JnnNet getNeuralNet() {
        return neuralNet;
    }

    public double[] getInputVector() {
        return nnIn;
    }

    public double[] getOutputVector() {
        return nnOut;
    }

    public static ThreadLocal<NeuralNetWrapper> create(InputStream inputStream, final int in, final int out) {
        final JnnNet jnnNet = loadNeuralNet(inputStream);
        return new ThreadLocal<NeuralNetWrapper>() {
            @Override
            protected NeuralNetWrapper initialValue() {
                return new NeuralNetWrapper(jnnNet.clone(), in, out);
            }
        };
    }

    private static JnnNet loadNeuralNet(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            Jnn.setOptimizing(true);
            return Jnn.readNna(reader);
        } catch (JnnException | IOException jnne) {
            throw new OperatorException(jnne);
        }
    }

}
