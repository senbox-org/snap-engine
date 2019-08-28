package org.esa.snap.product.library.v2.scihub;

import org.esa.snap.product.library.v2.repository.ProductRepositoryDownloader;
import org.esa.snap.product.library.v2.repository.ProductsRepositoryProvider;
import org.esa.snap.product.library.v2.repository.ProductListRepositoryDownloader;
import org.esa.snap.product.library.v2.parameters.QueryFilter;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.param.ParameterName;
import ro.cs.tao.datasource.remote.scihub.parameters.SciHubParameterProvider;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 26/8/2019.
 */
public class SciHubProductsRepositoryProvider implements ProductsRepositoryProvider {

    public SciHubProductsRepositoryProvider() {
    }

    @Override
    public String getRepositoryName() {
        return "ESA SciHub";
    }

    @Override
    public String[] getAvailableMissions() {
        SciHubParameterProvider sciHubParameterProvider = new SciHubParameterProvider();
        return sciHubParameterProvider.getSupportedSensors();
    }

    @Override
    public List<QueryFilter> getMissionParameters(String mission) {
        SciHubParameterProvider sciHubParameterProvider = new SciHubParameterProvider();
        Map<String, Map<ParameterName, DataSourceParameter>> supportedParameters = sciHubParameterProvider.getSupportedParameters();
        Map<ParameterName, DataSourceParameter> sensorParameters = supportedParameters.get(mission);
        Iterator<Map.Entry<ParameterName, DataSourceParameter>> it = sensorParameters.entrySet().iterator();
        List<QueryFilter> parameters = new ArrayList<QueryFilter>(sensorParameters.size());
        while (it.hasNext()) {
            Map.Entry<ParameterName, DataSourceParameter> entry = it.next();
            DataSourceParameter param = entry.getValue();
            Class<?> type;
            if (param.getName().equals(CommonParameterNames.FOOTPRINT)) {
                type = Rectangle.Double.class;
            } else {
                type = param.getType();
            }
            boolean required = param.isRequired();
            if (!required) {
                if (param.getName().equals(CommonParameterNames.PLATFORM) || param.getName().equals(CommonParameterNames.START_DATE)
                    || param.getName().equals(CommonParameterNames.END_DATE) || param.getName().equals(CommonParameterNames.FOOTPRINT)) {

                    required = true;
                }
            }
            QueryFilter queryParameter = new QueryFilter(param.getName(), type, param.getLabel(), param.getDefaultValue(), required, param.getValueSet());
            parameters.add(queryParameter);
        }
        return parameters;
    }

    @Override
    public ProductListRepositoryDownloader buildResultsDownloader() {
        return new SciHubProductListRepositoryDownloader();
    }

    @Override
    public ProductRepositoryDownloader buidProductDownloader(String mission) {
        return new SciHubProductRepositoryDownloader(mission);
    }
}
