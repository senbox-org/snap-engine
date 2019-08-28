package org.esa.snap.product.library.v2;

import java.util.List;

/**
 * Created by jcoravu on 9/8/2019.
 */
public interface ProductsDownloaderListener {

    public void notifyProductCount(long totalProductCount);

    public void notifyPageProducts(int pageNumber, List<RepositoryProduct> pageResults, long totalProductCount, int retrievedProductCount);
}
