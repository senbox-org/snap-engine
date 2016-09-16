package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.SceneTransformProvider;
import org.esa.snap.core.transform.AbstractTransform2D;
import org.esa.snap.core.transform.MathTransform2D;
import org.opengis.referencing.operation.NoninvertibleTransformException;
import org.opengis.referencing.operation.TransformException;

import java.awt.geom.Point2D;

/**
 * Not in use.
 *
 * @author Tonio Fincke
 */
class SlstrGeoCodingSceneTransformProvider implements SceneTransformProvider {

    private final GeoCoding sceneGeoCoding;
    private final GeoCoding modelGeoCoding;
    private final ModelToSceneTransform modelToSceneTransform;
    private final SceneToModelTransform sceneToModelTransform;

    /**
     * A SceneTransformProvider that provides sceneTransforms between scenes that call the getGeoPos() and getPixelPos()
     * methods of the * geocodings of the scene and the model. This SceneTransform is currently only used by SLSTR L1B,
     * but might in the future be meaningful to apply to other product types.
     *
     * @param sceneGeoCoding The geo-coding of the scene
     * @param modelGeoCoding The geo-coding of the model
     */
    public SlstrGeoCodingSceneTransformProvider(GeoCoding sceneGeoCoding, GeoCoding modelGeoCoding) {
        this.sceneGeoCoding = sceneGeoCoding;
        this.modelGeoCoding = modelGeoCoding;
        modelToSceneTransform = new ModelToSceneTransform();
        sceneToModelTransform = new SceneToModelTransform();
    }

    @Override
    public MathTransform2D getModelToSceneTransform() {
        return modelToSceneTransform;
    }

    @Override
    public MathTransform2D getSceneToModelTransform() {
        return sceneToModelTransform;
    }

    private abstract class SlstrSceneTransform extends AbstractTransform2D {

        protected Point2D transform(Point2D ptSrc, Point2D ptDst, GeoCoding from, GeoCoding to) throws TransformException {
            if (!from.canGetGeoPos() || !to.canGetPixelPos()) {
                throw new TransformException("Cannot transform");
            }
            PixelPos pixelPos = new PixelPos(ptSrc.getX(), ptSrc.getY());
            final GeoPos geoPos = from.getGeoPos(pixelPos, new GeoPos());
            pixelPos = to.getPixelPos(geoPos, pixelPos);
            if (Double.isNaN(geoPos.getLat()) || Double.isNaN(geoPos.getLon()) ||
                    Double.isNaN(pixelPos.getX()) || Double.isNaN(pixelPos.getY())) {
                throw new TransformException("Cannot transform");
            }
            ptDst.setLocation(pixelPos.getX(), pixelPos.getY());
            return ptDst;
        }

        GeoCoding getModelGeoCoding() {
            return modelGeoCoding;
        }

        GeoCoding getSceneGeoCoding() {
            return sceneGeoCoding;
        }

    }

    private class ModelToSceneTransform extends SlstrSceneTransform {

        @Override
        public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
            return transform(ptSrc, ptDst, modelGeoCoding, sceneGeoCoding);
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            return sceneToModelTransform;
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof ModelToSceneTransform)) {
                return false;
            }
            final ModelToSceneTransform that = (ModelToSceneTransform) object;
            return  that.getModelGeoCoding().equals(this.getModelGeoCoding()) &&
                    that.getSceneGeoCoding().equals(this.getSceneGeoCoding());
        }

        @Override
        public int hashCode() {
            return modelGeoCoding.hashCode() + sceneGeoCoding.hashCode() + 1;
        }
    }

    private class SceneToModelTransform extends SlstrSceneTransform {

        @Override
        public Point2D transform(Point2D ptSrc, Point2D ptDst) throws TransformException {
            return transform(ptSrc, ptDst, sceneGeoCoding, modelGeoCoding);
        }

        @Override
        public MathTransform2D inverse() throws NoninvertibleTransformException {
            return modelToSceneTransform;
        }

        @Override
        public boolean equals(Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof SceneToModelTransform)) {
                return false;
            }
            final SceneToModelTransform that = (SceneToModelTransform) object;
            return  that.getModelGeoCoding().equals(this.getModelGeoCoding()) &&
                    that.getSceneGeoCoding().equals(this.getSceneGeoCoding());
        }

        @Override
        public int hashCode() {
            return modelGeoCoding.hashCode() + sceneGeoCoding.hashCode() + 2;
        }
    }

}
