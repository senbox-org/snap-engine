package org.esa.snap.product.library.v2.preferences.model;

import org.apache.http.auth.Credentials;

import java.util.List;

public class RepositoryCredentials {

    private String repositoryId;
    private List<Credentials> credentialsList;

    public RepositoryCredentials(String repositoryId, List<Credentials> credentialsList) {
        this.repositoryId = repositoryId;
        this.credentialsList = credentialsList;
    }

    public String getRepositoryId() {
        return repositoryId;
    }

    public List<Credentials> getCredentialsList() {
        return credentialsList;
    }

}
