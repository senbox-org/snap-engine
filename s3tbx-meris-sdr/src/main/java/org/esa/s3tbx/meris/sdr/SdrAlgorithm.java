/*
 * $Id: SdrAlgorithm.java,v 1.1 2007/03/27 12:52:21 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.sdr;

import com.bc.jnn.JnnNet;
import org.esa.snap.core.util.math.MathUtils;

/**
 * Implements an algorithm for the surface directional reflectance based
 * on a neural network (multi layer perceptron).
 */
public final class SdrAlgorithm implements Cloneable {
    public final JnnNet _neuralNet;

    /**
     * Constructs a new algorithm instance.
     *
     * @param neuralNet the neural network
     */
    public SdrAlgorithm(JnnNet neuralNet) {
        if (neuralNet == null) {
            throw new IllegalArgumentException("neuralNet == null");
        }
        _neuralNet = neuralNet;
    }

    /**
     * Computes the surface directional reflectance.
     *
     * @param rhoNorm    normalized MERIS L2 reflectance in [dimless]
     * @param wavelength the wavelength in [nm]
     * @param vza        viewing zenith angle [degree]
     * @param sza        sun zenith angle [degree]
     * @param ada        azimuth difference [degree]
     * @param aot470     (MODIS) aerosol optical thickness at 470 nm [dimless]
     * @param aot660     (MODIS) aerosol optical thickness at 660 nm [dimless]
     * @param ang        (MODIS) angstroem coefficient [dimless]
     * @return the surface directional reflectance [dimless]
     */
    public double computeSdr(double rhoNorm,
                             double wavelength,
                             double vza,
                             double sza,
                             double ada,
                             double aot470,
                             double aot660,
                             double ang) {
        final double[] input = new double[9];
        final double[] output = new double[1];

        sza *= MathUtils.DTOR;
        vza *= MathUtils.DTOR;
        ada *= MathUtils.DTOR;

        input[0] = rhoNorm;
        input[1] = wavelength;
        input[2] = Math.cos(sza);
        input[3] = Math.sin(vza) * Math.cos(ada);
        input[4] = Math.sin(vza) * Math.sin(ada);
        input[5] = Math.cos(vza);
        input[6] = aot470;
        input[7] = aot660;
        input[8] = ang;
        computeSdr(input, output);
        return output[0];
    }

    /**
     * Computes the surface directional reflectance.
     * The parameter <code>inputVector</code> must provide the following information:
     * <p/>
     * inputVector[0] = rhoNorm:  Normalized MERIS L2 reflectance in [sr^-1]<br/>
     * inputVector[1] = wavelength: the wavelength of the MERIS L2 reflectance [nm] <br/>
     * inputVector[2] = cosSza:  cos(sza): the cosine of the sun zenith angle [dimless]  <br/>
     * inputVector[3] = x: sin(vza) * cos(adiff): adiff = aszimuth difference [dimless]<br/>
     * inputVector[4] = y: sin(vza) * sin(adiff): adiff = aszimuth difference [dimless] <br/>
     * inputVector[5] = z: cos(vza) [dimless] <br/>
     * inputVector[6] = aot470: MODIS aerosol optical thickness at 470 nm [dimless]<br/>
     * inputVector[7] = aot660: MODIS aerosol optical thickness at 660 nm [dimless] <br/>
     * inputVector[8] = ang470d660: MODIS angstroem coefficient aot470 / aot660 [dimless] <br/>
     * </p>
     *
     * @param inputVector  the input vector as a <code>double[9]</code> containing the 9 elements described above
     * @param outputVector the output vector as a <code>double[1]</code> containing the surface directional reflectance.
     */
    public void computeSdr(final double[] inputVector, final double[] outputVector) {
        _neuralNet.process(inputVector, outputVector);
    }

    @Override
    protected SdrAlgorithm clone() {
        return new SdrAlgorithm(_neuralNet.clone());
    }
}
