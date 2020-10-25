package org.esa.snap.product.library.v2.preferences.model;

import java.util.List;

/**
 * The configuration data of a remote repository.
 */
public class RepositoriesCredentialsConfigurations {

    private List<RemoteRepositoryCredentials> repositoriesCredentials;
    private boolean autoUncompress;
    private int nrRecordsOnPage;

    public RepositoriesCredentialsConfigurations(List<RemoteRepositoryCredentials> repositoriesCredentials, boolean autoUncompress, int nrRecordsOnPage) {
        this.repositoriesCredentials = repositoriesCredentials;
        this.autoUncompress = autoUncompress;
        this.nrRecordsOnPage = nrRecordsOnPage;
    }

    public List<RemoteRepositoryCredentials> getRepositoriesCredentials() {
        return repositoriesCredentials;
    }

    public boolean isAutoUncompress() {
        return autoUncompress;
    }

    public int getNrRecordsOnPage() {
        return nrRecordsOnPage;
    }
}
