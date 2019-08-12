package org.esa.snap.product.library.v2;

import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.QueryParameter;
import ro.cs.tao.datasource.remote.scihub.SciHubDataSource;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.Polygon2D;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class SciHubDownloader {

    private static final byte MAXIMUM_RESULTS_PER_PAGE = 100;

    private final DataQuery query;

    public SciHubDownloader(String username, String password, String sensor, Map<String, Object> parametersValues) {
        DataSource dataSource = getDatasourceRegistry().getService(SciHubDataSource.class);
        dataSource.setCredentials(username, password);

        this.query = dataSource.createQuery(sensor);

        Iterator<Map.Entry<String, Object>> it = parametersValues.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            String parameterName = entry.getKey();
            if (parameterName.equals(CommonParameterNames.FOOTPRINT)) {
                String value = (String)entry.getValue();
                Polygon2D polygon2D = Polygon2D.fromWKT(value);
                query.addParameter(parameterName, polygon2D);
            } else if (!parameterName.equals(CommonParameterNames.START_DATE) && !parameterName.equals(CommonParameterNames.END_DATE)) {
                query.addParameter(parameterName, entry.getValue());
            }
        }
        Date startDate = (Date)parametersValues.get(CommonParameterNames.START_DATE);
        Date endDate = (Date)parametersValues.get(CommonParameterNames.END_DATE);
        if (startDate != null || endDate != null) {
            QueryParameter<Date> begin = query.createParameter(CommonParameterNames.START_DATE, Date.class);
            begin.setMinValue(startDate);
            begin.setMaxValue(endDate);
            this.query.addParameter(begin);
        }
    }

    public List<EOProduct> downloadProducts(IProductsDownloaderListener downloaderListener) {
        long productCount = this.query.getCount();

        downloaderListener.notifyProductCount(productCount);

        List<EOProduct> totalResults;
        if (productCount > 0) {
            int pageSize = 1;

            long totalPageNumber = productCount / pageSize;
            if (productCount % pageSize != 0) {
                totalPageNumber++;
            }

            totalResults = new ArrayList<EOProduct>();
            for (int pageNumber=1; pageNumber<=totalPageNumber; pageNumber++) {
                this.query.setPageSize(pageSize);
                this.query.setPageNumber(pageNumber);
                List<EOProduct> pageResults = this.query.execute();
                totalResults.addAll(pageResults);

                downloaderListener.notifyPageProducts(pageNumber, pageResults);
            }
        } else {
            totalResults = Collections.emptyList();
        }
        System.out.println("totalResults.size="+totalResults.size());
        return totalResults;
    }

    private static ServiceRegistry<DataSource> getDatasourceRegistry() {
        return ServiceRegistryManager.getInstance().getServiceRegistry(DataSource.class);
    }
}
