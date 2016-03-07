package org.esa.s3tbx.insitu.ui;

import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class MinMaxGeoCoordinatesTest {

    @Test
    public void testCreate_NoProduct() throws Exception {
        ArrayList<Product> products = new ArrayList<>();
        InsituClientModel.MinMaxGeoCoordinates coordinates = InsituClientModel.MinMaxGeoCoordinates.create(products);
        assertEquals(90.0, coordinates.getMaxLat(), 1.0e-6);
        assertEquals(-90.0, coordinates.getMinLat(), 1.0e-6);
        assertEquals(180.0, coordinates.getMaxLon(), 1.0e-6);
        assertEquals(-180.0, coordinates.getMinLon(), 1.0e-6);
    }

    @Test
    public void testCreate_OneProduct() throws Exception {
        ArrayList<Product> products = new ArrayList<>();
        products.add(createProduct(1, -180, 90));
        InsituClientModel.MinMaxGeoCoordinates coordinates = InsituClientModel.MinMaxGeoCoordinates.create(products);
        assertEquals(90.0, coordinates.getMaxLat(), 1.0e-6);
        assertEquals(81.0, coordinates.getMinLat(), 1.0e-6);
        assertEquals(-171.0, coordinates.getMaxLon(), 1.0e-6);
        assertEquals(-180.0, coordinates.getMinLon(), 1.0e-6);
    }

    @Test
    public void testCreate_MultipleProduct() throws Exception {
        ArrayList<Product> products = new ArrayList<>();
        products.add(createProduct(1, -5, 80));
        products.add(createProduct(2, 165, 5));
        products.add(createProduct(3, -5, -70));
        products.add(createProduct(4, -175, 5));
        InsituClientModel.MinMaxGeoCoordinates coordinates = InsituClientModel.MinMaxGeoCoordinates.create(products);
        assertEquals(80.0, coordinates.getMaxLat(), 1.0e-6);
        assertEquals(-79.0, coordinates.getMinLat(), 1.0e-6);
        assertEquals(174.0, coordinates.getMaxLon(), 1.0e-6);
        assertEquals(-175.0, coordinates.getMinLon(), 1.0e-6);
    }

    private Product createProduct(int index, int easting, int northing) throws Exception {
        int width = 10;
        int height = 10;
        Product product = new Product("p" + index, "t", width, height);
        GeoCoding gc = new CrsGeoCoding(DefaultGeographicCRS.WGS84, width, height, easting, northing, 1.0, 1.0 , 0.5, 0.5);
        product.setSceneGeoCoding(gc);
        return product;
    }
}