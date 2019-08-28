package org.esa.snap.product.library.v2.repository;

import org.esa.snap.product.library.v2.parameters.QueryFilter;

import java.util.List;

/**
 * Created by jcoravu on 26/8/2019.
 */
public interface ProductsRepositoryProvider {

    public String getRepositoryName();

    public String[] getAvailableMissions();

    public List<QueryFilter> getMissionParameters(String mission);

    public ProductListRepositoryDownloader buildResultsDownloader();

    public ProductRepositoryDownloader buidProductDownloader(String mission);
}
