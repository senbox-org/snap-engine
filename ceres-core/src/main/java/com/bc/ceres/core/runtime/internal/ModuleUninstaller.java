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

package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.core.runtime.ModuleState;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Logger;

/**
 * An uninstaller for modules.
 */
public class ModuleUninstaller {

    private Logger logger;

    public ModuleUninstaller(Logger logger) {
        Assert.notNull(logger, "logger");
        this.logger = logger;
    }

    public void uninstallModule(ModuleImpl module) throws IOException {
        markLocationFileAsUninstalled(module);
        module.setState(ModuleState.UNINSTALLED);
    }

    public void uninstallModules(Path moduleDir, ProgressMonitor pm) {

        DirScanner dirScanner = new DirScanner(moduleDir, false);
        String[] uninstallMarkers = dirScanner.scan(path -> path.getFileName().toString().toLowerCase().endsWith(RuntimeImpl.UNINSTALL_FILE_SUFFIX));

        if (uninstallMarkers.length == 0) {
            return;
        }

        pm.beginTask("Uninstalling modules", uninstallMarkers.length);
        for (String uninstallMarker : uninstallMarkers) {
            String moduleFileName = uninstallMarker.substring(0,
                                                              uninstallMarker.length() - RuntimeImpl.UNINSTALL_FILE_SUFFIX.length());
            Path moduleFile = moduleDir.resolve(moduleFileName);
            boolean deleted = uninstallModuleFile(moduleFile, SubProgressMonitor.create(pm, 1));
            if (deleted) {
                Path uninstallMarkerFile = moduleDir.resolve(uninstallMarker);
                IOHelper.delete(uninstallMarkerFile);
            }
        }
        pm.done();
    }

    private void markLocationFileAsUninstalled(ModuleImpl module) throws IOException {
        URL location = module.getLocation();
        logger.info(MessageFormat.format("Marking module file [{0}] for deinstallation.", location));

        Path locationFile = UrlHelper.urlToFile(location);
        if (locationFile == null) {
            throw new IOException("Location is not a file.");
        }

        Path markerFile = Path.of(locationFile + RuntimeImpl.UNINSTALL_FILE_SUFFIX);
        try (Writer writer = Files.newBufferedWriter(markerFile)) {
            writer.write(location.toString());
        }
    }

    private boolean uninstallModuleFile(Path locationFile, ProgressMonitor pm) {
        logger.info(MessageFormat.format("Uninstalling module [{0}]...", locationFile));
        boolean deleted = true;
        if (Files.exists(locationFile)) {
            if (Files.isDirectory(locationFile)) {
                deleted = uninstallModuleDirectory(locationFile, pm);
            } else {
                deleted = uninstallModuleArchive(locationFile, pm);
            }
        }
        return deleted;
    }

    private static boolean uninstallModuleArchive(Path archiveFile, ProgressMonitor pm) {
        pm.beginTask(MessageFormat.format("Uninstalling {0}", archiveFile.getFileName()), 1);
        try {
            return IOHelper.delete(archiveFile);
        } finally {
            pm.done();
        }
    }

    private boolean uninstallModuleDirectory(Path moduleDir, ProgressMonitor pm) {
        Path installInfoFile = moduleDir.resolve(ModuleInstaller.INSTALL_INFO_XML);

        InstallInfo installInfo;
        try {
            try (Reader reader = Files.newBufferedReader(installInfoFile)) {
                installInfo = InstallInfo.read(reader);
            }
        } catch (FileNotFoundException e) {
            logger.warning(
                    MessageFormat.format("[{0}] not found.", installInfoFile.getFileName()));
            logger.warning(
                    MessageFormat.format("Please remove directory [{0}] manually to get rid of this warning.", moduleDir));
            return false;
        } catch (IOException e) {
            logger.warning(
                    MessageFormat.format("Failed to read [{0}]: {1}", installInfoFile.getFileName(), e.getMessage()));
            return false;
        }

        String[] items = installInfo.getItems();
        pm.beginTask(MessageFormat.format("Uninstalling {0}", moduleDir.getFileName()), items.length);
        try {
            for (int i = items.length - 1; i >= 0; --i) {
                String item = items[i];
                Path installedFile = moduleDir.resolve(item);
                pm.setTaskName(MessageFormat.format("Uninstalling {0}", installedFile.getFileName()));
                if (Files.isRegularFile(installedFile)) {
                    long installTime = installInfo.getDate().getTime();
                    long lastModifiedTime = IOHelper.getLastModifiedTime(installedFile);
                    long oneSecond = 1000L;
                    boolean fileIsOlderOrEqualInstallTime = lastModifiedTime <= installTime + oneSecond;
                    if (fileIsOlderOrEqualInstallTime) {
                        delete(installedFile);
                    } else {
                        logger.warning(MessageFormat.format("Module file component [{0}] has been modified since installation.",
                                                            installedFile));
                    }
                } else if (Files.isDirectory(installedFile)) {
                    delete(installedFile);
                } else {
                    logger.warning(MessageFormat.format("Module file component [{0}] no longer exists.", installedFile));
                }
                pm.worked(1);
            }
            delete(installInfoFile);
            delete(moduleDir);
        } finally {
            pm.done();
        }

        return Files.exists(moduleDir);
    }

    private void delete(Path file) {
        if (IOHelper.delete(file)) {
            logger.info(MessageFormat.format("Deleted module file component [{0}].", file));
        } else {
            logger.warning(MessageFormat.format("Unable to delete module file component [{0}].", file));
        }
    }
}
