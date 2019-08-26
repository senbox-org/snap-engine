package org.esa.snap.product.library.v2.scihub;

import org.esa.snap.product.library.v2.DownloadProductProgressListener;
import org.esa.snap.product.library.v2.DataSourceProductDownloader;
import org.esa.snap.product.library.v2.ProgressListener;
import org.esa.snap.product.library.v2.ProductLibraryItem;
import ro.cs.tao.datasource.remote.FetchMode;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel1DownloadStrategy;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel2ArchiveDownloadStrategy;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel3DownloadStrategy;
import ro.cs.tao.datasource.remote.scihub.download.SentinelDownloadStrategy;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 23/8/2019.
 */
public class SciHubDataSourceProductDownloader implements DataSourceProductDownloader {

    private final SentinelDownloadStrategy sentinelDownloadStrategy;

    public SciHubDataSourceProductDownloader(String mission, Path targetFolderPath) {
        if (mission.equals("Sentinel1")) {
            this.sentinelDownloadStrategy = new Sentinel1DownloadStrategy(targetFolderPath.toString());
        } else if (mission.equals("Sentinel2")) {
            this.sentinelDownloadStrategy = new Sentinel2ArchiveDownloadStrategy(targetFolderPath.toString());
        } else if (mission.equals("Sentinel3")) {
            this.sentinelDownloadStrategy = new Sentinel3DownloadStrategy(targetFolderPath.toString());
        } else {
            throw new IllegalArgumentException("Unknown mission '"+mission+"'.");
        }
    }

    @Override
    public Path download(ProductLibraryItem product, ProgressListener progressListener) throws IOException {
        SciHubProductLibraryItem productLibraryItem = (SciHubProductLibraryItem)product;
        this.sentinelDownloadStrategy.setFetchMode(FetchMode.OVERWRITE);
        this.sentinelDownloadStrategy.setProgressListener(new DownloadProductProgressListener(progressListener));
        return this.sentinelDownloadStrategy.fetch(productLibraryItem.getProduct());
    }

    @Override
    public void cancel() {
        this.sentinelDownloadStrategy.cancel();
    }
}
