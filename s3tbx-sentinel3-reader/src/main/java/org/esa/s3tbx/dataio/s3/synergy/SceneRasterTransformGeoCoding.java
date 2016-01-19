package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.AbstractGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.transform.GeoCodingMathTransform;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.datamodel.SceneRasterTransform;
import org.esa.snap.core.datamodel.SceneRasterTransformException;
import org.esa.snap.core.datamodel.SceneRasterTransformUtils;
import org.esa.snap.core.dataop.maptransf.Datum;

/**
 * @author Tonio Fincke
 */
class SceneRasterTransformGeoCoding extends AbstractGeoCoding {

    private final GeoCoding wrappedGeoCoding;
    private final SceneRasterTransform sceneRasterTransform;

    SceneRasterTransformGeoCoding(GeoCoding wrappedGeoCoding, SceneRasterTransform sceneRasterTransform) {
        this.wrappedGeoCoding = wrappedGeoCoding;
        setMapCRS(wrappedGeoCoding.getMapCRS());
        setGeoCRS(wrappedGeoCoding.getGeoCRS());
        this.sceneRasterTransform = sceneRasterTransform;
        setImageCRS(createImageCRS(wrappedGeoCoding.getMapCRS(), new GeoCodingMathTransform(this)));
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        return wrappedGeoCoding.isCrossingMeridianAt180();
    }

    @Override
    public boolean canGetPixelPos() {
        return sceneRasterTransform.getInverse() != null && wrappedGeoCoding.canGetPixelPos();
    }

    @Override
    public boolean canGetGeoPos() {
        return sceneRasterTransform.getForward() != null && wrappedGeoCoding.canGetGeoPos();
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        wrappedGeoCoding.getPixelPos(geoPos, pixelPos);
        try {
            pixelPos.setLocation(SceneRasterTransformUtils.transformToImageCoords(sceneRasterTransform, pixelPos));
            return pixelPos;
        } catch (SceneRasterTransformException e) {
            return new PixelPos(Double.NaN, Double.NaN);
        }
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        try {
            final PixelPos pixelPos1 =
                    SceneRasterTransformUtils.transformToSceneCoords(sceneRasterTransform, pixelPos);
            return wrappedGeoCoding.getGeoPos(pixelPos1, geoPos);
        } catch (SceneRasterTransformException e) {
            return new GeoPos(Double.NaN, Double.NaN);
        }
    }

    @Override
    public Datum getDatum() {
        return wrappedGeoCoding.getDatum();
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        //todo change this - tf 20160106
        return false;
    }

}
