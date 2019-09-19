package org.esa.snap.product.library.v2.preferences.model;

import org.apache.http.auth.Credentials;

import javax.security.auth.Subject;
import java.security.Principal;

public final class RepositoryCredential implements Credentials, Principal {

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
        return this;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the name of this principal.
     *
     * @return the name of this principal.
     */
    @Override
    public String getName() {
        return this.username;
    }

    /**
     * Returns true if the specified subject is implied by this principal.
     *
     * <p>The default implementation of this method returns true if
     * {@code subject} is non-null and contains at least one principal that
     * is equal to this principal.
     *
     * <p>Subclasses may override this with a different implementation, if
     * necessary.
     *
     * @param subject the {@code Subject}
     * @return true if {@code subject} is non-null and is
     * implied by this principal, or false otherwise.
     * @since 1.8
     */
    @Override
    public boolean implies(Subject subject) {
        return false;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RepositoryCredential) {
            RepositoryCredential target = (RepositoryCredential) obj;
            return this.username.contentEquals(target.username) && this.password.contentEquals(target.password);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.username.hashCode() + this.password.hashCode();
    }
}
