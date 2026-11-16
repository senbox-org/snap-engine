package org.esa.snap.statistics.percentile.interpolated;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.DateTimeUtils;
import org.esa.snap.core.util.GeoUtils;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

class Utils {

    static Area createProductArea(Product product) {
        GeneralPath[] boundary = GeoUtils.createGeoBoundaryPaths(product);
        Area area = new Area();
        for (GeneralPath generalPath : boundary) {
            area.add(new Area(generalPath));
        }
        return area;
    }

    static TreeMap<Long, List<Product>> groupProductsDaily(Product[] products) {
        final TreeMap<Long, List<Product>> groupedProducts = new TreeMap<Long, List<Product>>();
        for (Product product : products) {
            final long productMJD = getCenterDateAsModifiedJulianDay(product);
            List<Product> productList = groupedProducts.computeIfAbsent(productMJD, k -> new ArrayList<>());
            productList.add(product);
        }
        return groupedProducts;
    }

    static long utcToModifiedJulianDay(Date utc) {
        final double julianDate = DateTimeUtils.utcToJD(utc);
        final double modifiedJulianDate = DateTimeUtils.jdToMJD(julianDate);
        final double modifiedJulianDay = Math.floor(modifiedJulianDate);
        return (long) modifiedJulianDay;
    }

    private static long getCenterDateAsModifiedJulianDay(Product product) {
        final ProductData.UTC startTime = product.getStartTime();
        final ProductData.UTC endTime = product.getEndTime();
        final long endMillies = endTime.getAsDate().getTime();
        final long startMillies = startTime.getAsDate().getTime();
        final long centerMillies = (endMillies - startMillies) / 2 + startMillies;

        final Date centerUTCDate = new Date(centerMillies);
        return utcToModifiedJulianDay(centerUTCDate);
    }

}
