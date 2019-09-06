package org.esa.snap.remote.products.repository.tao;

import org.esa.snap.remote.products.repository.Attribute;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import ro.cs.tao.eodata.EOProduct;

import java.awt.image.BufferedImage;
import java.util.Date;

/**
 * Created by jcoravu on 28/8/2019.
 */
public abstract class AbstractTAORepositoryProduct implements RepositoryProduct {

    protected final EOProduct product;
    private final String mission;
    private final String downloadURL;

    protected Attribute[] attributes;
    private BufferedImage quickLookImage;

    protected AbstractTAORepositoryProduct(EOProduct product, String mission) {
        this.product = product;
        this.mission = mission;
        this.downloadURL = product.getLocation();
    }

    @Override
    public Attribute[] getAttributes() {
        return attributes;
    }

    @Override
    public String getMission() {
        return mission;
    }

    @Override
    public String getName() {
        return this.product.getName();
    }

    @Override
    public long getApproximateSize() {
        return this.product.getApproximateSize();
    }

    @Override
    public String getDownloadQuickLookImageURL() {
        return this.product.getQuicklookLocation();
    }

    @Override
    public String getDownloadURL() {
        return this.downloadURL;
    }

    @Override
    public Date getAcquisitionDate() {
        return this.product.getAcquisitionDate();
    }

    @Override
    public BufferedImage getQuickLookImage() {
        return quickLookImage;
    }

    @Override
    public void setQuickLookImage(BufferedImage quickLookImage) {
        this.quickLookImage = quickLookImage;
    }
}
