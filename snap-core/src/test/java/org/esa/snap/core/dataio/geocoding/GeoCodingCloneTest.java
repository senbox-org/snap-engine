package org.esa.snap.core.dataio.geocoding;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
        assertEquals(original.getForwardCoding(), clone.getForwardCoding());
        assertEquals(original.getInverseCoding(), clone.getInverseCoding());

        assertEquals(original.getGeoChecks(), clone.getGeoChecks());
        assertEquals(original.isCrossingMeridianAt180(), clone.isCrossingMeridianAt180());
    }
}
