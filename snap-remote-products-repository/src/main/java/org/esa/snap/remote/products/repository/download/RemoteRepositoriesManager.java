package org.esa.snap.remote.products.repository.download;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.esa.snap.remote.products.repository.*;
import org.esa.snap.remote.products.repository.geometry.AbstractGeometry2D;
import org.esa.snap.remote.products.repository.geometry.GeometryUtils;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.esa.snap.remote.products.repository.listener.ProgressListener;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import ro.cs.tao.configuration.ConfigurationManager;
import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSource;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.DataSourceManager;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.DataSourceParameter;
import ro.cs.tao.datasource.param.ParameterName;
import ro.cs.tao.datasource.param.QueryParameter;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;
import ro.cs.tao.products.landsat.Landsat8ProductHelper;
import ro.cs.tao.utils.HttpMethod;
import ro.cs.tao.utils.NetUtils;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class represents a singleton and it is used to access the remote repositories to search the product list, download products.
 *
 * Created by jcoravu on 21/11/2019.
 */
public class RemoteRepositoriesManager {

    private static final Logger logger = Logger.getLogger(RemoteRepositoriesManager.class.getName());

    private static final RemoteRepositoriesManager instance = new RemoteRepositoriesManager();

    private final RemoteProductsRepositoryProvider[] remoteRepositoryProductProviders;
    private final Map<String, DataSourceComponent> downloadingProducts;

    private RemoteRepositoriesManager() {
        ConfigurationManager.setConfigurationProvider(new RemoteConfigurationProvider());
        Set<DataSource> services = DataSourceManager.getInstance().getRegisteredDataSources();
        this.remoteRepositoryProductProviders = new RemoteProductsRepositoryProvider[services.size()];
        int index = 0;
        for (DataSource dataSource : services) {
            Map<String, Map<String, Map<String, String>>> filteredParameters = null;
            if ("Alaska Satellite Facility".equals(dataSource.getId())) {
                filteredParameters = buildAlaskaSatelliteFacilityParameterFilter();
            } else if ("Amazon Web Services".equals(dataSource.getId())) {
            } else if ("Scientific Data Hub".equals(dataSource.getId())) {
            } else if ("USGS".equals(dataSource.getId())) {
            }
            if (filteredParameters != null && filteredParameters.size() > 0) {
//                dataSource.setFilteredParameters(filteredParameters);
            }
            this.remoteRepositoryProductProviders[index++] = new RemoteProductsRepositoryProviderImpl(dataSource.getId());
        }

        if (this.remoteRepositoryProductProviders.length > 1) {
            // sort alphabetically by repository name
            Comparator<RemoteProductsRepositoryProvider> comparator = new Comparator<RemoteProductsRepositoryProvider>() {
                @Override
                public int compare(RemoteProductsRepositoryProvider leftProvider, RemoteProductsRepositoryProvider rightProvider) {
                    return leftProvider.getRepositoryName().compareToIgnoreCase(rightProvider.getRepositoryName());
                }
            };
            for (int i=0; i<this.remoteRepositoryProductProviders.length-1; i++) {
                for (int j=i+1; j<this.remoteRepositoryProductProviders.length; j++) {
                    int result = comparator.compare(this.remoteRepositoryProductProviders[i], this.remoteRepositoryProductProviders[j]);
                    if (result > 0) {
                        RemoteProductsRepositoryProvider aux = this.remoteRepositoryProductProviders[i];
                        this.remoteRepositoryProductProviders[i] = this.remoteRepositoryProductProviders[j];
                        this.remoteRepositoryProductProviders[j] = aux;
                    }
                }
            }
        }

        this.downloadingProducts = new HashMap<>();
    }

    public static RemoteRepositoriesManager getInstance() {
        return instance;
    }

    public RemoteProductsRepositoryProvider[] getRemoteProductsRepositoryProviders() {
        return this.remoteRepositoryProductProviders;
    }

