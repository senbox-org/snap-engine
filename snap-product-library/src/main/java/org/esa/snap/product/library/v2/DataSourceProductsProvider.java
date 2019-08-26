package org.esa.snap.product.library.v2;

import org.esa.snap.product.library.v2.parameters.QueryFilter;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by jcoravu on 26/8/2019.
 */
public interface DataSourceProductsProvider {

    public String getName();

    public String[] getAvailableMissions();

    public List<QueryFilter> getMissionParameters(String mission);

    public DataSourceResultsDownloader buildResultsDownloader();

    public DataSourceProductDownloader buidProductDownloader(String mission, Path targetFolderPath);
}
