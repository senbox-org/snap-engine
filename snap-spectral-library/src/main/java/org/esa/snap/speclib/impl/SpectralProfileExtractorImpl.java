package org.esa.snap.speclib.impl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.speclib.api.SpectralProfileExtractor;
import org.esa.snap.speclib.api.SpectralSampleProvider;
import org.esa.snap.speclib.model.SpectralProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class SpectralProfileExtractorImpl implements SpectralProfileExtractor {


    private final SpectralSampleProvider sampleProvider;


    public SpectralProfileExtractorImpl(SpectralSampleProvider sampleProvider) {
        this.sampleProvider = Objects.requireNonNull(sampleProvider, "sampleProvider must not be null");
    }


    @Override
    public Optional<SpectralProfile> extract(String name, List<Band> bands, int x, int y, int level, String unit) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        Objects.requireNonNull(bands, "bands must not be null");

        List<Double> wl = new ArrayList<>();
        List<Double> vv = new ArrayList<>();

        for (Band b : bands) {
            if (b == null) {
                continue;
            }
            final float wavelength = b.getSpectralWavelength();
            if (wavelength <= 0.0f) {
                continue;
            }
            if (!sampleProvider.isPixelValid(b, x, y, level)) {
                continue;
            }

            final double value = sampleProvider.readSample(b, x, y, level);
            final double noDataVal = sampleProvider.noDataValue(b);

            if (Double.compare(value, noDataVal) == 0) {
                continue;
            }
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                continue;
            }

            wl.add((double) wavelength);
            vv.add(value);
        }

        if (wl.isEmpty()) {
            return Optional.empty();
        }

        double[] wavelengths = new double[wl.size()];
        double[] values = new double[vv.size()];
        for (int i = 0; i < wl.size(); i++) {
            wavelengths[i] = wl.get(i);
            values[i] = vv.get(i);
        }

        return Optional.of(SpectralProfile.create(name, wavelengths, values, unit));
    }
}
