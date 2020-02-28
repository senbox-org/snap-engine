package org.esa.snap.dataio.netcdf.metadata.profiles.beam;

import org.esa.snap.core.dataio.geocoding.ComponentGeoCoding;
import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileReadContext;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.io.IOException;

import static org.esa.snap.core.dataio.Constants.GEOCODING;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingTestUtils.createProduct;
import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingTestUtils.initializeWithBands;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BeamGeocodingPartTest_ComponentGeocoding {

    private BeamGeocodingPart part;
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
        part = new BeamGeocodingPart();
        product = createProduct();
        initializeWithBands(product, false, false, true);
    }

    @Test
    public void decode() throws IOException {
        //preparation
        final Attribute attribute = mock(Attribute.class);
        when(attribute.getStringValue()).thenReturn(EXPECTED);

        final NetcdfFile netcdfFile = mock(NetcdfFile.class);
        when(netcdfFile.findGlobalAttribute(GEOCODING)).thenReturn(attribute);

        ProfileReadContext ctxR = mock(ProfileReadContext.class);
        when(ctxR.getNetcdfFile()).thenReturn(netcdfFile);

        final GeoCoding exp = product.getSceneGeoCoding();
        assertNotNull(exp);
        assertTrue(exp instanceof ComponentGeoCoding);
        final ComponentGeoCoding expected = (ComponentGeoCoding) exp;
        product.setSceneGeoCoding(null);
        assertNull(product.getSceneGeoCoding());

        //execution
        part.decode(ctxR, product);

        //verification
        final GeoCoding act = product.getSceneGeoCoding();
        assertNotNull(act);
        assertTrue(act instanceof ComponentGeoCoding);
        final ComponentGeoCoding actual = (ComponentGeoCoding) act;

        assertNotSame(expected, actual);
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
    public void preEncode() throws IOException {
        //preparation
        final NVariable nVariable = mock(NVariable.class);

        NFileWriteable nFileWriteable = mock(NFileWriteable.class);
        when(nFileWriteable.findVariable(anyString())).thenReturn(null);
        when(nFileWriteable.addVariable(any(), any(), any(), any())).thenReturn(nVariable);

        ProfileWriteContext ctxW = mock(ProfileWriteContext.class);
        when(ctxW.getNetcdfFileWriteable()).thenReturn(nFileWriteable);

        //execution
        part.preEncode(ctxW, product);

        //verification
        final ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> xmlStr = ArgumentCaptor.forClass(String.class);
        verify(nFileWriteable).addGlobalAttribute(name.capture(), xmlStr.capture());
        assertEquals(name.getValue(), GEOCODING);
        assertThat(strip(xmlStr.getValue()), is(equalTo(strip(EXPECTED))));
    }

    private String strip(String value) {
        return value
                .replace("\n", "")
                .replace("\r", "")
                .replaceAll("> *<", "><");

    }
}