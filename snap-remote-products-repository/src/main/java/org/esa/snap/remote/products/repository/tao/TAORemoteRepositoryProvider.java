package org.esa.snap.remote.products.repository.tao;

import org.apache.http.auth.Credentials;
import org.esa.snap.remote.products.repository.RemoteProductsRepositoryProvider;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.RepositoryQueryParameter;
import org.esa.snap.remote.products.repository.ThreadStatus;
import org.esa.snap.remote.products.repository.listener.ProductListDownloaderListener;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 28/8/2019.
 */
class TAORemoteRepositoryProvider implements RemoteProductsRepositoryProvider {

    private final String repositoryName;

    TAORemoteRepositoryProvider(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @Override
    public String getRepositoryName() {
        return this.repositoryName;
    }

    @Override
    public int getMaximumAllowedTransfersPerAccount() {
        return TAORemoteRepositoriesManager.getMaximumAllowedTransfers(getRepositoryName());
    }

    @Override
    public boolean requiresAuthentication() {
        return TAORemoteRepositoriesManager.requiresAuthentication(getRepositoryName());
    }

    @Override
    public String[] getAvailableMissions() {
        return TAORemoteRepositoriesManager.getAvailableMissions(getRepositoryName());
    }

    @Override
    public List<RepositoryQueryParameter> getMissionParameters(String mission) {
        return TAORemoteRepositoriesManager.getMissionParameters(getRepositoryName(), mission);
    }

    @Override
    public BufferedImage downloadProductQuickLookImage(Credentials credentials, String url, ThreadStatus thread) throws IOException, InterruptedException {
        return TAORemoteRepositoriesManager.downloadProductQuickLookImage(getRepositoryName(), credentials, url, thread);
    }

    @Override
    public List<RepositoryProduct> downloadProductList(Credentials credentials, String mission, Map<String, Object> parameterValues,
                                                       ProductListDownloaderListener downloaderListener, ThreadStatus thread)
                                                       throws Exception {

        return TAORemoteRepositoriesManager.downloadProductList(getRepositoryName(), mission, credentials, parameterValues, downloaderListener, thread);
    }

    @Override
    public Map<String, String> getDisplayedAttributes() {
        return null;
    }

    @Override
    public void cancelDownloadProduct(RepositoryProduct repositoryProduct) {
        TAORemoteRepositoriesManager.getInstance().cancelDownloadProduct(getRepositoryName(), repositoryProduct);
    }

    @Override
    public Path downloadProduct(RepositoryProduct repositoryProduct, Credentials credentials, Path targetFolderPath,
                                ProgressListener progressListener, boolean uncompressedDownloadedProduct)
                                throws Exception {

        TAORepositoryProduct taoRepositoryProduct = (TAORepositoryProduct)repositoryProduct;
        return TAORemoteRepositoriesManager.getInstance().downloadProduct(getRepositoryName(), taoRepositoryProduct, credentials, targetFolderPath,
                                                                progressListener, uncompressedDownloadedProduct);
    }
}
