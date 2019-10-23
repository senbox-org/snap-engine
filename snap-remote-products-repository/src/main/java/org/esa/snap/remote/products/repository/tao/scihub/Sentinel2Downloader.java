package org.esa.snap.remote.products.repository.tao.scihub;

import org.esa.snap.remote.products.repository.tao.AbstractTAOProductRepositoryDownloader;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel2ArchiveDownloadStrategy;

/**
 * Created by jcoravu on 23/10/2019.
 */
class Sentinel2Downloader extends Sentinel2ArchiveDownloadStrategy {

    public Sentinel2Downloader() {
        super((String)null);
    }

    @Override
    protected String getAuthenticationToken() {
        return AbstractTAOProductRepositoryDownloader.buildAuthenticationToken(this.credentials);
    }
}
