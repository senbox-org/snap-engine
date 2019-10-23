package org.esa.snap.remote.products.repository.tao.scihub;

import org.esa.snap.remote.products.repository.RepositoryProduct;
import org.esa.snap.remote.products.repository.tao.AbstractTAOProductRepositoryDownloader;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel1DownloadStrategy;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel2ArchiveDownloadStrategy;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel3DownloadStrategy;
import ro.cs.tao.eodata.EOProduct;

/**
 * Created by jcoravu on 23/8/2019.
 */
public class SciHubProductRepositoryDownloader extends AbstractTAOProductRepositoryDownloader {

    public SciHubProductRepositoryDownloader(String mission, String repositoryId) {
        super(mission, repositoryId);

        if (mission.equals("Sentinel1")) {
            this.downloadStrategy = new Sentinel1Downloader();
        } else if (mission.equals("Sentinel2")) {
            this.downloadStrategy = new Sentinel2Downloader();
        } else if (mission.equals("Sentinel3")) {
            this.downloadStrategy = new Sentinel3Downloader();
        } else {
            throw new IllegalArgumentException("Unknown mission '"+mission+"'.");
        }
    }

    @Override
    protected EOProduct getEOProduct(RepositoryProduct product) {
        return ((SciHubRepositoryProduct)product).getProduct();
    }
}
