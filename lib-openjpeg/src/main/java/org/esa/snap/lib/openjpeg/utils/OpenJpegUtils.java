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

import org.esa.snap.core.util.BucketMap;
import org.esa.snap.lib.openjpeg.dataio.Utils;
import org.esa.snap.lib.openjpeg.jp2.TileLayout;
import org.esa.snap.lib.openjpeg.header.CODMarkerSegment;
import org.esa.snap.lib.openjpeg.header.ContiguousCodestreamBox;
import org.esa.snap.lib.openjpeg.header.JP2FileReader;
import org.esa.snap.lib.openjpeg.header.SIZMarkerSegment;
import org.esa.snap.core.datamodel.ProductData;

import java.awt.image.DataBuffer;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class to work with openjpeg:
 * - retrieve TileLayout information from a granule
 * - execute openjpeg
 *
 * @author Oscar Picas-Puig
 */
public class OpenJpegUtils {

    public static final BucketMap<Integer, Integer> DATA_TYPE_MAP = new BucketMap<Integer, Integer>() {{
        put(1, 8, DataBuffer.TYPE_BYTE);
        put(9, 15, DataBuffer.TYPE_USHORT);
        put(16, DataBuffer.TYPE_SHORT);
        put(17, 32, DataBuffer.TYPE_FLOAT);
    }};

    public static final BucketMap<Integer, Integer> PRECISION_TYPE_MAP = new BucketMap<Integer, Integer>() {{
        put(1, 8, ProductData.TYPE_UINT8);
        put(9, 15, ProductData.TYPE_UINT16);
        put(16, ProductData.TYPE_INT16);
        put(17, 32, ProductData.TYPE_FLOAT32);
    }};

    public static boolean canReadJP2FileHeaderWithOpenJPEG() {
        String value = System.getProperty("jp2.read.header.with.open.jpeg");
        return Boolean.parseBoolean(value);
    }

    /**
     * Get the tile layout with opj_dump
     *
     * @param opjdumpPath path to opj_dump
     * @param jp2FilePath the path to the jpeg file
     * @return the tile layout for the openjpeg file
     * @throws IOException
     * @throws InterruptedException
     */
    public static TileLayout getTileLayoutWithOpenJPEG(String opjdumpPath, Path jp2FilePath) throws IOException, InterruptedException {
        if (opjdumpPath == null) {
            throw new IllegalStateException("Cannot retrieve tile layout, opj_dump cannot be found");
        }

        String pathToImageFile = jp2FilePath.toAbsolutePath().toString();
        if (org.apache.commons.lang.SystemUtils.IS_OS_WINDOWS) {
            pathToImageFile = Utils.GetIterativeShortPathNameW(pathToImageFile);
        }

        ProcessBuilder builder = new ProcessBuilder(opjdumpPath, "-i", pathToImageFile);
        builder.redirectErrorStream(true);

        CommandOutput exit = OpenJpegUtils.runProcess(builder);

        if (exit.getErrorCode() != 0) {
            StringBuilder sbu = new StringBuilder();
            for (String fragment : builder.command()) {
                sbu.append(fragment);
                sbu.append(' ');
            }
            throw new IOException(String.format("Command [%s] failed with error code [%d], stdoutput [%s] and stderror [%s]", sbu.toString(), exit.getErrorCode(), exit.getTextOutput(), exit.getErrorOutput()));
        } else {
            TileLayout tileLayout = OpenJpegUtils.parseOpjDump(exit.getTextOutput());
            if (tileLayout.numResolutions == 0) {
                return null;
            }
            return tileLayout;
        }
    }

    public static TileLayout getTileLayoutWithInputStream(Path jp2File, int bufferSize, boolean canSetFilePosition) throws IOException {
        JP2FileReader fileFormatReader = new JP2FileReader();
        fileFormatReader.readFileFormat(jp2File, bufferSize, canSetFilePosition);

        ContiguousCodestreamBox contiguousCodestreamBox = fileFormatReader.getHeaderDecoder();
        SIZMarkerSegment sizMarkerSegment = contiguousCodestreamBox.getSiz();
        CODMarkerSegment codMarkerSegment = contiguousCodestreamBox.getCod();

        int imageWidth = sizMarkerSegment.getImageWidth();
        int imageHeight = sizMarkerSegment.getImageHeight();
        int tileWidth = sizMarkerSegment.getNominalTileWidth();
        int tileHeight = sizMarkerSegment.getNominalTileHeight();
        int xTiles = sizMarkerSegment.computeNumTilesX();
        int yTiles = sizMarkerSegment.computeNumTilesY();
        int resolutions = codMarkerSegment.getNumberOfLevels() + 1;
        int precision = sizMarkerSegment.getComponentOriginBitDepthAt(0);

        return new TileLayout(imageWidth, imageHeight, tileWidth, tileHeight, xTiles, yTiles, resolutions, DATA_TYPE_MAP.get(precision));
    }

