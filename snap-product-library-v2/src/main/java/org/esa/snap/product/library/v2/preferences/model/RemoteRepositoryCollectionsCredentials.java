package org.esa.snap.product.library.v2.preferences.model;

import org.apache.http.auth.Credentials;
import org.esa.snap.core.util.StringUtils;

import java.util.Map;

/**
 * The credentials of a remote repository.
 */
public class RemoteRepositoryCollectionsCredentials {

    private final String remoteRepositoryName;
    private final Map<String, Credentials> collectionsCredentials;

    public RemoteRepositoryCollectionsCredentials(String remoteRepositoryName, Map<String, Credentials> collectionsCredentials) {
        this.remoteRepositoryName = remoteRepositoryName;
        this.collectionsCredentials = collectionsCredentials;
    }

    public String getRepositoryName() {
        return remoteRepositoryName;
    }

    public Map<String, Credentials> getCollectionsCredentials() {
        return collectionsCredentials;
    }

    public boolean credentialInvalid(Credentials credential) {
        String username = credential.getUserPrincipal().getName();
        String password = credential.getPassword();
        return StringUtils.isNullOrEmpty(username) || StringUtils.isNullOrEmpty(password);
    }
}
