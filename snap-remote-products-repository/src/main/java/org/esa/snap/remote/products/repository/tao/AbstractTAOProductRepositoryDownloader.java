package org.esa.snap.remote.products.repository.tao;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.DownloadProductProgressListener;
import org.esa.snap.remote.products.repository.listener.ProgressListener;
import ro.cs.tao.datasource.remote.DownloadStrategy;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.eodata.EOProduct;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 26/9/2019.
 */
public abstract class AbstractTAOProductRepositoryDownloader implements ProductRepositoryDownloader {

    private final String mission;
    private final String repositoryId;

    protected DownloadStrategy downloadStrategy;

    protected AbstractTAOProductRepositoryDownloader(String mission, String repositoryId) {
        this.mission = mission;
        this.repositoryId = repositoryId;
    }

    protected abstract EOProduct getEOProduct(RepositoryProduct product);

    @Override
    public String getRepositoryId() {
        return this.repositoryId;
    }

    @Override
    public Path download(RepositoryProduct product, Credentials credentials, Path targetFolderPath, ProgressListener progressListener)
                         throws InterruptedException, IOException {

        if (product.getMission().equals(this.mission)) {
            this.downloadStrategy.setCredentials(new UsernamePasswordCredentials(credentials.getUserPrincipal().getName(), credentials.getPassword()));
            this.downloadStrategy.setDestination(targetFolderPath.toString());
            this.downloadStrategy.setFetchMode(FetchMode.OVERWRITE);
            this.downloadStrategy.setProgressListener(new DownloadProductProgressListener(progressListener));
            try {
                return this.downloadStrategy.fetch(getEOProduct(product));
            } catch (ro.cs.tao.datasource.InterruptedException exception) {
                throw new java.lang.InterruptedException();
            }
        } else {
            throw new IllegalArgumentException("The product mission '" + product.getMission()+"' and the downloader mission '" + this.mission+"' does not match.");
        }
    }

    @Override
    public void cancel() {
        this.downloadStrategy.cancel();
    }
}
