package org.esa.snap.product.library.v2.preferences;

import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.engine_utilities.util.CryptoUtils;
import org.esa.snap.product.library.v2.preferences.model.RemoteRepositoryCollectionsCredentials;
import org.esa.snap.product.library.v2.preferences.model.RemoteRepositoryCredentials;
import org.esa.snap.product.library.v2.preferences.model.RepositoriesCredentialsConfigurations;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Save the repository credentials in a file from the local disk.
 */
public final class RepositoriesCredentialsPersistence {

    public static final byte VISIBLE_PRODUCTS_PER_PAGE = 10;

    public static final boolean UNCOMPRESSED_DOWNLOADED_PRODUCTS = false;

    public static final boolean DOWNLOAD_ALL_PAGES = false;

    /**
     * The pattern for remote repository.
     */
    private static final String REPO_ID_KEY = "%repo_id%";
    /**
     * The pattern for remote repository credential.
     */
    private static final String CRED_ID_KEY = "%cred_id%";
    /**
     * The regex for extract remote repositories IDs.
     */
    private static final String REPOSITORIES_IDS_REGEX = "repository_(.*?)\\.credential_.*?\\.username";
    /**
     * The preference key for remote repositories auto-uncompress setting.
     */
    private static final String PREFERENCE_KEY_AUTO_UNCOMPRESS = "auto_uncompress";
    /**
     * The preference key for remote repositories download all pages setting.
     */
    private static final String PREFERENCE_KEY_DOWNLOAD_ALL_PAGES = "download_all_pages";
    /**
     * The preference key for remote repositories records on page setting.
     */
    private static final String PREFERENCE_KEY_RECORDS_ON_PAGE = "records_on_page";
    /**
     * The preference key for remote repository item.
     */
    private static final String PREFERENCE_KEY_REPOSITORY = "repository_" + REPO_ID_KEY;
    /**
     * The regex for extract remote repository credentials IDs.
     */
    private static final String REPOSITORY_CREDENTIALS_IDS_REGEX = PREFERENCE_KEY_REPOSITORY + "\\.credential_(.*?)\\.username";
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
    /**
     * The pattern for remote repository collection.
     */
    private static final String COLL_ID_KEY = "%coll_id%";
    /**
     * The regex for extract collections IDs.
     */
    private static final String REPOSITORY_COLLECTIONS_IDS_REGEX = PREFERENCE_KEY_REPOSITORY + "\\.collection_(.*?)\\.search_credential\\.username";
    /**
     * The preference key for remote repository collection item.
     */
    private static final String PREFERENCE_KEY_REPOSITORY_COLLECTION = ".collection_" + COLL_ID_KEY;
    /**
     * The preference key for remote repository search credential item.
     */
    private static final String PREFERENCE_KEY_REPOSITORY_COLLECTION_SEARCH_CREDENTIAL = PREFERENCE_KEY_REPOSITORY + PREFERENCE_KEY_REPOSITORY_COLLECTION + ".search_credential";
    /**
     * The preference key for remote repository search credential item username.
     */
    private static final String PREFERENCE_KEY_REPOSITORY_COLLECTION_SEARCH_CREDENTIAL_USERNAME = PREFERENCE_KEY_REPOSITORY_COLLECTION_SEARCH_CREDENTIAL + ".username";
    /**
     * The preference key for remote repository search credential item password.
     */
    private static final String PREFERENCE_KEY_REPOSITORY_COLLECTION_SEARCH_CREDENTIAL_SECRET = PREFERENCE_KEY_REPOSITORY_COLLECTION_SEARCH_CREDENTIAL + ".password";

    private static String buildUsernameKey(String repositoryId, String credentialId) {
        String usernameKey = PREFERENCE_KEY_REPOSITORY_CREDENTIAL_USERNAME;
        usernameKey = usernameKey.replace(REPO_ID_KEY, repositoryId);
        usernameKey = usernameKey.replace(CRED_ID_KEY, credentialId);
        return usernameKey;
    }

    private static String buildSearchUsernameKey(String repositoryId, String collectionId) {
        String usernameKey = PREFERENCE_KEY_REPOSITORY_COLLECTION_SEARCH_CREDENTIAL_USERNAME;
        usernameKey = usernameKey.replace(REPO_ID_KEY, repositoryId);
        usernameKey = usernameKey.replace(COLL_ID_KEY, collectionId);
        return usernameKey;
    }

