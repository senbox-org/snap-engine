/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
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

package org.esa.snap.lib.openjpeg.activator;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.lib.openjpeg.utils.OpenJpegExecRetriever;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Activator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.apache.commons.lang.SystemUtils.IS_OS_UNIX;

/**
 * Activator class for deploying OpenJPEG binaries to the aux data dir
 *
 * @author Julien Malik
 */
public class OpenJPEGActivator implements Activator {

    public OpenJPEGActivator() {
    }

    @Override
    public void start() {
        Path sourceDirPath = ResourceInstaller.findModuleCodeBasePath(getClass()).resolve("auxdata/openjpeg");
        Path auxdataDirectory = OpenJpegExecRetriever.getOpenJPEGAuxDataPath();
        if (auxdataDirectory == null) {
            SystemUtils.LOG.severe("OpenJPEG configuration error: failed to retrieve auxdata path");
            return;
        }
        final ResourceInstaller resourceInstaller = new ResourceInstaller(sourceDirPath, auxdataDirectory);

        try {
            resourceInstaller.install(".*", ProgressMonitor.NULL);
            fixUpPermissions(auxdataDirectory);
        } catch (IOException e) {
            SystemUtils.LOG.severe("OpenJPEG configuration error: failed to create " + auxdataDirectory);
            return;
        }
    }

    @Override
    public void stop() {
        // Purposely no-op
    }

    private static void fixUpPermissions(Path destPath) throws IOException {
        Stream<Path> files = Files.list(destPath);
        files.forEach(path -> {
            if (Files.isDirectory(path)) {
                try {
                    fixUpPermissions(path);
                } catch (IOException e) {
                    SystemUtils.LOG.severe("OpenJPEG configuration error: failed to fix permissions on " + path);
                }
            }
            else {
                setExecutablePermissions(path);
            }
        });
    }

    private static void setExecutablePermissions(Path executablePathName) {
        if (IS_OS_UNIX) {
            Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE));
            try {
                Files.setPosixFilePermissions(executablePathName, permissions);
            } catch (IOException e) {
                // can't set the permissions for this file, eg. the file was installed as root
                // send a warning message, user will have to do that by hand.
                SystemUtils.LOG.severe("Can't set execution permissions for executable " + executablePathName.toString() +
                        ". If required, please ask an authorised user to make the file executable.");
            }
        }
    }
}
