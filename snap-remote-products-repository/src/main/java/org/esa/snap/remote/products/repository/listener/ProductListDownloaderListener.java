package org.esa.snap.remote.products.repository.listener;

import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.util.List;

/**
 * Created by jcoravu on 9/8/2019.
 */
public interface ProductListDownloaderListener {

    public void notifyProductCount(long totalProductCount);

    public void notifyPageProducts(int pageNumber, List<RepositoryProduct> pageResults, long totalProductCount, int retrievedProductCount);
}
