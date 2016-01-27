package org.esa.s3tbx.dataio.s3.synergy;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.AbstractGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.transform.GeoCodingMathTransform;
import org.esa.snap.core.transform.MathTransform2D;
import org.opengis.referencing.operation.TransformException;

/**
 * @author Tonio Fincke
 */
public class SynL1CSceneTransformGeoCoding extends AbstractGeoCoding {

    private final GeoCoding wrappedGeoCoding;
    private final MathTransform2D sceneToModelTransform;
    private final MathTransform2D modelToSceneTransform;

    SynL1CSceneTransformGeoCoding(GeoCoding wrappedGeoCoding, MathTransform2D sceneToModelTransform, MathTransform2D modelToSceneTransform) {
        this.wrappedGeoCoding = wrappedGeoCoding;
        setMapCRS(wrappedGeoCoding.getMapCRS());
        setGeoCRS(wrappedGeoCoding.getGeoCRS());
        this.sceneToModelTransform = sceneToModelTransform;
        this.modelToSceneTransform = modelToSceneTransform;
        setImageCRS(createImageCRS(wrappedGeoCoding.getMapCRS(), new GeoCodingMathTransform(this)));
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        return wrappedGeoCoding.isCrossingMeridianAt180();
    }

    @Override
    public boolean canGetPixelPos() {
        return sceneToModelTransform != MathTransform2D.NULL && wrappedGeoCoding.canGetPixelPos();
    }

    @Override
    public boolean canGetGeoPos() {
        return modelToSceneTransform != MathTransform2D.NULL && wrappedGeoCoding.canGetGeoPos();
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        final PixelPos wrappedPixelPos = wrappedGeoCoding.getPixelPos(geoPos, new PixelPos());
        //this works because for syn l1c the imagetomodeltransforms are all identity transforms. If this code should
        //ever be used for another product, make sure that imagetomodeltransfroms are 0. If they are not, transform
        //the pixels to model coordinates (and back from scene coordinates).
        try {
            sceneToModelTransform.transform(wrappedPixelPos, pixelPos);
        } catch (TransformException e) {
            if (pixelPos == null) {
                pixelPos = new PixelPos();
            }
            pixelPos.setLocation(Double.NaN, Double.NaN);
        }
        return pixelPos;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        //this works because for syn l1c the imagetomodeltransforms are all identity transforms. If this code should
        //ever be used for another product, make sure that imagetomodeltransfroms are 0. If they are not, transform
        //the pixels to model coordinates (and back from scene coordinates).
        try {
            final PixelPos modelPixelPos = new PixelPos();
            modelToSceneTransform.transform(pixelPos, modelPixelPos);
            geoPos = wrappedGeoCoding.getGeoPos(modelPixelPos, geoPos);
        } catch (TransformException e) {
            if (geoPos == null) {
                geoPos = new GeoPos();
            }
            geoPos.setLocation(Double.NaN, Double.NaN);
        }
        return geoPos;
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
        //todo maybe improve this method if necessary - tf 20160127
        if (subsetDef != null || srcScene.getProduct() != destScene.getProduct()) {
            return false;
        }
        destScene.setGeoCoding(new SynL1CSceneTransformGeoCoding(wrappedGeoCoding, sceneToModelTransform, modelToSceneTransform));
        return true;
    }

}
