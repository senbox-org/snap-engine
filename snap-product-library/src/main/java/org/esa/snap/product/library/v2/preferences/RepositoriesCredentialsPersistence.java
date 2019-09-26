package org.esa.snap.product.library.v2.preferences;

import org.apache.http.auth.Credentials;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.product.library.v2.preferences.model.RepositoryCredential;
import org.esa.snap.product.library.v2.preferences.model.RepositoryCredentials;
import ro.cs.tao.utils.Crypto;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class RepositoriesCredentialsPersistence {

    /**
     * The separator used on remote repositories/remote repository properties ids list.
     */
    private static final String LIST_ITEM_SEPARATOR = ";";
    /**
     * The pattern for remote repository.
     */
    private static final String REPO_ID_KEY = "%repo_id%";
    /**
     * The pattern for remote repository credential.
     */
    private static final String CRED_ID_KEY = "%cred_id%";
    /**
     * The preference key for remote repositories list.
     */
    private static final String PREFERENCE_KEY_REPOSITORIES = "repositories";
    /**
     * The preference key for remote repository item.
     */
    private static final String PREFERENCE_KEY_REPOSITORY = "repository_" + REPO_ID_KEY;
    /**
     * The preference key for remote repository credentials list.
     */
    private static final String PREFERENCE_KEY_REPOSITORY_CREDENTIALS = PREFERENCE_KEY_REPOSITORY + ".credentials";
    /**
     * The preference key for remote repository credential item.
     */
    private static final String PREFERENCE_KEY_REPOSITORY_CREDENTIAL = PREFERENCE_KEY_REPOSITORY + ".credential_" + CRED_ID_KEY;
    /**
     * The preference key for remote repository credential item username.
     */
    private static final String PREFERENCE_KEY_REPOSITORY_CREDENTIAL_USERNAME = PREFERENCE_KEY_REPOSITORY_CREDENTIAL + ".username";
    /**
     * The preference key for remote repository credential item password.
     */
    private static final String PREFERENCE_KEY_REPOSITORY_CREDENTIAL_SECRET = PREFERENCE_KEY_REPOSITORY_CREDENTIAL + ".password";

    private static String buildUsernameKey(String repositoryId, String credentialId) {
        String usernameKey = PREFERENCE_KEY_REPOSITORY_CREDENTIAL_USERNAME;
        usernameKey = usernameKey.replace(REPO_ID_KEY, repositoryId);
        usernameKey = usernameKey.replace(CRED_ID_KEY, credentialId);
        return usernameKey;
    }

    private static String buildPasswordKey(String repositoryId, String credentialId) {
        String passwordKey = PREFERENCE_KEY_REPOSITORY_CREDENTIAL_SECRET;
        passwordKey = passwordKey.replace(REPO_ID_KEY, repositoryId);
        passwordKey = passwordKey.replace(CRED_ID_KEY, credentialId);
        return passwordKey;
    }

    private static String buildCredentialsKey(String repositoryId) {
        String credentialsKey = PREFERENCE_KEY_REPOSITORY_CREDENTIALS;
        credentialsKey = credentialsKey.replace(REPO_ID_KEY, repositoryId);
        return credentialsKey;
    }

    public static void save(Path destFile, List<RepositoryCredentials> repositoriesCredentials) throws IOException {
        if (!repositoriesCredentials.isEmpty() && destFile != null) {
            Properties properties = new Properties();
            String repositoriesIds = "";
            for (RepositoryCredentials repositoryCredentials : repositoriesCredentials) {
                String repositoryCredentialsIds = "";
                int id = 1;
                for (Credentials credential : repositoryCredentials.getCredentialsList()) {
                    String credentialId = "" + id++;
                    boolean validCredential = true;
                    String username = credential.getUserPrincipal().getName();
                    if (StringUtils.isNotNullAndNotEmpty(username)) {
                        String usernameKey = buildUsernameKey(repositoryCredentials.getRepositoryId(), credentialId);
                        properties.setProperty(usernameKey, username);
                    } else {
                        validCredential = false;
                    }
                    String password = credential.getPassword();
                    if (StringUtils.isNotNullAndNotEmpty(password)) {
                        String encryptedPassword = Crypto.encrypt(password, repositoryCredentials.getRepositoryId());
                        String passwordKey = buildPasswordKey(repositoryCredentials.getRepositoryId(), credentialId);
                        properties.setProperty(passwordKey, encryptedPassword);
                    } else {
                        validCredential = false;
                    }
                    if (validCredential) {
                        repositoryCredentialsIds = !repositoryCredentialsIds.isEmpty() ? repositoryCredentialsIds + LIST_ITEM_SEPARATOR + credentialId : credentialId;
                    }
                }
                if (StringUtils.isNotNullAndNotEmpty(repositoryCredentialsIds)) {
                    String credentialsKey = buildCredentialsKey(repositoryCredentials.getRepositoryId());
                    properties.setProperty(credentialsKey, repositoryCredentialsIds);
                    if (repositoriesIds.isEmpty()) {
                        repositoriesIds = repositoryCredentials.getRepositoryId();
                    } else {
                        repositoriesIds = repositoriesIds.concat(LIST_ITEM_SEPARATOR + repositoryCredentials.getRepositoryId());
                    }
                }
            }
            if (StringUtils.isNotNullAndNotEmpty(repositoriesIds)) {
                properties.setProperty(PREFERENCE_KEY_REPOSITORIES, repositoriesIds);
            }
            OutputStream outputStream = null;
            try {
                if (!Files.exists(destFile)) {
                    Files.createDirectories(destFile.getParent());
                    Files.createFile(destFile);
                }
                outputStream = Files.newOutputStream(destFile);
                properties.store(outputStream, "");
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException ignored) {
                        //do nothing
                    }
                }
            }
        }
    }

    /**
     * Reads the Remote Repositories Credentials from SNAP configuration file.
     */
    public static List<RepositoryCredentials> load(Path destFile) throws IOException {
        List<RepositoryCredentials> repositoriesCredentials = new ArrayList<>();
        if (destFile == null) {
            return repositoriesCredentials;
        }
        Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            if (Files.exists(destFile)) {
                inputStream = Files.newInputStream(destFile);
                properties.load(inputStream);
            }
            String repositoriesIds = properties.getProperty(PREFERENCE_KEY_REPOSITORIES);
            if (StringUtils.isNullOrEmpty(repositoriesIds)) {
                return repositoriesCredentials;
            }
            String[] repositoriesIdsList = repositoriesIds.split(LIST_ITEM_SEPARATOR);
            for (String repositoryId : repositoriesIdsList) {
                List<Credentials> repositoryCredentials = new ArrayList<>();
                String credentialsKey = buildCredentialsKey(repositoryId);
                String repositoryCredentialsIdsString = properties.getProperty(credentialsKey);
                if (repositoryCredentialsIdsString == null) {
                    continue;
                }
                String[] repositoryCredentialsIds = repositoryCredentialsIdsString.split(LIST_ITEM_SEPARATOR);
                for (String credentialId : repositoryCredentialsIds) {
                    String usernameKey = buildUsernameKey(repositoryId, credentialId);
                    String username = properties.getProperty(usernameKey);
                    String passwordKey = buildPasswordKey(repositoryId, credentialId);
                    String password = properties.getProperty(passwordKey);
                    password = Crypto.decrypt(password, repositoryId);
                    if (StringUtils.isNotNullAndNotEmpty(username) && StringUtils.isNotNullAndNotEmpty(password)) {
                        repositoryCredentials.add(new RepositoryCredential(username, password));
                    }
                }
                if (!repositoryCredentials.isEmpty()) {
                    repositoriesCredentials.add(new RepositoryCredentials(repositoryId, repositoryCredentials));
                }
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                    //do nothing
                }
            }
        }
        return repositoriesCredentials;
    }

    public static void main(String[] args) throws IOException {
        List<Credentials> repositoryCredentialsList = new ArrayList<>();
        repositoryCredentialsList.add(new RepositoryCredential("u1", "p1"));
        repositoryCredentialsList.add(new RepositoryCredential("u2", "p2"));
        RepositoryCredentials repositoryCredentials = new RepositoryCredentials("b1", repositoryCredentialsList);

        List<Credentials> repositoryCredentialsList2 = new ArrayList<>();
        repositoryCredentialsList2.add(new RepositoryCredential("as1", "pw1"));
        repositoryCredentialsList2.add(new RepositoryCredential("us2", "pw2"));
        RepositoryCredentials repositoryCredentials2 = new RepositoryCredentials("b2", repositoryCredentialsList2);
        List<RepositoryCredentials> itemsToSave = new ArrayList<>();
        itemsToSave.add(repositoryCredentials);
        itemsToSave.add(repositoryCredentials2);
        Path credsFile = Paths.get("D:/Temp/test_pl.properties");

        save(credsFile, itemsToSave);

        List<RepositoryCredentials> itemsLoaded = load(credsFile);
        System.out.println(itemsLoaded.size());
    }

}
