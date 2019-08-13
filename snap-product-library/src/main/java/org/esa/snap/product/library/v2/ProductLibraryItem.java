package org.esa.snap.product.library.v2;

import java.util.Date;

/**
 * Created by jcoravu on 12/8/2019.
 */
public class ProductLibraryItem {

    private String name;
    private String type;
    private int width;
    private int height;
    private long approximateSize;
    private Date processingDate;
    private String quickLookLocation;
    private Date acquisitionDate;

    public ProductLibraryItem() {

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
}
