package org.esa.snap.product.library.v2.database;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The class contains utility methods to read the SQL statements from the text files.
 *
 * Created by jcoravu on 3/9/2019.
 */
public class DatabaseUtils {

    private DatabaseUtils() {
    }

    public static LinkedHashMap<Integer, List<String>> loadDatabaseStatements(String sourceFolderPath, String databaseFileNamePrefix, int currentDatabaseVersion)
                                                                              throws IOException {

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = sourceFolderPath.getClass().getClassLoader();
        }
        int nextDatabaseVersion = currentDatabaseVersion;
        boolean canContinue = true;
        LinkedHashMap<Integer, List<String>> allStatements = new LinkedHashMap<Integer, List<String>>(1);
        do {
            nextDatabaseVersion++; // increase the database version
            String fileLocation = sourceFolderPath + "/" + databaseFileNamePrefix + Integer.toString(nextDatabaseVersion) + ".sql";
            URL url = loader.getResource(fileLocation);
            if (url == null) {
                canContinue = false;
            } else {
                InputStream inputStream = url.openStream();
                try {
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    try {
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        try {
                            List<String> patchStatements = new ArrayList<String>();
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
                        } finally {
                            try {
                                bufferedReader.close();
                            } catch (Exception exception) {
                                // do nothing
                            }
                        }
                    } finally {
                        try {
                            inputStreamReader.close();
                        } catch (Exception exception) {
                            // do nothing
                        }
                    }
                } finally {
                    try {
                        inputStream.close();
                    } catch (Exception exception) {
                        // do nothing
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

    private static int getTrimmedLength(String input) {
        int start = getLeftTrimmedOffset(input);
        int end = getRightTrimmedOffset(input);
        return end - start;
    }

    private static boolean equalsIgnoreSpacesAndCase(String input1, String input2) {
        if (input1 == input2) {
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

    private static int getLeftTrimmedOffset(String input) {
        int length = input.length();
        int start = 0;
        while (start < length && input.charAt(start) <= ' ') {
            start++;
        }
        return start;
    }

    private static int getRightTrimmedOffset(String input) {
        int length = input.length();
        int end = length;
        while (end > 0 && input.charAt(end - 1) <= ' ') {
            end--;
        }
        return end;
    }
}
