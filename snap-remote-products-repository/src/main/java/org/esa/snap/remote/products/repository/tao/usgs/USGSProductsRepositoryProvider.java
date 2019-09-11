package org.esa.snap.remote.products.repository.tao.usgs;

import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.tao.AbstractTAORemoteRepositoryProvider;
import ro.cs.tao.datasource.remote.ProductHelper;
import ro.cs.tao.datasource.usgs.USGSDataSource;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.products.landsat.Landsat8ProductHelper;

/**
 * Created by jcoravu on 28/8/2019.
 */
public class USGSProductsRepositoryProvider extends AbstractTAORemoteRepositoryProvider<USGSDataSource> {

    public USGSProductsRepositoryProvider() {
    }

    @Override
    protected USGSRepositoryProduct buildRepositoryProduct(EOProduct product, String mission, Polygon2D polygon) {
        return new USGSRepositoryProduct(product, mission, polygon);
    }

    @Override
    protected Class<USGSDataSource> getDataSourceClass() {
        return USGSDataSource.class;
    }

    @Override
    protected ProductHelper buildProductHelper(String productName) {
        return new Landsat8ProductHelper(productName);
    }

    @Override
    public String getRepositoryName() {
        return "USGS";
    }

    @Override
    public ProductRepositoryDownloader buidProductDownloader(String mission) {
        return new USGSProductRepositoryDownloader(mission, getRepositoryId());
    }
}
