package org.esa.snap.product.library.v2.preferences;

import org.apache.http.auth.Credentials;
import org.esa.snap.product.library.v2.preferences.model.RemoteRepositoryCollectionsCredentials;
import org.esa.snap.product.library.v2.preferences.model.RemoteRepositoryCredentials;
import org.esa.snap.product.library.v2.preferences.model.RepositoriesCredentialsConfigurations;
import org.esa.snap.runtime.EngineConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A controller for Product Library Remote Repositories Credentials.
 * Used for establish a strategy with storing remote repositories credentials configuration data.
 *
 * @author Adrian Draghici
 */
public class RepositoriesCredentialsController {

    private static RepositoriesCredentialsController instance = new RepositoriesCredentialsController(getDefaultConfigFilePath());

    private static Logger logger = Logger.getLogger(RepositoriesCredentialsController.class.getName());

    private final Path plConfigFile;
    private RepositoriesCredentialsConfigurations repositoriesCredentialsConfigurations;


    /**
     * Creates the new Remote Repositories Credentials Controller with given config file.
     */
    private RepositoriesCredentialsController(Path plConfigFile) {
        this.plConfigFile = plConfigFile;
        try {
            this.repositoriesCredentialsConfigurations = RepositoriesCredentialsPersistence.load(this.plConfigFile);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read the credentials from the application preferences.", e);
        }
    }

    private static Path getDefaultConfigFilePath() {
        return Paths.get(EngineConfig.instance().userDir().toString() + "/config/Preferences/product-library.properties");
    }

    public static RepositoriesCredentialsController getInstance() {
        return instance;
    }

    /**
     * Gets the list of Remote Repositories Credentials.
     *
     * @return the list of Remote Repositories Credentials
     */
    public List<RemoteRepositoryCredentials> getRepositoriesCredentials() {
        return this.repositoriesCredentialsConfigurations.getRepositoriesCredentials();
    }

    /**
     * Gets the list of Remote Repositories Collections Credentials.
     *
     * @return the list of Remote Repositories Credentials
     */
    public List<RemoteRepositoryCollectionsCredentials> getRepositoriesCollectionsCredentials() {
        return this.repositoriesCredentialsConfigurations.getRemoteRepositoryCollectionsCredentials();
    }

    public void saveRepositoryCollectionCredential(String remoteRepositoryName, String collectionId, Credentials credential) {
        boolean found = false;
        if (!getRepositoriesCollectionsCredentials().isEmpty()) {
            for (RemoteRepositoryCollectionsCredentials collectionsCredentials : getRepositoriesCollectionsCredentials()) {
                if (collectionsCredentials.getRepositoryName().contentEquals(remoteRepositoryName)) {
                    found = true;
                    if (collectionsCredentials.getCollectionsCredentials().containsKey(collectionId)) {
                        collectionsCredentials.getCollectionsCredentials().replace(collectionId, credential);
                    } else {
                        collectionsCredentials.getCollectionsCredentials().put(collectionId, credential);
                    }
                }
            }
        }
        if (!found) {
            Map<String, Credentials> collectionCredentials = new LinkedHashMap<>();
            collectionCredentials.put(collectionId, credential);
            getRepositoriesCollectionsCredentials().add(new RemoteRepositoryCollectionsCredentials(remoteRepositoryName, collectionCredentials));
        }
        try {
            saveConfigurations(this.repositoriesCredentialsConfigurations);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Credentials getRepositoryCollectionCredential(String remoteRepositoryName, String collectionId) {
        if (!getRepositoriesCollectionsCredentials().isEmpty()) {
            for (RemoteRepositoryCollectionsCredentials collectionsCredentials : getRepositoriesCollectionsCredentials()) {
                if (collectionsCredentials.getRepositoryName().contentEquals(remoteRepositoryName) && collectionsCredentials.getCollectionsCredentials().containsKey(collectionId)) {
                    return collectionsCredentials.getCollectionsCredentials().get(collectionId);
                }
            }
        }
        return null;
    }

    /**
     * Gets whether auto-decompression of archived downloaded products is enabled
     *
     * @return {@code true} if the auto-decompression of archived downloaded products is enabled
     */
    public boolean isAutoUncompress() {
        return this.repositoriesCredentialsConfigurations.isAutoUncompress();
    }

    /**
     * Gets whether downloading all pages of search results is enabled
     *
     * @return {@code true} if downloading all pages of search results is enabled
     */
    public boolean downloadsAllPages() {
        return this.repositoriesCredentialsConfigurations.downloadsAllPages();
    }

    /**
     * Gets the number of products on search results page
     *
     * @return the number of products on search results page
     */
    public int getNrRecordsOnPage() {
        return this.repositoriesCredentialsConfigurations.getNrRecordsOnPage();
    }

    /**
     * Writes the provided Remote Repositories Credentials configurations on SNAP configuration file.
     */
    public void saveConfigurations(RepositoriesCredentialsConfigurations repositoriesCredentialsConfigurationsForSave) throws IOException {
        this.repositoriesCredentialsConfigurations = repositoriesCredentialsConfigurationsForSave;
        RepositoriesCredentialsPersistence.save(this.plConfigFile, this.repositoriesCredentialsConfigurations);
    }
}
