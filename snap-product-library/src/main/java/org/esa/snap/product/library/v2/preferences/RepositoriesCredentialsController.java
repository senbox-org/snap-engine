package org.esa.snap.product.library.v2.preferences;

import org.apache.http.auth.Credentials;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.product.library.v2.preferences.model.RepositoryCredential;
import org.esa.snap.product.library.v2.preferences.model.RepositoryCredentials;
import org.esa.snap.runtime.EngineConfig;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A controller for Product Library Remote Repositories Credentials.
 * Used for establish a strategy with storing remote repositories credentials data.
 *
 * @author Adrian Draghici
 */
public class RepositoriesCredentialsController {

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
     * The preference key for remote repository properties list.
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

    private static RepositoriesCredentialsController instance = new RepositoriesCredentialsController(getDefaultConfigFilePath());

    private static Logger logger = Logger.getLogger(RepositoriesCredentialsController.class.getName());

    private final Path plConfigFile;
    private List<RepositoryCredentials> repositoriesCredentials = new ArrayList<>();


    /**
     * Creates the new VFS Remote File Repositories Controller with given config file.
     */
    private RepositoriesCredentialsController(Path plConfigFile) {
        this.plConfigFile = plConfigFile;
        try {
            this.repositoriesCredentials = load(this.plConfigFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read the credentials from the application preferences.", e);
        }
    }

    private static Path getDefaultConfigFilePath() {
        return Paths.get(EngineConfig.instance().userDir().toString() + "/config/Preferences/pl.properties");
    }

    public static RepositoriesCredentialsController getInstance() {
        return instance;
    }

    public static RepositoriesCredentialsController getCustomInstance(Path plConfigFile) {
        return new RepositoriesCredentialsController(plConfigFile);
    }

    private static void save(Path destFile, List<RepositoryCredentials> repositoriesCredentials) throws IOException {
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
                        properties.setProperty(PREFERENCE_KEY_REPOSITORY_CREDENTIAL_USERNAME.replace(REPO_ID_KEY, repositoryCredentials.getRepositoryId()).replace(CRED_ID_KEY, credentialId), username);
                    } else {
                        validCredential = false;
                    }
                    String password = credential.getPassword();
                    if (StringUtils.isNotNullAndNotEmpty(password)) {
                        String encryptedPassword = Crypto.encrypt(password, repositoryCredentials.getRepositoryId());
                        properties.setProperty(PREFERENCE_KEY_REPOSITORY_CREDENTIAL_SECRET.replace(REPO_ID_KEY, repositoryCredentials.getRepositoryId()).replace(CRED_ID_KEY, credentialId), encryptedPassword);
                    } else {
                        validCredential = false;
                    }
                    if (validCredential) {
                        repositoryCredentialsIds = !repositoryCredentialsIds.isEmpty() ? repositoryCredentialsIds + LIST_ITEM_SEPARATOR + credentialId : credentialId;
                    }
                }
                if (StringUtils.isNotNullAndNotEmpty(repositoryCredentialsIds)) {
                    properties.setProperty(PREFERENCE_KEY_REPOSITORY_CREDENTIALS.replace(REPO_ID_KEY, repositoryCredentials.getRepositoryId()), repositoryCredentialsIds);
                    repositoriesIds = !repositoriesIds.isEmpty() ? repositoriesIds + LIST_ITEM_SEPARATOR + repositoryCredentials.getRepositoryId() : repositoryCredentials.getRepositoryId();
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
    private static List<RepositoryCredentials> load(Path destFile) throws IOException {
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
                String repositoryCredentialsIdsString = properties.getProperty(PREFERENCE_KEY_REPOSITORY_CREDENTIALS.replace(REPO_ID_KEY, repositoryId));
                if (repositoryCredentialsIdsString == null) {
                    continue;
                }
                String[] repositoryCredentialsIds = repositoryCredentialsIdsString.split(LIST_ITEM_SEPARATOR);
                for (String repositoryCredentialId : repositoryCredentialsIds) {
                    String repositoryCredentialUsername = properties.getProperty(PREFERENCE_KEY_REPOSITORY_CREDENTIAL_USERNAME.replace(REPO_ID_KEY, repositoryId).replace(CRED_ID_KEY, repositoryCredentialId));
                    String repositoryCredentialPassword = properties.getProperty(PREFERENCE_KEY_REPOSITORY_CREDENTIAL_SECRET.replace(REPO_ID_KEY, repositoryId).replace(CRED_ID_KEY, repositoryCredentialId));
                    repositoryCredentialPassword = Crypto.decrypt(repositoryCredentialPassword, repositoryId);
                    if (StringUtils.isNotNullAndNotEmpty(repositoryCredentialUsername) && StringUtils.isNotNullAndNotEmpty(repositoryCredentialPassword)) {
                        repositoryCredentials.add(new RepositoryCredential(repositoryCredentialUsername, repositoryCredentialPassword));
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

    public List<RepositoryCredentials> getRepositoriesCredentials() {
        return this.repositoriesCredentials;
    }

    /**
     * Writes the provided Remote Repositories Credentials on SNAP configuration file.
     */
    public void saveCredentials(List<RepositoryCredentials> repositoriesCredentialsForSave) throws IOException {
        this.repositoriesCredentials = repositoriesCredentialsForSave;
        save(this.plConfigFile, this.repositoriesCredentials);
    }

    /**
     * Gets the list of remote repository credentials.
     *
     * @param remoteRepositoryId The remote repository id
     * @return The list of remote repository properties
     */
    public List<Credentials> getRemoteRepositoryCredentials(String remoteRepositoryId) {
        for (RepositoryCredentials repositoryCredentials : repositoriesCredentials) {
            if (repositoryCredentials.getRepositoryId().contentEquals(remoteRepositoryId)) {
                return repositoryCredentials.getCredentialsList();
            }
        }
        return new ArrayList<>();
    }

}
