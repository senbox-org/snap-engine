package org.esa.snap.product.library.v2.database;

import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.DataFormatType;
import org.esa.snap.remote.products.repository.PixelType;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.SensorType;

import java.awt.image.BufferedImage;
import java.util.Date;
import java.util.List;

/**
 * Created by jcoravu on 5/9/2019.
 */
public class LocalRepositoryProduct implements RepositoryProduct {

    private final String name;
    private final String path;
    private final String type;
    private final Date acquisitionDate;
    private final long sizeInBytes;

    private List<Attribute> attributes;
    private BufferedImage quickLookImage;

    public LocalRepositoryProduct(String name, String type, Date acquisitionDate, String path, long sizeInBytes) {
        this.name = name;
        this.type = type;
        this.path = path;
        this.acquisitionDate = acquisitionDate;
        this.sizeInBytes = sizeInBytes;
    }

    @Override
    public List<Attribute> getAttributes() {
        return this.attributes;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public long getApproximateSize() {
        return this.sizeInBytes;
    }

    @Override
    public String getDownloadQuickLookImageURL() {
        return null;
    }

    @Override
    public String getDownloadURL() {
        return this.path;
    }

    @Override
    public Date getAcquisitionDate() {
        return this.acquisitionDate;
    }

    @Override
    public String getMission() {
        return this.type;
    }

    @Override
    public BufferedImage getQuickLookImage() {
        return quickLookImage;
    }

    @Override
    public PixelType getPixelType() {
        return null;
    }

    @Override
    public DataFormatType getDataFormatType() {
        return null;
    }

    @Override
    public SensorType getSensorType() {
        return null;
    }

    @Override
    public String getGeometry() {
        return null;
    }

    @Override
    public String getEntryPoint() {
        return null;
    }

    @Override
    public void setQuickLookImage(BufferedImage quickLookImage) {
        this.quickLookImage = quickLookImage;
    }

    void setAttributes(List<Attribute> attributes) {
        this.attributes = attributes;
    }
}
