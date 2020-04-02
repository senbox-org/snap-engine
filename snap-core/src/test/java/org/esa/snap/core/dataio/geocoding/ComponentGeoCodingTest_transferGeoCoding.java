package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.geocoding.forward.PixelInterpolatingForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointBilinearForward;
import org.esa.snap.core.dataio.geocoding.forward.TiePointSplineForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelQuadTreeInverse;
import org.esa.snap.core.dataio.geocoding.inverse.TiePointInverse;
import org.esa.snap.core.datamodel.*;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingTestUtils.*;
import static org.junit.Assert.*;

public class ComponentGeoCodingTest_transferGeoCoding {

    private Product srcProduct;

    @Before
    public void setUp() {
        srcProduct = createProduct();
    }

    @Test
    public void testTransferGeoCoding_TP_BILINEAR__NoSubsetDefinition() throws IOException {
        //preparation
        final boolean bilinear = true;
        boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(srcProduct, bilinear, antimeridian);

        final ProductSubsetDef subsetDef = null;

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getKey(), TiePointBilinearForward.KEY);
        assertEquals(geoCoding.getInverseCoding().getKey(), TiePointInverse.KEY);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertFalse(geoCoding.isCrossingMeridianAt180());
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getKey(), subsetGC.getForwardCoding().getKey());
        assertEquals(geoCoding.getInverseCoding().getKey(), subsetGC.getInverseCoding().getKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertFalse(subsetGC.isCrossingMeridianAt180());
        doForwardBackwardTest(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_TP_BILINEAR() throws IOException {
        //preparation
        final boolean bilinear = true;
        boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(srcProduct, bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getKey(), TiePointBilinearForward.KEY);
        assertEquals(geoCoding.getInverseCoding().getKey(), TiePointInverse.KEY);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertFalse(geoCoding.isCrossingMeridianAt180());
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getKey(), subsetGC.getForwardCoding().getKey());
        assertEquals(geoCoding.getInverseCoding().getKey(), subsetGC.getInverseCoding().getKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertFalse(subsetGC.isCrossingMeridianAt180());
        doForwardBackwardTest(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_TP_BILINEAR_SUBSAMPLING_3() throws IOException {
        //preparation
        final boolean bilinear = true;
        boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(srcProduct, bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        final int subSampling = 3;
        subsetDef.setSubSampling(subSampling, subSampling);
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getKey(), TiePointBilinearForward.KEY);
        assertEquals(geoCoding.getInverseCoding().getKey(), TiePointInverse.KEY);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getKey(), subsetGC.getForwardCoding().getKey());
        assertEquals(geoCoding.getInverseCoding().getKey(), subsetGC.getInverseCoding().getKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(subsetGC, subSampling);
    }

    @Test
    public void testTransferGeoCoding_TP_BILINEAR_with_ANTIMERIDIAN() throws IOException {
        //preparation
        final boolean bilinear = true;
        boolean antimeridian = true;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(srcProduct, bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getKey(), TiePointBilinearForward.KEY);
        assertEquals(geoCoding.getInverseCoding().getKey(), TiePointInverse.KEY);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), true);
        doForwardBackwardTestWithAntimeridian(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getKey(), subsetGC.getForwardCoding().getKey());
        assertEquals(geoCoding.getInverseCoding().getKey(), subsetGC.getInverseCoding().getKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), true);
        doForwardBackwardTestWithAntimeridian(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_TP_SPLINE() throws IOException {
        //preparation
        final boolean bilinear = false; // = SPLINE instead
        boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(srcProduct, bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getKey(), TiePointSplineForward.KEY);
        assertEquals(geoCoding.getInverseCoding().getKey(), TiePointInverse.KEY);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getKey(), subsetGC.getForwardCoding().getKey());
        assertEquals(geoCoding.getInverseCoding().getKey(), subsetGC.getInverseCoding().getKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_TP_SPLINE_SUBSAMPLING() throws IOException {
        //preparation
        final boolean bilinear = false; // = SPLINE instead
        boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(srcProduct, bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        final int subSampling = 4;
        subsetDef.setSubSampling(subSampling, subSampling);
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getKey(), TiePointSplineForward.KEY);
        assertEquals(geoCoding.getInverseCoding().getKey(), TiePointInverse.KEY);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getKey(), subsetGC.getForwardCoding().getKey());
        assertEquals(geoCoding.getInverseCoding().getKey(), subsetGC.getInverseCoding().getKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(subsetGC, subSampling);
    }

    @Test
    public void testTransferGeoCoding_TP_SPLINE_with_ANTIMERIDIAN() throws IOException {
        //preparation
        final boolean bilinear = false; // = SPLINE instead
        boolean antimeridian = true;
        final ComponentGeoCoding geoCoding = initializeWithTiePoints(srcProduct, bilinear, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getKey(), TiePointSplineForward.KEY);
        assertEquals(geoCoding.getInverseCoding().getKey(), TiePointInverse.KEY);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), true);
        doForwardBackwardTestWithAntimeridian(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getKey(), subsetGC.getForwardCoding().getKey());
        assertEquals(geoCoding.getInverseCoding().getKey(), subsetGC.getInverseCoding().getKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), true);
        doForwardBackwardTestWithAntimeridian(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_PIXELBASED() throws IOException {
        //preparation
        final boolean interpolating = true;
        final boolean quadTree = true;
        final boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithBands(srcProduct, interpolating, quadTree, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getKey(), PixelInterpolatingForward.KEY);
        assertEquals(geoCoding.getInverseCoding().getKey(), PixelQuadTreeInverse.KEY_INTERPOLATING);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getKey(), subsetGC.getForwardCoding().getKey());
        assertEquals(geoCoding.getInverseCoding().getKey(), subsetGC.getInverseCoding().getKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(subsetGC);
    }

    @Test
    public void testTransferGeoCoding_PIXELBASED_Subsampling_2() throws IOException {
        //preparation
        final boolean interpolating = true;
        final boolean quadTree = true;
        final boolean antimeridian = false;
        final ComponentGeoCoding geoCoding = initializeWithBands(srcProduct, interpolating, quadTree, antimeridian);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        final int subSampling = 2;
        subsetDef.setSubSampling(subSampling, subSampling);
        subsetDef.addNodeName("dummy");

        //execution
        final Product productSubset = srcProduct.createSubset(subsetDef, "subset", "desc");

        //verification
        assertTrue(geoCoding instanceof ComponentGeoCoding);
        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(geoCoding.getForwardCoding().getKey(), PixelInterpolatingForward.KEY);
        assertEquals(geoCoding.getInverseCoding().getKey(), PixelQuadTreeInverse.KEY_INTERPOLATING);
        assertEquals(GeoChecks.ANTIMERIDIAN, geoCoding.getGeoChecks());
        assertEquals(geoCoding.isCrossingMeridianAt180(), false);
        doForwardBackwardTest(geoCoding);

        assertTrue(productSubset.getSceneGeoCoding() instanceof ComponentGeoCoding);
        final ComponentGeoCoding subsetGC = (ComponentGeoCoding) productSubset.getSceneGeoCoding();
        assertEquals(DefaultGeographicCRS.WGS84, subsetGC.getGeoCRS());
        assertTrue(subsetGC.getImageCRS() instanceof DefaultDerivedCRS);
        assertNotSame(geoCoding.getForwardCoding(), subsetGC.getForwardCoding());
        assertNotSame(geoCoding.getInverseCoding(), subsetGC.getInverseCoding());
        assertEquals(geoCoding.getForwardCoding().getKey(), subsetGC.getForwardCoding().getKey());
        assertEquals(geoCoding.getInverseCoding().getKey(), subsetGC.getInverseCoding().getKey());
        assertEquals(GeoChecks.ANTIMERIDIAN, subsetGC.getGeoChecks());
        assertEquals(subsetGC.isCrossingMeridianAt180(), false);

        GeoPos geoPos;
        PixelPos pixelPos;
        PixelPos ppInverse;

        pixelPos = new PixelPos(0.5, 0.5);

        geoPos = subsetGC.getGeoPos(pixelPos, null);
        assertEquals(-19.4, geoPos.lon, 1e-12);
        assertEquals(48.6, geoPos.lat, 1e-12);
        ppInverse = subsetGC.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);

        pixelPos = new PixelPos(3.5, 3.5);
        geoPos = subsetGC.getGeoPos(pixelPos, null);
        assertEquals(-15.8, geoPos.lon, 1e-12);
        assertEquals(40.2, geoPos.lat, 1e-12);
        ppInverse = subsetGC.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);

        pixelPos = new PixelPos(2.5, 2.5);
        geoPos = subsetGC.getGeoPos(pixelPos, null);
        assertEquals(-17.0, geoPos.lon, 1e-12);
        assertEquals(43.0, geoPos.lat, 1e-12);
        ppInverse = subsetGC.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);
    }

    private void doForwardBackwardTest(ComponentGeoCoding gc) {
        doForwardBackwardTest(gc, 1);
    }

    private void doForwardBackwardTest(ComponentGeoCoding gc, int subSampling) {
        GeoPos geoPos;
        PixelPos pixelPos;
        PixelPos ppInverse;

        pixelPos = newPixelPos(0.5, 0.5, subSampling);

        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(-20, geoPos.lon, 1e-12);
        assertEquals(50, geoPos.lat, 1e-12);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);

        pixelPos = newPixelPos(5.5, 5.5, subSampling);
        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(-17, geoPos.lon, 1e-12);
        assertEquals(43, geoPos.lat, 1e-12);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);

        pixelPos = newPixelPos(3.5, 3.5, subSampling);
        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(-18.2, geoPos.lon, 1e-12);
        assertEquals(45.8, geoPos.lat, 1e-12);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-8);
        assertEquals(pixelPos.y, ppInverse.y, 1e-8);
    }

    private void doForwardBackwardTestWithAntimeridian(ComponentGeoCoding gc) {
        doForwardBackwardTestWithAntimeridian(gc, 1);
    }

    private void doForwardBackwardTestWithAntimeridian(ComponentGeoCoding gc, int subSampling) {
        GeoPos geoPos;
        PixelPos pixelPos;
        PixelPos ppInverse;

        pixelPos = newPixelPos(0.5, 0.5, subSampling);
        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(160.0, geoPos.lon, 1e-6);
        assertEquals(50.0, geoPos.lat, 1e-15);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-6);
        assertEquals(pixelPos.y, ppInverse.y, 1e-6);

        pixelPos = newPixelPos(5.5, 5.5, subSampling);
        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(163.0, geoPos.lon, 1e-6);
        assertEquals(43.0, geoPos.lat, 1e-15);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-6);
        assertEquals(pixelPos.y, ppInverse.y, 1e-6);

        pixelPos = newPixelPos(3.5, 3.5, subSampling);
        geoPos = gc.getGeoPos(pixelPos, null);
        assertEquals(161.8, geoPos.lon, 1e-3);
        assertEquals(45.8, geoPos.lat, 1e-15);
        ppInverse = gc.getPixelPos(geoPos, null);
        assertEquals(pixelPos.x, ppInverse.x, 1e-3);
        assertEquals(pixelPos.y, ppInverse.y, 1e-4);
    }

    private PixelPos newPixelPos(double x, double y, int subSampling) {
        final double rezi = 1.0 / subSampling;
        final double x1 = (x - 0.5) * rezi + 0.5;
        final double y1 = (y - 0.5) * rezi + 0.5;
        return new PixelPos(x1, y1);
    }


}
