package org.esa.snap.remote.products.repository.donwload;

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
 * The implementation class of a remote repository provider.
 *
 * Created by jcoravu on 28/8/2019.
 */
class RemoteProductsRepositoryProviderImpl implements RemoteProductsRepositoryProvider {

    private final String repositoryName;

    RemoteProductsRepositoryProviderImpl(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @Override
    public String getRepositoryName() {
        return this.repositoryName;
    }

    @Override
    public int getMaximumAllowedTransfersPerAccount() {
        return RemoteRepositoriesManager.getMaximumAllowedTransfers(getRepositoryName());
    }

    @Override
    public boolean requiresAuthentication() {
        return RemoteRepositoriesManager.requiresAuthentication(getRepositoryName());
    }

    @Override
    public String[] getAvailableMissions() {
        return RemoteRepositoriesManager.getAvailableMissions(getRepositoryName());
    }

    @Override
    public List<RepositoryQueryParameter> getMissionParameters(String mission) {
        return RemoteRepositoriesManager.getMissionParameters(getRepositoryName(), mission);
    }

    @Override
    public BufferedImage downloadProductQuickLookImage(Credentials credentials, String url, ThreadStatus thread) throws IOException, InterruptedException {
        return RemoteRepositoriesManager.downloadProductQuickLookImage(getRepositoryName(), credentials, url, thread);
    }

    @Override
    public List<RepositoryProduct> downloadProductList(Credentials credentials, String mission, Map<String, Object> parameterValues,
                                                       ProductListDownloaderListener downloaderListener, ThreadStatus thread)
                                                       throws Exception {

        return RemoteRepositoriesManager.downloadProductList(getRepositoryName(), mission, credentials, parameterValues, downloaderListener, thread);
    }

    @Override
    public Map<String, String> getDisplayedAttributes() {
        return null;
    }

    @Override
    public void cancelDownloadProduct(RepositoryProduct repositoryProduct) {
        RemoteRepositoryProductImpl taoRepositoryProduct = validateDownloadProduct(repositoryProduct);
        RemoteRepositoriesManager.getInstance().cancelDownloadProduct(taoRepositoryProduct);
    }

    @Override
    public Path downloadProduct(RepositoryProduct repositoryProduct, Credentials credentials, Path targetFolderPath,
                                ProgressListener progressListener, boolean uncompressedDownloadedProduct)
                                throws Exception {

        RemoteRepositoryProductImpl taoRepositoryProduct = validateDownloadProduct(repositoryProduct);
        return RemoteRepositoriesManager.getInstance().downloadProduct(taoRepositoryProduct, credentials, targetFolderPath, progressListener, uncompressedDownloadedProduct);
    }

    private RemoteRepositoryProductImpl validateDownloadProduct(RepositoryProduct repositoryProduct) {
        if (repositoryProduct == null) {
            throw new NullPointerException("The repository product is null.");
        }
        if (repositoryProduct.getRemoteMission() == null) {
            throw new NullPointerException("The repository product remote mission is null.");
        }
        if (!getRepositoryName().equals(repositoryProduct.getRemoteMission().getRepositoryName())) {
            throw new IllegalArgumentException("The remote repository name '" + getRepositoryName()+"' does not match with the remote product repository name '" + repositoryProduct.getRemoteMission().getRepositoryName() +"'.");
        }
        return (RemoteRepositoryProductImpl)repositoryProduct;
    }
}
