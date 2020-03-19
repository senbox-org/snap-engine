package org.esa.snap.remote.products.repository.donwload;

import ro.cs.tao.configuration.ConfigurationProvider;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Created by jcoravu on 27/1/2020.
 */
class RemoteConfigurationProvider implements ConfigurationProvider {

    RemoteConfigurationProvider() {
    }

    @Override
    public Path getApplicationHome() {
        return null;
    }

    @Override
    public String getValue(String s) {
        return null;
    }

    @Override
    public String getValue(String s, String s1) {
        return null;
    }

    @Override
    public boolean getBooleanValue(String s) {
        return false;
    }

    @Override
    public Map<String, String> getValues(String s) {
        return null;
    }

    @Override
    public Map<String, String> getAll() {
        return null;
    }

    @Override
    public void setValue(String s, String s1) {

    }

    @Override
    public void putAll(Properties properties) {

    }

    @Override
    public Path getScriptsFolder() {
        return null;
    }

    @Override
    public void setScriptsFolder(Path path) {

    }

    @Override
    public Path getConfigurationFolder() {
        return null;
    }

    @Override
    public void setConfigurationFolder(Path path) {

    }
}
