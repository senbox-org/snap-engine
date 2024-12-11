package org.esa.snap.oldImpl.performance.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private final String configFile = "test-config.properties";
    private final Properties properties = new Properties();

    public ConfigLoader() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(this.configFile)) {
            if (input == null) {
                throw new IOException("Configuration file '" + this.configFile + "' not found in classpath.");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new IOException("Failed to load configuration: " + e.getMessage(), e);
        }
    }

    public String get(String key) {
        return properties.getProperty(key);
    }
}
