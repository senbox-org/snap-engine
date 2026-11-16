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

package org.esa.snap.dataio.netcdf.util;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

class Sen3ExpAtmosphericConverter {

    public static void main(String[] args) {
        final String sourceDirPath = args[0];
        final Path sourceDir = Path.of(sourceDirPath);

        final String targetDirPath = args[1];
        final Path targetDir = Path.of(targetDirPath);

        final String propertyFilePath = args[2];
        final Path propertyFile = Path.of(propertyFilePath);

        convert(sourceDir, targetDir, propertyFile, new ErrorHandler() {
            @Override
            public void error(Throwable t) {
                System.out.println("Error: " + t.getMessage());
                System.exit(1);
            }

            @Override
            public void warning(Throwable t) {
                System.out.println("Warning: " + t.getMessage());
            }
        });
    }

    private static void convert(Path sourceDir, Path targetDir, Path propertyFile, ErrorHandler errorHandler) {
        final Properties properties = new Properties();
        try {
            try (InputStream inStream = Files.newInputStream(propertyFile)) {
                properties.load(inStream);
            }
        } catch (IOException e) {
            errorHandler.error(e);
        }

        final Collection<Path> sourceFiles = IOHelper.listFiles(sourceDir, path -> path.getFileName().toString().endsWith(".nc"));
        if (sourceFiles == null) {
            return;
        }

        for (final Path sourceFile : sourceFiles) {
            Product product = null;
            try {
                product = ProductIO.readProduct(sourceFile);
                if (product != null) {
                    for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
                        final Band band = product.getBand((String) entry.getKey());
                        if (band != null) {
                            band.setSpectralWavelength(Float.parseFloat((String) entry.getValue()));
                        }
                    }
                    ProductIO.writeProduct(product, targetDir.resolve(sourceFile.getFileName()),
                                           "NetCDF", false);
                }
            } catch (IOException e) {
                errorHandler.warning(e);
            } finally {
                if (product != null) {
                    product.dispose();
                }
            }
        }
    }

    interface ErrorHandler {

        void error(Throwable t);

        void warning(Throwable t);
    }
}
