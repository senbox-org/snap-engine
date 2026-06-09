package org.esa.snap.remote.products.repository.cdse;

import org.esa.snap.remote.products.repository.RepositoryProduct;

import java.util.List;

class CdseSearchResult {

    private final long totalCount;
    private final List<RepositoryProduct> products;

    CdseSearchResult(long totalCount, List<RepositoryProduct> products) {
        this.totalCount = totalCount;
        this.products = products;
    }

    long getTotalCount() {
        return totalCount;
    }

    List<RepositoryProduct> getProducts() {
        return products;
    }
}