    public void cancelDownloadProduct(RemoteRepositoryProductImpl repositoryProduct) {
        String dataSourceName = repositoryProduct.getRemoteMission().getRepositoryName();
        String key = buildKey(dataSourceName, repositoryProduct);
        DataSourceComponent downloadStrategy;
        synchronized (this.downloadingProducts) {
            downloadStrategy = this.downloadingProducts.remove(key);
        }
        if (downloadStrategy == null) {
            // the product is not downloading
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder logMessage = new StringBuilder();
                logMessage.append("The product '")
                        .append(repositoryProduct.getName())
                        .append("' from the '")
                        .append(dataSourceName)
                        .append("' remote repository using the '")
                        .append(repositoryProduct.getRemoteMission().getName())
                        .append("' mission is not downloading.");
                logger.log(Level.FINE, logMessage.toString());
            }
        } else {
            // the product is still downloading
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder logMessage = new StringBuilder();
                logMessage.append("Cancel downloading the product '")
                        .append(repositoryProduct.getName())
                        .append("' from the '")
                        .append(dataSourceName)
                        .append("' remote repository using the '")
                        .append(repositoryProduct.getRemoteMission().getName())
                        .append("' mission.");
                logger.log(Level.FINE, logMessage.toString());
            }

            downloadStrategy.cancel();
        }
    }

    public Path downloadProduct(RemoteRepositoryProductImpl repositoryProduct, Credentials credentials, Path targetFolderPath,
                                ProgressListener progressListener, boolean uncompressedDownloadedProduct)
                                throws Exception {

        String dataSourceName = repositoryProduct.getRemoteMission().getRepositoryName();
        String key = buildKey(dataSourceName, repositoryProduct);

        if (StringUtils.isBlank(repositoryProduct.getURL())) {
            throw new NullPointerException("The remote repository product url is null or empty.");
        }

        DataSourceComponent dataSourceComponent = null;
        try {
            if (logger.isLoggable(Level.FINE)) {
                StringBuilder logMessage = new StringBuilder();
                logMessage.append("Start downloading the product '")
                        .append(repositoryProduct.getName())
                        .append("' from the '")
                        .append(dataSourceName)
                        .append("' remote repository using the '")
                        .append(repositoryProduct.getRemoteMission().getName())
                        .append("' mission.");
                logger.log(Level.FINE, logMessage.toString());
            }

            synchronized (this.downloadingProducts) {
                dataSourceComponent = this.downloadingProducts.get(key);
                if (dataSourceComponent == null) {
                    dataSourceComponent = new DataSourceComponent();
                    this.downloadingProducts.put(key, dataSourceComponent);
                } else {
                    throw new IllegalArgumentException("The product '" + repositoryProduct.getName()+"' is already downloading from the '" + dataSourceName+"' remote repository using the '" + repositoryProduct.getRemoteMission().getName()+"' mission.");
                }
            }

            DownloadProductProgressListener taoProgressListener = new DownloadProductProgressListener(progressListener, dataSourceName, repositoryProduct.getRemoteMission().getName(), repositoryProduct.getName());
            DownloadProductStatusListener taoProductStatusListener = new DownloadProductStatusListener();

            dataSourceComponent.setDataSourceName(dataSourceName);
            dataSourceComponent.setSensorName(repositoryProduct.getRemoteMission().getName());
            dataSourceComponent.setFetchMode(FetchMode.RESUME);
            dataSourceComponent.setUserName(credentials.getUserPrincipal().getName());
            dataSourceComponent.setPassword(credentials.getPassword());
            dataSourceComponent.setProgressListener(taoProgressListener);
            dataSourceComponent.setProductStatusListener(taoProductStatusListener);

            EOProduct product = new EOProduct() {
                @Override
                public void setApproximateSize(long approximateSize) {
                    super.setApproximateSize(approximateSize);

                    if (approximateSize > 0) {
                        progressListener.notifyApproximateSize(approximateSize);
                    }
                }
            };
            product.setApproximateSize(repositoryProduct.getApproximateSize());
            product.setId(repositoryProduct.getId());
            product.setProductType(repositoryProduct.getRemoteMission().getName());
            product.setName(repositoryProduct.getName());
            product.setLocation(repositoryProduct.getURL());

            List<EOProduct> products = new ArrayList<>(1);
            products.add(product); // add the product to be downloaded

            Properties additionalProperties = new Properties();
            additionalProperties.put("auto.uncompress", Boolean.toString(uncompressedDownloadedProduct));
            additionalProperties.put("progress.interval", "1500");

            dataSourceComponent.doFetch(products, null, targetFolderPath.toString(), null, additionalProperties);

            if (product.getProductStatus() == ProductStatus.DOWNLOADED) {
                // the product has been downloaded
                String productPath = product.getLocation();
                if (productPath == null) {
                    throw new NullPointerException("The path of the downloaded product '" + repositoryProduct.getName() + "' is null when downloading it from the '" + dataSourceName+"' remote repository using the '"+repositoryProduct.getRemoteMission().getName()+"' mission.");
                }
                URI uri = new URI(productPath);
                return Paths.get(uri);
            } else {
                // the product has not been downloaded and check if downloading the product was cancelled before throwing an exception
                boolean downloadingProductCancelled;
                synchronized (this.downloadingProducts) {
                    DataSourceComponent previousDataSource = this.downloadingProducts.remove(key);
                    downloadingProductCancelled = (previousDataSource == null);
                }
                if (downloadingProductCancelled) {
                    throw new java.lang.InterruptedException("Downloading the product '" + repositoryProduct.getName() + "' has been cancelled.");
                } else {
                    throw new IllegalStateException(buildFailedDownloadExceptionMessage(repositoryProduct.getName(), dataSourceName, repositoryProduct.getRemoteMission().getName(), taoProductStatusListener.getDownloadMessages()));
                }
            }
        } finally {
            if (dataSourceComponent != null) {
                synchronized (this.downloadingProducts) {
                    this.downloadingProducts.remove(key);
                }
            }
        }
    }

    public static int getMaximumAllowedTransfers(String dataSourceName) {
        return findDataSource(dataSourceName).getMaximumAllowedTransfers();
    }

    public static boolean requiresAuthentication(String dataSourceName) {
        return findDataSource(dataSourceName).requiresAuthentication();
    }

    public static String[] getAvailableMissions(String dataSourceName) {
        return findDataSource(dataSourceName).getSupportedSensors();
    }

    public static BufferedImage downloadProductQuickLookImage(String dataSourceName, Credentials credentials, String url, ThreadStatus thread) throws IOException, InterruptedException {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Download the quick look image: remote repository '" + dataSourceName+"', url '"+url+"'.");
        }

        try (CloseableHttpResponse response = NetUtils.openConnection(HttpMethod.GET, url, credentials)) {
            if (response != null) {

                ThreadStatus.checkCancelled(thread);

                StatusLine statusLine = response.getStatusLine();
                switch (statusLine.getStatusCode()) {
                    case HttpURLConnection.HTTP_OK:
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
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        throw new HTTPServerException(HttpURLConnection.HTTP_UNAUTHORIZED, "Unauthorized or the supplied credentials are invalid.");
                    case HttpURLConnection.HTTP_INTERNAL_ERROR:
                        throw new HTTPServerException(HttpURLConnection.HTTP_INTERNAL_ERROR, "Internal Server Error");
                    default:
                        throw new HTTPServerException(statusLine.getStatusCode(), String.valueOf(statusLine.getStatusCode()) + ": " + statusLine.getReasonPhrase());
                }
            } else {
                throw new IOException(String.format("Null response (maybe url %s is not reachable", url));
            }
        }
    }

    public static List<RepositoryQueryParameter> getMissionParameters(String dataSourceName, String mission) {
        DataSource dataSource = findDataSource(dataSourceName);
        Map<String, Map<ParameterName, DataSourceParameter>> supportedParameters = dataSource.getSupportedParameters();
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

    public static List<RepositoryProduct> downloadProductList(String dataSourceName, String mission, Credentials credentials, Map<String, Object> parameterValues,
                                                              ProductListDownloaderListener downloaderListener, ThreadStatus thread)
                                                              throws Exception {

        if (logger.isLoggable(Level.FINE)) {
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("Start downloading the product list from the '")
                    .append(dataSourceName)
                    .append("' remote repository using the '")
                    .append(mission)
                    .append("' mission.\nThe query parameters are:");
            Iterator<Map.Entry<String, Object>> iterator = parameterValues.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                logMessage.append("\n   - ")
                        .append(entry.getKey())
                        .append(": ")
                        .append(entry.getValue());
            }
            logger.log(Level.FINE, logMessage.toString());
        }

        // avoid Read timeout errors from SciHub
        NetUtils.setTimeout(300000);

        DataQuery query = buildDataQuery(dataSourceName, mission, credentials, parameterValues);
        query.setPageNumber(0);
        query.setPageSize(0);
        long totalProductCount = query.getCount();

        ThreadStatus.checkCancelled(thread);

        RemoteMission remoteMission = new RemoteMission(mission, dataSourceName);
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

                List<RepositoryProduct> downloadedPageProducts = convertProducts(remoteMission, pageResults, wktReader);
                productList.addAll(downloadedPageProducts);

                ThreadStatus.checkCancelled(thread);

                downloaderListener.notifyPageProducts(pageNumber, downloadedPageProducts, totalProductCount, productList.size());
            }
        } else {
            List<EOProduct> pageResults = query.execute();

            ThreadStatus.checkCancelled(thread);

            WKTReader wktReader = new WKTReader();
            List<RepositoryProduct> downloadedPageProducts = convertProducts(remoteMission, pageResults, wktReader);
            productList = new ArrayList<>(downloadedPageProducts);

            downloaderListener.notifyPageProducts(1, downloadedPageProducts, productList.size(), productList.size());
        }
        return productList;
    }

    private static DataQuery buildDataQuery(String dataSourceName, String mission, Credentials credentials, Map<String, Object> parametersValues)
                                            throws InstantiationException, IllegalAccessException {

        DataSource dataSource = findDataSource(dataSourceName);
        if (credentials != null) {
            dataSource.setCredentials(credentials.getUserPrincipal().getName(), credentials.getPassword());
        }

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

        Calendar calendar = Calendar.getInstance();

        Date startDate = (Date)parametersValues.get(CommonParameterNames.START_DATE);
        if (startDate != null) {
            calendar.setTimeInMillis(startDate.getTime());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            QueryParameter<Date> begin = query.createParameter(CommonParameterNames.START_DATE, Date.class);
            begin.setValue(calendar.getTime());
            query.addParameter(begin);
        }
        Date endDate = (Date)parametersValues.get(CommonParameterNames.END_DATE);
        if (endDate != null) {
            calendar.setTimeInMillis(endDate.getTime());
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 0);

            QueryParameter<Date> end = query.createParameter(CommonParameterNames.END_DATE, Date.class);
            end.setValue(calendar.getTime());
            query.addParameter(end);
        }

        return query;
    }

    private static List<RepositoryProduct> convertProducts(RemoteMission mission, List<EOProduct> pageResults, WKTReader wktReader) throws InterruptedException, ParseException {
        List<RepositoryProduct> downloadedPageProducts = new ArrayList<>(pageResults.size());
        for (int i=0; i<pageResults.size(); i++) {
            EOProduct taoProduct = pageResults.get(i);

            Geometry productGeometry = wktReader.read(taoProduct.getGeometry());
            AbstractGeometry2D geometry = GeometryUtils.convertProductGeometry(productGeometry);

            List<ro.cs.tao.eodata.Attribute> taoRemoteAttributes = taoProduct.getAttributes();
            List<Attribute> remoteAttributes = new ArrayList<>(taoRemoteAttributes.size());
            for (int k=0; k<taoRemoteAttributes.size(); k++) {
                ro.cs.tao.eodata.Attribute remoteAttribute = taoRemoteAttributes.get(k);
                remoteAttributes.add(new Attribute(remoteAttribute.getName(), remoteAttribute.getValue()));
            }

            RemoteRepositoryProductImpl repositoryProduct = new RemoteRepositoryProductImpl(taoProduct.getId(), taoProduct.getName(), taoProduct.getLocation(), mission, geometry, taoProduct.getAcquisitionDate(), taoProduct.getApproximateSize());
            repositoryProduct.setRemoteAttributes(remoteAttributes);
            repositoryProduct.setDataFormatType(convertToDataFormatType(taoProduct.getFormatType()));
            repositoryProduct.setPixelType(convertToPixelType(taoProduct.getPixelType()));
            repositoryProduct.setSensorType(convertToSensorType(taoProduct.getSensorType()));
            repositoryProduct.setDownloadQuickLookImageURL(taoProduct.getQuicklookLocation());

            downloadedPageProducts.add(repositoryProduct);
        }
        return downloadedPageProducts;
    }

    private static String buildKey(String dataSourceName, RemoteRepositoryProductImpl repositoryProduct) {
        if (StringUtils.isBlank(dataSourceName)) {
            throw new NullPointerException("The data source name is null or empty.");
        }
        if (repositoryProduct == null) {
            throw new NullPointerException("The remote repository product is null.");
        }
        if (repositoryProduct.getRemoteMission() == null) {
            throw new NullPointerException("The remote mission is null.");
        }
        if (StringUtils.isBlank(repositoryProduct.getRemoteMission().getName())) {
            throw new NullPointerException("The remote repository product mission is null or empty.");
        }
        if (StringUtils.isBlank(repositoryProduct.getId())) {
            throw new NullPointerException("The remote repository product id is null or empty.");
        }
        return dataSourceName + "|" + repositoryProduct.getRemoteMission().getName() + "|" + repositoryProduct.getId();
    }

    private static DataFormatType convertToDataFormatType(ro.cs.tao.eodata.enums.DataFormat dataFormat) {
        if (dataFormat == ro.cs.tao.eodata.enums.DataFormat.RASTER) {
            return DataFormatType.RASTER;
        }
        if (dataFormat == ro.cs.tao.eodata.enums.DataFormat.VECTOR) {
            return DataFormatType.VECTOR;
        }
        if (dataFormat == ro.cs.tao.eodata.enums.DataFormat.OTHER) {
            return DataFormatType.OTHER;
        }
        throw new IllegalArgumentException("Unknown data format: "+ dataFormat);
    }

    private static SensorType convertToSensorType(ro.cs.tao.eodata.enums.SensorType sensorType) {
        if (sensorType == ro.cs.tao.eodata.enums.SensorType.OPTICAL) {
            return SensorType.OPTICAL;
        }
        if (sensorType == ro.cs.tao.eodata.enums.SensorType.RADAR) {
            return SensorType.RADAR;
        }
        if (sensorType == ro.cs.tao.eodata.enums.SensorType.ALTIMETRIC) {
            return SensorType.ALTIMETRIC;
        }
        if (sensorType == ro.cs.tao.eodata.enums.SensorType.ATMOSPHERIC) {
            return SensorType.ATMOSPHERIC;
        }
        if (sensorType == ro.cs.tao.eodata.enums.SensorType.UNKNOWN) {
            return SensorType.UNKNOWN;
        }
        throw new IllegalArgumentException("Unknown sensor type: "+ sensorType);
    }

    private static PixelType convertToPixelType(ro.cs.tao.eodata.enums.PixelType pixelType) {
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.UINT8) {
            return PixelType.UINT8;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.UINT8) {
            return PixelType.UINT8;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.INT8) {
            return PixelType.INT8;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.UINT16) {
            return PixelType.UINT16;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.UINT32) {
            return PixelType.UINT32;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.INT32) {
            return PixelType.INT32;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.FLOAT32) {
            return PixelType.FLOAT32;
        }
        if (pixelType == ro.cs.tao.eodata.enums.PixelType.FLOAT64) {
            return PixelType.FLOAT64;
        }
        throw new IllegalArgumentException("Unknown pixel type: "+ pixelType);
    }

    private static DataSource findDataSource(String dataSourceName) {
        Set<DataSource> services = DataSourceManager.getInstance().getRegisteredDataSources();
        for (DataSource dataSource : services) {
            if (dataSource.getId().equals(dataSourceName)) {
                return dataSource;
            }
        }
        throw new IllegalStateException("Unknown data source '" + dataSourceName + "'.");
    }

    private static String buildFailedDownloadExceptionMessage(String productName, String dataSourceName, String mission, List<String> downloadMessages) {
        StringBuilder exceptionMessage = new StringBuilder();
        exceptionMessage.append("Downloading the product '")
                .append(productName)
                .append("' from the '")
                .append(dataSourceName)
                .append("' remote repository using the '")
                .append(mission)
                .append("' mission has failed.");
        if (downloadMessages.size() > 0) {
            exceptionMessage.append(" The possible causes may be: ");
            for (int i=0;i<downloadMessages.size(); i++) {
                if (i > 0) {
                    exceptionMessage.append("; ");
                }
                exceptionMessage.append(downloadMessages.get(i));
            }
        }
        return exceptionMessage.toString();
    }

    /**
     * The structure of the map is: Map<Sensor, Map<ParameterName, Map<ValueSetEntry, ValueSetFriendlyValue>>>
     */
    private static Map<String, Map<String, Map<String, String>>> buildAlaskaSatelliteFacilityParameterFilter() {
//        Map<String, String> productTypeParameterValues = new HashMap<>();
//        productTypeParameterValues.put("GRD_HS", "GRD_HS");
//        productTypeParameterValues.put("GRD_HD", "GRD_HD");
//        productTypeParameterValues.put("GRD_MS", "GRD_MS");
//        productTypeParameterValues.put("GRD_MD", "GRD_MD");
//        productTypeParameterValues.put("GRD_FS", "GRD_FS");
//        productTypeParameterValues.put("GRD_FD", "GRD_FD");
//        productTypeParameterValues.put("SLC", "SLC");
//        productTypeParameterValues.put("OCN", "OCN");
//
//        Map<String, Map<String, String>> sentinel1Parameters = new HashMap<>();
//        sentinel1Parameters.put("productType", productTypeParameterValues);
//
//        Map<String, Map<String, Map<String, String>>> filteredParameters = new HashMap<>();
//        filteredParameters.put("Sentinel1", sentinel1Parameters);
//        return filteredParameters;
        return null;
    }

    //TODO Jean temporary method until the Landsat8 product reader will be changed to read the product from a folder
    public static Path prepareProductPathToOpen(Path productPath, RepositoryProduct repositoryProduct) {
        if (Files.isDirectory(productPath)) {
            // the product parth is a folder
            RemoteMission productRemoteMission = repositoryProduct.getRemoteMission();
            if ("Landsat8".equalsIgnoreCase(productRemoteMission.getName())) {
                Landsat8ProductHelper landsat8ProductHelper = new Landsat8ProductHelper(repositoryProduct.getName());
                return productPath.resolve(landsat8ProductHelper.getMetadataFileName());
            }
        }
        return productPath;
    }
}
