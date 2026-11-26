package eu.esa.snap.core.renderer.classfraction;

import eu.esa.snap.core.renderer.TileRenderer;


/**
 * Pixel compositor for class fractions/probabilities. Transparent pixels are represented by ARGB zero.
 */
public final class ClassFractionTileRenderer implements TileRenderer {


    private final ClassBandGroupConfig group;


    ClassFractionTileRenderer(ClassFractionRendererConfig config) {
        this.group = config.getClassBandGroup();
    }

    @Override
    public int compose(double[] values, boolean[] sourceValid, boolean groupMaskValid, double[] noDataValues) {
        if (!groupMaskValid || values.length != group.getBands().size() || sourceValid.length != values.length) {
            return 0;
        }

        double red = 0.0;
        double green = 0.0;
        double blue = 0.0;
        boolean hasVisibleClass = false;

        for (int i = 0; i < values.length; i++) {
            double value = values[i];
            if (!Double.isFinite(value) || isNoData(value, noDataValues, i)) {
                return 0;
            }
            if (group.getValidityPolicy() == ClassBandGroupConfig.ValidityPolicy.STRICT_SOURCE_VALIDITY && !sourceValid[i]) {
                return 0;
            }

            ClassBandConfig band = group.getBands().get(i);
            if (band.isVisible()) {
                int color = band.getArgb();
                red += value * ((color >>> 16) & 0xFF);
                green += value * ((color >>> 8) & 0xFF);
                blue += value * (color & 0xFF);
                hasVisibleClass = true;
            }
        }

        if (!hasVisibleClass) {
            return 0;
        }
        double displayScale = group.getDisplayScale();
        return 0xFF000000 | (clamp(red / displayScale) << 16) | (clamp(green / displayScale) << 8) | clamp(blue / displayScale);
    }

    private static boolean isNoData(double value, double[] noDataValues, int index) {
        return noDataValues != null
                && index < noDataValues.length
                && Double.isFinite(noDataValues[index])
                && Double.compare(value, noDataValues[index]) == 0;
    }

    private static int clamp(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }
}
