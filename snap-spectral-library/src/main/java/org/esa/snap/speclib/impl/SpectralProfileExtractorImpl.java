package org.esa.snap.speclib.impl;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.PixelPos;
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
    public Optional<SpectralProfile> extract(String name, SpectralAxis axis, List<Band> bands, int x, int y, int level, String yUnit, String productId) {
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

    @Override
    public List<SpectralProfile> extractBulk(String baseName, SpectralAxis axis, List<Band> bands, List<PixelPos> pixels, int level, String yUnit, String productId) {
        int n = pixels.size();
        int[] xs = new int[n];
        int[] ys = new int[n];

        fillXYArrays(pixels, xs, ys);
        double[][] samples = sampleProvider.readSamples(bands, xs, ys, level);

        return buildProfilesFromSamples(baseName, xs, ys, pixels, level, yUnit, productId, samples);
    }


    private static void fillXYArrays(List<PixelPos> pixels, int[] xs, int[] ys) {
        for (int i = 0; i < pixels.size(); i++) {
            PixelPos p = pixels.get(i);
            xs[i] = (p == null) ? -1 : (int) p.x;
            ys[i] = (p == null) ? -1 : (int) p.y;
        }
    }

    private static List<SpectralProfile> buildProfilesFromSamples(String baseName, int[] xs, int[] ys, List<PixelPos> pixels, int level, String yUnit, String productId, double[][] samples) {
        int n = pixels.size();
        List<SpectralProfile> out = new ArrayList<>(n);

        int ii = 1;
        for (int i = 0; i < n; i++, ii++) {
            if (pixels.get(i) == null) {
                continue;
            }
            double[] values = samples[i];
            sanitize(values);

            if (!anyFinite(values)) {
                continue;
            }
            out.add(createProfile(baseName, ii, xs[i], ys[i], level, yUnit, productId, values));
        }
        return Collections.unmodifiableList(out);
    }

    private static void sanitize(double[] values) {
        for (int k = 0; k < values.length; k++) {
            double v = values[k];
            if (Double.isNaN(v) || Double.isInfinite(v)) {
                values[k] = Double.NaN;
            }
        }
    }

    private static boolean anyFinite(double[] values) {
        for (double v : values) {
            if (!Double.isNaN(v) && !Double.isInfinite(v)) {
                return true;
            }
        }
        return false;
    }

    private static SpectralProfile createProfile(String baseName, int index, int x, int y, int level, String yUnit, String productId, double[] values) {
        SpectralSignature sig = (yUnit == null) ? SpectralSignature.of(values) : SpectralSignature.of(values, yUnit);
        String name = baseName + index;

        SpectralProfile.SourceRef ref = new SpectralProfile.SourceRef(x, y, level, productId);
        return new SpectralProfile(UUID.randomUUID(), name, sig, Map.of(), ref);
    }
}
