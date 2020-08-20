package org.esa.snap.product.library.v2.database;

import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;

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
        DataAccess.upgradeDatabase();
    }
}
