package org.esa.snap.product.library.v2;

import ro.cs.tao.eodata.EOProduct;

import java.awt.geom.Path2D;
import java.util.Date;

/**
 * Created by jcoravu on 12/8/2019.
 */
public class ProductLibraryItem {

    private final EOProduct product;
    private final String sensor;

    private String name;
    private String type;
    private int width;
    private int height;
    private long approximateSize;
    private Date processingDate;
    private String quickLookLocation;
    private Date acquisitionDate;
    private Path2D.Double path;

    public ProductLibraryItem(EOProduct product, String sensor) {
        this.product = product;
        this.sensor = sensor;
    }

    String getSensor() {
        return sensor;
    }

    EOProduct getProduct() {
        return product;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getApproximateSize() {
        return approximateSize;
    }

    public void setApproximateSize(long approximateSize) {
        this.approximateSize = approximateSize;
    }

    public Date getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(Date processingDate) {
        this.processingDate = processingDate;
    }

    public String getQuickLookLocation() {
        return quickLookLocation;
    }

    public void setQuickLookLocation(String quickLookLocation) {
        this.quickLookLocation = quickLookLocation;
    }

    public Date getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(Date acquisitionDate) {
        this.acquisitionDate = acquisitionDate;
    }

    public void setPath(Path2D.Double path) {
        this.path = path;
    }

    public Path2D.Double getPath() {
        return path;
    }
}
