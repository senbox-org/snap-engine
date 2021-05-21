package org.esa.snap.product.library.v2.preferences.model;

import java.util.List;

/**
 * The configuration data of a remote repository.
 */
public class RepositoriesCredentialsConfigurations {

    private List<RemoteRepositoryCredentials> repositoriesCredentials;
    private boolean autoUncompress;
    private boolean downloadAllPages;
    private int nrRecordsOnPage;

    public RepositoriesCredentialsConfigurations(List<RemoteRepositoryCredentials> repositoriesCredentials, boolean autoUncompress, boolean downloadAllPages, int nrRecordsOnPage) {
        this.repositoriesCredentials = repositoriesCredentials;
        this.autoUncompress = autoUncompress;
        this.downloadAllPages = downloadAllPages;
        this.nrRecordsOnPage = nrRecordsOnPage;
    }

    public List<RemoteRepositoryCredentials> getRepositoriesCredentials() {
        return repositoriesCredentials;
    }

    public boolean isAutoUncompress() {
        return autoUncompress;
    }

    public boolean downloadsAllPages() {
        return downloadAllPages;
    }

    public int getNrRecordsOnPage() {
        return nrRecordsOnPage;
    }
}
