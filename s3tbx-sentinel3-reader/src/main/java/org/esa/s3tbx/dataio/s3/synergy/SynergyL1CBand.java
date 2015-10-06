package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.util.SystemUtils;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;

import java.util.logging.Level;

/**
 * @author Tonio Fincke
 */
public class SynergyL1CBand extends VirtualBand {

    private SceneRasterTransform sceneRasterTransform;

    /**
     * Constructs a new <code>Band</code>.
     *
     * @param name       the name of the new object
     * @param dataType   the raster data type, must be one of the multiple <code>ProductData.TYPE_<i>X</i></code>
     *                   constants, with the exception of <code>ProductData.TYPE_UINT32</code>
     * @param width      the width of the raster in pixels
     * @param height     the height of the raster in pixels
     * @param expression the expression code
     */
    public SynergyL1CBand(String name, int dataType, int width, int height, String expression) {
        super(name, dataType, width, height, expression);
    }

    @Override
    public void setGeoCoding(GeoCoding geoCoding) {
        super.setGeoCoding(geoCoding);
        computeSceneRasterTransform();
    }

    /**
     * Gets a transformation allowing to transform from this raster CS to the product's scene raster CS.
     *
     * @return The transformation or {@code null}, if no such exists.
     * @since SNAP 2.0
     */
    public SceneRasterTransform getSceneRasterTransform() {
        if (sceneRasterTransform != null) {
            return sceneRasterTransform;
        }
        computeSceneRasterTransform();
        return sceneRasterTransform;
    }

    /**
     * Sets the transformation allowing to transform from this raster CS to the product's scene raster CS.
     *
     * @param sceneRasterTransform The transformation or {@code null}.
     * @since SNAP 2.0
     */
    public void setSceneRasterTransform(SceneRasterTransform sceneRasterTransform) {
        this.sceneRasterTransform = sceneRasterTransform;
    }

    /**
     * Computes a transformation allowing to transform from this raster CS o the product's scene raster CS.
     * This method is called if no transformation has been set using the
     * {@link #setSceneRasterTransform(SceneRasterTransform)} method.
     *
     * @since SNAP 2.0
     */
    protected void computeSceneRasterTransform() {
        Product product = getProduct();
        if (product != null) {
            GeoCoding pgc = product.getGeoCoding();
            GeoCoding rgc = getGeoCoding();
            if (pgc == rgc || pgc != null && rgc == null) {
                sceneRasterTransform = SceneRasterTransform.IDENTITY;
                return;
            }
            if (pgc != null) {
                CoordinateReferenceSystem pcrs = pgc.getMapCRS();
                CoordinateReferenceSystem rcrs = rgc.getMapCRS();
                if (pcrs != null && rcrs != null && pcrs.equals(rcrs)) {
                    try {
                        MathTransform ri2m = rgc.getImageToMapTransform();
                        MathTransform pm2i = pgc.getImageToMapTransform().inverse();
                        MathTransform mathTransform = ConcatenatedTransform.create(ri2m, pm2i);
                        if (mathTransform instanceof MathTransform2D) {
                            MathTransform2D forward = (MathTransform2D) mathTransform;
                            MathTransform2D inverse = forward.inverse();
                            sceneRasterTransform = new DefaultSceneRasterTransform(forward, inverse);
                            return;
                        }
                    } catch (NoninvertibleTransformException e) {
                        SystemUtils.LOG.log(Level.SEVERE,
                                            "failed to create SceneRasterTransform for raster '" + getName() + "'", e);
                    }
                }
            }
        }
        sceneRasterTransform = null;
    }

}
