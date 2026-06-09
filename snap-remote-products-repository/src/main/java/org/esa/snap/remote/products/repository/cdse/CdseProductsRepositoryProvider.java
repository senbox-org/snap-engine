package org.esa.snap.remote.products.repository.cdse;

import org.apache.http.auth.Credentials;
import org.esa.snap.remote.products.repository.HTTPServerException;
import org.esa.snap.remote.products.repository.RemoteMission;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.esa.snap.remote.products.repository.ThreadStatus;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CdseProductsRepositoryProvider implements RemoteProductsRepositoryProvider {

    public static final String REPOSITORY_NAME = "Copernicus DataSpace";
    private static final String SENTINEL_3 = "Sentinel3";
    private static final String DOWNLOAD_PRODUCTS_URL = "https://download.dataspace.copernicus.eu/odata/v1/Products";
    private static final CdseProductsRepositoryProvider INSTANCE = new CdseProductsRepositoryProvider();

    private final CdseHttpClient httpClient;
    private final CdseProductMapper productMapper;
    private final CdseProductDownloader productDownloader;

    public static CdseProductsRepositoryProvider getInstance() {
        return INSTANCE;
    }

    public CdseProductsRepositoryProvider() {
        this(new UrlConnectionCdseHttpClient());
    }

    CdseProductsRepositoryProvider(CdseHttpClient httpClient) {
        this.httpClient = httpClient;
        this.productMapper = new CdseProductMapper();
        this.productDownloader = new CdseProductDownloader(httpClient, new CdseAuthClient(httpClient), new CdseZipExtractor());
    }

    @Override
    public int getMaximumAllowedTransfersPerAccount() {
        return 2;
    }

    @Override
    public boolean requiresAuthentication() {
        return true;
    }

    @Override
    public String getRepositoryName() {
        return REPOSITORY_NAME;
    }

    @Override
    public String[] getAvailableMissions() {
        return new String[]{SENTINEL_3};
    }

    @Override
    public List<RepositoryQueryParameter> getMissionParameters(String mission) {
        if (!SENTINEL_3.equals(mission)) {
            return Collections.emptyList();
        }
        return List.of(
                new RepositoryQueryParameter("platformName", String.class, "Platform", "SENTINEL-3", true, new Object[]{"SENTINEL-3"}),
                new RepositoryQueryParameter("platformSerialIdentifier", String.class, "Platform Identifier S3(A/B)", null, false, new Object[]{"A", "B"}),
                new RepositoryQueryParameter("instrument", String.class, "Instrument", "OLCI", false, new Object[]{"OLCI", "SRAL", "SLSTR", "SYNERGY"}),
                new RepositoryQueryParameter("productType", String.class, "Product Type", "OL_1_EFR___", false, new Object[]{
                        "OL_1_EFR___", "OL_1_ERR___", "OL_2_LFR___", "OL_2_LRR___", "OL_2_WFR___", "OL_2_WRR___",
                        "SL_1_RBT___", "SL_2_AOD___", "SL_2_FRP___", "SL_2_LST___", "SL_2_WST___",
                        "SR_1_SRA___", "SR_1_SRA_A_", "SR_1_SRA_BS", "SR_2_LAN___", "SR_2_LAN_HY", "SR_2_LAN_LI", "SR_2_LAN_SI", "SR_2_WAT___",
                        "SY_2_AOD___", "SY_2_SYN___", "SY_2_V10___", "SY_2_VG1___", "SY_2_VGP___"
                }),
                new RepositoryQueryParameter("processingLevel", String.class, "Processing Level", "1", false, new Object[]{"1", "2"}),
                new RepositoryQueryParameter(RepositoryQueryParameter.START_DATE, LocalDateTime.class, "Start Date", null, false, null),
                new RepositoryQueryParameter(RepositoryQueryParameter.END_DATE, LocalDateTime.class, "End Date", null, false, null),
                new RepositoryQueryParameter(RepositoryQueryParameter.FOOTPRINT, Rectangle2D.class, "Area of Interest", null, false, null),
                new RepositoryQueryParameter("productIdentifier", String.class, "Product Name", null, false, null),
                new RepositoryQueryParameter("platform", String.class, "Platform", null, false, new Object[]{"S3A", "S3B"})
        );
    }

    @Override
    public List<RepositoryProduct> downloadProductList(Credentials credentials, String mission, int pageSize, Map<String, Object> parameterValues,
                                                       ProductListDownloaderListener downloaderListener, ThreadStatus thread)
                                                       throws Exception {
        return downloadProductList(credentials, mission, pageSize, pageSize, parameterValues, downloaderListener, thread);
    }

    @Override
    public List<RepositoryProduct> downloadProductList(Credentials credentials, String mission, int pageSize, int maxResults,
                                                       Map<String, Object> parameterValues, ProductListDownloaderListener downloaderListener,
                                                       ThreadStatus thread)
                                                       throws Exception {
        ThreadStatus.checkCancelled(thread);
        int limit = maxResults > 0 ? maxResults : pageSize;
        CdseHttpResponse response = httpClient.execute(new CdseHttpRequest("GET", CdseSearchQueryBuilder.buildProductsUrl(mission, parameterValues, limit)));
        if (!response.isSuccessful()) {
            throw new HTTPServerException(response.getStatusCode(), response.getBodyAsString());
        }
        ThreadStatus.checkCancelled(thread);
        CdseSearchResult searchResult = productMapper.mapSearchResult(response.getBodyAsString(), new RemoteMission(mission, REPOSITORY_NAME));
        if (downloaderListener != null) {
            downloaderListener.notifyProductCount(searchResult.getTotalCount());
            downloaderListener.notifyPageProducts(1, searchResult.getProducts(), searchResult.getTotalCount(), searchResult.getProducts().size());
        }
        return searchResult.getProducts();
    }

    @Override
    public BufferedImage downloadProductQuickLookImage(Credentials credentials, String url, ThreadStatus thread) throws IOException, InterruptedException {
        ThreadStatus.checkCancelled(thread);
        CdseHttpResponse response = httpClient.execute(new CdseHttpRequest("GET", url));
        if (!response.isSuccessful()) {
            throw new HTTPServerException(response.getStatusCode(), response.getBodyAsString());
        }
        ThreadStatus.checkCancelled(thread);
        return ImageIO.read(new ByteArrayInputStream(response.getBody()));
    }

    @Override
    public Map<String, String> getDisplayedAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public void cancelDownloadProduct(RepositoryProduct repositoryProduct) {
    }

    @Override
    public Path downloadProduct(RepositoryProduct repositoryProduct, Credentials credentials, Path targetFolderPath,
                                ProgressListener progressListener, boolean uncompressedDownloadedProduct)
                                throws IOException {
        return productDownloader.download(repositoryProduct, credentials, targetFolderPath, progressListener, uncompressedDownloadedProduct);
    }

    static String downloadUrl(String productId) {
        return DOWNLOAD_PRODUCTS_URL + "(" + productId + ")/$value";
    }
}
