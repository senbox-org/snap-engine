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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RepositoryCredentials) {
            RepositoryCredentials target = (RepositoryCredentials) obj;
            return this.repositoryId.contentEquals(target.repositoryId) && (this.credentialsList.size() == target.credentialsList.size() && this.credentialsList.equals(target.credentialsList));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.repositoryId.hashCode() + this.credentialsList.hashCode();
    }
}
