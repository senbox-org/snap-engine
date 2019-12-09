package org.esa.snap.dataio.geotiff;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFRenderedImage;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.engine_utilities.util.FileSystemUtils;
import org.esa.snap.engine_utilities.util.FindChildFileVisitor;
import org.esa.snap.engine_utilities.util.NotRegularFileException;
import org.esa.snap.engine_utilities.util.ZipFileSystemBuilder;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;

/**
 * Created by jcoravu on 22/11/2019.
 */
public class GeoTiffImageReader implements Closeable {

    private static final int BUFFER_SIZE = 1024 * 1024;
    private static final byte FIRST_IMAGE = 0;

    private final TIFFImageReader imageReader;
    private final Closeable closeable;

    public GeoTiffImageReader(ImageInputStream imageInputStream) throws IOException {
        this.imageReader = findImageReader(imageInputStream);
        this.closeable = null;
    }

    public GeoTiffImageReader(File file) throws IOException {
        this.imageReader = buildImageReader(file);
        this.closeable = null;
    }

    public GeoTiffImageReader(InputStream inputStream, Closeable closeable) throws IOException {
        this.imageReader = buildImageReader(inputStream);
        this.closeable = closeable;
    }

    @Override
    public void close() {
        try {
            ImageInputStream imageInputStream = (ImageInputStream) this.imageReader.getInput();
            try {
                imageInputStream.close();
            } catch (IOException e) {
                // ignore
            }
        } finally {
            if (this.closeable != null) {
                try {
                    this.closeable.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    public TIFFImageMetadata getImageMetadata() throws IOException {
        return (TIFFImageMetadata) this.imageReader.getImageMetadata(FIRST_IMAGE);
    }

    public Raster readRect(int sourceOffsetX, int sourceOffsetY, int sourceStepX, int sourceStepY, int destOffsetX, int destOffsetY, int destWidth, int destHeight)
                           throws IOException {

        ImageReadParam readParam = this.imageReader.getDefaultReadParam();
        int subsamplingXOffset = sourceOffsetX % sourceStepX;
        int subsamplingYOffset = sourceOffsetY % sourceStepY;
        readParam.setSourceSubsampling(sourceStepX, sourceStepY, subsamplingXOffset, subsamplingYOffset);
        RenderedImage subsampledImage = this.imageReader.readAsRenderedImage(FIRST_IMAGE, readParam);
        return subsampledImage.getData(new Rectangle(destOffsetX, destOffsetY, destWidth, destHeight));
    }

    public int getImageWidth() throws IOException {
        return this.imageReader.getWidth(FIRST_IMAGE);
    }

    public int getImageHeight() throws IOException {
        return this.imageReader.getHeight(FIRST_IMAGE);
    }

    public int getTileHeight() throws IOException {
        return this.imageReader.getTileHeight(FIRST_IMAGE);
    }

    public int getTileWidth() throws IOException {
        return this.imageReader.getTileWidth(FIRST_IMAGE);
    }

    public SampleModel getSampleModel() throws IOException {
        ImageReadParam readParam = this.imageReader.getDefaultReadParam();
        TIFFRenderedImage baseImage = (TIFFRenderedImage) this.imageReader.readAsRenderedImage(FIRST_IMAGE, readParam);
        return baseImage.getSampleModel();
    }

    public TIFFRenderedImage getBaseImage() throws IOException {
        ImageReadParam readParam = this.imageReader.getDefaultReadParam();
        return (TIFFRenderedImage) this.imageReader.readAsRenderedImage(FIRST_IMAGE, readParam);
    }

    public Dimension computePreferredTiling(int rasterWidth, int rasterHeight) throws IOException {
        int imageWidth = getImageWidth();
        int imageHeight = getImageHeight();
        int tileWidth = getTileWidth();
        int tileHeight = getTileHeight();
        boolean isBadTiling = (tileWidth <= 1 || tileHeight <= 1 || imageWidth == tileWidth || imageHeight == tileHeight);
        Dimension dimension;
        if (isBadTiling) {
            dimension = JAIUtils.computePreferredTileSize(rasterWidth, rasterHeight, 1);
        } else {
            if (tileWidth > rasterWidth) {
                tileWidth = rasterWidth;
            }
            if (tileHeight > rasterHeight) {
                tileHeight = rasterHeight;
            }
            dimension = new Dimension(tileWidth, tileHeight);
        }
        return dimension;
    }

    public Dimension validateSize(int metadataImageWidth, int metadataImageHeight) throws IOException {
        Dimension defaultBandSize = new Dimension(getImageWidth(), getImageHeight());
        if (defaultBandSize.width != metadataImageWidth) {
            throw new IllegalStateException("The width " + metadataImageWidth + " from the metadata file is not equal with the image width " + defaultBandSize.width + ".");
        }
        if (defaultBandSize.height != metadataImageHeight) {
            throw new IllegalStateException("The height " + metadataImageHeight + " from the metadata file is not equal with the image height " + defaultBandSize.height + ".");
        }
        return defaultBandSize;
    }

    private static TIFFImageReader buildImageReader(Object sourceImage) throws IOException {
        TIFFImageReader imageReader = null;
        ImageInputStream imageInputStream = ImageIO.createImageInputStream(sourceImage);
        try {
            imageReader = findImageReader(imageInputStream);
        } finally {
            if (imageReader == null) {
                imageInputStream.close(); // failed to get the image reader
            }
        }
        return imageReader;
    }

    private static TIFFImageReader findImageReader(ImageInputStream imageInputStream) {
        Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
        while (imageReaders.hasNext()) {
            ImageReader reader = imageReaders.next();
            if (reader instanceof TIFFImageReader) {
                TIFFImageReader imageReader = (TIFFImageReader) reader;
                imageReader.setInput(imageInputStream);
                return imageReader;
            }
        }
        throw new IllegalStateException("GeoTiff imageReader not found.");
    }

    public static GeoTiffImageReader buildGeoTiffImageReader(Path productPath) throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {
        return buildGeoTiffImageReader(productPath, null);
    }

    public static GeoTiffImageReader buildGeoTiffImageReader(Path productPath, String childRelativePath)
                                            throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {

        if (Files.exists(productPath)) {
            // the product path exists
            if (Files.isDirectory(productPath)) {
                // the product path represents a folder
                Path child = productPath.resolve(childRelativePath);
                if (Files.exists(child)) {
                    if (Files.isRegularFile(child)) {
                        return new GeoTiffImageReader(child.toFile());
                    } else {
                        throw new NotRegularFileException("The product folder '"+productPath.toString()+"' does not contain the file '" + childRelativePath+"'.");
                    }
                } else {
                    throw new FileNotFoundException("The product folder '"+productPath.toString()+"' does not contain the path '" + childRelativePath+"'.");
                }
            } else if (Files.isRegularFile(productPath)) {
                // the product path represents a file
                if (productPath.getFileName().toString().toLowerCase().endsWith(GeoTiffProductReaderPlugIn.ZIP_FILE_EXTENSION)) {
                    if (childRelativePath == null) {
                        return buildGeoTiffImageReaderFromZipArchive(productPath);
                    } else {
                        return buildGeoTiffImageReaderFromZipArchive(productPath, childRelativePath);
                    }
                } else {
                    return new GeoTiffImageReader(productPath.toFile());
                }
            } else {
                // the product path does not represent a folder or a file
                throw new NotRegularFileException(productPath.toString());
            }
        } else {
            // the product path does not exist
            throw new FileNotFoundException("The product path '"+productPath+"' does not exist.");
        }
    }

    private static GeoTiffImageReader buildGeoTiffImageReaderFromZipArchive(Path productPath, String zipEntryPath)
                                                        throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {

        boolean success = false;
        FileSystem fileSystem = null;
        try {
            fileSystem = ZipFileSystemBuilder.newZipFileSystem(productPath);
            Iterator<Path> it = fileSystem.getRootDirectories().iterator();
            while (it.hasNext()) {
                Path zipArchiveRoot = it.next();
                Path entryPathToFind = ZipFileSystemBuilder.buildZipEntryPath(zipArchiveRoot, zipEntryPath);
                FindChildFileVisitor findChildFileVisitor = new FindChildFileVisitor(entryPathToFind);
                Files.walkFileTree(zipArchiveRoot, findChildFileVisitor);
                if (findChildFileVisitor.getExistingChildFile() != null) {
                    // the entry exists into the zip archive
                    GeoTiffImageReader geoTiffImageReader = buildGeoTiffImageReaderObject(findChildFileVisitor.getExistingChildFile(), fileSystem);
                    success = true;
                    return geoTiffImageReader;
                }
            } // end 'while (it.hasNext())'
            throw new IllegalArgumentException("The zip archive '" + productPath.toString() + "' does not contain the file '" + zipEntryPath + "'.");
        } finally {
            if (fileSystem != null && !success) {
                fileSystem.close();
            }
        }
    }

    private static GeoTiffImageReader buildGeoTiffImageReaderFromZipArchive(Path productPath)
                                                        throws IOException, IllegalAccessException, InstantiationException, InvocationTargetException {

        boolean success = false;
        FileSystem fileSystem = null;
        try {
            fileSystem = ZipFileSystemBuilder.newZipFileSystem(productPath);
            TreeSet<String> filePaths = FileSystemUtils.listAllFilePaths(fileSystem);
            Iterator<String> itFileNames = filePaths.iterator();
            while (itFileNames.hasNext() && !success) {
                String filePath = itFileNames.next();
                boolean extensionMatches = Arrays.stream(GeoTiffProductReaderPlugIn.TIFF_FILE_EXTENSION).anyMatch(filePath.toLowerCase()::endsWith);
                if (extensionMatches) {
                    int startIndex = 0;
                    if (filePath.startsWith(fileSystem.getSeparator())) {
                        startIndex = fileSystem.getSeparator().length(); // the file path starts with '/' (the root folder in the zip archive)
                    }
                    if (filePath.indexOf(fileSystem.getSeparator(), startIndex) < 0) {
                        Path tiffImagePath = fileSystem.getPath(filePath);
                        GeoTiffImageReader geoTiffImageReader = buildGeoTiffImageReaderObject(tiffImagePath, fileSystem);
                        success = true;
                        return geoTiffImageReader;
                    }
                }
            }
            throw new IllegalArgumentException("The zip archive '" + productPath.toString() + "' does not contain an image.");
        } finally {
            if (fileSystem != null && !success) {
                fileSystem.close();
            }
        }
    }

    private static GeoTiffImageReader buildGeoTiffImageReaderObject(Path tiffPath, Closeable closeable) throws IOException {
        boolean success = false;
        InputStream inputStream = Files.newInputStream(tiffPath);
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, BUFFER_SIZE);
            InputStream inputStreamToReturn;
            if (tiffPath.getFileName().toString().endsWith(".gz")) {
                inputStreamToReturn = new GZIPInputStream(bufferedInputStream);
            } else {
                inputStreamToReturn = bufferedInputStream;
            }
            GeoTiffImageReader geoTiffImageReader = new GeoTiffImageReader(inputStreamToReturn, closeable);
            success = true;
            return geoTiffImageReader;
        } finally {
            if (!success) {
                inputStream.close();
            }
        }
    }
}
