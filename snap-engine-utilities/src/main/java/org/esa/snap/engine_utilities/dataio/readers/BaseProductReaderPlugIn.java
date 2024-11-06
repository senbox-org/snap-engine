/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
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

package org.esa.snap.engine_utilities.dataio.readers;

import eu.esa.snap.core.lib.FileHelper;
import org.esa.snap.engine_utilities.dataio.VirtualDirEx;
import org.esa.snap.core.dataio.AbstractProductReader;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Base class for product reader plugins which follow the logic of checking consistency
 * of products using naming consistency rules.
 *
 * @see org.esa.snap.engine_utilities.dataio.readers.ProductContentEnforcer
 * @author Cosmin Cara
 */
public abstract class BaseProductReaderPlugIn implements ProductReaderPlugIn {

    private static final Logger logger = Logger.getLogger(BaseProductReaderPlugIn.class.getName());

    private static Map<Object, String[]> CACHED_FILES = new WeakHashMap<>();

    protected final ProductContentEnforcer enforcer;
    private final Path colorPaletteFilePath;
    protected int folderDepth;

    protected BaseProductReaderPlugIn(String colorPaletteFilePathFromSources) {
        this.folderDepth = 1;
        String[] patternList = getMinimalPatternList();
        for (String pattern : patternList) {
            this.folderDepth = Math.max(this.folderDepth, pattern.split("\\[/").length - 1);
        }
        this.enforcer = ProductContentEnforcer.create(patternList, getExclusionPatternList());
        registerRGBProfile();

        if (StringUtils.isNullOrEmpty(colorPaletteFilePathFromSources)) {
            this.colorPaletteFilePath = null;
        } else {
            int index = colorPaletteFilePathFromSources.lastIndexOf("/");
            if (index >= 0) {
                index++;
            } else {
                index = 0;
            }
            String colorPaletteFileName = colorPaletteFilePathFromSources.substring(index);
            this.colorPaletteFilePath = SystemUtils.getAuxDataPath().resolve("color_palettes").resolve(colorPaletteFileName);
            if (!Files.exists(this.colorPaletteFilePath)) {
                URL colorPaletteFileURLFromSources = getClass().getClassLoader().getResource(colorPaletteFilePathFromSources);
                if (colorPaletteFileURLFromSources == null) {
                    logger.log(Level.SEVERE, "The reader color palette file '" + colorPaletteFilePathFromSources + "' does not exist in the sources.");
                } else {
                    try {
                        FileHelper.copyFile(colorPaletteFileURLFromSources, this.colorPaletteFilePath);
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, String.format("Unable to copy the reader color palette '%s' from the sources.", colorPaletteFilePathFromSources), ex);
                    }
                }
            }
        }
    }

    public final Path getColorPaletteFilePath() {
        return this.colorPaletteFilePath;
    }

    public static Path convertInputToPath(Object input) {
        return AbstractProductReader.convertInputToPath(input);
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        Path inputPath = convertInputToPath(input);
        VirtualDirEx virtualDir;
        try {
            virtualDir = VirtualDirEx.build(inputPath, false, true);
        } catch (Throwable e) { //getDecodeQualification should not throw any exception, so use Throwable instead of IOException
            return DecodeQualification.UNABLE;
        }
        DecodeQualification returnValue = DecodeQualification.UNABLE;
        Pattern[] patternList = this.enforcer.getMinimalFilePatternList();
        String[] filteredFiles = null;
        if (virtualDir.isCompressed()) {
            // the selected file is an archive
            String[] availableFiles = CACHED_FILES.get(input);
            if (availableFiles == null) {
                // list all the files without filters and apply the filters later
                availableFiles = virtualDir.listAll();
                if (availableFiles == null) {
                    //throw new NullPointerException("The files array is null."); //getDecodeQualification should not throw any exception
                    return DecodeQualification.UNABLE;
                }
                CACHED_FILES.put(input, availableFiles);
            }
            // apply the reader plugin filters
            List<String> filteredFileNames = new ArrayList<String>();
            for (int i=0; i<availableFiles.length; i++) {
                if (VirtualDirEx.matchFilters(availableFiles[i], patternList)) {
                    filteredFileNames.add(availableFiles[i]);
                }
            }
            filteredFiles = new String[filteredFileNames.size()];
            filteredFileNames.toArray(filteredFiles);
        } else if (Files.isRegularFile(inputPath)) {
            boolean matches = Arrays.stream(patternList).anyMatch(p -> p.matcher(inputPath.getFileName().toString()).matches());
            if (matches) {
                filteredFiles = virtualDir.listAll(patternList);
            }
        }
        if (filteredFiles != null && filteredFiles.length >= patternList.length && this.enforcer.isConsistent(filteredFiles)) {
            returnValue = DecodeQualification.INTENDED;
        }
        return returnValue;
    }

    @Override
    public abstract Class[] getInputTypes();

    @Override
    public abstract ProductReader createReaderInstance();

    @Override
    public abstract String[] getFormatNames();

    @Override
    public abstract String[] getDefaultFileExtensions();

    @Override
    public abstract String getDescription(Locale locale);

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new BaseProductFileFilter(this, folderDepth);
    }

    /**
     * Returns the minimal list of file patterns of a product.
     * @return  The list of regular expressions.
     */
    protected abstract String[] getMinimalPatternList();

    /**
     * Returns the exclusion list (i.e. anti-patterns) of a product.
     * @return  The list of regular expressions.
     */
    protected abstract String[] getExclusionPatternList();

    /**
     * Registers a RGB profile for the reader plugin.
     */
    protected abstract void registerRGBProfile();

    /**
     * Returns the list of files in a folder, up to the given depth of the folder,
     * using NIO API.
     *
     * @param parent    The parent folder
     * @param depth     The depth to look for files
     * @return The list of files
     * @throws IOException
     */
    static List<String> listFiles(File parent, int depth) throws IOException {
        if (parent == null)
            return null;
        List<String> files = new ArrayList<>();
        Files.walkFileTree(Paths.get(parent.getAbsolutePath()),
                EnumSet.noneOf(FileVisitOption.class),
                depth,
                new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        files.add(file.toFile().getAbsolutePath().replace(parent.getAbsolutePath(), "").substring(1));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        return FileVisitResult.CONTINUE;
                    }
                });
        return files;
    }

    /**
     * Default implementation for a file filter using product naming rules.
     */
    private class BaseProductFileFilter extends SnapFileFilter {

        private final Map<File, Boolean> processed;
        private final int depth;

        public BaseProductFileFilter(BaseProductReaderPlugIn plugIn, int folderDepth) {
            super(plugIn.getFormatNames()[0], plugIn.getDefaultFileExtensions(), plugIn.getDescription(Locale.getDefault()));

            this.processed = new HashMap<>();
            this.depth = folderDepth;
        }

        @Override
        public boolean accept(File file) {
            boolean shouldAccept = super.accept(file);
            if (shouldAccept && file.isFile() && !VirtualDirEx.isPackedFile(file.toPath())) {
                File folder = file.getParentFile();
                if (!processed.containsKey(folder)) {
                    try {
                        List<String> files = listFiles(folder, depth);
                        shouldAccept = enforcer.isConsistent(files);
                        processed.put(folder, shouldAccept);
                    } catch (IOException e) {
                        Logger.getLogger(BaseProductFileFilter.class.getName()).warning(e.getMessage());
                    }
                } else {
                    shouldAccept = processed.get(folder);
                }
            }
            return shouldAccept;
        }
    }
}
