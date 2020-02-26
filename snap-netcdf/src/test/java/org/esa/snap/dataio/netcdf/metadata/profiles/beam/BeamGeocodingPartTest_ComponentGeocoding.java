package org.esa.snap.dataio.netcdf.metadata.profiles.beam;

import static org.esa.snap.core.dataio.geocoding.ComponentGeoCodingTestUtils.*;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.dataio.netcdf.ProfileWriteContext;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.mockito.Mockito.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

public class BeamGeocodingPartTest_ComponentGeocoding {

    private BeamGeocodingPart part;
    private ProfileWriteContext ctx;
    private Product product;
    private NFileWriteable nFileWriteable;

    @Before
    public void setUp() throws Exception {
        final NVariable nVariable = mock(NVariable.class);

        nFileWriteable = mock(NFileWriteable.class);
        when(nFileWriteable.findVariable(anyString())).thenReturn(null);
        when(nFileWriteable.addVariable(any(), any(), any(), any())).thenReturn(nVariable);

        ctx = mock(ProfileWriteContext.class);
        when(ctx.getNetcdfFileWriteable()).thenReturn(nFileWriteable);

        part = new BeamGeocodingPart();
        product = createProduct();
        initializeWithBands(product, false, false, true);
    }

    @Test
    public void decode() {
        // TODO: 26.02.2020 SE -- implement
    }

    @Test
    public void preEncode() throws IOException {

        part.preEncode(ctx, product);

        final ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> xmlStr = ArgumentCaptor.forClass(String.class);
        verify(nFileWriteable).addGlobalAttribute(name.capture(), xmlStr.capture());
        assertEquals(name.getValue(), BeamGeocodingPart.SNAP_GEOCODING);
        final String expected = "<ComponentGeoCoding>" +
                                "  <ForwardCodingKey>FWD_PIXEL</ForwardCodingKey>" +
                                "  <InverseCodingKey>INV_PIXEL_GEO_INDEX</InverseCodingKey>" +
                                "  <GeoChecks>ANTIMERIDIAN</GeoChecks>" +
                                "  <GeoCRS>GEOGCS[\"WGS84(DD)\", DATUM[\"WGS84\", SPHEROID[\"WGS84\", 6378137.0, 298.257223563]], PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Geodetic longitude\", EAST], AXIS[\"Geodetic latitude\", NORTH]]</GeoCRS>" +
                                "  <LonVariableName>Lon</LonVariableName>" +
                                "  <LatVariableName>Lat</LatVariableName>" +
                                "  <RasterResolutionKm>300.0</RasterResolutionKm>" +
                                "</ComponentGeoCoding>";
        assertThat(strip(xmlStr.getValue()), is(equalTo(strip(expected))));
    }

    private String strip(String value) {
        return value
                .replace("\n", "")
                .replace("\r", "")
                .replaceAll("> *<", "><");

    }
}