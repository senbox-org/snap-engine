package org.esa.snap.product.library.v2.repository.usgs;

import org.esa.snap.product.library.v2.RepositoryProduct;
import org.esa.snap.product.library.v2.repository.AbstractTAORemoteRepositoryProvider;
import org.esa.snap.product.library.v2.repository.ProductRepositoryDownloader;
import org.esa.snap.product.library.v2.repository.scihub.SciHubRepositoryProduct;
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
        return null;
    }
}
