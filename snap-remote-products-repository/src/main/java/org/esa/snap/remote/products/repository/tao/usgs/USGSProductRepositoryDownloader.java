package org.esa.snap.remote.products.repository.tao.usgs;

import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.listener.DownloadProductProgressListener;
import org.esa.snap.remote.products.repository.listener.ProgressListener;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.datasource.remote.SimpleArchiveDownloadStrategy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Created by jcoravu on 3/9/2019.
 */
public class USGSProductRepositoryDownloader implements ProductRepositoryDownloader {

    private final String mission;
    private final String repositoryId;
    private final SimpleArchiveDownloadStrategy simpleArchiveDownloadStrategy;

    public USGSProductRepositoryDownloader(String mission, String repositoryId) {
        this.mission = mission;
        this.repositoryId = repositoryId;
        if (mission.equals("Landsat8")) {
            this.simpleArchiveDownloadStrategy = new SimpleArchiveDownloadStrategy(null, new Properties());
        } else {
            throw new IllegalArgumentException("Unknown mission '"+mission+"'.");
        }
    }

    @Override
    public String getRepositoryId() {
        return this.repositoryId;
    }

    @Override
    public Path download(RepositoryProduct product, Path targetFolderPath, ProgressListener progressListener) throws IOException {
        if (product.getMission().equals(this.mission)) {
            USGSRepositoryProduct productLibraryItem = (USGSRepositoryProduct)product;
            this.simpleArchiveDownloadStrategy.setDestination(targetFolderPath.toString());
            this.simpleArchiveDownloadStrategy.setFetchMode(FetchMode.OVERWRITE);
            this.simpleArchiveDownloadStrategy.setProgressListener(new DownloadProductProgressListener(progressListener));
            return this.simpleArchiveDownloadStrategy.fetch(productLibraryItem.getProduct());
        } else {
            throw new IllegalArgumentException("The product misssion '" + product.getMission()+"' and the downloader mission '" + this.mission+"' does not match.");
        }
    }

    @Override
    public void cancel() {
        this.simpleArchiveDownloadStrategy.cancel();
    }
}
