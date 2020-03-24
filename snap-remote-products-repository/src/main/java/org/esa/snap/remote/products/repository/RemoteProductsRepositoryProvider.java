package org.esa.snap.remote.products.repository;

import org.apache.http.auth.Credentials;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.esa.snap.remote.products.repository.listener.ProgressListener;
import org.esa.snap.remote.products.repository.donwload.RemoteRepositoriesManager;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 26/8/2019.
 */
public interface RemoteProductsRepositoryProvider {

    public int getMaximumAllowedTransfersPerAccount();

    public boolean requiresAuthentication();

    public String getRepositoryName();

    public String[] getAvailableMissions();

    public List<RepositoryQueryParameter> getMissionParameters(String mission);

    public List<RepositoryProduct> downloadProductList(Credentials credentials, String mission, Map<String, Object> parameterValues,
                                                       ProductListDownloaderListener downloaderListener, ThreadStatus thread)
                                                       throws Exception;

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
}
