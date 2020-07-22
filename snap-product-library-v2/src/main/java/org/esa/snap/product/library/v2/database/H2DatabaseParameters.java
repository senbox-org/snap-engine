package org.esa.snap.product.library.v2.database;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * The parameters to connect to a H2 local database.
 *
 * Created by jcoravu on 20/1/2020.
 */
public class H2DatabaseParameters {

    private final Path parentFolderPath;
    private final Properties properties;

    public H2DatabaseParameters(Path databaseParentFolderPath) {
        if (databaseParentFolderPath == null) {
            throw new NullPointerException("The database parent folder path is null.");
        }
        this.parentFolderPath = databaseParentFolderPath;

        this.properties = new Properties();
        this.properties.put("user", "snap");
        this.properties.put("password", "snap");
    }

    public Properties getProperties() {
        return properties;
    }

    public String getUrl() {
        String databaseName = "products";
        return "jdbc:h2:" + this.parentFolderPath.resolve(databaseName).toString();
    }

    public Path getParentFolderPath() {
        return parentFolderPath;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(getUrl(), getProperties());
    }
}
