package org.esa.snap.product.library.v2.repository;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.esa.snap.product.library.v2.ProductsDownloaderListener;
import org.esa.snap.product.library.v2.RepositoryProduct;
import org.esa.snap.product.library.v2.ThreadStatus;
import org.esa.snap.product.library.v2.parameters.QueryFilter;
import org.esa.snap.product.library.v2.repository.scihub.SciHubRepositoryProduct;
import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.param.ParameterName;
import ro.cs.tao.datasource.param.QueryParameter;
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
 * Created by jcoravu on 28/8/2019.
 */
public abstract class AbstractTAORemoteRepositoryProvider<T extends DataSource> implements ProductsRepositoryProvider {

    protected AbstractTAORemoteRepositoryProvider() {
    }

    protected abstract RepositoryProduct buildRepositoryProduct(EOProduct product, String mission);

    protected abstract Class<T> getDataSourceClass();

    @Override
    public String[] getAvailableMissions() {
        DataSource dataSource = getDataSource();
        return dataSource.getSupportedSensors();
    }
    @Override
    public String getRepositoryId() {
        DataSource dataSource = getDataSource();
        return dataSource.getId();
    }

    @Override
    public List<QueryFilter> getMissionParameters(String mission) {
        DataSource dataSource = getDataSource();
        Map<String, Map<ParameterName, DataSourceParameter>> supportedParameters = dataSource.getSupportedParameters();
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
    public BufferedImage downloadProductQuickLookImage(Credentials credentials, String url, ThreadStatus thread) throws IOException, java.lang.InterruptedException {
        try (CloseableHttpResponse response = NetUtils.openConnection(HttpMethod.GET, url, credentials)) {
            if (response != null) {

                ThreadStatus.checkCancelled(thread);

                StatusLine statusLine = response.getStatusLine();
                switch (statusLine.getStatusCode()) {
                    case 200:
                        HttpEntity entity = response.getEntity();
                        InputStream inputStream = entity.getContent();
                        if (inputStream == null) {
                            return null;
                        }
                        try {
                            ThreadStatus.checkCancelled(thread);

                            ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);
                            if (imageInputStream == null) {
                                return null;
                            }

                            ThreadStatus.checkCancelled(thread);

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

    @Override
    public List<RepositoryProduct> downloadProductList(Credentials credentials, String mission, Map<String, Object> parameterValues,
                                                       ProductsDownloaderListener downloaderListener, ThreadStatus thread)
                                                       throws InterruptedException {

        DataQuery query = buildDataQuery(credentials.getUserPrincipal().getName(), credentials.getPassword(), mission, parameterValues);
        query.setPageNumber(0);
        query.setPageSize(0);
        long totalProductCount = query.getCount();

        ThreadStatus.checkCancelled(thread);

        downloaderListener.notifyProductCount(totalProductCount);

        int pageSize = 100;
        List<RepositoryProduct> totalResults;
        if (totalProductCount > 0) {
            long totalPageNumber = totalProductCount / pageSize;
            if (totalProductCount % pageSize != 0) {
                totalPageNumber++;
            }

            query.setPageSize(pageSize);

            totalResults = new ArrayList<RepositoryProduct>();
            for (int pageNumber=1; pageNumber<=totalPageNumber && totalResults.size() < totalProductCount; pageNumber++) {
                query.setPageNumber(pageNumber);

                ThreadStatus.checkCancelled(thread);

                List<EOProduct> pageResults = query.execute();

                ThreadStatus.checkCancelled(thread);

                List<RepositoryProduct> downloadedPageProducts = new ArrayList<>(pageResults.size());
                for (int i=0; i<pageResults.size(); i++) {
                    EOProduct product = pageResults.get(i);
                    RepositoryProduct repositoryProduct = buildRepositoryProduct(product, mission);
                    downloadedPageProducts.add(repositoryProduct);
                    totalResults.add(repositoryProduct);
                }

                ThreadStatus.checkCancelled(thread);

                downloaderListener.notifyPageProducts(pageNumber, downloadedPageProducts, totalProductCount, totalResults.size());
            }
        } else {
            totalResults = Collections.emptyList();
        }
        return totalResults;
    }

    private DataSource getDataSource() {
        ServiceRegistry<DataSource> serviceRegistry = ServiceRegistryManager.getInstance().getServiceRegistry(DataSource.class);
        return serviceRegistry.getService(getDataSourceClass());
    }

    private DataQuery buildDataQuery(String username, String password, String mission, Map<String, Object> parametersValues) {
        DataSource dataSource = getDataSource();
        dataSource.setCredentials(username, password);

        DataQuery query = dataSource.createQuery(mission);

        Iterator<Map.Entry<String, Object>> it = parametersValues.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            String parameterName = entry.getKey();
            if (parameterName.equals(CommonParameterNames.FOOTPRINT)) {
                Rectangle.Double selectionArea = (Rectangle.Double) entry.getValue();
                Polygon2D polygon2D = new Polygon2D();
                polygon2D.append(selectionArea.x, selectionArea.y); // the top left corner
                polygon2D.append(selectionArea.x + selectionArea.width, selectionArea.y); // the top right corner
                polygon2D.append(selectionArea.x + selectionArea.width, selectionArea.y + selectionArea.height); // the bottom right corner
                polygon2D.append(selectionArea.x, selectionArea.y + selectionArea.height); // the bottom left corner
                polygon2D.append(selectionArea.x, selectionArea.y); // the top left corner
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
}
