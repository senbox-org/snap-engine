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
import java.util.Properties;

/**
 * Created by jcoravu on 26/9/2019.
 */
class TAOProductRepositoryDownloader implements ProductRepositoryDownloader {

    private final String mission;
    private final String repositoryId;
    private final DownloadStrategy downloadStrategy;

    TAOProductRepositoryDownloader(String mission, String repositoryId, DownloadStrategy downloadStrategy) {
        this.mission = mission;
        this.repositoryId = repositoryId;
        this.downloadStrategy = downloadStrategy;
    }

    @Override
    public String getRepositoryId() {
        return this.repositoryId;
    }

    @Override
    public Path download(RepositoryProduct repositoryProduct, Credentials credentials, Path targetFolderPath, ProgressListener progressListener)
                         throws InterruptedException, IOException {

        if (repositoryProduct.getMission().equals(this.mission)) {
            Properties properties = new Properties();
            properties.put("auto.uncompress", "true");
            this.downloadStrategy.addProperties(properties);
            this.downloadStrategy.setCredentials(new UsernamePasswordCredentials(credentials.getUserPrincipal().getName(), credentials.getPassword()));
            this.downloadStrategy.setDestination(targetFolderPath.toString());
            this.downloadStrategy.setFetchMode(FetchMode.OVERWRITE);
            this.downloadStrategy.setProgressListener(new DownloadProductProgressListener(progressListener));
            try {
                EOProduct product = ((TAORepositoryProduct)repositoryProduct).getProduct();
                return this.downloadStrategy.fetch(product);
            } catch (ro.cs.tao.datasource.InterruptedException exception) {
                throw new java.lang.InterruptedException();
            }
        } else {
            throw new IllegalArgumentException("The product mission '" + repositoryProduct.getMission()+"' and the downloader mission '" + this.mission+"' does not match.");
        }
    }

    @Override
    public void cancel() {
        this.downloadStrategy.cancel();
    }
}