    public static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static CommandOutput runProcess(ProcessBuilder builder) throws InterruptedException, IOException {
        return runProcess(builder, null);
    }

    public static CommandOutput runProcess(ProcessBuilder builder, String newLineSeparator) throws InterruptedException, IOException {
        builder.environment().putAll(System.getenv());

        StringBuilder output = new StringBuilder();
        Process process = builder.start();
        InputStream inputStream = process.getInputStream(); // get the process output
        try {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            try {
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                try {
                    boolean isStopped = false;
                    while (!isStopped) {
                        if (process.isAlive()) {
                            Thread.yield(); // yield the control to other threads
                        } else {
                            isStopped = true;
                        }
                        while (bufferedReader.ready()) {
                            String line = bufferedReader.readLine();
                            if (line != null && !line.isEmpty()) {
                                output.append(line);
                                if (newLineSeparator != null) {
                                    output.append(newLineSeparator);
                                }
                            }
                        }
                    }
                } finally {
                    closeStream(bufferedReader);
                }
            } finally {
                closeStream(inputStreamReader);
            }
        } finally {
            closeStream(inputStream);
        }

        int exitCode = process.exitValue();

        InputStream errorInputStream = process.getErrorStream();
        try {
            String errorOutput = OpenJpegUtils.convertStreamToString(errorInputStream);
            return new CommandOutput(exitCode, output.toString(), errorOutput);
        } finally {
            closeStream(errorInputStream);
        }
    }

    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                // nothing to do
            }
        }
    }

    /**
     * Parse the {@code String} returned by opj_dump command
     *
     * @param content the tring returned by opj_dump
     * @return the TileLayout extracted from this string
     */
    public static TileLayout parseOpjDump(String content) {
        List<String> splittedContent = new ArrayList<>();
        Collections.addAll(splittedContent, content.split(content.contains("\n") ? "\n" : "\t"));
        return parseOpjDump(splittedContent);
    }

    /**
     * Parse opj_dump result lines
     *
     * @param content the lines of text returned from opj_dump command
     * @return the TileLayout extracted from the lines
     */
    public static TileLayout parseOpjDump(List<String> content) {
        int imageWidth = 0;
        int imageHeight = 0;
        int tileWidth = 0;
        int tileHeight = 0;
        int xTiles = 0;
        int yTiles = 0;
        int resolutions = 0;
        int precision = 0;

        for (String line : content) {
            if (line.contains("x1") && line.contains("y1")) {
                String[] segments = line.trim().split(",");
                imageWidth = Integer.parseInt(segments[0].split("\\=")[1]);
                imageHeight = Integer.parseInt(segments[1].split("\\=")[1]);
            }
            if (line.contains("tdx") && line.contains("tdy")) {
                String[] segments = line.trim().split(",");
                tileWidth = Integer.parseInt(segments[0].split("\\=")[1]);
                tileHeight = Integer.parseInt(segments[1].split("\\=")[1]);
            }
            if (line.contains("tw") && line.contains("th")) {
                String[] segments = line.trim().split(",");
                xTiles = Integer.parseInt(segments[0].split("\\=")[1]);
                yTiles = Integer.parseInt(segments[1].split("\\=")[1]);
            }
            if (line.contains("numresolutions")) {
                resolutions = Integer.parseInt(line.trim().split("\\=")[1]);
            }
            if (line.contains("prec=")) {
                precision = Integer.parseInt(line.trim().split("\\=")[1]);
            }
        }

        return new TileLayout(imageWidth, imageHeight, tileWidth, tileHeight, xTiles, yTiles, resolutions, DATA_TYPE_MAP.get(precision));
    }

    public static boolean validateOpenJpegExecutables(String opjdumpPath, String opjdecompPath) {

        ProcessBuilder builder = new ProcessBuilder(opjdumpPath, "-h");
        builder.redirectErrorStream(true);

        CommandOutput exit;
        try {
            exit = OpenJpegUtils.runProcess(builder);
        } catch (Exception e) {
            return false;
        }

        if (exit.getErrorCode() != 1) {
            return false;
        }

        builder = new ProcessBuilder(opjdecompPath, "-h");
        builder.redirectErrorStream(true);

        try {
            exit = OpenJpegUtils.runProcess(builder);
        } catch (Exception e) {
            return false;
        }

        if (exit.getErrorCode() != 1) {
            return false;
        }

        return true;
    }
}
