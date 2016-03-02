package org.esa.s3tbx.insitu.ui;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class TimeSpanTest {

    @Test
    public void testCreate_NoProduct() throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        ArrayList<Product> products = new ArrayList<>();
        InsituClientModel.TimeSpan timeSpan = InsituClientModel.TimeSpan.create(products);
        Calendar utcCalendar = InsituClientModel.createUtcCalendar();
        Date expectedStopDate = utcCalendar.getTime();

        assertEquals(ProductData.UTC.parse("01-JAN-1970 12:00:00").getAsDate(), timeSpan.getStartDate());
        assertEquals(expectedStopDate, timeSpan.getStopDate());
    }

    @Test
    public void testCreate_OneProduct() throws Exception {
        ArrayList<Product> products = new ArrayList<>();
        products.add(createProduct(1, ProductData.UTC.parse("16-MAR-2007 12:35:14"), ProductData.UTC.parse("16-MAR-2007 12:55:14")));
        InsituClientModel.TimeSpan timeSpan = InsituClientModel.TimeSpan.create(products);
        assertEquals(ProductData.UTC.parse("16-MAR-2007 12:35:14").getAsDate(), timeSpan.getStartDate());
        assertEquals(ProductData.UTC.parse("16-MAR-2007 12:55:14").getAsDate(), timeSpan.getStopDate());
    }

    private Product createProduct(int index, ProductData.UTC startTime,ProductData.UTC endTime) throws Exception {
        Product product = new Product("p" + index, "t", 10, 10);
        product.setStartTime(startTime);
        product.setEndTime(endTime);
        return product;
    }

}