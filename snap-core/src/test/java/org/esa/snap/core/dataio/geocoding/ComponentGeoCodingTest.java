package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.*;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.operation.MathTransform;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ComponentGeoCodingTest {

    private ForwardCoding forwardCoding;
    private InverseCoding inverseCoding;

    @Before
    public void setUp() {
        forwardCoding = mock(ForwardCoding.class);
        inverseCoding = mock(InverseCoding.class);
    }

    @Test
    public void testConstructWithDefaultCRS() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, forwardCoding, inverseCoding);

        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(forwardCoding, geoCoding.getForwardCoding());
        assertEquals(inverseCoding, geoCoding.getInverseCoding());
        assertEquals(GeoChecks.NONE, geoCoding.getGeoChecks());
    }

    @Test
    public void testConstructWith_CRS() {
        final DefaultGeographicCRS crs = new DefaultGeographicCRS(DefaultGeodeticDatum.WGS84, DefaultEllipsoidalCS.GEODETIC_2D);
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, forwardCoding, inverseCoding, crs);

        assertSame(crs, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(forwardCoding, geoCoding.getForwardCoding());
        assertEquals(inverseCoding, geoCoding.getInverseCoding());
        assertEquals(GeoChecks.NONE, geoCoding.getGeoChecks());
    }

    @Test
    public void testConstructWithDefaultCRS_anti_meridian_poles() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, forwardCoding, inverseCoding, GeoChecks.POLES);

        assertEquals(DefaultGeographicCRS.WGS84, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(forwardCoding, geoCoding.getForwardCoding());
        assertEquals(inverseCoding, geoCoding.getInverseCoding());
        assertEquals(GeoChecks.POLES, geoCoding.getGeoChecks());
    }

    @Test
    public void testConstructWith_CRS_anti_meridian_poles() {
        final DefaultGeographicCRS crs = new DefaultGeographicCRS(DefaultGeodeticDatum.WGS84, DefaultEllipsoidalCS.GEODETIC_2D);
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, forwardCoding, inverseCoding, GeoChecks.POLES, crs);

        assertSame(crs, geoCoding.getGeoCRS());
        assertTrue(geoCoding.getImageCRS() instanceof DefaultDerivedCRS);
        assertEquals(forwardCoding, geoCoding.getForwardCoding());
        assertEquals(inverseCoding, geoCoding.getInverseCoding());
        assertEquals(GeoChecks.POLES, geoCoding.getGeoChecks());
    }

    @Test
    public void testCanGetGeoPos_noForward() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, null, null);

        assertFalse(geoCoding.canGetGeoPos());
    }

    @Test
    public void testCanGetGeoPos_forward() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, forwardCoding, null);

        assertTrue(geoCoding.canGetGeoPos());
    }

    @Test
    public void testCanGetPixelPos_noInverse() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, null, null);

        assertFalse(geoCoding.canGetPixelPos());
    }

    @Test
    public void testCanGetPixelPos_inverse() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, null, inverseCoding);

        assertTrue(geoCoding.canGetPixelPos());
    }

    @Test
    public void testDispose_bothCodings() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, forwardCoding, inverseCoding);

        geoCoding.dispose();

        verify(forwardCoding, times(1)).dispose();
        verify(inverseCoding, times(1)).dispose();
        verifyNoMoreInteractions(forwardCoding);
        verifyNoMoreInteractions(inverseCoding);
    }

    @Test
    public void testDispose_onlyForwardCoding() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, forwardCoding, null);

        geoCoding.dispose();

        verify(forwardCoding, times(1)).dispose();
        verifyNoMoreInteractions(forwardCoding);
    }

    @Test
    public void testDispose_onlyInverseCodings() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, null, inverseCoding);

        geoCoding.dispose();

        verify(inverseCoding, times(1)).dispose();
        verifyNoMoreInteractions(inverseCoding);
    }

    @Test
    public void testGetGeoPos_noForwardCoding() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, null, null);

        final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(23, 45), null);
        assertNotNull(geoPos);
        assertFalse(geoPos.isValid());
    }

    @Test
    public void testGetGeoPos() {
        when(forwardCoding.getGeoPos(any(), any())).thenReturn(new GeoPos(-23.6, 108.22));

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, forwardCoding, null);

        final GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(23, 45), null);
        assertNotNull(geoPos);
        assertEquals(-23.6, geoPos.lat, 1e-8);
        assertEquals(108.22, geoPos.lon, 1e-8);

        verify(forwardCoding, times(1)).getGeoPos(any(), any());
        verifyNoMoreInteractions(forwardCoding);
    }

    @Test
    public void testGetPixelPos_noInverseCoding() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, null, null);

        final PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(4.09, 28.243), null);
        assertNotNull(pixelPos);
        assertFalse(pixelPos.isValid());
    }

    @Test
    public void testGetPixelPos() {
        when(inverseCoding.getPixelPos(any(), any())).thenReturn(new PixelPos(12, 13));

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, null, inverseCoding);

        final PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(4.09, 28.243), null);
        assertNotNull(pixelPos);
        assertEquals(12.0, pixelPos.x, 1e-8);
        assertEquals(13.0, pixelPos.y, 1e-8);

        verify(inverseCoding, times(1)).getPixelPos(any(), any());
        verifyNoMoreInteractions(inverseCoding);
    }

    @Test
    public void testGetImageToMapTransform_default() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, null, null);

        final MathTransform imageToMapTransform = geoCoding.getImageToMapTransform();
        assertNotNull(imageToMapTransform);
        assertEquals("PARAM_MT[]", imageToMapTransform.toWKT());
    }

    @Test
    public void testInitialize_noChecks() {
        final GeoRaster geoRaster = new GeoRaster(null, null, null, null, 5, 6, 7.5);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding);

        geoCoding.initialize();

        verify(forwardCoding, times(1)).initialize(geoRaster, false, new PixelPos[0]);
        verify(inverseCoding, times(1)).initialize(geoRaster, false, new PixelPos[0]);
        verifyNoMoreInteractions(forwardCoding);
        verifyNoMoreInteractions(inverseCoding);
    }

    @Test
    public void testInitialize_noChecks_noForward() {
        final GeoRaster geoRaster = new GeoRaster(null, null, null, null, 5, 6, 7.5);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, null, inverseCoding);

        geoCoding.initialize();

        verify(inverseCoding, times(1)).initialize(geoRaster, false, new PixelPos[0]);
        verifyNoMoreInteractions(inverseCoding);
    }

    @Test
    public void testInitialize_noChecks_noInverse() {
        final GeoRaster geoRaster = new GeoRaster(null, null, null, null, 5, 6, 7.5);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, null);

        geoCoding.initialize();

        verify(forwardCoding, times(1)).initialize(geoRaster, false, new PixelPos[0]);
        verifyNoMoreInteractions(forwardCoding);
    }

    @Test
    public void testInitialize_checks_anti_meridian_not_contained() {
        final GeoRaster geoRaster = TestData.get_MER_RR();
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.ANTIMERIDIAN);

        geoCoding.initialize();

        verify(forwardCoding, times(1)).initialize(geoRaster, false, new PixelPos[0]);
        verify(inverseCoding, times(1)).initialize(geoRaster, false, new PixelPos[0]);
        verifyNoMoreInteractions(forwardCoding);
        verifyNoMoreInteractions(inverseCoding);
    }

    @Test
    public void testInitialize_checks_anti_meridian_contained() {
        final GeoRaster geoRaster = new GeoRaster(AMSUB.AMSUB_ANTI_MERID_LON, AMSUB.AMSUB_ANTI_MERID_LAT, null, null, 31, 31, 16.0);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.ANTIMERIDIAN);

        geoCoding.initialize();

        verify(forwardCoding, times(1)).initialize(geoRaster, true, new PixelPos[0]);
        verify(inverseCoding, times(1)).initialize(geoRaster, true, new PixelPos[0]);
        verifyNoMoreInteractions(forwardCoding);
        verifyNoMoreInteractions(inverseCoding);
    }

    @Test
    public void testInitialize_checks_poles_anti_meridian_not_contained() {
        final GeoRaster geoRaster = TestData.get_MER_RR();
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.POLES);

        geoCoding.initialize();

        verify(forwardCoding, times(1)).initialize(geoRaster, false, new PixelPos[0]);
        verify(inverseCoding, times(1)).initialize(geoRaster, false, new PixelPos[0]);
        verifyNoMoreInteractions(forwardCoding);
        verifyNoMoreInteractions(inverseCoding);
    }

    @Test
    public void testInitialize_checks_poles_anti_meridian() {
        final GeoRaster geoRaster = new GeoRaster(AMSUB.AMSUB_POLE_LON, AMSUB.AMSUB_POLE_LAT, null, null, 25, 25, 16.0);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.POLES);

        geoCoding.initialize();

        final PixelPos[] poleLocations = new PixelPos[]{new PixelPos(22.0, 8.0), new PixelPos(22.0, 9.0)};
        verify(forwardCoding, times(1)).initialize(geoRaster, true, poleLocations);
        verify(inverseCoding, times(1)).initialize(geoRaster, true, poleLocations);
        verifyNoMoreInteractions(forwardCoding);
        verifyNoMoreInteractions(inverseCoding);
    }

    @Test
    public void testIsCrossingMeridianAt180_uninitialized() {
        final GeoRaster geoRaster = TestData.get_MER_RR();

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding);

        try {
            geoCoding.isCrossingMeridianAt180();
            fail("IllegalStateException expected");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void testIsCrossingMeridianAt180_not() {
        final GeoRaster geoRaster = TestData.get_MER_RR();

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.ANTIMERIDIAN);
        geoCoding.initialize();

        assertFalse(geoCoding.isCrossingMeridianAt180());
    }

    @Test
    public void testIsCrossingMeridianAt180_is() {
        final GeoRaster geoRaster = new GeoRaster(AMSUB.AMSUB_POLE_LON, AMSUB.AMSUB_POLE_LAT, null, null, 25, 25, 16.0);

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.ANTIMERIDIAN);
        geoCoding.initialize();

        assertTrue(geoCoding.isCrossingMeridianAt180());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testTransferRequiredRaster_tiePoints() {
        final GeoRaster geoRaster = TestData.get_MER_RR();

        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.NONE);
        geoCoding.initialize();

        final Product sourceProduct = new Product("source", "TEST", 5, 5);
        sourceProduct.addTiePointGrid(new TiePointGrid(geoRaster.getLonVariableName(), geoRaster.getRasterWidth(), geoRaster.getRasterHeight(),
                geoRaster.getOffsetX(), geoRaster.getOffsetY(), geoRaster.getSubsamplingX(), geoRaster.getSubsamplingY(),
                RasterUtils.toFloat(geoRaster.getLongitudes())));

        sourceProduct.addTiePointGrid(new TiePointGrid(geoRaster.getLatVariableName(), geoRaster.getRasterWidth(), geoRaster.getRasterHeight(),
                geoRaster.getOffsetX(), geoRaster.getOffsetY(), geoRaster.getSubsamplingX(), geoRaster.getSubsamplingY(),
                RasterUtils.toFloat(geoRaster.getLatitudes())));

        final Scene sourceScene = SceneFactory.createScene(sourceProduct);

        final Product targetProduct = new Product("target", "TEST", 5, 5);
        final Scene targetScene = SceneFactory.createScene(targetProduct);

        geoCoding.transferRequiredRasters(sourceScene, targetScene, new ProductSubsetDef());

        final TiePointGrid longitude = targetScene.getProduct().getTiePointGrid("longitude");
        assertNotNull(longitude);
        final TiePointGrid latitude = targetScene.getProduct().getTiePointGrid("latitude");
        assertNotNull(latitude);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testTransferRequiredRaster_geoLocationBands_subsampling() {
        final GeoRaster geoRaster = TestData.get_OLCI();
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.NONE);
        geoCoding.initialize();

        final Product sourceProduct = new Product("source", "TEST", geoRaster.getRasterWidth(), geoRaster.getRasterHeight());
        final Band lonBand = new Band(geoRaster.getLonVariableName(), ProductData.TYPE_FLOAT64, geoRaster.getRasterWidth(), geoRaster.getRasterHeight());
        lonBand.setRasterData(ProductData.createInstance(geoRaster.getLongitudes()));
        final Band latBand = new Band(geoRaster.getLatVariableName(), ProductData.TYPE_FLOAT64, geoRaster.getRasterWidth(), geoRaster.getRasterHeight());
        lonBand.setRasterData(ProductData.createInstance(geoRaster.getLatitudes()));
        sourceProduct.addBand(lonBand);
        sourceProduct.addBand(latBand);

        final Scene sourceScene = SceneFactory.createScene(sourceProduct);

        final Product targetProduct = new Product("target", "TEST", 16, 18);
        final Scene targetScene = SceneFactory.createScene(targetProduct);

        final ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setSubSampling(2, 2);

        geoCoding.transferRequiredRasters(sourceScene, targetScene, subsetDef);

        final Band longitude = targetScene.getProduct().getBand("longitude");
        assertNotNull(longitude);

        final Band latitude = targetScene.getProduct().getBand("latitude");
        assertNotNull(latitude);
    }

    @Test
    public void testCloneGeoRaster() {
        final GeoRaster geoRaster = TestData.get_OLCI();
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(geoRaster, forwardCoding, inverseCoding, GeoChecks.NONE);
        geoCoding.initialize();

        final GeoRaster cloned = geoCoding.cloneGeoRaster("himpelchen", "pimpelchen");
        assertEquals(geoRaster.getRasterWidth(), cloned.getRasterWidth());
        assertEquals(geoRaster.getRasterHeight(), cloned.getRasterHeight());
        assertEquals("himpelchen", cloned.getLonVariableName());
        assertEquals("pimpelchen", cloned.getLatVariableName());
        assertArrayEquals(geoRaster.getLongitudes(), cloned.getLongitudes(), 1e-8);
        assertArrayEquals(geoRaster.getLatitudes(), cloned.getLatitudes(), 1e-8);
        assertEquals(geoRaster.getSceneWidth(), cloned.getSceneWidth());
        assertEquals(geoRaster.getSceneHeight(), cloned.getSceneHeight());
        assertEquals(geoRaster.getRasterResolutionInKm(), cloned.getRasterResolutionInKm(), 1e-8);
        assertEquals(geoRaster.getOffsetX(), cloned.getOffsetX(), 1e-8);
        assertEquals(geoRaster.getOffsetY(), cloned.getOffsetY(), 1e-8);
        assertEquals(geoRaster.getSubsamplingX(), cloned.getSubsamplingX(), 1e-8);
        assertEquals(geoRaster.getSubsamplingY(), cloned.getSubsamplingY(), 1e-8);
    }

    @Test
    public void testGetDatum() {
        final ComponentGeoCoding geoCoding = new ComponentGeoCoding(null, forwardCoding, inverseCoding);

        try {
            geoCoding.getDatum();
            fail("NotImplementedException expected");
        } catch (NotImplementedException expected) {
        }
    }
}
