package org.esa.snap.remote.products.repository.tao.scihub;

import org.esa.snap.remote.products.repository.tao.AbstractTAOProductRepositoryDownloader;
import ro.cs.tao.datasource.remote.scihub.download.Sentinel3DownloadStrategy;

/**
 * Created by jcoravu on 23/10/2019.
 */
class Sentinel3Downloader extends Sentinel3DownloadStrategy {

    public Sentinel3Downloader() {
        super((String)null);
    }

    @Override
    protected String getAuthenticationToken() {
        return AbstractTAOProductRepositoryDownloader.buildAuthenticationToken(this.credentials);
    }
}
