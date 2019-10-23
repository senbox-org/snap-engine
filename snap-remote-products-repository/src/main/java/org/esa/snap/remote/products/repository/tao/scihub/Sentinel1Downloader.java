package org.esa.snap.remote.products.repository.tao.scihub;

import org.esa.snap.remote.products.repository.tao.AbstractTAOProductRepositoryDownloader;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel1DownloadStrategy;

/**
 * Created by jcoravu on 23/10/2019.
 */
class Sentinel1Downloader extends Sentinel1DownloadStrategy {

    public Sentinel1Downloader() {
        super((String)null);
    }

    @Override
    protected String getAuthenticationToken() {
        return AbstractTAOProductRepositoryDownloader.buildAuthenticationToken(this.credentials);
    }
}
