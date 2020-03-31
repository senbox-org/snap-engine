package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.ProfileWriteContextImpl;
import org.esa.snap.dataio.netcdf.nc.NFileWriteable;
import org.esa.snap.dataio.netcdf.nc.NVariable;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.DataType;

import java.awt.Dimension;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CfMetadataPartTest {

    private Product product;
    private TestFileWriteable testFileWriteable;
    private ProfileWriteContextImpl profileWriteContext;
    private CfMetadataPart metadataPart;

    @Before
    public void setUp() {
        product = new Product("no meta", "test", 3, 3);
        testFileWriteable = new TestFileWriteable();
        profileWriteContext = new ProfileWriteContextImpl(testFileWriteable);
        metadataPart = new CfMetadataPart();
    }

    @Test
    public void testPreEncode_no_metadata() throws IOException {
        metadataPart.preEncode(profileWriteContext, product);

        final Map<String, String> globalStringAttributes = testFileWriteable.getGlobalStringAttributes();
        assertEquals(0, globalStringAttributes.size());

        final Map<String, Number> globalNumberAttributes = testFileWriteable.getGlobalNumberAttributes();
        assertEquals(0, globalNumberAttributes.size());
    }

    @Test
    public void testPreEncode_string_metadata() throws IOException {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addAttribute(new MetadataAttribute("one", ProductData.createInstance("This is a value"), true));
        metadataRoot.addAttribute(new MetadataAttribute("two", ProductData.createInstance("This is another value"), true));

        metadataPart.preEncode(profileWriteContext, product);

        final Map<String, String> globalStringAttributes = testFileWriteable.getGlobalStringAttributes();
        assertEquals(2, globalStringAttributes.size());
        assertEquals("This is a value", globalStringAttributes.get("one"));
        assertEquals("This is another value", globalStringAttributes.get("two"));

        final Map<String, Number> globalNumberAttributes = testFileWriteable.getGlobalNumberAttributes();
        assertEquals(0, globalNumberAttributes.size());
    }

    @Test
    public void testPreEncode_number_metadata() throws IOException {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addAttribute(new MetadataAttribute("byte", ProductData.createInstance(ProductData.TYPE_INT8, new byte[]{12}), true));
        metadataRoot.addAttribute(new MetadataAttribute("short", ProductData.createInstance(ProductData.TYPE_INT16, new short[]{13}), true));
        metadataRoot.addAttribute(new MetadataAttribute("int", ProductData.createInstance(ProductData.TYPE_INT32, new int[]{14}), true));
        metadataRoot.addAttribute(new MetadataAttribute("ubyte", ProductData.createInstance(ProductData.TYPE_UINT8, new byte[]{15}), true));
        metadataRoot.addAttribute(new MetadataAttribute("ushort", ProductData.createInstance(ProductData.TYPE_UINT16, new short[]{16}), true));
        metadataRoot.addAttribute(new MetadataAttribute("uint", ProductData.createInstance(ProductData.TYPE_UINT32, new int[]{17}), true));
        metadataRoot.addAttribute(new MetadataAttribute("float", ProductData.createInstance(ProductData.TYPE_FLOAT32, new float[]{18.0f}), true));
        metadataRoot.addAttribute(new MetadataAttribute("double", ProductData.createInstance(ProductData.TYPE_FLOAT64, new double[]{19.0}), true));
        metadataRoot.addAttribute(new MetadataAttribute("long", ProductData.createInstance(ProductData.TYPE_INT64, new long[]{20}), true));

        metadataPart.preEncode(profileWriteContext, product);

        final Map<String, String> globalStringAttributes = testFileWriteable.getGlobalStringAttributes();
        assertEquals(0, globalStringAttributes.size());

        final Map<String, Number> globalNumberAttributes = testFileWriteable.getGlobalNumberAttributes();
        assertEquals(9, globalNumberAttributes.size());
        assertEquals(12, globalNumberAttributes.get("byte"));
        assertEquals(13, globalNumberAttributes.get("short"));
        assertEquals(14, globalNumberAttributes.get("int"));
        assertEquals(15, globalNumberAttributes.get("ubyte"));
        assertEquals(16, globalNumberAttributes.get("ushort"));
        assertEquals(17, globalNumberAttributes.get("uint"));
        assertEquals(18.0, (float) globalNumberAttributes.get("float"), 1e-8);
        assertEquals(19.0, (double) globalNumberAttributes.get("double"), 1e-8);
        assertEquals(20L, globalNumberAttributes.get("long"));
    }

    @Test
    public void testPreEncode_number_metadata_arrays_rejected() throws IOException {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addAttribute(new MetadataAttribute("int", ProductData.createInstance(ProductData.TYPE_INT32, new int[]{14, 15, 16}), true));

        metadataPart.preEncode(profileWriteContext, product);

        final Map<String, String> globalStringAttributes = testFileWriteable.getGlobalStringAttributes();
        assertEquals(0, globalStringAttributes.size());

        final Map<String, Number> globalNumberAttributes = testFileWriteable.getGlobalNumberAttributes();
        assertEquals(0, globalNumberAttributes.size());
    }

    @Test
    public void testPreEncode_mixed_types() throws IOException {
        final MetadataElement metadataRoot = product.getMetadataRoot();
        metadataRoot.addAttribute(new MetadataAttribute("yo", ProductData.createInstance("man"), true));
        metadataRoot.addAttribute(new MetadataAttribute("what", ProductData.createInstance(ProductData.TYPE_INT32, new int[]{42}), true));
        metadataRoot.addAttribute(new MetadataAttribute("cool", ProductData.createInstance("Iceland"), true));
        metadataRoot.addAttribute(new MetadataAttribute("original", ProductData.createInstance(ProductData.TYPE_INT32, new int[]{-182}), true));

        metadataPart.preEncode(profileWriteContext, product);

        final Map<String, String> globalStringAttributes = testFileWriteable.getGlobalStringAttributes();
        assertEquals(2, globalStringAttributes.size());
        assertEquals("man", globalStringAttributes.get("yo"));
        assertEquals("Iceland", globalStringAttributes.get("cool"));

        final Map<String, Number> globalNumberAttributes = testFileWriteable.getGlobalNumberAttributes();
        assertEquals(2, globalNumberAttributes.size());
        assertEquals(42, globalNumberAttributes.get("what"));
        assertEquals(-182, globalNumberAttributes.get("original"));
    }

    private static class TestFileWriteable extends NFileWriteable {
        private Map<String, String> globalStringAttributes;
        private Map<String, Number> globalNumberAttributes;

        TestFileWriteable() {
            globalStringAttributes = new HashMap<>();
            globalNumberAttributes = new HashMap<>();
        }

        @Override
        public void addDimension(String name, int length)  {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public String getDimensions() {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void addGlobalAttribute(String name, String value) {
            globalStringAttributes.put(name, value);
        }

        @Override
        public void addGlobalAttribute(String name, Number value)  {
            globalNumberAttributes.put(name, value);
        }

        @Override
        public NVariable addScalarVariable(String name, DataType dataType) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public NVariable addVariable(String name, DataType dataType, Dimension tileSize, String dims) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public NVariable addVariable(String name, DataType dataType, boolean unsigned, Dimension tileSize, String dims)  {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public NVariable addVariable(String name, DataType dataType, boolean unsigned, Dimension tileSize, String dims, int compressionLevel) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public NVariable findVariable(String variableName) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean isNameValid(String name) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public String makeNameValid(String name) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public DataType getNetcdfDataType(int dataType) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void create() {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public void close()  {
            throw new RuntimeException("Not implemented");
        }

        Map<String, String> getGlobalStringAttributes() {
            return globalStringAttributes;
        }

        Map<String, Number> getGlobalNumberAttributes() {
            return globalNumberAttributes;
        }
    }
}
