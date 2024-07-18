/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.stac.database;

import org.esa.snap.core.util.SystemUtils;
//import org.h2gis.functions.factory.H2GISFunctions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractDatabase implements Database {

    private static final String DATABASE_DEFINITION_LANGUAGE_SOURCE_FOLDER_PATH = "org/esa/snap/stac/database";
    private static final String DATABASE_SQL_FILE_NAME_PREFIX = "h2gis-database-script-";

    private H2DatabaseParameters dbParams;
    protected Connection connection;

    public void initialize() throws Exception {
        // load H2 driver
        Class.forName("org.h2.Driver");
        dbParams = new H2DatabaseParameters(SystemUtils.getApplicationDataDir(true).toPath().resolve(getDatabaseName()));
        getConnection();
        upgradeDatabase();
    }

    protected abstract String getDatabaseName();

    protected Connection getConnection() throws SQLException {
        if (connection == null) {
            connection = DriverManager.getConnection(dbParams.getUrl(getDatabaseName()), dbParams.getProperties());
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        }
        return connection;
    }

    private void upgradeDatabase() throws SQLException, IOException {
        int currentDatabaseVersion = 0;
        // check if the 'version' table exists into the database
        if (existsTable("VERSIONS")) {
            // the 'version' table exists and load the current database version number
            currentDatabaseVersion = getCurrentDBVersion();
        }

        final LinkedHashMap<Integer, List<String>> allStatements =
                loadDatabaseStatements(DATABASE_DEFINITION_LANGUAGE_SOURCE_FOLDER_PATH,
                        DATABASE_SQL_FILE_NAME_PREFIX, currentDatabaseVersion);
        if (allStatements.size() > 0) {
            if (!connection.getAutoCommit()) {
                throw new IllegalStateException("The connection has an opened transaction.");
            }
            try {
                if (currentDatabaseVersion == 0) {
                    //H2GISFunctions.load(connection);
                }
                connection.setAutoCommit(false);
                for (Map.Entry<Integer, List<String>> entry : allStatements.entrySet()) {
                    final Statement statement = connection.createStatement();
                    for (String sql : entry.getValue()) {
                        statement.addBatch(sql);
                    }
                    statement.addBatch(String.format("INSERT INTO versions (id) VALUES (%d)", entry.getKey()));
                    statement.executeBatch();
                }
                connection.commit();
            } catch (Exception e) {
                // rollback the statements from the transaction
                connection.rollback();
            }
        }
    }

    private int getCurrentDBVersion() throws SQLException {
        int currentDatabaseVersion = 0;
        final PreparedStatement statement = connection.prepareStatement("SELECT MAX(id) FROM versions");
        try (ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                currentDatabaseVersion = result.getInt(1);
            }
        }
        return currentDatabaseVersion;
    }

    private boolean existsTable(final String tableName) throws SQLException {
        DatabaseMetaData databaseMetaData = connection.getMetaData();
        ResultSet result = databaseMetaData.getTables(null, null, tableName.toUpperCase(), null);
        return result.next();
    }

    private static LinkedHashMap<Integer, List<String>> loadDatabaseStatements(String sourceFolderPath,
                                                                              String databaseFileNamePrefix,
                                                                              int currentDatabaseVersion)
            throws IOException {

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = sourceFolderPath.getClass().getClassLoader();
        }
        int nextDatabaseVersion = currentDatabaseVersion;
        boolean canContinue = true;
        final LinkedHashMap<Integer, List<String>> allStatements = new LinkedHashMap<>();
        do {
            nextDatabaseVersion++; // increase the database version
            String fileLocation = sourceFolderPath + "/" + databaseFileNamePrefix + nextDatabaseVersion + ".sql";
            URL url = loader.getResource(fileLocation);
            if (url == null) {
                canContinue = false;
            } else {
                try (InputStream inputStream = url.openStream()) {
                    try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
                        try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                            final List<String> patchStatements = new ArrayList<>();
                            StringBuilder currentStatement = new StringBuilder();
                            String line = bufferedReader.readLine();
                            while (line != null) {
                                if (getTrimmedLength(line) > 0) {
                                    if (!startsWithIgnoreSpacesAndCase(line, "//") && !startsWithIgnoreSpacesAndCase(line, "--")) {
                                        if (currentStatement.length() > 0) {
                                            currentStatement.append("\n");
                                        }
                                        if (equalsIgnoreSpacesAndCase(line, ";")) {
                                            patchStatements.add(currentStatement.toString());
                                            currentStatement = new StringBuilder();
                                        } else {
                                            currentStatement.append(line);
                                        }
                                    }
                                }
                                line = bufferedReader.readLine(); // read the next line
                            }
                            allStatements.put(nextDatabaseVersion, patchStatements);
                        }
                    }
                }
            }
        } while (canContinue);

        return allStatements;
    }

    private static boolean startsWithIgnoreSpacesAndCase(String value, String valueToCheck) {
        int length = value.length();
        int start = 0;
        while (start < length && value.charAt(start) <= ' ') {
            start++;
        }
        int size = length - start;
        if (size >= valueToCheck.length()) {
            return value.regionMatches(true, start, valueToCheck, 0, valueToCheck.length());
        }
        return false;
    }

    private static int getTrimmedLength(final String input) {
        int start = getLeftTrimmedOffset(input);
        int end = getRightTrimmedOffset(input);
        return end - start;
    }

    private static boolean equalsIgnoreSpacesAndCase(final String input1, final String input2) {
        if (input1 != null && input1.equals(input2)) {
            return true;
        }
        if (input1 != null && input2 != null) {
            int startInput1 = getLeftTrimmedOffset(input1);
            int endInput1 = getRightTrimmedOffset(input1);
            int size1 = endInput1 - startInput1;

            int startInput2 = getLeftTrimmedOffset(input2);
            int endInput2 = getRightTrimmedOffset(input2);
            int size2 = endInput2 - startInput2;
            if (size1 == size2) {
                return input1.regionMatches(true, startInput1, input2, startInput2, size1);
            }
        }
        return false;
    }

    private static int getLeftTrimmedOffset(final String input) {
        int length = input.length();
        int start = 0;
        while (start < length && input.charAt(start) <= ' ') {
            start++;
        }
        return start;
    }

    private static int getRightTrimmedOffset(final String input) {
        int end = input.length();
        while (end > 0 && input.charAt(end - 1) <= ' ') {
            end--;
        }
        return end;
    }
}
