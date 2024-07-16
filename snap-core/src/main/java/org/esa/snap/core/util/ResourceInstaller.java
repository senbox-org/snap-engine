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
package org.esa.snap.core.util;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.snap.core.util.io.FileUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Installs resources from a given source to a given target.
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class ResourceInstaller {

    private final Set<PosixFilePermission> rwxr_xr_x = PosixFilePermissions.fromString("rwxr-xr-x");

    private final Path sourceBasePath;
    private final Path targetDirPath;

    /**
     * Creates an instance with a given source to a given target.
     *
     * @param sourceDirPath the source directory path
     * @param targetDirPath the target directory
     */
    public ResourceInstaller(Path sourceDirPath, Path targetDirPath) {
        this.sourceBasePath = sourceDirPath;
        this.targetDirPath = targetDirPath;
    }

    /**
     * Installs all resources found, matching the given pattern. Existing resources are left as-is
     * and are not overwritten.
     *
     * @param patternString the search pattern. Specifies the pattern and the syntax for searching for resources.
     *                      The syntax can either be 'glob:' or 'regex:'. If the syntax does not start with one of the syntax
     *                      identifiers 'regex:' is pre-pended.
     * @param pm            progress monitor for indicating progress
     * @see FileSystem#getPathMatcher(String)
     */
    public void install(String patternString, ProgressMonitor pm) throws IOException {
        System.out.println("[HDF-AT debug]: install start");
        if (!patternString.startsWith("glob:") && !patternString.startsWith("regex:")) {
            patternString = "regex:" + patternString;
        }

        pm.beginTask("Installing resources...", 100);
        try {
            Collection<Path> resources = collectResources(patternString);
            System.out.println("[HDF-AT debug]: testActivate resources:");
            for(Path resource:resources){
                System.out.println("[HDF-AT debug]: resource: "+resource);
            }
            pm.worked(20);
            copyResources(resources, new SubProgressMonitor(pm, 80));
        } finally {
            pm.done();
        }
    }




    private void copyResources(Collection<Path> resources, ProgressMonitor pm) throws IOException {
        synchronized (ResourceInstaller.class) {
            pm.beginTask("Copying resources...", resources.size());
            try {
                for (Path resource : resources) {
                    Path relFilePath = sourceBasePath.relativize(resource);
                    String relPathString = relFilePath.toString();
                    Path targetFile = targetDirPath.resolve(relPathString);
                    System.out.println("[HDF-AT debug]: copyResources resource:"+resource);
                    System.out.println("[HDF-AT debug]: copyResources targetFile:"+targetFile);
                    if (mustInstallResource(targetFile, resource)) {
                        System.out.println("[HDF-AT debug]: copyResources in mustInstallResource start");
                        Path parentPath = targetFile.getParent();
                        System.out.println("[HDF-AT debug]: copyResources parentPath:"+parentPath);
                        if (parentPath == null) {
                            throw new IOException("Could not retrieve the parent directory of '" + targetFile.toString() + "'.");
                        }
                        Files.createDirectories(parentPath);
                        Files.copy(resource, targetFile, REPLACE_EXISTING, COPY_ATTRIBUTES);
                        // set executable here since maven resource plugin is broken and drops them
                        if (Files.getFileAttributeView(targetFile, PosixFileAttributeView.class) != null) {
                            Files.setPosixFilePermissions(targetFile, rwxr_xr_x);
                        }
                        System.out.println("[HDF-AT debug]: copyResources in mustInstallResource end");
                    }
                    pm.worked(1);
                }
            } finally {
                pm.done();
            }
        }
    }

    boolean mustInstallResource(Path targetFile, Path resource) throws IOException {
        if (!Files.exists(targetFile)) {
            System.out.println("[HDF-AT debug]: mustInstallResource !Files.exists: "+targetFile);
            return true;
        }
        System.out.println("[HDF-AT debug]: mustInstallResource Files.exists: "+targetFile);
        final Path realTargetFile = targetFile.toRealPath();
        final Path realResource = resource.toRealPath();
        final boolean sizeIsDifferent = Files.size(realTargetFile) != Files.size(realResource) && Files.size(realResource) != 0;
        final FileTime existingFileModifiedTime = Files.getLastModifiedTime(realTargetFile);
        final FileTime newFileModifiedTime = Files.getLastModifiedTime(realResource);
        final boolean newFileIsNewer = existingFileModifiedTime.compareTo(newFileModifiedTime) < 0 && Files.size(realResource) != 0;  // access to attributes of files in jars is restricted
        System.out.println("[HDF-AT debug]: mustInstallResource newFileIsNewer: "+newFileIsNewer);
        System.out.println("[HDF-AT debug]: mustInstallResource sizeIsDifferent: "+newFileIsNewer);
        System.out.println("[HDF-AT debug]: mustInstallResource Files.isRegularFile: "+Files.isRegularFile(resource));
        return (newFileIsNewer || sizeIsDifferent) && Files.isRegularFile(resource);
    }

    private Collection<Path> collectResources(String patternString) throws IOException {
        Collection<Path> resources = new ArrayList<>();
        collectResources(sourceBasePath, resources, patternString);
        return resources;
    }


    private static void collectResources(Path searchPath, Collection<Path> resourcePaths, String patternString) throws IOException {
        if (Files.isDirectory(searchPath)) {
            PathMatcher pathMatcher = searchPath.getFileSystem().getPathMatcher(patternString);
            collectResources(searchPath, resourcePaths, pathMatcher);
        } else {
            resourcePaths.add(searchPath);
        }

    }

    private static void collectResources(Path searchPath, Collection<Path> resourcePaths, PathMatcher pathMatcher) throws IOException {
        Stream<Path> files = Files.list(searchPath);
        files.forEach(path -> {
            if (pathMatcher.matches(path)) {
                resourcePaths.add(path);
            }
            if (Files.isDirectory(path)) {
                try {
                    collectResources(path, resourcePaths, pathMatcher);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        });

    }

    public static Path findModuleCodeBasePath(Class clazz) {
        try {
            URI uri = clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
            return FileUtils.getPathFromURI(FileUtils.ensureJarURI(uri));
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Failed to detect the module's code base path", e);
        }
    }

}
