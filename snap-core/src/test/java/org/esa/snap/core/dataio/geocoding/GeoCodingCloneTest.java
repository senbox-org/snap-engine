package org.esa.snap.core.dataio.geocoding;

import org.esa.snap.core.dataio.geocoding.forward.PixelForward;
import org.esa.snap.core.dataio.geocoding.inverse.PixelGeoIndexInverse;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class GeoCodingCloneTest {

    @Test
    public void testClone_noCodings() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();

        final ComponentGeoCoding original = new ComponentGeoCoding(geoRaster, null, null);
        original.initialize();

        final ComponentGeoCoding clone = (ComponentGeoCoding) original.clone();
        final GeoRaster cloneRaster = clone.getGeoRaster();

        assertSame(geoRaster, cloneRaster);
        assertSame(original, clone);

        assertSameCRS(original, clone);
    }

    @Test
    public void testClone_onlyInverse() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();
        final InverseCoding inverse = mock(InverseCoding.class);

        final ComponentGeoCoding original = new ComponentGeoCoding(geoRaster, null, inverse);
        original.initialize();

        final ComponentGeoCoding clone = (ComponentGeoCoding) original.clone();
        final GeoRaster cloneRaster = clone.getGeoRaster();

        assertSame(geoRaster, cloneRaster);
        assertSame(original, clone);

        verify(inverse, times(1)).initialize(any(GeoRaster.class), anyBoolean(), anyObject());
        verify(inverse, times(1)).clone();
        verifyNoMoreInteractions(inverse);
    }

    @Test
    public void testClone_onlyForward() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();
        final ForwardCoding forward = mock(ForwardCoding.class);

        final ComponentGeoCoding original = new ComponentGeoCoding(geoRaster, forward, null);
        original.initialize();

        final ComponentGeoCoding clone = (ComponentGeoCoding) original.clone();
        final GeoRaster cloneRaster = clone.getGeoRaster();

        assertSame(geoRaster, cloneRaster);
        assertSame(original, clone);

        verify(forward, times(1)).initialize(any(GeoRaster.class), anyBoolean(), anyObject());
        verify(forward, times(1)).clone();
        verifyNoMoreInteractions(forward);
    }

    @Test
    public void testClone_both_dispose() {
        final GeoRaster geoRaster = TestData.get_SLSTR_OL();
        final ForwardCoding forward = mock(ForwardCoding.class);
        final InverseCoding inverse = mock(InverseCoding.class);

        final ComponentGeoCoding original = new ComponentGeoCoding(geoRaster, forward, inverse);
        original.initialize();

        final ComponentGeoCoding clone = (ComponentGeoCoding) original.clone();

        original.dispose();

        clone.dispose();

        verify(forward, times(1)).initialize(any(GeoRaster.class), anyBoolean(), anyObject());
        verify(forward, times(1)).clone();
        verify(forward, times(1)).dispose();
        verifyNoMoreInteractions(forward);

        verify(inverse, times(1)).initialize(any(GeoRaster.class), anyBoolean(), anyObject());
        verify(inverse, times(1)).clone();
        verify(inverse, times(1)).dispose();
        verifyNoMoreInteractions(inverse);
    }

    @Test
    public void testClone_noCodings_disposeOriginal() {
        final GeoRaster geoRaster = TestData.get_OLCI();

        final double[] origLongitude = geoRaster.getLongitudes();
        final double[] origLatitude = geoRaster.getLatitudes();
        final String origLonVarName = geoRaster.getLonVariableName();
        final String origLatVarName = geoRaster.getLatVariableName();

        final ComponentGeoCoding original = new ComponentGeoCoding(geoRaster, null, null);
        original.initialize();

        final ComponentGeoCoding clone = (ComponentGeoCoding) original.clone();
        final GeoRaster cloneRaster = clone.getGeoRaster();

        original.dispose();

        assertEquals(origLongitude, cloneRaster.getLongitudes());
        assertEquals(origLatitude, cloneRaster.getLatitudes());
        assertEquals(origLonVarName, cloneRaster.getLonVariableName());
        assertEquals(origLatVarName, cloneRaster.getLatVariableName());
    }

    @Test
    public void testClone_both_integrationTest() {
        final GeoRaster geoRaster = TestData.get_OLCI();
        final ForwardCoding forward = ComponentFactory.getForward(PixelForward.KEY);
        final InverseCoding inverse = ComponentFactory.getInverse(PixelGeoIndexInverse.KEY);

        final ComponentGeoCoding coding = new ComponentGeoCoding(geoRaster, forward, inverse);
        coding.initialize();

        final PixelPos pixelPos = new PixelPos(6, 8);

        GeoPos geoPos = coding.getGeoPos(pixelPos, null);
        assertEquals(-24.163803, geoPos.lon, 1e-8);
        assertEquals(66.50602, geoPos.lat, 1e-8);

        final GeoCoding clone = coding.clone();
        geoPos = clone.getGeoPos(pixelPos, null);
        assertEquals(-24.163803, geoPos.lon, 1e-8);
        assertEquals(66.50602, geoPos.lat, 1e-8);

        coding.dispose();

        geoPos = clone.getGeoPos(pixelPos, null);
        assertEquals(-24.163803, geoPos.lon, 1e-8);
        assertEquals(66.50602, geoPos.lat, 1e-8);

        clone.dispose();
    }

    private void assertSameCRS(ComponentGeoCoding original, ComponentGeoCoding clone) {
        assertEquals(original.getMapCRS(), clone.getMapCRS());
        assertEquals(original.getGeoCRS(), clone.getGeoCRS());
        assertEquals(original.getImageCRS().toString(), clone.getImageCRS().toString());
        assertEquals(original.getImageToMapTransform().toWKT(), clone.getImageToMapTransform().toWKT());
    }

    private void assertSame(GeoRaster geoRaster, GeoRaster cloneRaster) {
        assertEquals(geoRaster.getLongitudes(), cloneRaster.getLongitudes());
        assertEquals(geoRaster.getLatitudes(), cloneRaster.getLatitudes());
        assertEquals(geoRaster.getSubsamplingX(), cloneRaster.getSubsamplingX(), 1e-8);
        assertEquals(geoRaster.getSubsamplingY(), cloneRaster.getSubsamplingY(), 1e-8);
        assertEquals(geoRaster.getOffsetX(), cloneRaster.getOffsetX(), 1e-8);
        assertEquals(geoRaster.getOffsetY(), cloneRaster.getOffsetY(), 1e-8);
        assertEquals(geoRaster.getRasterResolutionInKm(), cloneRaster.getRasterResolutionInKm(), 1e-8);
        assertEquals(geoRaster.getSceneWidth(), cloneRaster.getSceneWidth());
        assertEquals(geoRaster.getSceneHeight(), cloneRaster.getSceneHeight());
        assertEquals(geoRaster.getRasterWidth(), cloneRaster.getRasterWidth());
        assertEquals(geoRaster.getRasterHeight(), cloneRaster.getRasterHeight());
        assertEquals(geoRaster.getLonVariableName(), cloneRaster.getLonVariableName());
        assertEquals(geoRaster.getLatVariableName(), cloneRaster.getLatVariableName());
    }

    private void assertSame(ComponentGeoCoding original, ComponentGeoCoding clone) {
        assertEquals(original.getGeoChecks(), clone.getGeoChecks());
        assertEquals(original.isCrossingMeridianAt180(), clone.isCrossingMeridianAt180());
    }
}
