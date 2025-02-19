package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.nc.N3Variable;
import org.esa.snap.dataio.netcdf.util.Constants;
import org.esa.snap.dataio.netcdf.util.DataTypeUtils;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CfBandPartTest {

    private Band spectralBand;
    private NetcdfFileWriter writeable;
    private Variable variable;

    @Before
    public void setUp() throws Exception {
        spectralBand = new Band("spectralBand", ProductData.TYPE_UINT16, 10, 10);
        spectralBand.setSpectralWavelength(342.5f);

        writeable = NetcdfFileWriter.createNew("not stored", false);
        writeable.addDimension("y", spectralBand.getRasterHeight());
        writeable.addDimension("x", spectralBand.getRasterWidth());
        final DataType ncDataType = DataTypeUtils.getNetcdfDataType(spectralBand.getDataType());
        variable = writeable.addVariable(spectralBand.getName(), ncDataType, "y x");
    }

    @Test
    public void testWriteWavelength() throws Exception {
        //execution
        CfBandPart.writeCfBandAttributes(spectralBand, new N3Variable(variable, writeable));

        //verification
        final Variable spectralVariable = writeable.findVariable("spectralBand");
        assertThat(spectralVariable, is(notNullValue()));

        final Attribute attWavelength = spectralVariable.findAttribute(Constants.RADIATION_WAVELENGTH);
        assertThat(attWavelength, is(notNullValue()));
        assertThat(attWavelength.getDataType(), is(equalTo(DataType.FLOAT)));
        assertThat(attWavelength.getLength(), is(1));
        assertThat(attWavelength.getNumericValue().floatValue(), is(342.5f));

        final Attribute attUnit = spectralVariable.findAttribute(Constants.RADIATION_WAVELENGTH_UNIT);
        assertThat(attUnit, is(notNullValue()));
        assertThat(attUnit.getDataType(), is(DataType.STRING));
        assertThat(attUnit.getStringValue(), is("nm"));
    }

    @Test
    public void testDecodeSpectralWavelength_noWavelengthAtt() throws IOException {
        assertThat(CfBandPart.getSpectralWavelength(variable), is(0.0f));
    }

    @Test
    @STTM("SNAP-3601")
    public void testDecodeSpectralWavelength_emptyWavelengthAtt() throws IOException {
        final Variable mockVar = mock(Variable.class);

        final Attribute attribute = mock(Attribute.class);
        when(attribute.getNumericValue()).thenReturn(null);

        when(mockVar.findAttribute(Constants.RADIATION_WAVELENGTH)).thenReturn(attribute);

        assertThat(CfBandPart.getSpectralWavelength(mockVar), is(0.0f));
    }

    @Test
    public void testDecodeSpectralWavelength_onlyWavelengthAtt() throws IOException {
        variable.addAttribute(new Attribute(Constants.RADIATION_WAVELENGTH, 23.4f));

        assertThat(CfBandPart.getSpectralWavelength(variable), is(23.4f));
    }

    @Test
    public void testDecodeSpectralWavelength_WavelengthAndUnitAttribute() throws IOException {
        variable.addAttribute(new Attribute(Constants.RADIATION_WAVELENGTH, 23.4f));
        variable.addAttribute(new Attribute(Constants.RADIATION_WAVELENGTH_UNIT, "nm"));

        assertThat(CfBandPart.getSpectralWavelength(variable), is(23.4f));
    }

    @Test
    public void testDecodeSpectralWavelength_WavelengthConversionFromMicroMeter() throws IOException {
        variable.addAttribute(new Attribute(Constants.RADIATION_WAVELENGTH, 0.0234f));
        variable.addAttribute(new Attribute(Constants.RADIATION_WAVELENGTH_UNIT, "um"));

        assertThat(CfBandPart.getSpectralWavelength(variable), is(23.4f));
    }

    @Test
    public void testDecodeSpectralWavelength_WavelengthConversionFromPicoMeter() throws IOException {
        variable.addAttribute(new Attribute(Constants.RADIATION_WAVELENGTH, 23400.0f));
        variable.addAttribute(new Attribute(Constants.RADIATION_WAVELENGTH_UNIT, "pm"));

        assertThat(CfBandPart.getSpectralWavelength(variable), is(23.4f));
    }

    @Test
    public void testDecodeSpectralWavelength_unconvertableUnit() throws IOException {
        variable.addAttribute(new Attribute(Constants.RADIATION_WAVELENGTH, 23.4f));
        variable.addAttribute(new Attribute(Constants.RADIATION_WAVELENGTH_UNIT, "~m"));

        assertThat(CfBandPart.getSpectralWavelength(variable), is(0.0f));
    }

    @Test
    @STTM("SNAP-3886")
    public void testIsLogitudeVariablename() {
        assertTrue(CfBandPart.isLongitudeVarName("lon_intern"));
        assertTrue(CfBandPart.isLongitudeVarName("longitude"));
        assertTrue(CfBandPart.isLongitudeVarName("lon"));

        assertFalse(CfBandPart.isLongitudeVarName("lon_corr"));
        assertFalse(CfBandPart.isLongitudeVarName("FHWM"));
    }

    @Test
    @STTM("SNAP-3886")
    public void testIsLatitudeVariablename() {
        assertTrue(CfBandPart.isLatitudeVarName("lat_intern"));
        assertTrue(CfBandPart.isLatitudeVarName("latitude"));
        assertTrue(CfBandPart.isLatitudeVarName("lat"));

        assertFalse(CfBandPart.isLatitudeVarName("latitude_on_dem"));
        assertFalse(CfBandPart.isLatitudeVarName("sea_level_pressure"));
    }
}