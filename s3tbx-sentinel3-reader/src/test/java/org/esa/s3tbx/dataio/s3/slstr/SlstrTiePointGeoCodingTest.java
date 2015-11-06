package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.junit.Before;
import org.junit.Test;

import java.awt.geom.AffineTransform;

import static junit.framework.Assert.*;

/**
 * @author Tonio Fincke
 */
public class SlstrTiePointGeoCodingTest {

    private TiePointGrid lat;
    private TiePointGrid lon;
    private TiePointGeoCoding referenceGeoCoding;

    @Before
    public void setUp() {
        lat = new TiePointGrid("lat", 2, 2, 0, 0, 16, 16, new float[]{1f, 9f, 1f, 9f});
        lon = new TiePointGrid("lon", 2, 2, 0, 0, 16, 16, new float[]{1f, 1f, 9f, 9f});
        referenceGeoCoding = new TiePointGeoCoding(lat, lon);
    }

    @Test
    public void testGetGeoPos_IdentityTransform() throws Exception {
        final PixelPos pixelPos = new PixelPos(6, 6);
        final GeoPos referenceGeoPos = new GeoPos();
        referenceGeoCoding.getGeoPos(pixelPos, referenceGeoPos);

        final SlstrTiePointGeoCoding slstrTiePointGeoCoding =
                new SlstrTiePointGeoCoding(lat, lon, new AffineTransform2D(new AffineTransform()));
        final GeoPos slstrGeoPos = new GeoPos();
        slstrTiePointGeoCoding.getGeoPos(pixelPos, slstrGeoPos);

        assertEquals(referenceGeoPos.getLat(), slstrGeoPos.getLat(), 1e-8);
        assertEquals(referenceGeoPos.getLon(), slstrGeoPos.getLon(), 1e-8);
    }

    @Test
    public void testGetPixelPos_IdentityTransform() throws Exception {
        final GeoPos geoPos = new GeoPos(6, 6);
        final PixelPos referencePixelPos = new PixelPos();
        referenceGeoCoding.getPixelPos(geoPos, referencePixelPos);

        final SlstrTiePointGeoCoding slstrTiePointGeoCoding =
                new SlstrTiePointGeoCoding(lat, lon, new AffineTransform2D(new AffineTransform()));
        final PixelPos slstrPixelPos = new PixelPos();
        slstrTiePointGeoCoding.getPixelPos(geoPos, slstrPixelPos);

        assertEquals(referencePixelPos.getX(), slstrPixelPos.getX(), 1e-8);
        assertEquals(referencePixelPos.getY(), slstrPixelPos.getY(), 1e-8);
    }

    @Test
    public void testGetGeoPos_Transform() throws Exception {
        final PixelPos pixelPos1 = new PixelPos(4, 7);
        final PixelPos pixelPos2 = new PixelPos(6, 6);
        final PixelPos pixelPos3 = new PixelPos(7, 4);
        final GeoPos referenceGeoPos1 = new GeoPos();
        final GeoPos referenceGeoPos2 = new GeoPos();
        final GeoPos referenceGeoPos3 = new GeoPos();
        referenceGeoCoding.getGeoPos(pixelPos1, referenceGeoPos1);
        referenceGeoCoding.getGeoPos(pixelPos2, referenceGeoPos2);
        referenceGeoCoding.getGeoPos(pixelPos3, referenceGeoPos3);

        final AffineTransform transform = new AffineTransform();
        transform.scale(2.0, 2.0);
        transform.translate(1.0, 1.0);
        final SlstrTiePointGeoCoding slstrTiePointGeoCoding =
                new SlstrTiePointGeoCoding(lat, lon, new AffineTransform2D(transform));
        final GeoPos slstrGeoPos1 = new GeoPos();
        final GeoPos slstrGeoPos2 = new GeoPos();
        final GeoPos slstrGeoPos3 = new GeoPos();
        slstrTiePointGeoCoding.getGeoPos(pixelPos1, slstrGeoPos1);
        slstrTiePointGeoCoding.getGeoPos(pixelPos2, slstrGeoPos2);
        slstrTiePointGeoCoding.getGeoPos(pixelPos3, slstrGeoPos3);

        assertEquals(referenceGeoPos1.getLat() * 2 , slstrGeoPos1.getLat(), 1e-8);
        assertEquals(referenceGeoPos1.getLon() * 2, slstrGeoPos1.getLon(), 1e-8);

        assertEquals(referenceGeoPos2.getLat() * 2 , slstrGeoPos2.getLat(), 1e-8);
        assertEquals(referenceGeoPos2.getLon() * 2, slstrGeoPos2.getLon(), 1e-8);

        assertEquals(referenceGeoPos3.getLat() * 2 , slstrGeoPos3.getLat(), 1e-8);
        assertEquals(referenceGeoPos3.getLon() * 2, slstrGeoPos3.getLon(), 1e-8);
    }

    @Test
    public void testGetPixelPos_Transform() throws Exception {
        final GeoPos geoPos1 = new GeoPos(4, 7);
        final GeoPos geoPos2 = new GeoPos(6, 6);
        final GeoPos geoPos3 = new GeoPos(7, 4);
        final PixelPos referencePixelPos1 = new PixelPos();
        final PixelPos referencePixelPos2 = new PixelPos();
        final PixelPos referencePixelPos3 = new PixelPos();
        referenceGeoCoding.getPixelPos(geoPos1, referencePixelPos1);
        referenceGeoCoding.getPixelPos(geoPos2, referencePixelPos2);
        referenceGeoCoding.getPixelPos(geoPos3, referencePixelPos3);

        final AffineTransform transform = new AffineTransform();
        transform.scale(2.0, 2.0);
        transform.translate(1.0, 1.0);
        final SlstrTiePointGeoCoding slstrTiePointGeoCoding =
                new SlstrTiePointGeoCoding(lat, lon, new AffineTransform2D(transform));
        final PixelPos slstrPixelPos1 = new PixelPos();
        final PixelPos slstrPixelPos2 = new PixelPos();
        final PixelPos slstrPixelPos3 = new PixelPos();
        slstrTiePointGeoCoding.getPixelPos(geoPos1, slstrPixelPos1);
        slstrTiePointGeoCoding.getPixelPos(geoPos2, slstrPixelPos2);
        slstrTiePointGeoCoding.getPixelPos(geoPos3, slstrPixelPos3);

        assertEquals((referencePixelPos1.getX() / 2) - 1, slstrPixelPos1.getX(), 1e-8);
        assertEquals((referencePixelPos1.getY() / 2) - 1, slstrPixelPos1.getY(), 1e-8);

        assertEquals((referencePixelPos2.getX() / 2) - 1, slstrPixelPos2.getX(), 1e-8);
        assertEquals((referencePixelPos2.getY() / 2) - 1, slstrPixelPos2.getY(), 1e-8);

        assertEquals((referencePixelPos3.getX() / 2) - 1, slstrPixelPos3.getX(), 1e-8);
        assertEquals((referencePixelPos3.getY() / 2) - 1, slstrPixelPos3.getY(), 1e-8);
    }

}