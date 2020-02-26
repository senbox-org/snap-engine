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

package org.esa.snap.lib.openjpeg.utils;


import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.apache.commons.lang.SystemUtils.*;

/**
 * Utility class to get executables from OpenJpeg module
 *
 * @author Oscar Picas-Puig
 */
public class OpenJpegExecRetriever {

    /**
     * Compute the path to the openjpeg compressor utility
     *
     * @return The path to opj_compress
     */
    public static String getOpjCompress() {
        return findOpenJpegExecPath(OSCategory.getOSCategory().getCompressor());
    }

    /**
     * Compute the path to the openjpeg decompressor utility
     *
     * @return The path to opj_decompress
     */
    public static String getOpjDecompress() {
        return findOpenJpegExecPath(OSCategory.getOSCategory().getDecompressor());
    }

    /**
     * Compute the path to the openjpeg dump utility
     *
     * @return The path to opj_dump
     */
    public static String getOpjDump() {
        return findOpenJpegExecPath(OSCategory.getOSCategory().getDump());
    }

    public static String getOpenJp2() {
        return findOpenJpegExecPath(OSCategory.getOSCategory().getCodec());
    }

    public static Path getOpenJPEGAuxDataPath() {
        Path versionFile = ResourceInstaller.findModuleCodeBasePath(OpenJpegExecRetriever.class).resolve("version").resolve("version.properties");
        Properties versionProp = new Properties();

        try (InputStream inputStream = Files.newInputStream(versionFile)){
            versionProp.load(inputStream);
        } catch (IOException e) {
            SystemUtils.LOG.severe("OpenJPEG configuration error: failed to read " + versionFile.toString());
            return null;
        }

        String version = versionProp.getProperty("project.version");
        if (version == null)
        {
            SystemUtils.LOG.severe("OpenJPEG configuration error: unable to get property project.version from " + versionFile.toString());
            return null;
        }

        return SystemUtils.getAuxDataPath().resolve("openjpeg").resolve(version);
    }

    private static String findOpenJpegExecPath(String endPath) {
        if (endPath == null) {
            return null;
        }

        Path path = getOpenJPEGAuxDataPath().resolve(endPath);
        String pathString = null;
        if (path != null) {
            pathString = path.toString();
        }
        return pathString;
    }

    /* The different OS for which OpenJPEG executables are released */
    private enum OSCategory {
        WIN_32("openjpeg-2.1.0-win32", Paths.get("bin","opj_compress.exe").toString(), Paths.get("bin","opj_decompress.exe").toString(), Paths.get("bin","opj_dump.exe").toString(), Paths.get("bin","libopenjp2.dll").toString()),
        WIN_64("openjpeg-2.1.0-win64", Paths.get("bin","opj_compress.exe").toString(), Paths.get("bin","opj_decompress.exe").toString(), Paths.get("bin","opj_dump.exe").toString(), Paths.get("bin","libopenjp2.dll").toString()),
        LINUX_64("openjpeg-2.1.0-linux64", Paths.get("bin","opj_compress").toString(), Paths.get("bin","opj_decompress").toString(), Paths.get("bin","opj_dump").toString(), Paths.get("bin","libopenjp2.so").toString()),
        MAC_OS_X("openjpeg-2.1.0-macosx", Paths.get("bin","opj_compress").toString(), Paths.get("bin","opj_decompress").toString(), Paths.get("bin","opj_dump").toString(), Paths.get("bin","libopenjp2.dylib").toString()),
        UNSUPPORTED(null, null, null, null, null);


        String directory;
        String compressor;
        String decompressor;
        String dump;
        String codec;

        OSCategory(String directory, String compressor, String decompressor, String dump, String codec) {
            this.directory = directory;
            this.compressor = compressor;
            this.decompressor = decompressor;
            this.dump = dump;
            this.codec = codec;
        }

        String getCompressor() {
            return Paths.get(directory,compressor).toString();
        }

        String getDecompressor() {
            return Paths.get(directory,decompressor).toString();
        }

        String getDump() {
            return Paths.get(directory,dump).toString();
        }

        String getCodec() { return Paths.get(directory,codec).toString(); }

        static OSCategory getOSCategory() {
            OSCategory category;
            if (IS_OS_LINUX) {
                category = OSCategory.LINUX_64;
            } else if (IS_OS_MAC_OSX) {
                category = OSCategory.MAC_OS_X;
            } else if (IS_OS_WINDOWS) {
                String sysArch = System.getProperty("os.arch").toLowerCase();
                if (sysArch.contains("amd64") || sysArch.contains("x86_x64")) {
                    category = OSCategory.WIN_64;
                } else {
                    category = OSCategory.WIN_32;
                }
            } else {
                // we should never be here since we do not release installers for other systems.
                category = OSCategory.UNSUPPORTED;
            }
            return category;
        }
    }

}
