package org.esa.snap.product.library.v2.repository.scihub;

import org.esa.snap.product.library.v2.RepositoryProduct;
import org.esa.snap.product.library.v2.repository.AbstractTAORemoteRepositoryProvider;
import org.esa.snap.product.library.v2.repository.ProductRepositoryDownloader;
import ro.cs.tao.datasource.remote.scihub.SciHubDataSource;
import ro.cs.tao.eodata.EOProduct;

/**
 * Created by jcoravu on 26/8/2019.
 */
public class SciHubProductsRepositoryProvider extends AbstractTAORemoteRepositoryProvider<SciHubDataSource> {

    public SciHubProductsRepositoryProvider() {
    }

    @Override
    protected RepositoryProduct buildRepositoryProduct(EOProduct product, String mission) {
        return new SciHubRepositoryProduct(product, mission);
    }

    @Override
    protected Class<SciHubDataSource> getDataSourceClass() {
        return SciHubDataSource.class;
    }

    @Override
    public String getRepositoryName() {
        return "ESA SciHub";
    }

    @Override
    public ProductRepositoryDownloader buidProductDownloader(String mission) {
        return new SciHubProductRepositoryDownloader(mission);
    }
}
