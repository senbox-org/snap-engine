package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.geotools.referencing.operation.transform.AffineTransform2D;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

/**
 * @author Tonio Fincke
 */
public class SlstrTiePointGeoCoding extends TiePointGeoCoding {

    private final AffineTransform2D transform;
    private final AffineTransform inverse;

    public SlstrTiePointGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid, AffineTransform2D transform)
            throws NoninvertibleTransformException {
        super(latGrid, lonGrid);
        this.transform = transform;
        inverse = transform.createInverse();
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        PixelPos transformedPixelPos = new PixelPos();
        transform.transform(pixelPos, transformedPixelPos);
        return super.getGeoPos(transformedPixelPos, geoPos);
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        pixelPos = super.getPixelPos(geoPos, pixelPos);
        PixelPos transformedPixelPos = new PixelPos();
        inverse.transform(pixelPos, transformedPixelPos);
        pixelPos.setLocation(transformedPixelPos);
        return transformedPixelPos;
    }

}
