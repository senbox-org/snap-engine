package org.esa.snap.remote.products.repository.tao;

import org.esa.snap.remote.products.repository.RepositoryProduct;
import ro.cs.tao.eodata.EOProduct;

import java.util.Date;

/**
 * Created by jcoravu on 28/8/2019.
 */
public abstract class AbstractTAORepositoryProduct implements RepositoryProduct {

    protected final EOProduct product;
    private final String mission;

    protected AbstractTAORepositoryProduct(EOProduct product, String mission) {
        this.product = product;
        this.mission = mission;
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
    public String getQuickLookLocation() {
        return this.product.getQuicklookLocation();
    }

    @Override
    public String getLocation() {
        return this.product.getLocation();
    }

    @Override
    public Date getAcquisitionDate() {
        return this.product.getAcquisitionDate();
    }
}
