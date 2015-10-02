package org.esa.s3tbx.dataio.s3.synergy;

import org.opengis.referencing.operation.MathTransform2D;

/**
 * Default implementation of the {@link SceneRasterTransform} interface.
 */
class DefaultSceneRasterTransform implements SceneRasterTransform {
    private final MathTransform2D forward;
    private final MathTransform2D inverse;

    public DefaultSceneRasterTransform(MathTransform2D forward, MathTransform2D inverse) {
        this.forward = forward;
        this.inverse = inverse;
    }

    @Override
    public MathTransform2D getForward() {
        return forward;
    }

    @Override
    public MathTransform2D getInverse() {
        return inverse;
    }
}
