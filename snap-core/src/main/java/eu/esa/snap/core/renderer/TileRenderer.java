package eu.esa.snap.core.renderer;


/**
 * Internal seam for a renderer-owned display tile.
 */
public interface TileRenderer {

    int compose(double[] values, boolean[] sourceValid, boolean groupMaskValid, double[] noDataValues);
}
