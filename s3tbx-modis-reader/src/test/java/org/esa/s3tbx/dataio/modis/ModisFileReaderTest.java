package org.esa.s3tbx.dataio.modis;

import org.esa.s3tbx.dataio.modis.hdf.HdfDataField;
import org.esa.s3tbx.dataio.modis.productdb.ModisBandDescription;
import org.esa.s3tbx.dataio.modis.productdb.ModisSpectralInfo;
import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.math.Range;
import org.junit.Test;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.*;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ModisFileReaderTest {

    @Test
    public void testGetTypeString_typeNull() {
        final Product product = new Product("Name", "PROD_TYPE", 5, 5);

        assertEquals("PROD_TYPE", ModisFileReader.getTypeString(null, product));
    }

    @Test
    public void testGetTypeString_typeSupplied() {
        final Product product = new Product("Name", "PROD_TYPE", 5, 5);

        assertEquals("TYPE_STRING", ModisFileReader.getTypeString("TYPE_STRING", product));
    }

    @Test
    public void testCreateRangeFromArray_nullArray() {
        assertNull(ModisFileReader.createRangeFromArray(null));
    }

    @Test
    public void testCreateRangeFromArray_tooShortArray() {
        assertNull(ModisFileReader.createRangeFromArray(new int[]{34}));
    }

    @Test
    public void testCreateRangeFromArray_orderedInts() {
        final Range range = ModisFileReader.createRangeFromArray(new int[]{34, 3809});
        assertNotNull(range);

        assertEquals(34.0, range.getMin(), 1e-8);
        assertEquals(3809.0, range.getMax(), 1e-8);
    }

    @Test
    public void testCreateRangeFromArray_inverseOrderedInts() {
        final Range range = ModisFileReader.createRangeFromArray(new int[]{9886, 14});
        assertNotNull(range);

        assertEquals(14.0, range.getMin(), 1e-8);
        assertEquals(9886.0, range.getMax(), 1e-8);
    }

    @Test
    public void testHasInvalidScaleAndOffset_invalidScale() {
        assertTrue(ModisFileReader.hasInvalidScaleAndOffset(new float[2], new float[4], 3));
    }

    @Test
    public void testHasInvalidScaleAndOffset_invalidOffset() {
        assertTrue(ModisFileReader.hasInvalidScaleAndOffset(new float[4], new float[2], 3));
    }

    @Test
    public void testHasInvalidScaleAndOffset() {
        assertFalse(ModisFileReader.hasInvalidScaleAndOffset(new float[4], new float[4], 3));
    }

    @Test
    public void testSetSpectralBandInfo_notSpectral() {
        final Band band = new Band("bla", ProductData.TYPE_FLOAT32, 3, 3);
        final ModisBandDescription description = new ModisBandDescription("", "false", "", "", "", "", "", "");

        ModisFileReader.setBandSpectralInformation(description, "", band);

        assertEquals(-1, band.getSpectralBandIndex());
        assertEquals(0.f, band.getSpectralWavelength(), 1e-8);
        assertEquals(0.f, band.getSpectralBandwidth(), 1e-8);
    }

    @Test
    public void testSetSpectralBandInfo_fromBandIndex() {
        final Band band = new Band("bla", ProductData.TYPE_FLOAT32, 3, 3);
        final ModisBandDescription description = new ModisBandDescription("", "true", "", "", "", "", "", "");

        ModisFileReader.setBandSpectralInformation(description, "4", band);

        assertEquals(6, band.getSpectralBandIndex());
        assertEquals(555.f, band.getSpectralWavelength(), 1e-8);
        assertEquals(20.f, band.getSpectralBandwidth(), 1e-8);
    }

    @Test
    public void testSetSpectralBandInfo_fromSpecInfo() {
        final Band band = new Band("bla", ProductData.TYPE_FLOAT32, 3, 3);
        final ModisBandDescription description = new ModisBandDescription("", "true", "", "", "", "", "", "");
        final ModisSpectralInfo spectralInfo = new ModisSpectralInfo("2", "3", "4");
        description.setSpecInfo(spectralInfo);

        ModisFileReader.setBandSpectralInformation(description, "", band);

        assertEquals(4, band.getSpectralBandIndex());
        assertEquals(2.f, band.getSpectralWavelength(), 1e-8);
        assertEquals(3.f, band.getSpectralBandwidth(), 1e-8);
    }

    @Test
    public void testIsEosGridType() throws IOException {
        final TestGlobalAttributes globalAttributes = new TestGlobalAttributes();

        globalAttributes.setEosType(ModisConstants.EOS_TYPE_GRID);
        assertTrue(ModisFileReader.isEosGridType(globalAttributes));

        globalAttributes.setEosType("EOS_invalid_and_ausgedacht");
        assertFalse(ModisFileReader.isEosGridType(globalAttributes));
    }

    @Test
    public void testInvert() {
        final float[] scales = new float[]{24.7f, 0.f, -100.f};

        ModisFileReader.invert(scales);

        assertEquals(0.04048583f, scales[0], 1e-8);
        assertEquals(0.f, scales[1], 1e-8);
        assertEquals(-0.01f, scales[2], 1e-8);
    }

    ////////////////////////////////////////////////////////////////////////////////
    /////// INNER CLASS
    ////////////////////////////////////////////////////////////////////////////////


    private class TestGlobalAttributes implements ModisGlobalAttributes {
        private String eosType;

        @Override
        public String getProductName() throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public String getProductType() throws IOException {
            throw new NotImplementedException();
        }

        @Override
        public Dimension getProductDimensions(List<ucar.nc2.Dimension> netcdfFileDimensions) {
            throw new NotImplementedException();
        }

        @Override
        public HdfDataField getDatafield(String name) throws ProductIOException {
            throw new NotImplementedException();
        }

        @Override
        public Date getSensingStart() {
            throw new NotImplementedException();
        }

        @Override
        public Date getSensingStop() {
            throw new NotImplementedException();
        }

        @Override
        public int[] getSubsamplingAndOffset(String dimensionName) {
            throw new NotImplementedException();
        }

        @Override
        public boolean isImappFormat() {
            throw new NotImplementedException();
        }

        @Override
        public String getEosType() {
            return eosType;
        }

        void setEosType(String eosType) {
            this.eosType = eosType;
        }

        @Override
        public GeoCoding createGeocoding() {
            throw new NotImplementedException();
        }
    }
}
