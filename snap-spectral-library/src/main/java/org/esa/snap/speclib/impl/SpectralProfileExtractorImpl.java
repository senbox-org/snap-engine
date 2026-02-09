package org.esa.snap.speclib.impl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.speclib.api.SpectralProfileExtractor;
import org.esa.snap.speclib.api.SpectralSampleProvider;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;

import java.util.*;


public class SpectralProfileExtractorImpl implements SpectralProfileExtractor {


    private final SpectralSampleProvider sampleProvider;


    public SpectralProfileExtractorImpl(SpectralSampleProvider sampleProvider) {
        this.sampleProvider = Objects.requireNonNull(sampleProvider, "sampleProvider must not be null");
    }


    @Override
    public Optional<SpectralProfile> extract(String name,
                                             SpectralAxis axis,
                                             List<Band> bands,
                                             int x,
                                             int y,
                                             int level,
                                             String yUnit,
                                             String productId) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(axis, "axis must not be null");
        Objects.requireNonNull(bands, "bands must not be null");

        if (bands.size() != axis.size()) {
            throw new IllegalArgumentException("bands size must match axis size");
        }

        double[] values = sampleProvider.readSamples(bands, x, y, level);

        double nodataVal = Double.NaN;
        for (int i = 0; i < values.length; i++) {
            double v = values[i];
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                values[i] = nodataVal;
            }
        }

        boolean anyFinite = false;
        for (double v : values) {
            if (!Double.isNaN(v) && !Double.isInfinite(v)) {
                anyFinite = true;
                break;
            }
        }
        if (!anyFinite) {
            return Optional.empty();
        }

        SpectralSignature sig = (yUnit == null)
                ? SpectralSignature.of(values)
                : SpectralSignature.of(values, yUnit);

        SpectralProfile.SourceRef ref = new SpectralProfile.SourceRef(x, y, level, productId);
        return Optional.of(new SpectralProfile(UUID.randomUUID(), name, sig, Map.of(), ref));
    }
}
