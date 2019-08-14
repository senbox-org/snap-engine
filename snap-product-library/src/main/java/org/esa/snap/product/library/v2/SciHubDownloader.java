package org.esa.snap.product.library.v2;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.esa.snap.product.library.v2.parameters.QueryFilter;
import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.param.ParameterName;
import ro.cs.tao.datasource.param.QueryParameter;
import ro.cs.tao.datasource.remote.scihub.SciHubDataSource;
import ro.cs.tao.datasource.remote.scihub.parameters.SciHubParameterProvider;
import ro.cs.tao.datasource.util.HttpMethod;
import ro.cs.tao.datasource.util.NetUtils;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.Polygon2D;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
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

    public static String[] getSupportedSensors() {
        SciHubParameterProvider sciHubParameterProvider = new SciHubParameterProvider();
        return sciHubParameterProvider.getSupportedSensors();
    }

    public static List<QueryFilter> getSensorParameters(String sensor) {
        SciHubParameterProvider sciHubParameterProvider = new SciHubParameterProvider();
        Map<String, Map<ParameterName, DataSourceParameter>> supportedParameters = sciHubParameterProvider.getSupportedParameters();
        Map<ParameterName, DataSourceParameter> sensorParameters = supportedParameters.get(sensor);
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

    private static DataQuery buildDataQuery(String username, String password, String sensor, Map<String, Object> parametersValues) {
        DataSource dataSource = getDatasourceRegistry().getService(SciHubDataSource.class);
        dataSource.setCredentials(username, password);

        DataQuery query = dataSource.createQuery(sensor);

        Iterator<Map.Entry<String, Object>> it = parametersValues.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            String parameterName = entry.getKey();
            if (parameterName.equals(CommonParameterNames.FOOTPRINT)) {
                Rectangle.Double selectionArea = (Rectangle.Double)entry.getValue();
                Polygon2D polygon2D = new Polygon2D();
                polygon2D.append(selectionArea.x, selectionArea.y);
                polygon2D.append(selectionArea.x + selectionArea.width, selectionArea.y);
                polygon2D.append(selectionArea.x + selectionArea.width, selectionArea.y + selectionArea.height);
                polygon2D.append(selectionArea.x, selectionArea.y + selectionArea.height);
                polygon2D.append(selectionArea.x, selectionArea.y);
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
        return query;
    }

    public static List<ProductLibraryItem> downloadProductList(Credentials credentials, String sensor, Map<String, Object> parametersValues,
                                                               IProductsDownloaderListener downloaderListener, IThread thread, int pageSize) {

        DataQuery query = buildDataQuery(credentials.getUserPrincipal().getName(), credentials.getPassword(), sensor, parametersValues);
        query.setPageNumber(0);
        query.setPageSize(0);
        long productCount = query.getCount();

        if (thread != null && !thread.isRunning()) {
            return null; // stop running
        }

        downloaderListener.notifyProductCount(productCount);

        List<ProductLibraryItem> totalResults;
        if (productCount > 0) {
            long totalPageNumber = productCount / pageSize;
            if (productCount % pageSize != 0) {
                totalPageNumber++;
            }

            query.setPageSize(pageSize);

            totalResults = new ArrayList<ProductLibraryItem>();
            for (int pageNumber=1; pageNumber<=totalPageNumber; pageNumber++) {
                query.setPageNumber(pageNumber);

                if (thread != null && !thread.isRunning()) {
                    return null; // stop running
                }

                List<EOProduct> pageResults = query.execute();

                if (thread != null && !thread.isRunning()) {
                    return null; // stop running
                }

                List<ProductLibraryItem> downloadedPageProducts = new ArrayList<>(pageResults.size());
                for (int i=0; i<pageResults.size(); i++) {
                    EOProduct product = pageResults.get(i);
                    ProductLibraryItem productLibraryItem = new ProductLibraryItem();
                    productLibraryItem.setName(product.getName());
                    productLibraryItem.setType(product.getProductType());
                    productLibraryItem.setQuickLookLocation(product.getQuicklookLocation());
                    productLibraryItem.setApproximateSize(product.getApproximateSize());
                    productLibraryItem.setAcquisitionDate(product.getAcquisitionDate());

                    downloadedPageProducts.add(productLibraryItem);
                    totalResults.add(productLibraryItem);
                }

                downloaderListener.notifyPageProducts(pageNumber, downloadedPageProducts);
            }
        } else {
            totalResults = Collections.emptyList();
        }
        return totalResults;
    }

    public static BufferedImage downloadQuickLookImage(String url, Credentials credentials, IThread thread) throws IOException {
        try (CloseableHttpResponse response = NetUtils.openConnection(HttpMethod.GET, url, credentials)) {
            if (response != null) {

                if (thread != null && !thread.isRunning()) {
                    return null; // stop running
                }

                StatusLine statusLine = response.getStatusLine();
                switch (statusLine.getStatusCode()) {
                    case 200:
                        HttpEntity entity = response.getEntity();
                        InputStream inputStream = entity.getContent();
                        if (inputStream == null) {
                            return null;
                        }
                        try {
                            if (thread != null && !thread.isRunning()) {
                                return null; // stop running
                            }

                            ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);
                            if (imageInputStream == null) {
                                return null;
                            }

                            if (thread != null && !thread.isRunning()) {
                                return null; // stop running
                            }

                            BufferedImage bufferedImage = ImageIO.read(imageInputStream);
                            if (bufferedImage == null) {
                                imageInputStream.close();
                            }
                            return bufferedImage;
                        } finally {
                            inputStream.close();
                        }
                    case 401:
                        throw new IOException("401: Unauthorized or the supplied credentials are invalid");
                    default:
                        throw new IOException(String.valueOf(statusLine.getStatusCode()) + ": " + statusLine.getReasonPhrase());
                }
            } else {
                throw new IOException(String.format("Null response (maybe url %s is not reachable", url));
            }
        }
    }

    private static ServiceRegistry<DataSource> getDatasourceRegistry() {
        return ServiceRegistryManager.getInstance().getServiceRegistry(DataSource.class);
    }
}
