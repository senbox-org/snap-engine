package org.esa.snap.remote.products.repository.tao.usgs;

import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.tao.AbstractTAOProductRepositoryDownloader;
import ro.cs.tao.datasource.remote.SimpleArchiveDownloadStrategy;
import ro.cs.tao.eodata.EOProduct;

import java.util.Properties;

/**
 * Created by jcoravu on 3/9/2019.
 */
public class USGSProductRepositoryDownloader extends AbstractTAOProductRepositoryDownloader {

    public USGSProductRepositoryDownloader(String mission, String repositoryId) {
        super(mission, repositoryId);

        if (mission.equals("Landsat8")) {
            this.downloadStrategy = new SimpleArchiveDownloadStrategy(null, new Properties());
        } else {
            throw new IllegalArgumentException("Unknown mission '"+mission+"'.");
        }
    }

    @Override
    protected EOProduct getEOProduct(RepositoryProduct product) {
        return ((USGSRepositoryProduct)product).getProduct();
    }
}
