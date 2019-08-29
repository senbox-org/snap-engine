package org.esa.snap.product.library.v2;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.runtime.Config;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Created by jcoravu on 29/8/2019.
 */
public class RemoteRepositoryCredentials {

    private static final RemoteRepositoryCredentials instance = new RemoteRepositoryCredentials();

    private static final String PREFIX = "credential.";
    private static final String USERNAME = ".username";
    private static final String PASSWORD = ".password";

    private final Preferences credentialsPreferences;
    private final StandardPBEStringEncryptor encryptor;

    private RemoteRepositoryCredentials() {
        this.credentialsPreferences = Config.instance("Credentials").load().preferences();
        this.encryptor = new StandardPBEStringEncryptor();
        this.encryptor.setPassword("Mzg1YWFkNjY0MjA2MGY1ZTIyMThjYjFj");
    }

    public static RemoteRepositoryCredentials getInstance() {
        return instance;
    }

    public Credentials save(String remoteRepositoryId, String username, String password) throws BackingStoreException {
        this.credentialsPreferences.put(PREFIX + remoteRepositoryId + USERNAME, username);
        this.credentialsPreferences.put(PREFIX + remoteRepositoryId + PASSWORD, encryptor.encrypt(password));
        this.credentialsPreferences.flush();
        return new UsernamePasswordCredentials(username, password);
    }

    public Credentials read(String remoteRepositoryId) {
        final String username = this.credentialsPreferences.get(PREFIX + remoteRepositoryId + USERNAME, null);
        final String encryptedPassword = this.credentialsPreferences.get(PREFIX + remoteRepositoryId + PASSWORD, null);
        String password = this.encryptor.decrypt(encryptedPassword);
        return (username == null || password == null) ? null : new UsernamePasswordCredentials(username, password);
    }
}
