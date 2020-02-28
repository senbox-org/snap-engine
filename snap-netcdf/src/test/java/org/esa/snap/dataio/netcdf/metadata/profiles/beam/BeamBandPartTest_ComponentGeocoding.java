package org.esa.snap.dataio.netcdf.metadata.profiles.beam;

import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.io.IOException;

import static org.esa.snap.core.dataio.Constants.GEOCODING;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingTestUtils.createProduct;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingTestUtils.initializeWithBands;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BeamBandPartTest_ComponentGeocoding {

    private BeamBandPart part;
    private Product product;
    public static final String EXPECTED = "<ComponentGeoCoding>" +
            "  <ForwardCodingKey>FWD_PIXEL</ForwardCodingKey>" +
            "  <InverseCodingKey>INV_PIXEL_GEO_INDEX</InverseCodingKey>" +
            "  <GeoChecks>ANTIMERIDIAN</GeoChecks>" +
            "  <GeoCRS>GEOGCS[\"WGS84(DD)\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH]]</GeoCRS>" +
            "  <LonVariableName>Lon</LonVariableName>" +
            "  <LatVariableName>Lat</LatVariableName>" +
            "  <RasterResolutionKm>300.0</RasterResolutionKm>" +
            "  <OffsetX>0.5</OffsetX><OffsetY>0.5</OffsetY><SubsamplingX>1.0</SubsamplingX><SubsamplingY>1.0</SubsamplingY>" +
            "</ComponentGeoCoding>";

    @Before
    public void setUp() throws Exception {
        part = new BeamBandPart();
        product = createProduct();
        initializeWithBands(product, false, false, true);
    }

    @Test
    public void decode() throws IOException {
        //preparation
        final ProfileReadContext ctxR = mock(ProfileReadContext.class);
        final Variable variable = mock(Variable.class);
        when(variable.findAttribute(GEOCODING)).thenReturn(new Attribute(GEOCODING, EXPECTED));

        final GeoCoding exp = product.getSceneGeoCoding();
        assertNotNull(exp);
        assertTrue(exp instanceof ComponentGeoCoding);

        final Band band = product.getBand("dummy");
        assertSame(band.getGeoCoding(), exp);

        final ComponentGeoCoding expected = (ComponentGeoCoding) exp;

        //execution
        part.setGeoCoding(ctxR, product, variable, band);

        //verification
        assertNotNull(band.getGeoCoding());
        assertTrue(band.getGeoCoding() instanceof ComponentGeoCoding);
        assertNotSame(band.getGeoCoding(), product.getSceneGeoCoding());
        final ComponentGeoCoding actual = (ComponentGeoCoding) band.getGeoCoding();

        assertEquals(expected.getForwardCoding().getKey(), actual.getForwardCoding().getKey());
        assertEquals(expected.getInverseCoding().getKey(), actual.getInverseCoding().getKey());
        assertEquals(expected.isCrossingMeridianAt180(), actual.isCrossingMeridianAt180());
        assertEquals(expected.getGeoCRS().toWKT(), actual.getGeoCRS().toWKT());
        final GeoRaster expGeoRaster = expected.getGeoRaster();
        final GeoRaster actGeoRaster = actual.getGeoRaster();
        assertNotSame(expGeoRaster, actGeoRaster);
        assertEquals(expGeoRaster.getLonVariableName(), actGeoRaster.getLonVariableName());
        assertEquals(expGeoRaster.getLatVariableName(), actGeoRaster.getLatVariableName());
        assertNotSame(expGeoRaster.getLongitudes(), actGeoRaster.getLongitudes());
        assertArrayEquals(expGeoRaster.getLongitudes(), actGeoRaster.getLongitudes(), Double.MIN_VALUE);
        assertNotSame(expGeoRaster.getLatitudes(), actGeoRaster.getLatitudes());
        assertArrayEquals(expGeoRaster.getLatitudes(), actGeoRaster.getLatitudes(), Double.MIN_VALUE);
        assertEquals(expGeoRaster.getRasterResolutionInKm(), actGeoRaster.getRasterResolutionInKm(), Double.MIN_VALUE);
        assertEquals(expGeoRaster.getRasterWidth(), actGeoRaster.getRasterWidth());
        assertEquals(expGeoRaster.getRasterHeight(), actGeoRaster.getRasterHeight());
        assertEquals(expGeoRaster.getSceneWidth(), actGeoRaster.getSceneWidth());
        assertEquals(expGeoRaster.getSceneHeight(), actGeoRaster.getSceneHeight());
        assertEquals(expGeoRaster.getOffsetX(), actGeoRaster.getOffsetX(), Double.MIN_VALUE);
        assertEquals(expGeoRaster.getOffsetY(), actGeoRaster.getOffsetY(), Double.MIN_VALUE);
        assertEquals(expGeoRaster.getSubsamplingX(), actGeoRaster.getSubsamplingX(), Double.MIN_VALUE);
        assertEquals(expGeoRaster.getSubsamplingY(), actGeoRaster.getSubsamplingY(), Double.MIN_VALUE);
    }

    @Test
    public void encodeGeoCoding() throws IOException {
        //preparation
        final NVariable nVariable = mock(NVariable.class);

        product.getBand("dummy").setGeoCoding(product.getSceneGeoCoding());
        product.setSceneGeoCoding(null);

        assertNull(product.getSceneGeoCoding());
        assertNotNull(product.getBand("dummy"));
        assertNotNull(product.getBand("dummy").getGeoCoding());
        assertThat(product.getBand("dummy").getGeoCoding(), is(instanceOf(ComponentGeoCoding.class)));

        //execution
        part.encodeGeoCoding(null, product.getBand("dummy"), product, nVariable);

        //verification
        final ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> xmlStr = ArgumentCaptor.forClass(String.class);
        verify(nVariable).addAttribute(name.capture(), xmlStr.capture());
        assertEquals(name.getValue(), GEOCODING);
        assertEquals(strip(xmlStr.getValue()), strip(EXPECTED));
    }

    private String strip(String value) {
        return value
                .replace("\n", "")
                .replace("\r", "")
                .replaceAll("> *<", "><");
    }
}