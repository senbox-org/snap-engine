package org.esa.snap.speclib.util.resampling;

import com.google.common.primitives.Doubles;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class SpectralResampling {

    public SpectralResampling() {
    }

    /**
     * Resamples an input spectrum given on a source wavelength array onto a target wavelength array.
     * Implementation follows EnMAP Box Python code:
     * <a href="https://github.com/EnMAP-Box/enmap-box/blob/main/enmapboxprocessing/algorithm/spectralresamplingbyresponsefunctionconvolutionalgorithmbase.py">...</a>
     *
     * @param inputSpectrum - the input spectrum
     * @param inputWvls     - the input wavelengths
     * @param srfList       - list of Spectral Response functions (target reference wavelength + weights around it)
     * @return double[] resampledSpectrum
     */
    public static double[] resample(double[] inputSpectrum, double[] inputWvls,
                                    List<SpectralResponseFunction> srfList) {

        Logger logger = Logger.getLogger(SpectralResampling.class.getName());
        List<Double> resampledSpectrumList = new ArrayList<>();

        srfList.iterator().forEachRemaining(srf -> {
            List<Integer> indices = new ArrayList<>();
            List<Float> weights = new ArrayList<>();

            for (int i = 0; i < inputWvls.length; i++) {
                final int inputWvl = (int) Math.round(inputWvls[i]);
                for (int j = 0; j < srf.getSpectralResponsesList().size(); j++) {
                    final int srWvl = Math.round(srf.getSpectralResponsesList().get(j).getWvl());
                    if (inputWvl == srWvl) {
                        final float srWeight = srf.getSpectralResponsesList().get(j).getWeight();
                        indices.add(i);
                        weights.add(srWeight);
                    }
                }
            }
            if (indices.isEmpty()) {
                logger.fine("Spectral Resampling: No input wavelengths covered by target wavelength '" + srf.getRefWvl() + "'");
                resampledSpectrumList.add(0.0);
            } else {
                double sumWeightedSpectrum = 0.0;
                double sumWeights = 0.0;
                for (int i = 0; i < indices.size(); i++) {
                    final int index = indices.get(i);
                    sumWeightedSpectrum += (inputSpectrum[index] * weights.get(i));
                    sumWeights += weights.get(i);
                }
                resampledSpectrumList.add(sumWeightedSpectrum / sumWeights);
            }
        });

        return Doubles.toArray(resampledSpectrumList);
    }

}
