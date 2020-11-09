package org.esa.snap.product.library.v2.preferences.model;

import org.apache.http.auth.Credentials;
import org.esa.snap.core.util.StringUtils;

import java.util.List;

/**
 * The credentials of a remote repository.
 */
public class RemoteRepositoryCredentials {

    private final String remoteRepositoryName;
    private final List<Credentials> credentialsList;

    public RemoteRepositoryCredentials(String remoteRepositoryName, List<Credentials> credentialsList) {
        this.remoteRepositoryName = remoteRepositoryName;
        this.credentialsList = credentialsList;
    }

    public String getRepositoryName() {
        return remoteRepositoryName;
    }

    public List<Credentials> getCredentialsList() {
        return credentialsList;
    }

    private int findCredential(String targetUser, String targetPassword) {
        int matchesCount = 0;
        for (Credentials credential : credentialsList) {
            String username = credential.getUserPrincipal().getName();
            String password = credential.getPassword();
            if (StringUtils.isNotNullAndNotEmpty(targetUser)) {
                if (targetUser.equals(username)) {
                    if (StringUtils.isNotNullAndNotEmpty(targetPassword)) {
                        if (targetPassword.equals(password)) {
                            matchesCount++;
                        }
                    } else {
                        matchesCount++;
                    }
                }
            } else {
                if (StringUtils.isNotNullAndNotEmpty(targetPassword) && targetPassword.equals(password)) {
                    matchesCount++;
                }
            }
        }
        return matchesCount;
    }

    public boolean credentialExists(Credentials credential) {
        return findCredential(credential.getUserPrincipal().getName(), credential.getPassword()) > 0;
    }

    private boolean credentialDuplicate(String username) {
        return findCredential(username, "") > 1;
    }

    public boolean credentialInvalid(Credentials credential) {
        String username = credential.getUserPrincipal().getName();
        String password = credential.getPassword();
        return StringUtils.isNullOrEmpty(username) || StringUtils.isNullOrEmpty(password) || credentialDuplicate(username);
    }
}
