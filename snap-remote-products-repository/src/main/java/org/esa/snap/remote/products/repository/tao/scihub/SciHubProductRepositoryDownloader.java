package org.esa.snap.remote.products.repository.tao.scihub;

import org.esa.snap.remote.products.repository.listener.DownloadProductProgressListener;
import org.esa.snap.remote.products.repository.ProductRepositoryDownloader;
import org.esa.snap.remote.products.repository.listener.ProgressListener;
import org.esa.snap.remote.products.repository.RepositoryProduct;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.datasource.remote.ProductHelper;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel1DownloadStrategy;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel2ArchiveDownloadStrategy;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel3DownloadStrategy;
import ro.cs.tao.datasource.remote.scihub.download.SentinelDownloadStrategy;
import ro.cs.tao.products.sentinels.Sentinel1ProductHelper;
import ro.cs.tao.products.sentinels.SentinelProductHelper;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 23/8/2019.
 */
public class SciHubProductRepositoryDownloader implements ProductRepositoryDownloader {

    private final String mission;
    private final String repositoryId;
    private final SentinelDownloadStrategy sentinelDownloadStrategy;

    public SciHubProductRepositoryDownloader(String mission, String repositoryId) {
        this.mission = mission;
        this.repositoryId = repositoryId;
        if (mission.equals("Sentinel1")) {
            this.sentinelDownloadStrategy = new Sentinel1DownloadStrategy(null);
        } else if (mission.equals("Sentinel2")) {
            this.sentinelDownloadStrategy = new Sentinel2ArchiveDownloadStrategy(null);
        } else if (mission.equals("Sentinel3")) {
            this.sentinelDownloadStrategy = new Sentinel3DownloadStrategy(null);
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
            SciHubRepositoryProduct productLibraryItem = (SciHubRepositoryProduct)product;
            this.sentinelDownloadStrategy.setDestination(targetFolderPath.toString());
            this.sentinelDownloadStrategy.setFetchMode(FetchMode.OVERWRITE);
            this.sentinelDownloadStrategy.setProgressListener(new DownloadProductProgressListener(progressListener));
            return this.sentinelDownloadStrategy.fetch(productLibraryItem.getProduct());
        } else {
            throw new IllegalArgumentException("The product misssion '" + product.getMission()+"' and the downloader mission '" + this.mission+"' does not match.");
        }
    }

    @Override
    public void cancel() {
        this.sentinelDownloadStrategy.cancel();
    }
}
