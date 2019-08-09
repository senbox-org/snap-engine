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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 7/8/2019.
 */
public class SciHubDownloader {

    public static List<EOProduct> downloadProductList(String username, String password, String sensor, Map<String, Object> parametersValues) {
        DataSource dataSource = getDatasourceRegistry().getService(SciHubDataSource.class);
        dataSource.setCredentials(username, password);

        DataQuery query = dataSource.createQuery(sensor);

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
            query.addParameter(begin);
        }

        long count = query.getCount();
        System.out.println("count="+count);

        query.setPageSize(100);
        query.setMaxResults(100);
        List<EOProduct> results = query.execute();
        System.out.println("results.size="+results.size());
        return results;
    }

    public static List<EOProduct> downloadProductList(String username, String password, String sensor, Date startDate, Date endDate, Polygon2D areaOfInterest, double cloudCover) {
        DataSource dataSource = getDatasourceRegistry().getService(SciHubDataSource.class);
        dataSource.setCredentials(username, password);

        DataQuery query = dataSource.createQuery(sensor);
        //query.addParameter(CommonParameterNames.PLATFORM, "Sentinel-2");

        QueryParameter<Date> begin = query.createParameter(CommonParameterNames.START_DATE, Date.class);
        begin.setMinValue(startDate);
        begin.setMaxValue(endDate);
        query.addParameter(begin);

        query.addParameter(CommonParameterNames.FOOTPRINT, areaOfInterest);

//        query.addParameter(CommonParameterNames.CLOUD_COVER, cloudCover);
        query.setPageSize(2);
        query.setMaxResults(100);
        List<EOProduct> results = query.execute();
        return results;
    }

    private static ServiceRegistry<DataSource> getDatasourceRegistry() {
        return ServiceRegistryManager.getInstance().getServiceRegistry(DataSource.class);
    }
}
