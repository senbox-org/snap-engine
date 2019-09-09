package org.esa.snap.product.library.v2.activator;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.product.library.v2.database.DatabaseTableNames;
import org.esa.snap.runtime.Activator;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 3/9/2019.
 */
public class ProductLibraryActivator implements Activator {

    private static final Logger logger = Logger.getLogger(ProductLibraryActivator.class.getName());

    public ProductLibraryActivator() {
    }

    public static Connection getConnection() throws SQLException {
        File databaseFolder = getDatabaseFolder();
        String databaseName = "products";

        Properties properties = new Properties();
        properties.put("user", "snap");
        properties.put("password", "");
        String databaseURL = "jdbc:h2:" + databaseFolder.getAbsolutePath() + "/" + databaseName;
        return DriverManager.getConnection(databaseURL, properties);
    }

    private static File getDatabaseFolder() {
//        return new File("d:/_snap-h2-database");
        File applicationDataFolder = SystemUtils.getApplicationDataDir(true);
        return new File(applicationDataFolder, "product-library");
    }

    @Override
    public void start() {
        try {
            // load Derby driver
            Class.forName("org.h2.Driver");

            try (Connection connection = getConnection()) {
                int currentDatabaseVersion = 0;
                // check if the 'version' table exists into the database
                if (doesTableExists(DatabaseTableNames.VERSIONS, connection)) {
                    // the 'version' table exists and load the current database version number
                    currentDatabaseVersion = loadCurrentDatabaseVersionNumber(connection);
                }

                String sourceFolderPath = "org/esa/snap/product/library/v2/database";
                String databaseFileNamePrefix = "h2gis-database-script-";
                LinkedHashMap<Integer, List<String>> allStatements = DatabaseUtils.loadDatabaseStatements(sourceFolderPath, databaseFileNamePrefix, currentDatabaseVersion);
                if (allStatements.size() > 0) {
                    connection.setAutoCommit(false);
                    try {
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
                    } catch (Exception exception) {
                        // rollback the statements from the transaction
                        connection.rollback();
                        throw exception;
                    }
                }
            }
        } catch (Exception exception) {
            logger.log(Level.SEVERE, "Failed to initialize the database.", exception);
        }
    }

    @Override
    public void stop() {
        // do nothing
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

    public static void main(String[] args) throws IOException {
        ProductLibraryActivator activator = new ProductLibraryActivator();
        activator.start();
    }
}