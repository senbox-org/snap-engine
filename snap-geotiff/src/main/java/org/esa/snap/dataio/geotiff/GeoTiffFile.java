package org.esa.snap.dataio.geotiff;

import com.bc.ceres.util.CleanUpState;
import com.bc.ceres.util.CleanerRegistry;
import eu.esa.snap.core.lib.FileHelper;
import eu.esa.snap.core.lib.NotRegularFileException;
import org.esa.snap.engine_utilities.file.AbstractFile;
import org.esa.snap.engine_utilities.util.FindChildFileVisitor;
import org.esa.snap.engine_utilities.util.ZipFileSystemBuilder;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 4/8/2020.
 */
public class GeoTiffFile implements Closeable {

    private static final Logger logger = Logger.getLogger(GeoTiffFile.class.getName());

    private final Path imageParentPath;
    private final String imageRelativeFilePath;
    private final boolean copyFileFromZipArchiveOnLocalDisk;
    private final Path localTempFolder;

    private final GeoTiffFileState state;

    public GeoTiffFile(Path imageParentPath, String imageRelativeFilePath, boolean copyFileFromZipArchiveOnLocalDisk, Path localTempFolder) {
        if (imageParentPath == null) {
            throw new NullPointerException("The image path is null.");
        }
        if (localTempFolder == null) {
            throw new NullPointerException("The local temp folder is null.");
        }
        this.imageParentPath = imageParentPath;
        this.localTempFolder = localTempFolder;
        this.imageRelativeFilePath = imageRelativeFilePath; // the relative path may be null when the parent path is a file
        this.copyFileFromZipArchiveOnLocalDisk = copyFileFromZipArchiveOnLocalDisk;

        this.state = new GeoTiffFileState();
        CleanerRegistry.getInstance().register(this, state);
    }


    @Override
    public final void close() throws IOException {
        cleanup();
    }

    protected void cleanup() throws IOException {
        CleanerRegistry.getInstance().cleanup(this);
    }

    public GeoTiffImageReader buildImageReader() throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {
        GeoTiffImageReader geoTiffImageReader = processLocalCopiedFile();
        if (geoTiffImageReader != null) {
            return geoTiffImageReader;
        }
        if (Files.exists(this.imageParentPath)) {
            // the product path exists
            if (Files.isDirectory(this.imageParentPath)) {
                // the product path represents a folder
                return processImageFolder();
            } else if (Files.isRegularFile(this.imageParentPath)) {
                // the product path represents a file
                if (this.imageParentPath.getFileName().toString().toLowerCase().endsWith(GeoTiffProductReaderPlugIn.ZIP_FILE_EXTENSION)) {
                    // the path is an archive
                    return processImageZipArchive();
                } else {
                    if (this.imageRelativeFilePath != null) {
                        throw new IllegalStateException("The relative file path '"+this.imageRelativeFilePath+"' must be null.");
                    }
                    return new GeoTiffImageReader(this.imageParentPath.toFile());
                }
            } else {
                // the product path does not represent a folder or a file
                throw new NotRegularFileException(this.imageParentPath.toString());
            }
        } else {
            // the product path does not exist
            throw new FileNotFoundException("The product path '"+this.imageParentPath+"' does not exist.");
        }
    }

    private GeoTiffImageReader processImageFolder() throws IOException {
        if (this.imageRelativeFilePath == null) {
            throw new NullPointerException("The relative file path is null.");
        }
        Path child = this.imageParentPath.resolve(this.imageRelativeFilePath);
        if (Files.exists(child)) {
            if (Files.isRegularFile(child)) {
                return new GeoTiffImageReader(child.toFile());
            } else {
                throw new NotRegularFileException("The product folder '"+this.imageParentPath.toString()+"' does not contain the file '" + this.imageRelativeFilePath + "'.");
            }
        } else {
            throw new FileNotFoundException("The product folder '"+this.imageParentPath.toString()+"' does not contain the path '" + this.imageRelativeFilePath + "'.");
        }
    }

    private GeoTiffImageReader processImageZipArchive() throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {
        // the path is an archive
        if (this.imageRelativeFilePath == null) {
            throw new NullPointerException("The relative file path is null.");
        }
        boolean success = false;
        FileSystem fileSystem = null;
        try {
            fileSystem = ZipFileSystemBuilder.newZipFileSystem(this.imageParentPath);
            Iterator<Path> it = fileSystem.getRootDirectories().iterator();
            while (it.hasNext()) {
                Path zipArchiveRoot = it.next();
                Path entryPathToFind = ZipFileSystemBuilder.buildZipEntryPath(zipArchiveRoot, this.imageRelativeFilePath);
                FindChildFileVisitor findChildFileVisitor = new FindChildFileVisitor(entryPathToFind);
                Files.walkFileTree(zipArchiveRoot, findChildFileVisitor);
                if (findChildFileVisitor.getExistingChildFile() != null) {
                    // the entry exists into the zip archive
                    if (this.copyFileFromZipArchiveOnLocalDisk) {
                        copyFileOnLocalDisk(findChildFileVisitor.getExistingChildFile());
                        return new GeoTiffImageReader(state.localFile.toFile());
                    } else {
                        GeoTiffImageReader geoTiffImageReader = GeoTiffImageReader.buildGeoTiffImageReaderObject(findChildFileVisitor.getExistingChildFile(), fileSystem);
                        success = true;
                        return geoTiffImageReader;
                    }
                }
            } // end 'while (it.hasNext())'
            throw new FileNotFoundException("The zip archive '" + this.imageRelativeFilePath.toString() + "' does not contain the file '" + this.imageRelativeFilePath + "'.");
        } finally {
            if (fileSystem != null && !success) {
                fileSystem.close();
            }
        }
    }

    private GeoTiffImageReader processLocalCopiedFile() throws IOException {
        if (state.localFile != null && Files.exists(state.localFile)) {
            if (Files.isRegularFile(state.localFile)) {
                return new GeoTiffImageReader(state.localFile.toFile());
            } else {
                // the path does not represent a folder or a file
                throw new NotRegularFileException(state.localFile.toString());
            }
        }
        return null;
    }

    private void copyFileOnLocalDisk(Path sourcePath) throws IOException {
        // copy the file from the zip archive on the local disk
        if (state.localFile != null) {
            throw new IllegalStateException("The local file path '"+state.localFile+"' must be null.");
        }
        state.localFile = this.localTempFolder.resolve(this.imageRelativeFilePath);
        if (FileHelper.canCopyOrReplaceFile(sourcePath, state.localFile)) {
            Path parentFolder = state.localFile.getParent();

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Copy file '" + sourcePath.toString() + "' to local folder '" + parentFolder.toString() + "'.");
            }

            if (!Files.exists(parentFolder)) {
                Files.createDirectories(parentFolder);
            }
            FileHelper.copyFileUsingInputStream(sourcePath, state.localFile.toString(), AbstractFile.BUFFER_SIZE);
        }
    }


    private static final class GeoTiffFileState implements CleanUpState {
        private Path localFile;

        @Override
        public synchronized void run() {
            if (localFile != null) {
                try {
                    Files.deleteIfExists(localFile);
                } catch (IOException ignore) {
                }
                localFile = null;
            }
        }
    }
}
