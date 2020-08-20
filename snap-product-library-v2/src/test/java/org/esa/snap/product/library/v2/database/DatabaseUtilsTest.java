package org.esa.snap.product.library.v2.database;

import org.junit.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by jcoravu on 20/1/2020.
 */
public class DatabaseUtilsTest {

    public DatabaseUtilsTest() {
    }

    @Test
    public void testLoadDatabaseStatements() throws IOException {
        String sourceFolderPath = DataAccess.DATABASE_DEFINITION_LANGUAGE_SOURCE_FOLDER_PATH;
        String databaseFileNamePrefix = DataAccess.DATABASE_SQL_FILE_NAME_PREFIX;
        LinkedHashMap<Integer, List<String>> allStatements = DatabaseUtils.loadDatabaseStatements(sourceFolderPath, databaseFileNamePrefix, 0);
        assertNotNull(allStatements);
        assertTrue(allStatements.size() > 0);
    }
}
