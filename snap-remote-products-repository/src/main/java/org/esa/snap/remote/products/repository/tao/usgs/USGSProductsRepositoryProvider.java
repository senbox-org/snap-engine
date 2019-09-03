package org.esa.snap.remote.products.repository.tao.usgs;

import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.tao.AbstractTAORemoteRepositoryProvider;
import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import ro.cs.tao.datasource.usgs.USGSDataSource;
import ro.cs.tao.eodata.EOProduct;

/**
 * Created by jcoravu on 28/8/2019.
 */
public class USGSProductsRepositoryProvider extends AbstractTAORemoteRepositoryProvider<USGSDataSource> {

    public USGSProductsRepositoryProvider() {
    }

    @Override
    protected RepositoryProduct buildRepositoryProduct(EOProduct product, String mission) {
        return new USGSRepositoryProduct(product, mission);
    }

    @Override
    protected Class<USGSDataSource> getDataSourceClass() {
        return USGSDataSource.class;
    }

    @Override
    public String getRepositoryName() {
        return "USGS";
    }

    @Override
    public ProductRepositoryDownloader buidProductDownloader(String mission) {
        return new USGSProductRepositoryDownloader(mission);
    }
}
