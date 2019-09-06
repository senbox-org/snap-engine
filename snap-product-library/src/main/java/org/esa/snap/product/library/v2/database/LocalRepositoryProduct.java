package org.esa.snap.product.library.v2.database;

import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.awt.image.BufferedImage;
import java.util.Date;

/**
 * Created by jcoravu on 5/9/2019.
 */
public class LocalRepositoryProduct implements RepositoryProduct {

    private final String name;
    private final String path;
    private final String mission;
    private final Date acquisitionDate;
    private final long sizeInBytes;

    private Attribute[] remoteAttributes;
    private BufferedImage quickLookImage;

    public LocalRepositoryProduct(String name, String mission, Date acquisitionDate, String path, long sizeInBytes) {
        this.name = name;
        this.mission = mission;
        this.path = path;
        this.acquisitionDate = acquisitionDate;
        this.sizeInBytes = sizeInBytes;
    }

    @Override
    public Attribute[] getAttributes() {
        return this.remoteAttributes;
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
        return this.mission;
    }

    @Override
    public BufferedImage getQuickLookImage() {
        return quickLookImage;
    }

    @Override
    public void setQuickLookImage(BufferedImage quickLookImage) {
        this.quickLookImage = quickLookImage;
    }

    void setRemoteAttributes(Attribute[] remoteAttributes) {
        this.remoteAttributes = remoteAttributes;
    }
}
