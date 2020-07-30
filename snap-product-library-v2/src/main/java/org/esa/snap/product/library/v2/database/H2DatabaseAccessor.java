package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.h2gis.functions.factory.H2GISFunctions;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/**
 * The class contains utility methods to open a connection on a database, upgrade the database structure.
 *
 * Created by jcoravu on 10/9/2019.
 */
public class H2DatabaseAccessor {

    public static final String DATABASE_DEFINITION_LANGUAGE_SOURCE_FOLDER_PATH = "org/esa/snap/product/library/v2/database";
    public static final String DATABASE_SQL_FILE_NAME_PREFIX = "h2gis-database-script-";

    private H2DatabaseAccessor() {
    }

    public static Connection getConnection(H2DatabaseParameters databaseParameters) throws SQLException {
        return getConnection(databaseParameters.getUrl(), databaseParameters.getProperties());
    }

    public static Path getDatabaseParentFolder() {
        Path applicationDataFolder = SystemUtils.getApplicationDataDir(true).toPath();
        return applicationDataFolder.resolve("product-library");
    }

    public static void upgradeDatabase(String databaseUrl, Properties databaseProperties) throws SQLException, IOException {
        try (Connection connection = getConnection(databaseUrl, databaseProperties)) {
            int currentDatabaseVersion = 0;
            // check if the 'version' table exists into the database
            if (doesTableExists(DatabaseTableNames.VERSIONS, connection)) {
                // the 'version' table exists and load the current database version number
                currentDatabaseVersion = loadCurrentDatabaseVersionNumber(connection);
            }

            String sourceFolderPath = DATABASE_DEFINITION_LANGUAGE_SOURCE_FOLDER_PATH;
            String databaseFileNamePrefix = DATABASE_SQL_FILE_NAME_PREFIX;
            LinkedHashMap<Integer, List<String>> allStatements = DatabaseUtils.loadDatabaseStatements(sourceFolderPath, databaseFileNamePrefix, currentDatabaseVersion);
            if (allStatements.size() > 0) {
                if (!connection.getAutoCommit()) {
                    throw new IllegalStateException("The connection has an opened transaction.");
                }
                boolean success = false;
                connection.setAutoCommit(false);
                try {
                    if (currentDatabaseVersion == 0) {
                        H2GISFunctions.load(connection);
                    }
                    try (Statement statement = connection.createStatement()) {
                        Iterator<Map.Entry<Integer, List<String>>> it = allStatements.entrySet().iterator();
                        while (it.hasNext()) {
                            Map.Entry<Integer, List<String>> entry = it.next();
                            int patchNumber = entry.getKey().intValue();
                            List<String> patchStatements = entry.getValue();
                            for (int i = 0; i < patchStatements.size(); i++) {
                                statement.addBatch(patchStatements.get(i));
                            }
                            StringBuilder sql = new StringBuilder();
                            sql.append("INSERT INTO ")
                                    .append(DatabaseTableNames.VERSIONS)
                                    .append(" (id) VALUES (")
                                    .append(patchNumber)
                                    .append(")");
                            statement.addBatch(sql.toString());
                            statement.executeBatch();
                            statement.clearBatch();
                        }
                    }
                    // commit the statements
                    connection.commit();

                    success = true;
                } finally {
                    if (!success) {
                        // rollback the statements from the transaction
                        connection.rollback();
                    }
                }
            }
        }
    }

    private static int loadCurrentDatabaseVersionNumber(Connection connection) throws SQLException {
        int currentDatabaseVersion = 0;
        try (Statement statement = connection.createStatement()) {
            String sql = "SELECT id FROM " + DatabaseTableNames.VERSIONS;
            try (ResultSet result = statement.executeQuery(sql)) {
                while (result.next()) {
                    int versionNumber = result.getInt("id");
                    if (currentDatabaseVersion < versionNumber) {
                        currentDatabaseVersion = versionNumber;
                    }
                }
            }
        }
        return currentDatabaseVersion;
    }

    private static boolean doesTableExists(String tableName, Connection connection) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        try (ResultSet result = databaseMetaData.getTables(null, null, tableName.toUpperCase(), null)) {
            return result.next();
        }
    }

    private static Connection getConnection(String databaseUrl, Properties databaseProperties) throws SQLException {
        if (StringUtils.isNullOrEmpty(databaseUrl)) {
            throw new NullPointerException("The database url is null or empty.");
        }
        if (databaseProperties == null) {
            throw new NullPointerException("The database properties are null.");
        }
        return DriverManager.getConnection(databaseUrl, databaseProperties);
    }
}