    private static String buildPasswordKey(String repositoryId, String credentialId) {
        String passwordKey = PREFERENCE_KEY_REPOSITORY_CREDENTIAL_SECRET;
        passwordKey = passwordKey.replace(REPO_ID_KEY, repositoryId);
        passwordKey = passwordKey.replace(CRED_ID_KEY, credentialId);
        return passwordKey;
    }

    private static String buildSearchPasswordKey(String repositoryId, String collectionId) {
        String passwordKey = PREFERENCE_KEY_REPOSITORY_COLLECTION_SEARCH_CREDENTIAL_SECRET;
        passwordKey = passwordKey.replace(REPO_ID_KEY, repositoryId);
        passwordKey = passwordKey.replace(COLL_ID_KEY, collectionId);
        return passwordKey;
    }

    private static String buildCredentialsIdsRegex(String repositoryId) {
        String credentialsIdsRegex = REPOSITORY_CREDENTIALS_IDS_REGEX;
        credentialsIdsRegex = credentialsIdsRegex.replace(REPO_ID_KEY, repositoryId);
        return credentialsIdsRegex;
    }

    private static String buildCollectionSearchCredentialsIdsRegex(String repositoryId) {
        String credentialsIdsRegex = REPOSITORY_COLLECTIONS_IDS_REGEX;
        credentialsIdsRegex = credentialsIdsRegex.replace(REPO_ID_KEY, repositoryId);
        return credentialsIdsRegex;
    }

