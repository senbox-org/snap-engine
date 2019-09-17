package org.esa.snap.product.library.v2.database;

import org.esa.snap.core.util.SystemUtils;
import org.h2.Driver;
import org.h2.util.OsgiDataSourceFactory;
import org.h2gis.functions.factory.H2GISDBFactory;
import org.h2gis.functions.factory.H2GISFunctions;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
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

/**
 * Created by jcoravu on 10/9/2019.
 */
public class H2DatabaseAccessor {

    private H2DatabaseAccessor() {
    }

    public static Connection getConnection() throws SQLException {
        Path databaseParentFolder = getDatabaseParentFolder();
        String databaseName = "products";
        Properties properties = new Properties();
        properties.put("user", "snap");
        properties.put("password", "");
        String databaseURL = "jdbc:h2:" + databaseParentFolder.resolve(databaseName).toString();
        return DriverManager.getConnection(databaseURL, properties);
    }

    public static Path getDatabaseParentFolder() {
        Path applicationDataFolder = SystemUtils.getApplicationDataDir(true).toPath();
        return applicationDataFolder.resolve("product-library");
    }

    public static void upgradeDatabase() throws SQLException, IOException {
        try (Connection connection = H2DatabaseAccessor.getConnection()) {
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
                } catch (Exception exception) {
                    // rollback the statements from the transaction
                    connection.rollback();
                    throw exception;
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
}
