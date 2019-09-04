package org.esa.snap.product.library.v2.activator;

import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.product.library.v2.database.DatabaseTableNames;
import org.esa.snap.runtime.Activator;

import java.io.File;
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
public class DerbyDatabaseActivator implements Activator {

    private static final Logger logger = Logger.getLogger(DerbyDatabaseActivator.class.getName());

    public DerbyDatabaseActivator() {
    }

    public static Connection getConnection(boolean createDatabaseifMissing) throws SQLException {
        File derbyDatabaseFolder = getDatabaseFolder();

        Properties properties = new Properties();
        properties.put("user", "nestuser");
        properties.put("password", "snapuser");
        String databaseURL = "jdbc:derby:" + derbyDatabaseFolder.getAbsolutePath();
        if (createDatabaseifMissing) {
            databaseURL += ";create=true";
        }
        return DriverManager.getConnection(databaseURL, properties);
    }

    private static File getDatabaseFolder() {
        File applicationDataFolder = SystemUtils.getApplicationDataDir(true);
        return new File(applicationDataFolder, "product-library-database");
    }

    @Override
    public void start() {
        try {
            // load Derby driver
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");

            try (Connection connection = getConnection(true)) {
                int currentDatabaseVersion = 0;
                // check if the 'version' table exists into the database
                if (doesTableExists(DatabaseTableNames.VERSIONS, connection)) {
                    // the 'version' table exists and load the current database version number
                    currentDatabaseVersion = loadCurrentDatabaseVersionNumber(connection);
                }

                String sourceFolderPath = "org/esa/snap/product/library/v2/database";
                String databaseFileNamePrefix = "derby-database-script-";
                LinkedHashMap<Integer, List<String>> allStatements = DatabaseUtils.loadDatabaseStatements(sourceFolderPath, databaseFileNamePrefix, currentDatabaseVersion);
                if (allStatements.size() > 0) {
                    connection.setAutoCommit(false);

                    Iterator<Map.Entry<Integer, List<String>>> it = allStatements.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<Integer, List<String>> entry = it.next();
                        int patchNumber = entry.getKey().intValue();
                        List<String> patchStatements = entry.getValue();
                        try {
                            try (Statement statement = connection.createStatement()) {
                                for (int i = 0; i < patchStatements.size(); i++) {
                                    statement.execute(patchStatements.get(i));
                                }
                                StringBuilder sql = new StringBuilder();
                                sql.append("INSERT INTO ")
                                        .append(DatabaseTableNames.VERSIONS)
                                        .append(" (number) VALUES (")
                                        .append(patchNumber)
                                        .append(")");
                                statement.execute(sql.toString());
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
            String sql = "SELECT number FROM " + DatabaseTableNames.VERSIONS;
            try (ResultSet result = statement.executeQuery(sql)) {
                while (result.next()) {
                    int versionNumber = result.getInt("number");
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

    public static void main(String[] args) {
        DerbyDatabaseActivator activator = new DerbyDatabaseActivator();
        activator.start();
    }
}