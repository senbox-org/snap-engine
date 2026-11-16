/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataop.dem;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.util.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>AbstractElevationModelDescriptor</code> implements general behaviour
 * common to all {@link ElevationModelDescriptor}
 *
 * @author Luis Veci
 * @author Norman Fomferra
 * @author Marco Peters
 */
public abstract class AbstractElevationModelDescriptor implements ElevationModelDescriptor {

    private final static String PROPERTIES_FILE_NAME = "dem.properties";
    private final static String INSTALL_DIR_PROPERTY_NAME = "dem.installDir";
    private final static Logger LOG = Logger.getLogger(AbstractElevationModelDescriptor.class.getName());

    private final Path demInstallDir;
    private final Properties properties;

    protected AbstractElevationModelDescriptor() {
        Path demPropertiesDir = SystemUtils.getAuxDataPath().resolve("dem").resolve(getName());
        if (!Files.exists(demPropertiesDir)) {
            IOHelper.createDirectories(demPropertiesDir);
        }
        properties = loadProperties(demPropertiesDir.resolve(PROPERTIES_FILE_NAME));
        demInstallDir = getDemInstallDir(demPropertiesDir);
    }

    public Path getDemInstallDir() {
        return demInstallDir;
    }

    private String getProperty(String propertyName) {
        return properties.getProperty(propertyName);
    }

    /**
     * Loads DEM properties from the specified file.
     */
    private Properties loadProperties(Path propertiesFile) {
        final Properties properties = new Properties();
        if (Files.exists(propertiesFile)) {
            try (InputStream stream = Files.newInputStream(propertiesFile)) {
                properties.load(stream);
            } catch (IOException ioe) {
                LOG.log(Level.WARNING, String.format("Could not load properties from '%s'", propertiesFile), ioe);
            }
        }
        return properties;
    }

    private Path getDemInstallDir(Path defaultDirectory) {
        final String installDirPath = getProperty(INSTALL_DIR_PROPERTY_NAME);
        if (installDirPath != null && installDirPath.length() > 0) {
            final Path installDir = Path.of(installDirPath);
            if (Files.exists(installDir)) {
                return installDir;
            }
        }
        return defaultDirectory;
    }

}
