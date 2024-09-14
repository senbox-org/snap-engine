package org.esa.snap.engine_utilities.gpf;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestOperatorUtils {

    @Test
    public void testCreateProductName() {
        String productName = "originalName";
        String suffix = "_suffix";

        String newName = OperatorUtils.createProductName(productName, suffix);
        assertEquals(productName+suffix, newName);

        String newName2 = OperatorUtils.createProductName(productName, suffix);
        assertEquals(newName, newName2);
    }

    // createProductName returns original name if suffix is present
    @Test
    public void test_createProductName_returns_original_name() {
        String productName = "product_suffix";
        String suffix = "_suffix";
        String result = OperatorUtils.createProductName(productName, suffix);
        assertEquals("product_suffix", result);
    }

    // getIncidenceAngle retrieves the correct tie point grid
    @Test
    public void test_getIncidenceAngle_retrieves_correct_tie_point_grid() {
        Product mockProduct = mock(Product.class);
        TiePointGrid mockTPG = mock(TiePointGrid.class);
        when(mockProduct.getTiePointGrid(OperatorUtils.TPG_INCIDENT_ANGLE)).thenReturn(mockTPG);
        TiePointGrid result = OperatorUtils.getIncidenceAngle(mockProduct);
        assertEquals(mockTPG, result);
    }

    @Test
    public void testGetLatTPGs() {
        final Product product = TestUtils.createProduct("type", 10, 10);

        TiePointGrid latTPG = OperatorUtils.getLatitude(product);
        assertNotNull(latTPG);
        assertEquals(OperatorUtils.TPG_LATITUDE, latTPG.getName());
    }

    @Test
    public void testGetLonTPGs() {
        final Product product = TestUtils.createProduct("type", 10, 10);

        TiePointGrid lonTPG = OperatorUtils.getLongitude(product);
        assertNotNull(lonTPG);
        assertEquals(OperatorUtils.TPG_LONGITUDE, lonTPG.getName());
    }

    // getPolarizationFromBandName throws exception for multiple polarizations
    @Test(expected = OperatorException.class)
    public void test_getPolarizationFromBandName_throws_exception_for_multiple_polarizations() {
        String bandName = "x_HH_times_VV_conj";
        OperatorUtils.getPolarizationFromBandName(bandName);
    }

    // getBandPolarization handles null metadata element
    @Test
    public void test_getBandPolarization_handles_null_metadata_element() {
        String bandName = "band_HH";
        MetadataElement absRoot = null;
        String result = OperatorUtils.getBandPolarization(bandName, absRoot);
        assertEquals("hh", result);
    }

    // getSourceBands handles null or empty sourceBandNames
    @Test
    public void test_getSourceBands_handles_null_or_empty_sourceBandNames() {
        Product mockProduct = mock(Product.class);
        Band[] bands = new Band[0];
        when(mockProduct.getBands()).thenReturn(bands);
        Band[] result = OperatorUtils.getSourceBands(mockProduct, null, false);
        assertArrayEquals(bands, result);
    }

    // addSelectedBands handles mismatched real and imaginary bands
    @Test(expected = OperatorException.class)
    @STTM("SRM-147")
    public void test_addSelectedBands_handles_mismatched_real_and_imaginary_bands() {
        Product sourceProduct = mock(Product.class);
        Product targetProduct = new Product("target", "type", 100, 100);
        Map<String, String[]> targetBandNameToSourceBandName = new HashMap<>();
        Band realBand = new Band("real_band", ProductData.TYPE_FLOAT32, 100, 100);
        realBand.setUnit(Unit.REAL);
        Band[] bands = {realBand};
        when(sourceProduct.getBands()).thenReturn(bands);
        when(sourceProduct.getNumBands()).thenReturn(1);

        MetadataElement absRoot = mock(MetadataElement.class);
        when(absRoot.getAttributeInt(AbstractMetadata.polsarData, 0)).thenReturn(0);

        AbstractMetadata absMetaMock = mock(AbstractMetadata.class);
        when(absMetaMock.getAbstractedMetadata(sourceProduct)).thenReturn(absRoot);

        OperatorUtils.addSelectedBands(sourceProduct, new String[]{"real_band"}, targetProduct, targetBandNameToSourceBandName, true, false);
    }

    // computeImageGeoBoundary throws exception for missing geocoding
    @Test(expected = OperatorException.class)
    public void test_computeImageGeoBoundary_throws_exception_for_missing_geocoding() {
        Product sourceProduct = mock(Product.class);
        when(sourceProduct.getSceneGeoCoding()).thenReturn(null);

        OperatorUtils.computeImageGeoBoundary(sourceProduct);
    }
}
