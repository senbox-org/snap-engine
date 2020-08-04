package org.esa.snap.dataio.geotiff;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.engine_utilities.file.AbstractFile;
import org.esa.snap.engine_utilities.file.FileHelper;
import org.esa.snap.engine_utilities.util.FindChildFileVisitor;
import org.esa.snap.engine_utilities.util.NotRegularFileException;
import org.esa.snap.engine_utilities.util.ZipFileSystemBuilder;

import java.io.Closeable;
import java.io.File;
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

    private File localTempFolder;

    public GeoTiffFile(Path imageParentPath, String imageRelativeFilePath, boolean copyFileFromZipArchiveOnLocalDisk) {
        if (imageParentPath == null) {
            throw new NullPointerException("The image path is null.");
        }
        this.imageParentPath = imageParentPath;
        this.imageRelativeFilePath = imageRelativeFilePath; // the relative path may be null when the parent path is a file
        this.copyFileFromZipArchiveOnLocalDisk = copyFileFromZipArchiveOnLocalDisk;
    }

    @Override
    protected final void finalize() throws Throwable {
        super.finalize();

        cleanup();
    }

    @Override
    public final void close() throws IOException {
        cleanup();
    }

    public GeoTiffImageReader buildImageReader() throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {
        if (this.localTempFolder != null) {
            return processLocalCopiedFile();
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
                        throw new NullPointerException("The relative file path '"+this.imageRelativeFilePath+"' must be null.");
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

    protected void cleanup() {
        if (this.localTempFolder != null) {
            VirtualDir.deleteFileTree(this.localTempFolder);
            this.localTempFolder = null;
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
                        Path fileToReturn = copyFileOnLocalDisk(findChildFileVisitor.getExistingChildFile());
                        return new GeoTiffImageReader(fileToReturn.toFile());
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
        Path localFilePath = this.localTempFolder.toPath().resolve(this.imageRelativeFilePath);
        if (Files.exists(this.imageParentPath)) {
            if (Files.isRegularFile(localFilePath)) {
                return new GeoTiffImageReader(localFilePath.toFile());
            } else {
                // the product path does not represent a folder or a file
                throw new NotRegularFileException(localFilePath.toString());
            }
        } else {
            // the product path does not exist
            throw new FileNotFoundException("The local path '"+localFilePath+"' does not exist.");
        }
    }

    private Path copyFileOnLocalDisk(Path sourcePath) throws IOException {
        // copy the file from the zip archive on the local disk
        if (this.localTempFolder == null) {
            this.localTempFolder = VirtualDir.createUniqueTempDir();
        }
        Path localFilePath = this.localTempFolder.toPath().resolve(this.imageRelativeFilePath);
        if (FileHelper.canCopyOrReplaceFile(sourcePath, localFilePath)) {
            Path parentFolder = localFilePath.getParent();

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Copy file '" + sourcePath.toString() + "' to local folder '" + parentFolder.toString() + "'.");
            }

            if (!Files.exists(parentFolder)) {
                Files.createDirectories(parentFolder);
            }
            FileHelper.copyFileUsingInputStream(sourcePath, localFilePath.toString(), AbstractFile.BUFFER_SIZE);
        }
        return localFilePath;
    }
}
