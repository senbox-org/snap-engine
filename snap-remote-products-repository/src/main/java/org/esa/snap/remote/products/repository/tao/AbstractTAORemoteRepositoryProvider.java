package org.esa.snap.remote.products.repository.tao;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.remote.products.repository.QueryFilter;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.ThreadStatus;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.param.ParameterName;
import ro.cs.tao.datasource.param.QueryParameter;
import ro.cs.tao.datasource.remote.ProductHelper;
import ro.cs.tao.datasource.util.HttpMethod;
import ro.cs.tao.datasource.util.NetUtils;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.spi.ServiceRegistry;
import ro.cs.tao.spi.ServiceRegistryManager;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 28/8/2019.
 */
public abstract class AbstractTAORemoteRepositoryProvider<T extends DataSource> implements RemoteProductsRepositoryProvider {

    protected AbstractTAORemoteRepositoryProvider() {
    }

    protected abstract AbstractTAORepositoryProduct buildRepositoryProduct(EOProduct product, String mission, Polygon2D polygon);

    protected abstract Class<T> getDataSourceClass();

    protected abstract DataSource buildNewDataSource() throws URISyntaxException;

    protected abstract ProductHelper buildProductHelper(String productName);

    @Override
    public boolean requiresAuthentication() {
        DataSource dataSource = getDataSource();
        return dataSource.requiresAuthentication();
    }

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
                type = Rectangle2D.class;
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
                                                       ProductListDownloaderListener downloaderListener, ThreadStatus thread)
                                                       throws Exception {

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

            totalResults = new ArrayList<>();
            WKTReader wktReader = new WKTReader();
            for (int pageNumber=1; pageNumber<=totalPageNumber && totalResults.size() < totalProductCount; pageNumber++) {
                query.setPageNumber(pageNumber);

                ThreadStatus.checkCancelled(thread);

                List<EOProduct> pageResults = query.execute();

                ThreadStatus.checkCancelled(thread);

                List<RepositoryProduct> downloadedPageProducts = new ArrayList<>(pageResults.size());
                for (int i=0; i<pageResults.size(); i++) {
                    EOProduct product = pageResults.get(i);
                    Geometry productGeometry = wktReader.read(product.getGeometry());
                    if (!(productGeometry instanceof Polygon)) {
                        throw new IllegalStateException("The product geometry type '"+productGeometry.getClass().getName()+"' is not a '"+Polygon.class.getName()+"' type.");
                    }
                    Coordinate[] coordinates = ((Polygon)productGeometry).getExteriorRing().getCoordinates();
                    Coordinate firstCoordinate = coordinates[0];
                    Coordinate lastCoordinate = coordinates[coordinates.length-1];
                    if (firstCoordinate.getX() != lastCoordinate.getX() || firstCoordinate.getY() != lastCoordinate.getY()) {
                        throw new IllegalStateException("The first and last coordinates of the polygon do not match.");
                    }
                    Polygon2D polygon = new Polygon2D();
                    for (Coordinate coordinate : coordinates) {
                        polygon.append(coordinate.getX(), coordinate.getY());
                    }
                    ProductHelper productHelper = buildProductHelper(product.getName());
                    product.setEntryPoint(productHelper.getMetadataFileName());
                    AbstractTAORepositoryProduct repositoryProduct = buildRepositoryProduct(product, mission, polygon);
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

    @Override
    public Map<String, String> getDisplayedAttributes() {
        return null;
    }

    private DataSource getDataSource() {
        ServiceRegistry<DataSource> serviceRegistry = ServiceRegistryManager.getInstance().getServiceRegistry(DataSource.class);
        return serviceRegistry.getService(getDataSourceClass());
    }

    private DataQuery buildDataQuery(String username, String password, String mission, Map<String, Object> parametersValues) throws URISyntaxException {
        DataSource dataSource = buildNewDataSource();
        dataSource.setCredentials(username, password);

        DataQuery query = dataSource.createQuery(mission);

        Iterator<Map.Entry<String, Object>> it = parametersValues.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            String parameterName = entry.getKey();
            if (parameterName.equals(CommonParameterNames.FOOTPRINT)) {
                Rectangle2D selectionArea = (Rectangle2D) entry.getValue();

                ro.cs.tao.eodata.Polygon2D polygon2D = new ro.cs.tao.eodata.Polygon2D();
                polygon2D.append(selectionArea.getX(), selectionArea.getY()); // the top left corner
                polygon2D.append(selectionArea.getX() + selectionArea.getWidth(), selectionArea.getY()); // the top right corner
                polygon2D.append(selectionArea.getX() + selectionArea.getWidth(), selectionArea.getY() + selectionArea.getHeight()); // the bottom right corner
                polygon2D.append(selectionArea.getX(), selectionArea.getY() + selectionArea.getHeight()); // the bottom left corner
                polygon2D.append(selectionArea.getX(), selectionArea.getY()); // the top left corner
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
