package org.esa.snap.remote.products.repository.tao;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.esa.snap.remote.products.repository.Polygon2D;
import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.ThreadStatus;
import org.esa.snap.remote.products.repository.listener.DownloadProductProgressListener;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.esa.snap.remote.products.repository.listener.ProgressListener;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.datasource.ProductFetchStrategy;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.param.ParameterName;
import ro.cs.tao.datasource.param.QueryParameter;
import ro.cs.tao.datasource.remote.DownloadStrategy;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.utils.HttpMethod;
import ro.cs.tao.utils.NetUtils;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by jcoravu on 28/8/2019.
 */
public class TAORemoteRepositoryProvider implements RemoteProductsRepositoryProvider {

    private final DataSource dataSource;

    private final Map<RepositoryProduct, DownloadStrategy> downloadingProducts;

    public TAORemoteRepositoryProvider(DataSource dataSource) {
        this.dataSource = dataSource;

        this.downloadingProducts = new HashMap<>();
    }

    @Override
    public String getRepositoryName() {
        return this.dataSource.defaultId();
    }

    @Override
    public int getMaximumAllowedTransfersPerAccount() {
        return this.dataSource.getMaximumAllowedTransfers();
    }

    @Override
    public boolean requiresAuthentication() {
        return this.dataSource.requiresAuthentication();
    }

    @Override
    public String[] getAvailableMissions() {
        return this.dataSource.getSupportedSensors();
    }

