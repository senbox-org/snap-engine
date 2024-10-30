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

import java.nio.file.Path;
import java.util.Properties;

/**
 * The parameters to connect to a H2 local database.
 * <p>
 * Created by jcoravu on 20/1/2020.
 */
public class H2DatabaseParameters {

    private final Path parentFolderPath;
    private final Properties properties;

    public H2DatabaseParameters(final Path databaseParentFolderPath) {
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

    public String getUrl(final String databaseName) {
        return "jdbc:h2:" + this.parentFolderPath.resolve(databaseName) + ";AUTO_SERVER=TRUE";
    }

    public Path getParentFolderPath() {
        return parentFolderPath;
    }
}