    public static boolean validCredentials(List<RemoteRepositoryCredentials> repositoriesCredentials) {
        for (RemoteRepositoryCredentials repositoryCredentials : repositoriesCredentials) {
            for (Credentials credentials : repositoryCredentials.getCredentialsList()) {
                if (repositoryCredentials.credentialInvalid(credentials)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean validCollectionsCredentials(List<RemoteRepositoryCollectionsCredentials> repositoriesCollectionsCredentials) {
        for (RemoteRepositoryCollectionsCredentials repositoryCollectionsCredentials : repositoriesCollectionsCredentials) {
            for (Credentials credentials : repositoryCollectionsCredentials.getCollectionsCredentials().values()) {
                if (repositoryCollectionsCredentials.credentialInvalid(credentials)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void saveCredentials(Properties properties, List<RemoteRepositoryCredentials> repositoriesCredentials) {
        if (validCredentials(repositoriesCredentials)) {
            for (RemoteRepositoryCredentials repositoryCredentials : repositoriesCredentials) {
                String repositoryId = repositoryCredentials.getRepositoryName();
                int id = 1;
                for (Credentials credential : repositoryCredentials.getCredentialsList()) {
                    String credentialId = "" + id++;
                    String username = credential.getUserPrincipal().getName();
                    if (StringUtils.isNotNullAndNotEmpty(username)) {
                        String usernameKey = buildUsernameKey(repositoryId, credentialId);
                        properties.setProperty(usernameKey, username);
                    } else {
                        throw new IllegalArgumentException("empty username");
                    }
                    String password = credential.getPassword();
                    if (StringUtils.isNotNullAndNotEmpty(password)) {
                        String encryptedPassword;
                        try {
                            encryptedPassword = CryptoUtils.encrypt(password, repositoryId);
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to encrypt the password.", e);
                        }
                        String passwordKey = buildPasswordKey(repositoryId, credentialId);
                        properties.setProperty(passwordKey, encryptedPassword);
                    } else {
                        throw new IllegalArgumentException("empty password");
                    }
                }
            }

        } else {
            throw new IllegalArgumentException("invalid credentials (empty or duplicates)");
        }
    }

    private static void saveCollectionsCredentials(Properties properties, List<RemoteRepositoryCollectionsCredentials> repositoriesCollectionsCredentials) {
        if (validCollectionsCredentials(repositoriesCollectionsCredentials)) {
            for (RemoteRepositoryCollectionsCredentials repositoryCollectionsCredentials : repositoriesCollectionsCredentials) {
                String repositoryId = repositoryCollectionsCredentials.getRepositoryName();
                for (Map.Entry<String, Credentials> collectionCredential : repositoryCollectionsCredentials.getCollectionsCredentials().entrySet()) {
                    String collectionId = collectionCredential.getKey();
                    String username = collectionCredential.getValue().getUserPrincipal().getName();
                    if (StringUtils.isNotNullAndNotEmpty(username)) {
                        String usernameKey = buildSearchUsernameKey(repositoryId, collectionId);
                        properties.setProperty(usernameKey, username);
                    } else {
                        throw new IllegalArgumentException("empty username");
                    }
                    String password = collectionCredential.getValue().getPassword();
                    if (StringUtils.isNotNullAndNotEmpty(password)) {
                        String encryptedPassword;
                        try {
                            encryptedPassword = CryptoUtils.encrypt(password, repositoryId);
                        } catch (Exception e) {
                            throw new IllegalStateException("Failed to encrypt the password.", e);
                        }
                        String passwordKey = buildSearchPasswordKey(repositoryId, collectionId);
                        properties.setProperty(passwordKey, encryptedPassword);
                    } else {
                        throw new IllegalArgumentException("empty password");
                    }
                }
            }

        } else {
            throw new IllegalArgumentException("invalid credentials (empty or duplicates)");
        }
    }

    private static void saveAutoUncompress(Properties properties, boolean autoUncompress) {
        String autoUncompressVal = "false";
        if (autoUncompress) {
            autoUncompressVal = "true";
        }
        properties.setProperty(PREFERENCE_KEY_AUTO_UNCOMPRESS, autoUncompressVal);
    }

    private static void saveDownloadAllPages(Properties properties, boolean downloadAllPages) {
        String downloadAllPagesVal = "false";
        if (downloadAllPages) {
            downloadAllPagesVal = "true";
        }
        properties.setProperty(PREFERENCE_KEY_DOWNLOAD_ALL_PAGES, downloadAllPagesVal);
    }

    private static void saveRecordsOnPage(Properties properties, int recordsOnPageToSave) {
        String recordsOnPageVal = "" + recordsOnPageToSave;
        properties.setProperty(PREFERENCE_KEY_RECORDS_ON_PAGE, recordsOnPageVal);
    }

    static void save(Path destFile, RepositoriesCredentialsConfigurations repositoriesCredentialsConfigurations) throws IOException {
        if (destFile == null) {
            return;
        }
        Properties properties = new Properties();
        saveCredentials(properties, repositoriesCredentialsConfigurations.getRepositoriesCredentials());
        saveCollectionsCredentials(properties, repositoriesCredentialsConfigurations.getRemoteRepositoryCollectionsCredentials());
        saveAutoUncompress(properties, repositoriesCredentialsConfigurations.isAutoUncompress());
        saveDownloadAllPages(properties, repositoriesCredentialsConfigurations.downloadsAllPages());
        saveRecordsOnPage(properties, repositoriesCredentialsConfigurations.getNrRecordsOnPage());
        if (!Files.exists(destFile)) {
            Files.createDirectories(destFile.getParent());
            Files.createFile(destFile);
        }
        try (OutputStream outputStream = Files.newOutputStream(destFile)) {
            properties.store(outputStream, "");
        }
    }

    /**
     * Reads the Remote Repositories Credentials list from SNAP configuration file.
     */
    private static List<RemoteRepositoryCredentials> loadCredentials(Properties properties) {
        List<RemoteRepositoryCredentials> repositoriesCredentials = new ArrayList<>();
        Set<String> propertyNames = properties.stringPropertyNames();
        List<String> repositoriesIdsList = new ArrayList<>();
        for (String propertyName : propertyNames) {
            String repositoryId = propertyName.replaceAll(REPOSITORIES_IDS_REGEX, "$1");
            if (!repositoriesIdsList.contains(repositoryId)) {
                repositoriesIdsList.add(repositoryId);
            }
        }
        for (String repositoryId : repositoriesIdsList) {
            List<Credentials> repositoryCredentials = new ArrayList<>();
            List<String> repositoryCredentialsIds = new ArrayList<>();
            for (String propertyName : propertyNames) {
                String credentialId = propertyName.replaceAll(buildCredentialsIdsRegex(repositoryId), "$1");
                repositoryCredentialsIds.add(credentialId);
            }
            for (String credentialId : repositoryCredentialsIds) {
                String usernameKey = buildUsernameKey(repositoryId, credentialId);
                String username = properties.getProperty(usernameKey);
                String passwordKey = buildPasswordKey(repositoryId, credentialId);
                String password = properties.getProperty(passwordKey);
                try {
                    password = CryptoUtils.decrypt(password, repositoryId);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to decrypt the password.", e);
                }
                if (StringUtils.isNotNullAndNotEmpty(username) && StringUtils.isNotNullAndNotEmpty(password)) {
                    repositoryCredentials.add(new UsernamePasswordCredentials(username, password));
                }
            }
            if (!repositoryCredentials.isEmpty()) {
                repositoriesCredentials.add(new RemoteRepositoryCredentials(repositoryId, repositoryCredentials));
            }
        }
        return repositoriesCredentials;
    }

    /**
     * Reads the Remote Repositories Credentials list from SNAP configuration file.
     */
    private static List<RemoteRepositoryCollectionsCredentials> loadCollectionCredentials(Properties properties) {
        List<RemoteRepositoryCollectionsCredentials> repositoriesCollectionCredentials = new ArrayList<>();
        Set<String> propertyNames = properties.stringPropertyNames();
        List<String> repositoriesIdsList = new ArrayList<>();
        for (String propertyName : propertyNames) {
            String repositoryId = propertyName.replaceAll(REPOSITORIES_IDS_REGEX, "$1");
            if (!repositoriesIdsList.contains(repositoryId)) {
                repositoriesIdsList.add(repositoryId);
            }
        }
        for (String repositoryId : repositoriesIdsList) {
            Map<String, Credentials> repositoryCollectionCredentials = new LinkedHashMap<>();
            List<String> collectionsIds = new ArrayList<>();
            for (String propertyName : propertyNames) {
                String collectionId = propertyName.replaceAll(buildCollectionSearchCredentialsIdsRegex(repositoryId), "$1");
                collectionsIds.add(collectionId);
            }
            for (String collectionId : collectionsIds) {
                String usernameKey = buildSearchUsernameKey(repositoryId, collectionId);
                String username = properties.getProperty(usernameKey);
                String passwordKey = buildSearchPasswordKey(repositoryId, collectionId);
                String password = properties.getProperty(passwordKey);
                try {
                    password = CryptoUtils.decrypt(password, repositoryId);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to decrypt the password.", e);
                }
                if (StringUtils.isNotNullAndNotEmpty(username) && StringUtils.isNotNullAndNotEmpty(password)) {
                    repositoryCollectionCredentials.put(collectionId, new UsernamePasswordCredentials(username, password));
                }
            }
            if (!repositoryCollectionCredentials.isEmpty()) {
                repositoriesCollectionCredentials.add(new RemoteRepositoryCollectionsCredentials(repositoryId, repositoryCollectionCredentials));
            }
        }
        return repositoriesCollectionCredentials;
    }

    /**
     * Reads the Remote Repositories Credentials auto-uncompress setting from SNAP configuration file.
     */
    private static boolean loadAutoUncompress(Properties properties) {
        String autoUncompressVal = properties.getProperty(PREFERENCE_KEY_AUTO_UNCOMPRESS);
        if (autoUncompressVal != null && !autoUncompressVal.isEmpty()) {
            return autoUncompressVal.contentEquals("true");
        }
        return UNCOMPRESSED_DOWNLOADED_PRODUCTS;
    }

    /**
     * Reads the Remote Repositories Credentials auto-uncompress setting from SNAP configuration file.
     */
    private static boolean loadDownloadAllPages(Properties properties) {
        String downloadAllPagesVal = properties.getProperty(PREFERENCE_KEY_DOWNLOAD_ALL_PAGES);
        if (downloadAllPagesVal != null && !downloadAllPagesVal.isEmpty()) {
            return downloadAllPagesVal.contentEquals("true");
        }
        return DOWNLOAD_ALL_PAGES;
    }

    /**
     * Reads the Remote Repositories Credentials nr repositories on page setting from SNAP configuration file.
     */
    private static int loadRecordsOnPage(Properties properties) {
        String recordsOnPageVal = properties.getProperty(PREFERENCE_KEY_RECORDS_ON_PAGE);
        if (recordsOnPageVal != null && !recordsOnPageVal.isEmpty()) {
            return Integer.parseInt(recordsOnPageVal);
        }
        return VISIBLE_PRODUCTS_PER_PAGE;
    }

    /**
     * Reads the Remote Repositories Credentials configurations from SNAP configuration file.
     */
    static RepositoriesCredentialsConfigurations load(Path destFile) throws IOException {
        if (destFile == null || !Files.exists(destFile)) {
            return new RepositoriesCredentialsConfigurations(new ArrayList<>(), new ArrayList<>(), UNCOMPRESSED_DOWNLOADED_PRODUCTS, DOWNLOAD_ALL_PAGES, VISIBLE_PRODUCTS_PER_PAGE);
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(destFile)) {
            properties.load(inputStream);
        }
        List<RemoteRepositoryCredentials> repositoriesCredentials = loadCredentials(properties);
        List<RemoteRepositoryCollectionsCredentials> repositoriesCollectionsCredentials = loadCollectionCredentials(properties);
        boolean autoUncompress = loadAutoUncompress(properties);
        boolean downloadAllPages = loadDownloadAllPages(properties);
        int recordsOnPage = loadRecordsOnPage(properties);
        return new RepositoriesCredentialsConfigurations(repositoriesCredentials, repositoriesCollectionsCredentials, autoUncompress, downloadAllPages, recordsOnPage);
    }


    public static void main(String[] args) throws IOException {
        List<Credentials> repositoryCredentialsList = new ArrayList<>();
        repositoryCredentialsList.add(new UsernamePasswordCredentials("u1", "p1"));
        repositoryCredentialsList.add(new UsernamePasswordCredentials("u2", "p2"));
        RemoteRepositoryCredentials repositoryCredentials = new RemoteRepositoryCredentials("b1", repositoryCredentialsList);

        List<Credentials> repositoryCredentialsList2 = new ArrayList<>();
        repositoryCredentialsList2.add(new UsernamePasswordCredentials("as1", "pw1"));
        repositoryCredentialsList2.add(new UsernamePasswordCredentials("us2", "pw2"));
        RemoteRepositoryCredentials repositoryCredentials2 = new RemoteRepositoryCredentials("b2", repositoryCredentialsList2);
        List<RemoteRepositoryCredentials> itemsToSave = new ArrayList<>();
        itemsToSave.add(repositoryCredentials);
        itemsToSave.add(repositoryCredentials2);
        Map<String, Credentials> repositoryCollectionsCredentialsMap = new LinkedHashMap<>();
        repositoryCollectionsCredentialsMap.put("c1", new UsernamePasswordCredentials("u1", "p1"));
        repositoryCollectionsCredentialsMap.put("c2", new UsernamePasswordCredentials("u2", "p2"));
        RemoteRepositoryCollectionsCredentials repositoryCollectionsCredentials = new RemoteRepositoryCollectionsCredentials("b1", repositoryCollectionsCredentialsMap);
        Map<String, Credentials> repositoryCollectionsCredentialsMap2 = new LinkedHashMap<>();
        repositoryCollectionsCredentialsMap2.put("c1", new UsernamePasswordCredentials("as1", "pw1"));
        repositoryCollectionsCredentialsMap2.put("c2", new UsernamePasswordCredentials("us2", "pw2"));
        RemoteRepositoryCollectionsCredentials repositoryCollectionsCredentials2 = new RemoteRepositoryCollectionsCredentials("b2", repositoryCollectionsCredentialsMap2);
        List<RemoteRepositoryCollectionsCredentials> collectionsItemsToSave = new ArrayList<>();
        collectionsItemsToSave.add(repositoryCollectionsCredentials);
        collectionsItemsToSave.add(repositoryCollectionsCredentials2);

        boolean autoUncompressToSave = false;
        boolean downloadAllPagesToSave = true;
        int recordsOnPageToSave = 20;

        Path credsFile = Paths.get("D:/Temp/test_pl.properties");

        save(credsFile, new RepositoriesCredentialsConfigurations(itemsToSave, collectionsItemsToSave, autoUncompressToSave, downloadAllPagesToSave, recordsOnPageToSave));

        RepositoriesCredentialsConfigurations repositoriesCredentialsConfigurations = load(credsFile);
        List<RemoteRepositoryCredentials> itemsLoaded = repositoriesCredentialsConfigurations.getRepositoriesCredentials();
        List<RemoteRepositoryCollectionsCredentials> collectionsItemsLoaded = repositoriesCredentialsConfigurations.getRemoteRepositoryCollectionsCredentials();
        boolean autoUncompressLoaded = repositoriesCredentialsConfigurations.isAutoUncompress();
        boolean downloadAllPagesLoaded = repositoriesCredentialsConfigurations.downloadsAllPages();
        int recordsOnPageLoaded = repositoriesCredentialsConfigurations.getNrRecordsOnPage();

        if (itemsToSave != itemsLoaded) {
            throw new IllegalStateException("items mismatch");
        }
        if (collectionsItemsToSave != collectionsItemsLoaded) {
            throw new IllegalStateException("collection items mismatch");
        }
        if (autoUncompressToSave != autoUncompressLoaded) {
            throw new IllegalStateException("auto uncompress mismatch");
        }
        if (downloadAllPagesToSave != downloadAllPagesLoaded) {
            throw new IllegalStateException("download all pages mismatch");
        }
        if (recordsOnPageToSave != recordsOnPageLoaded) {
            throw new IllegalStateException("records on page mismatch");
        }
    }
}
