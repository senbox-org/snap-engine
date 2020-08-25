package org.esa.snap.product.library.v2.database;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by jcoravu on 20/1/2020.
 */
public class H2DatabaseAccessorTest {

    public H2DatabaseAccessorTest() {
    }

    @Test
    public void testUpgradeDatabase() throws SQLException, IOException, ClassNotFoundException {
        // load H2 driver
        Class.forName("org.h2.Driver");
        DataAccess.setDbParams(new MemoryH2DatabaseParameters());
        DataAccess.upgradeDatabase();
    }

    private static class MemoryH2DatabaseParameters extends  H2DatabaseParameters {

        public MemoryH2DatabaseParameters() {
            super(Paths.get("."));
        }

        @Override
        public String getUrl() {
            String databaseName = "products";
            return "jdbc:h2:mem:" + databaseName;
        }

        @Override
        public Path getParentFolderPath() {
            throw new UnsupportedOperationException("The method is not implemented for in memory database.");
        }
    }
}
