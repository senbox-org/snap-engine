package org.esa.snap.core.util.grid.isin;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;

import static org.esa.snap.core.util.grid.isin.IsinAPI.Raster.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IsinUtilsTest {

    @Test
    public void testCreateFilename() {
        Date startDate = new Date(12345678900L);
        Date endDate = new Date(12385678999L);
        Date processingDate = new Date(52385678999L);

        String fileName = IsinUtils.createFileName("S3A", startDate, endDate, processingDate, "h08v17");
        assertEquals("S3A_OL_3_VEG_19700523_19700524_19710830_h08v17_v1_MPC_O.nc", fileName);

        startDate = new Date(312345678900L);
        endDate = new Date(322385678999L);
        processingDate = new Date(332385678999L);

        fileName = IsinUtils.createFileName("S3B", startDate, endDate, processingDate, "h11v19");
        assertEquals("S3B_OL_3_VEG_19791125_19800320_19800714_h11v19_v1_MPC_O.nc", fileName);
    }

    @Test
    public void testGetGeoLocations_1km() {
        final IsinUtils.GeoLocations geoLocations = IsinUtils.getGeoLocations(15, 16, GRID_1_KM);

        final ProductData longitudes = geoLocations.getLongitudes();
        final ProductData latitudes = geoLocations.getLatitudes();
        assertEquals(-87.72136688232422, longitudes.getElemFloatAt(0), 1e-8);
        assertEquals(-70.00416564941406, latitudes.getElemFloatAt(0), 1e-8);

        assertEquals(-87.69699096679688, longitudes.getElemFloatAt(1), 1e-8);
        assertEquals(-70.00416564941406, latitudes.getElemFloatAt(1), 1e-8);

        assertEquals(-87.67262268066406, longitudes.getElemFloatAt(2), 1e-8);
        assertEquals(-70.00416564941406, latitudes.getElemFloatAt(2), 1e-8);

        assertEquals(-73.09910583496094, longitudes.getElemFloatAt(600), 1e-8);
        assertEquals(-70.00416564941406, latitudes.getElemFloatAt(600), 1e-8);

        assertEquals(-87.75701141357422, longitudes.getElemFloatAt(1200), 1e-8);
        assertEquals(-70.01249694824219, latitudes.getElemFloatAt(1200), 1e-8);

        assertEquals(-148.36138916015625, longitudes.getElemFloatAt(1200000), 1e-8);
        assertEquals(-78.3375015258789, latitudes.getElemFloatAt(1200000), 1e-8);

        assertEquals(-148.46339416503906, longitudes.getElemFloatAt(1201200), 1e-8);
        assertEquals(-78.34583282470703, latitudes.getElemFloatAt(1201200), 1e-8);

        assertEquals(-115.21918487548828, longitudes.getElemFloatAt(1439997), 1e-8);
        assertEquals(-79.99583435058594, latitudes.getElemFloatAt(1439997), 1e-8);

        assertEquals(-115.17121887207031, longitudes.getElemFloatAt(1439998), 1e-8);
        assertEquals(-79.99583435058594, latitudes.getElemFloatAt(1439998), 1e-8);

        assertEquals(-115.12325286865234, longitudes.getElemFloatAt(1439999), 1e-8);
        assertEquals(-79.99583435058594, latitudes.getElemFloatAt(1439999), 1e-8);
    }

    @Test
    public void testGetGeoLocations_500m() {
        final IsinUtils.GeoLocations geoLocations = IsinUtils.getGeoLocations(14, 15, GRID_500_M);

        final ProductData longitudes = geoLocations.getLongitudes();
        final ProductData latitudes = geoLocations.getLatitudes();
        assertEquals(-79.99722290039062, longitudes.getElemFloatAt(0), 1e-8);
        assertEquals(-60.00208282470703, latitudes.getElemFloatAt(0), 1e-8);

        assertEquals(-79.9888916015625, longitudes.getElemFloatAt(1), 1e-8);
        assertEquals(-60.00208282470703, latitudes.getElemFloatAt(1), 1e-8);

        assertEquals(-79.98055267333984, longitudes.getElemFloatAt(2), 1e-8);
        assertEquals(-60.00208282470703, latitudes.getElemFloatAt(2), 1e-8);

        assertEquals(-69.99652862548828, longitudes.getElemFloatAt(1200), 1e-8);
        assertEquals(-60.00208282470703, latitudes.getElemFloatAt(1200), 1e-8);

        assertEquals(-80.01065063476562, longitudes.getElemFloatAt(2400), 1e-8);
        assertEquals(-60.006248474121094, latitudes.getElemFloatAt(2400), 1e-8);

        assertEquals(-91.79779815673828, longitudes.getElemFloatAt(2400000), 1e-8);
        assertEquals(-64.16874694824219, latitudes.getElemFloatAt(2400000), 1e-8);

        assertEquals(-91.80521392822266, longitudes.getElemFloatAt(2402400), 1e-8);
        assertEquals(-64.17292022705078, latitudes.getElemFloatAt(2402400), 1e-8);

        assertEquals(-87.73119354248047, longitudes.getElemFloatAt(5759997), 1e-8);
        assertEquals(-69.99791717529297, latitudes.getElemFloatAt(5759997), 1e-8);

        assertEquals(-87.7190170288086, longitudes.getElemFloatAt(5759998), 1e-8);
        assertEquals(-69.99791717529297, latitudes.getElemFloatAt(5759998), 1e-8);

        assertEquals(-87.70683288574219, longitudes.getElemFloatAt(5759999), 1e-8);
        assertEquals(-69.99791717529297, latitudes.getElemFloatAt(5759999), 1e-8);
    }

    @Test
    public void testGetGeoLocations_250m() {
        final IsinUtils.GeoLocations geoLocations = IsinUtils.getGeoLocations(13, 14, GRID_250_M);

        final ProductData longitudes = geoLocations.getLongitudes();
        final ProductData latitudes = geoLocations.getLatitudes();
        assertEquals(-77.78484344482422, longitudes.getElemFloatAt(0), 1e-8);
        assertEquals(-50.001041412353516, latitudes.getElemFloatAt(0), 1e-8);

        assertEquals(-77.78160095214844, longitudes.getElemFloatAt(1), 1e-8);
        assertEquals(-50.001041412353516, latitudes.getElemFloatAt(1), 1e-8);

        assertEquals(-77.77835845947266, longitudes.getElemFloatAt(2), 1e-8);
        assertEquals(-50.001041412353516, latitudes.getElemFloatAt(2), 1e-8);

        assertEquals(-70.00603485107422, longitudes.getElemFloatAt(2400), 1e-8);
        assertEquals(-50.001041412353516, latitudes.getElemFloatAt(2400), 1e-8);

        assertEquals(-77.78996276855469, longitudes.getElemFloatAt(4800), 1e-8);
        assertEquals(-50.00312423706055, latitudes.getElemFloatAt(4800), 1e-8);

        assertEquals(-81.36495971679688, longitudes.getElemFloatAt(4800000), 1e-8);
        assertEquals(-52.084373474121094, latitudes.getElemFloatAt(4800000), 1e-8);

        assertEquals(-81.36709594726562, longitudes.getElemFloatAt(4804800), 1e-8);
        assertEquals(-52.08646011352539, latitudes.getElemFloatAt(4804800), 1e-8);

        assertEquals(-80.00555419921875, longitudes.getElemFloatAt(23039997), 1e-8);
        assertEquals(-59.998958587646484, latitudes.getElemFloatAt(23039997), 1e-8);

        assertEquals(-80.00138854980469, longitudes.getElemFloatAt(23039998), 1e-8);
        assertEquals(-59.998958587646484, latitudes.getElemFloatAt(23039998), 1e-8);

        assertEquals(-79.99722290039062, longitudes.getElemFloatAt(23039999), 1e-8);
        assertEquals(-59.998958587646484, latitudes.getElemFloatAt(23039999), 1e-8);
    }

    @Test
    public void testCreateMpcVegetationPrototype_1km() {
        final Product product = IsinUtils.createMpcVegetationPrototype(12, 14, GRID_1_KM);

        assertEquals(1200, product.getSceneRasterWidth());
        assertEquals(1200, product.getSceneRasterHeight());
        assertEquals("MPC_VEG_OL_L3", product.getDisplayName());
        assertEquals("Level3", product.getProductType());

        // @todo 1 tb/tb add tests 2019-07-29
        final MetadataElement metadataRoot = product.getMetadataRoot();
        assertEquals("OLCI Level 3 vegetation data", metadataRoot.getAttribute("title").getData().getElemString());
        assertEquals("Brockmann Consult GmbH", metadataRoot.getAttribute("institution").getData().getElemString());
        assertEquals("OLCI Level 2 Land data (OLCI L2 L)", metadataRoot.getAttribute("source").getData().getElemString());
        assertEquals("This dataset was produced at Brockmann Consult GmbH for the Sentinel-3 Mission Performance Centre under ESA contract no. TODO", metadataRoot.getAttribute("comment").getData().getElemString());

        final Band lonBand = product.getBand("lon");
        assertEquals(ProductData.TYPE_FLOAT32, lonBand.getDataType());
        assertEquals(-96.00452423095703, lonBand.getPixelFloat(128, 256), 1e-8);
        assertEquals(-96.0158462524414, lonBand.getPixelFloat(129, 257), 1e-8);
        assertTrue(Double.isNaN(lonBand.getNoDataValue()));
        assertEquals("degrees_east", lonBand.getUnit());

        final Band latBand = product.getBand("lat");
        assertEquals(ProductData.TYPE_FLOAT32, latBand.getDataType());
        assertEquals(-52.15416717529297, latBand.getPixelFloat(130, 258), 1e-8);
        assertEquals(-52.162498474121094, latBand.getPixelFloat(131, 259), 1e-8);
        assertTrue(Double.isNaN(latBand.getNoDataValue()));
        assertEquals("degrees_north", latBand.getUnit());

        final Band ogvi_mean = product.getBand("OGVI_mean");
        assertEquals(ProductData.TYPE_FLOAT32, ogvi_mean.getDataType());
        assertTrue(Double.isNaN(ogvi_mean.getNoDataValue()));
        assertTrue(ogvi_mean.isNoDataValueUsed());
        assertTrue(Double.isNaN(ogvi_mean.getPixelFloat(132, 260)));

        final Band ogvi_sigma = product.getBand("OGVI_sigma");
        assertEquals(ProductData.TYPE_FLOAT32, ogvi_sigma.getDataType());
        assertTrue(Double.isNaN(ogvi_sigma.getNoDataValue()));
        assertTrue(ogvi_sigma.isNoDataValueUsed());
        assertTrue(Double.isNaN(ogvi_sigma.getPixelFloat(133, 261)));

        final Band ogvi_count = product.getBand("OGVI_count");
        assertEquals(ProductData.TYPE_INT32, ogvi_count.getDataType());
        assertEquals(Integer.MIN_VALUE, (int)(ogvi_count.getNoDataValue()));
        assertTrue(ogvi_count.isNoDataValueUsed());
        assertEquals(Integer.MIN_VALUE, ogvi_count.getPixelInt(134, 262));

        final Band otci_mean = product.getBand("OTCI_mean");
        assertEquals(ProductData.TYPE_FLOAT32, otci_mean.getDataType());
        assertTrue(Double.isNaN(otci_mean.getNoDataValue()));
        assertTrue(otci_mean.isNoDataValueUsed());
        assertTrue(Double.isNaN(otci_mean.getPixelFloat(135, 263)));

        final Band otci_sigma = product.getBand("OTCI_sigma");
        assertEquals(ProductData.TYPE_FLOAT32, otci_sigma.getDataType());
        assertTrue(Double.isNaN(otci_sigma.getNoDataValue()));
        assertTrue(otci_sigma.isNoDataValueUsed());
        assertTrue(Double.isNaN(otci_sigma.getPixelFloat(136, 264)));

        final Band otci_count = product.getBand("OTCI_count");
        assertEquals(ProductData.TYPE_INT32, otci_count.getDataType());
        assertEquals(Integer.MIN_VALUE, (int)(otci_count.getNoDataValue()));
        assertTrue(otci_count.isNoDataValueUsed());
        assertEquals(Integer.MIN_VALUE, otci_count.getPixelInt(134, 262));
    }

    @Test
    public void testCreateMpcVegetationPrototype_500m() {
        final Product product = IsinUtils.createMpcVegetationPrototype(11, 13, GRID_500_M);

        assertEquals(2400, product.getSceneRasterWidth());
        assertEquals(2400, product.getSceneRasterHeight());
        assertEquals("MPC_VEG_OL_L3", product.getDisplayName());
        assertEquals("Level3", product.getProductType());

        // @todo 1 tb/tb add tests 2019-07-29
        final MetadataElement metadataRoot = product.getMetadataRoot();

        final Band lonBand = product.getBand("lon");
        assertEquals(ProductData.TYPE_FLOAT32, lonBand.getDataType());
        assertEquals(-85.16436767578125, lonBand.getPixelFloat(1622, 492), 1e-8);
        assertEquals(-85.1640625, lonBand.getPixelFloat(1623, 493), 1e-8);
        assertTrue(Double.isNaN(lonBand.getNoDataValue()));
        assertEquals("degrees_east", lonBand.getUnit());

        final Band latBand = product.getBand("lat");
        assertEquals(ProductData.TYPE_FLOAT32, latBand.getDataType());
        assertEquals(-42.06041717529297, latBand.getPixelFloat(1624, 494), 1e-8);
        assertEquals(-42.06458282470703, latBand.getPixelFloat(1625, 495), 1e-8);
        assertTrue(Double.isNaN(latBand.getNoDataValue()));
        assertEquals("degrees_north", latBand.getUnit());

        final Band ogvi_mean = product.getBand("OGVI_mean");
        assertEquals(ProductData.TYPE_FLOAT32, ogvi_mean.getDataType());
        assertTrue(Double.isNaN(ogvi_mean.getNoDataValue()));
        assertTrue(ogvi_mean.isNoDataValueUsed());
        assertTrue(Double.isNaN(ogvi_mean.getPixelFloat(1626, 496)));

        final Band ogvi_sigma = product.getBand("OGVI_sigma");
        assertEquals(ProductData.TYPE_FLOAT32, ogvi_sigma.getDataType());
        assertTrue(Double.isNaN(ogvi_sigma.getNoDataValue()));
        assertTrue(ogvi_sigma.isNoDataValueUsed());
        assertTrue(Double.isNaN(ogvi_sigma.getPixelFloat(1627, 497)));

        final Band ogvi_count = product.getBand("OGVI_count");
        assertEquals(ProductData.TYPE_INT32, ogvi_count.getDataType());
        assertEquals(Integer.MIN_VALUE, (int)(ogvi_count.getNoDataValue()));
        assertTrue(ogvi_count.isNoDataValueUsed());
        assertEquals(Integer.MIN_VALUE, ogvi_count.getPixelInt(1628, 498));

        final Band otci_mean = product.getBand("OTCI_mean");
        assertEquals(ProductData.TYPE_FLOAT32, otci_mean.getDataType());
        assertTrue(Double.isNaN(otci_mean.getNoDataValue()));
        assertTrue(otci_mean.isNoDataValueUsed());
        assertTrue(Double.isNaN(otci_mean.getPixelFloat(1629, 499)));

        final Band otci_sigma = product.getBand("OTCI_sigma");
        assertEquals(ProductData.TYPE_FLOAT32, otci_sigma.getDataType());
        assertTrue(Double.isNaN(otci_sigma.getNoDataValue()));
        assertTrue(otci_sigma.isNoDataValueUsed());
        assertTrue(Double.isNaN(otci_sigma.getPixelFloat(1630, 500)));

        final Band otci_count = product.getBand("OTCI_count");
        assertEquals(ProductData.TYPE_INT32, otci_count.getDataType());
        assertEquals(Integer.MIN_VALUE, (int)(otci_count.getNoDataValue()));
        assertTrue(otci_count.isNoDataValueUsed());
        assertEquals(Integer.MIN_VALUE, otci_count.getPixelInt(1631, 501));
    }

    @Test
    @Ignore // @todo 1 tb/tb check why we get out-of-memory here and resolve 2019-09-18
    public void testCreateMpcVegetationPrototype_250m() {
        final Product product = IsinUtils.createMpcVegetationPrototype(10, 12, GRID_250_M);

        assertEquals(4800, product.getSceneRasterWidth());
        assertEquals(4800, product.getSceneRasterHeight());
        assertEquals("MPC_VEG_OL_L3", product.getDisplayName());
        assertEquals("Level3", product.getProductType());

        // @todo 1 tb/tb add tests 2019-07-29
        final MetadataElement metadataRoot = product.getMetadataRoot();

        final Band lonBand = product.getBand("lon");
        assertEquals(ProductData.TYPE_FLOAT32, lonBand.getDataType());
        assertEquals(-95.54296112060547, lonBand.getPixelFloat(896, 2466), 1e-8);
        assertEquals(-95.54371643066406, lonBand.getPixelFloat(897, 2467), 1e-8);
        assertTrue(Double.isNaN(lonBand.getNoDataValue()));
        assertEquals("degrees_east", lonBand.getUnit());

        final Band latBand = product.getBand("lat");
        assertEquals(ProductData.TYPE_FLOAT32, latBand.getDataType());
        assertEquals(-35.14270782470703, latBand.getPixelFloat(898, 2468), 1e-8);
        assertEquals(-35.14479064941406, latBand.getPixelFloat(899, 2469), 1e-8);
        assertTrue(Double.isNaN(latBand.getNoDataValue()));
        assertEquals("degrees_north", latBand.getUnit());

        final Band ogvi_mean = product.getBand("OGVI_mean");
        assertEquals(ProductData.TYPE_FLOAT32, ogvi_mean.getDataType());
        assertTrue(Double.isNaN(ogvi_mean.getNoDataValue()));
        assertTrue(Double.isNaN(ogvi_mean.getPixelFloat(900, 2470)));

        final Band ogvi_sigma = product.getBand("OGVI_sigma");
        assertEquals(ProductData.TYPE_FLOAT32, ogvi_sigma.getDataType());
        assertTrue(Double.isNaN(ogvi_sigma.getNoDataValue()));
        assertTrue(Double.isNaN(ogvi_sigma.getPixelFloat(901, 2471)));

        final Band ogvi_count = product.getBand("OGVI_count");
        assertEquals(ProductData.TYPE_INT32, ogvi_count.getDataType());
        assertEquals(Integer.MIN_VALUE, (int)(ogvi_count.getNoDataValue()));
        assertEquals(Integer.MIN_VALUE, ogvi_count.getPixelInt(902, 2472));

        final Band otci_mean = product.getBand("OTCI_mean");
        assertEquals(ProductData.TYPE_FLOAT32, otci_mean.getDataType());
        assertTrue(Double.isNaN(otci_mean.getNoDataValue()));
        assertTrue(Double.isNaN(otci_mean.getPixelFloat(903, 2473)));

        final Band otci_sigma = product.getBand("OTCI_sigma");
        assertEquals(ProductData.TYPE_FLOAT32, otci_sigma.getDataType());
        assertTrue(Double.isNaN(otci_sigma.getNoDataValue()));
        assertTrue(Double.isNaN(otci_sigma.getPixelFloat(904, 2474)));

        final Band otci_count = product.getBand("OTCI_count");
        assertEquals(ProductData.TYPE_INT32, otci_count.getDataType());
        assertEquals(Integer.MIN_VALUE, (int)(otci_count.getNoDataValue()));
        assertEquals(Integer.MIN_VALUE, otci_count.getPixelInt(905, 2475));
    }
}
