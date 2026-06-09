package org.esa.snap.remote.products.repository;

import org.apache.http.auth.Credentials;
import org.esa.snap.remote.products.repository.cdse.CdseProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.download.RemoteRepositoriesManager;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The repository provider used to access the remote repository to search, download products.
 *
 * Created by jcoravu on 26/8/2019.
 */
public interface RemoteProductsRepositoryProvider {

    public int getMaximumAllowedTransfersPerAccount();

    public boolean requiresAuthentication();

    public String getRepositoryName();

    public String[] getAvailableMissions();

    public List<RepositoryQueryParameter> getMissionParameters(String mission);

    public List<RepositoryProduct> downloadProductList(Credentials credentials, String mission, int pageSize, Map<String, Object> parameterValues,
                                                       ProductListDownloaderListener downloaderListener, ThreadStatus thread)
                                                       throws Exception;

    public default List<RepositoryProduct> downloadProductList(Credentials credentials, String mission, int pageSize, int maxResults,
                                                               Map<String, Object> parameterValues, ProductListDownloaderListener downloaderListener,
                                                               ThreadStatus thread)
                                                               throws Exception {
        List<RepositoryProduct> products = downloadProductList(credentials, mission, pageSize, parameterValues, downloaderListener, thread);
        if (maxResults > 0 && products.size() > maxResults) {
            return new ArrayList<>(products.subList(0, maxResults));
        }
        return products;
    }

    public BufferedImage downloadProductQuickLookImage(Credentials credentials, String url, ThreadStatus thread)
                                                       throws IOException, java.lang.InterruptedException;

    public Map<String, String> getDisplayedAttributes();

    public void cancelDownloadProduct(RepositoryProduct repositoryProduct);

    public Path downloadProduct(RepositoryProduct repositoryProduct, Credentials credentials, Path targetFolderPath,
                                ProgressListener progressListener, boolean uncompressedDownloadedProduct)
                                throws Exception;

    public static RemoteProductsRepositoryProvider[] getRemoteProductsRepositoryProviders() {
        return RemoteRepositoriesManager.getInstance().getRemoteProductsRepositoryProviders();
    }

    public static RemoteProductsRepositoryProvider getRemoteProductsRepositoryProvider(String repositoryName) {
        if (CdseProductsRepositoryProvider.REPOSITORY_NAME.equals(repositoryName)) {
            return CdseProductsRepositoryProvider.getInstance();
        }
        for (RemoteProductsRepositoryProvider repositoryProvider : getRemoteProductsRepositoryProviders()) {
            if (repositoryProvider.getRepositoryName().equals(repositoryName)) {
                return repositoryProvider;
            }
        }
        throw new IllegalStateException("Unknown remote products repository '" + repositoryName + "'.");
    }

    //TODO Jean temporary method until the Landsat8 product reader will be changed to read the product from a folder
    public static Path prepareProductPathToOpen(Path productPath, RepositoryProduct repositoryProduct) {
        return RemoteRepositoriesManager.prepareProductPathToOpen(productPath, repositoryProduct);
    }
}