    @Override
    public List<RepositoryQueryParameter> getMissionParameters(String mission) {
        Map<String, Map<ParameterName, DataSourceParameter>> supportedParameters = this.dataSource.getSupportedParameters();
        Map<ParameterName, DataSourceParameter> sensorParameters = supportedParameters.get(mission);
        Iterator<Map.Entry<ParameterName, DataSourceParameter>> it = sensorParameters.entrySet().iterator();
        List<RepositoryQueryParameter> parameters = new ArrayList<RepositoryQueryParameter>(sensorParameters.size());
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
            RepositoryQueryParameter queryParameter = new RepositoryQueryParameter(param.getName(), type, param.getLabel(), param.getDefaultValue(), required, param.getValueSet());
            parameters.add(queryParameter);
        }
        return parameters;
    }

    @Override
    public BufferedImage downloadProductQuickLookImage(Credentials credentials, String url, ThreadStatus thread) throws IOException, InterruptedException {
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

        List<RepositoryProduct> productList;
        if (totalProductCount == 0) {
            downloaderListener.notifyProductCount(totalProductCount);

            productList = Collections.emptyList();
        } else if (totalProductCount > 0) {
            downloaderListener.notifyProductCount(totalProductCount);

            int pageSize = 100;
            long totalPageNumber = totalProductCount / pageSize;
            if (totalProductCount % pageSize != 0) {
                totalPageNumber++;
            }

            query.setPageSize(pageSize);

            WKTReader wktReader = new WKTReader();
            productList = new ArrayList<>();
            for (int pageNumber = 1; pageNumber <= totalPageNumber && productList.size() < totalProductCount; pageNumber++) {
                query.setPageNumber(pageNumber);

                ThreadStatus.checkCancelled(thread);

                List<EOProduct> pageResults = query.execute();

                ThreadStatus.checkCancelled(thread);

                List<RepositoryProduct> downloadedPageProducts = downloadProductList(mission, pageResults, wktReader);
                productList.addAll(downloadedPageProducts);

                ThreadStatus.checkCancelled(thread);

                downloaderListener.notifyPageProducts(pageNumber, downloadedPageProducts, totalProductCount, productList.size());
            }
        } else {
            List<EOProduct> pageResults = query.execute();

            ThreadStatus.checkCancelled(thread);

            WKTReader wktReader = new WKTReader();
            List<RepositoryProduct> downloadedPageProducts = downloadProductList(mission, pageResults, wktReader);
            productList = new ArrayList<>(downloadedPageProducts);

            downloaderListener.notifyPageProducts(1, downloadedPageProducts, productList.size(), productList.size());
        }
        return productList;
    }

    @Override
    public Map<String, String> getDisplayedAttributes() {
        return null;
    }

    @Override
    public void cancelDownloadProduct(RepositoryProduct repositoryProduct) {
        DownloadStrategy downloadStrategy;
        synchronized (this.downloadingProducts) {
            downloadStrategy = this.downloadingProducts.remove(repositoryProduct);
        }
        if (downloadStrategy != null) {
            downloadStrategy.cancel();
        }
    }

    @Override
    public Path downloadProduct(RepositoryProduct repositoryProduct, Credentials credentials, Path targetFolderPath, ProgressListener progressListener)
            throws Exception {

        DownloadStrategy downloadStrategy = null;
        try {
            synchronized (this.downloadingProducts) {
                downloadStrategy = this.downloadingProducts.get(repositoryProduct);
                if (downloadStrategy == null) {
                    DataSource newDataSource = this.dataSource.getClass().newInstance();
                    newDataSource.setCredentials(credentials.getUserPrincipal().getName(), credentials.getPassword());
                    ProductFetchStrategy productFetchStrategy = newDataSource.getProductFetchStrategy(repositoryProduct.getMission());
                    if (productFetchStrategy == null) {
                        throw new NullPointerException("The download strategy is null for mission '" + repositoryProduct.getMission() + "'.");
                    }
                    downloadStrategy = (DownloadStrategy) productFetchStrategy.clone();
                    this.downloadingProducts.put(repositoryProduct, downloadStrategy);
                } else {
                    throw new IllegalArgumentException("The product '" + repositoryProduct.getName()+"' is already downloading.");
                }
            }

            Properties properties = new Properties();
            properties.put("auto.uncompress", "true");
            downloadStrategy.addProperties(properties);
            downloadStrategy.setCredentials(new UsernamePasswordCredentials(credentials.getUserPrincipal().getName(), credentials.getPassword()));
            downloadStrategy.setDestination(targetFolderPath.toString());
            downloadStrategy.setFetchMode(FetchMode.OVERWRITE);
            downloadStrategy.setProgressListener(new DownloadProductProgressListener(progressListener));

            try {
                EOProduct product = ((TAORepositoryProduct)repositoryProduct).getProduct();
                return downloadStrategy.fetch(product);
            } catch (ro.cs.tao.datasource.InterruptedException exception) {
                throw new java.lang.InterruptedException();
            }
        } finally {
            if (downloadStrategy != null) {
                synchronized (this.downloadingProducts) {
                    this.downloadingProducts.remove(repositoryProduct);
                }
            }
        }
    }

    private DataQuery buildDataQuery(String username, String password, String mission, Map<String, Object> parametersValues) throws InstantiationException, IllegalAccessException {
        DataSource newDataSource = this.dataSource.getClass().newInstance();
        newDataSource.setCredentials(username, password);

        DataQuery query = newDataSource.createQuery(mission);

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
        if (startDate != null) {
            QueryParameter<Date> begin = query.createParameter(CommonParameterNames.START_DATE, Date.class);
            begin.setValue(startDate);
            query.addParameter(begin);
        }
        Date endDate = (Date)parametersValues.get(CommonParameterNames.END_DATE);
        if (endDate != null) {
            QueryParameter<Date> end = query.createParameter(CommonParameterNames.END_DATE, Date.class);
            end.setValue(endDate);
            query.addParameter(end);
        }

        return query;
    }

    private static List<RepositoryProduct> downloadProductList(String mission, List<EOProduct> pageResults, WKTReader wktReader) throws InterruptedException, ParseException {
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
            TAORepositoryProduct repositoryProduct = new TAORepositoryProduct(product, mission, polygon);
            downloadedPageProducts.add(repositoryProduct);
        }
        return downloadedPageProducts;
    }
}
