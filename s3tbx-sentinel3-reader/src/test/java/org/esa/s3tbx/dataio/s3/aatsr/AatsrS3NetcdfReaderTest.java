package org.esa.s3tbx.dataio.s3.aatsr;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.sun.medialib.mlib.mediaLibImageInterpTable;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.SampleCoding;
import org.junit.*;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

/**
 * Created by Sabine on 20.02.2017.
 */
public class AatsrS3NetcdfReaderTest {

    private AatsrS3NetcdfReader reader;
    private Product product;
    private boolean synthetic;
    private Variable variable;

    @Before
    public void setUp() throws Exception {
        reader = new AatsrS3NetcdfReader();
        product = new Product("p", "t", 44, 22);
        synthetic = true;
        variable = mock(Variable.class);
    }

    @Test
    public void addVariableAsBand_DataType_CHAR() throws Exception {
        //preparation
        when(variable.getDataType()).thenReturn(DataType.CHAR);
        when(variable.getDescription()).thenReturn("v-description");
        when(variable.getUnitsString()).thenReturn("v-unit");
        when(variable.findAttribute("scaling_factor")).thenReturn(attr("scaling_factor", 1.23));
        when(variable.findAttribute("add_offset")).thenReturn(attr("add_offset", 2.34));
        when(variable.findAttribute("_FillValue")).thenReturn(attr("_FillValue", 3));
        when(variable.findAttribute("flag_values")).thenReturn(attr("flag_values", arr(new byte[]{1, 2, 4})));
        when(variable.findAttribute("flag_meanings")).thenReturn(attr("flag_meanings", "a b c"));

        //execution
        reader.addVariableAsBand(product, variable, "v-name", synthetic);

        //verification
        final Band band = product.getBand("v-name");
        assertNotNull(band);
        assertEquals("v-description", band.getDescription());
        assertEquals("v-unit", band.getUnit());
        assertEquals("1.23", String.valueOf(band.getScalingFactor()));
        assertEquals("2.34", String.valueOf(band.getScalingOffset()));
        assertEquals(synthetic, band.isSynthetic());
        assertEquals(true, band.isNoDataValueUsed());
        assertEquals(true, band.isNoDataValueSet());
        assertEquals("3.0", String.valueOf(band.getNoDataValue()));
        final SampleCoding sampleCoding = band.getSampleCoding();
        assertEquals("v-name", sampleCoding.getName());
        assertEquals(null, sampleCoding.getDescription());
        assertEquals(3, sampleCoding.getNumAttributes());
        assertEquals("a", sampleCoding.getAttributeAt(0).getName());
        assertEquals(1, sampleCoding.getAttributeAt(0).getData().getElemInt());
        assertEquals("b", sampleCoding.getAttributeAt(1).getName());
        assertEquals(2, sampleCoding.getAttributeAt(1).getData().getElemInt());
        assertEquals("c", sampleCoding.getAttributeAt(2).getName());
        assertEquals(4, sampleCoding.getAttributeAt(2).getData().getElemInt());
    }

    @Test
    public void testAddVariableAsBand_DataType_Byte() {
        //preparation
        when(variable.getDataType()).thenReturn(DataType.BYTE);

        //execution
        reader.addVariableAsBand(product, variable, "byte-name", synthetic);

        //verification
        final Band band = product.getBand("byte-name");
        assertNotNull(band);
    }

    private Array arr(Object javaArray) {
        return Array.factory(javaArray);
    }

    private Attribute attr(String name, String val) {
        return new Attribute(name, val);
    }

    private Attribute attr(String name, Number val) {
        return new Attribute(name, val);
    }

    private Attribute attr(String name, Array arr) {
        return new Attribute(name, arr);
    }
}