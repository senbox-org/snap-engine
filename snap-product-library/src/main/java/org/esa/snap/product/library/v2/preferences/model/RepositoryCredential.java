package org.esa.snap.product.library.v2.preferences.model;

import org.apache.http.auth.BasicUserPrincipal;
import org.apache.http.auth.Credentials;

import java.security.Principal;

public final class RepositoryCredential implements Credentials {

    private String username;
    private String password;

    public RepositoryCredential(Credentials credentials) {
        this(credentials.getUserPrincipal().getName(), credentials.getPassword());
    }

    public RepositoryCredential(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public Principal getUserPrincipal() {
        return new BasicUserPrincipal(username);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
