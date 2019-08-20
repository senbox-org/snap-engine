package org.esa.snap.product.library.v2;

import java.util.List;

/**
 * Created by jcoravu on 9/8/2019.
 */
public interface IProductsDownloaderListener {

    void notifyProductCount(long totalProductCount);

    void notifyPageProducts(int pageNumber, List<ProductLibraryItem> pageResults, long totalProductCount, int retrievedProductCount);
}
