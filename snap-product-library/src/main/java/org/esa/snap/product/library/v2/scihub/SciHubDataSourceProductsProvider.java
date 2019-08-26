package org.esa.snap.product.library.v2.scihub;

import org.esa.snap.product.library.v2.DataSourceProductDownloader;
import org.esa.snap.product.library.v2.DataSourceProductsProvider;
import org.esa.snap.product.library.v2.DataSourceResultsDownloader;
import org.esa.snap.product.library.v2.parameters.QueryFilter;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.param.ParameterName;
import ro.cs.tao.datasource.remote.scihub.parameters.SciHubParameterProvider;

import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 26/8/2019.
 */
public class SciHubDataSourceProductsProvider implements DataSourceProductsProvider {

    public SciHubDataSourceProductsProvider() {
    }

    @Override
    public String getName() {
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
            QueryFilter queryParameter = new QueryFilter(param.getName(), type, param.getLabel(), param.getDefaultValue(), param.isRequired(), param.getValueSet());
            parameters.add(queryParameter);
        }
        return parameters;
    }

    @Override
    public DataSourceResultsDownloader buildResultsDownloader() {
        return new SciHubDataSourceResultsDownloader();
    }

    @Override
    public DataSourceProductDownloader buidProductDownloader(String mission, Path targetFolderPath) {
        return new SciHubDataSourceProductDownloader(mission, targetFolderPath);
    }
}
