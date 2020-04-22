package org.esa.snap.dataio.geotiff;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFRenderedImage;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.engine_utilities.util.FileSystemUtils;
import org.esa.snap.engine_utilities.util.FindChildFileVisitor;
import org.esa.snap.engine_utilities.util.NotRegularFileException;
import org.esa.snap.engine_utilities.util.ZipFileSystemBuilder;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffConstants;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffException;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffIIOMetadataDecoder;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffMetadata2CRSAdapter;
import org.geotools.coverage.grid.io.imageio.geotiff.PixelScale;
import org.geotools.coverage.grid.io.imageio.geotiff.TiePoint;
import org.geotools.factory.Hints;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.matrix.GeneralMatrix;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.InterpolationNearest;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Created by jcoravu on 22/11/2019.
 */
public class GeoTiffImageReader implements Closeable, GeoTiffRasterRegion {

    private static final Logger logger = Logger.getLogger(GeoTiffImageReader.class.getName());

    private static final int BUFFER_SIZE = 1024 * 1024;
    private static final byte FIRST_IMAGE = 0;

    private final TIFFImageReader imageReader;
    private final Closeable closeable;

    private RenderedImage swappedSubsampledImage;
    private Rectangle rectangle;
    private ImageReadParam readParam;

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
    protected void finalize() throws Throwable {
        super.finalize();

        close();
    }

    @Override
    public void close() {
        try {
            ImageInputStream imageInputStream = (ImageInputStream) this.imageReader.getInput();
            try {
                imageInputStream.close();
            } catch (IOException ignore) {
                // ignore
            }
            this.rectangle = null;
            this.readParam = null;
            this.swappedSubsampledImage = null;
        } finally {
            if (this.closeable != null) {
                try {
                    this.closeable.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
    }

    public TIFFImageMetadata getImageMetadata() throws IOException {
        return (TIFFImageMetadata) this.imageReader.getImageMetadata(FIRST_IMAGE);
    }

    @Override
    public Raster readRect(boolean isGlobalShifted180, int sourceOffsetX, int sourceOffsetY, int sourceStepX, int sourceStepY,
                           int destOffsetX, int destOffsetY, int destWidth, int destHeight)
                           throws IOException {

        if (this.readParam == null) {
            this.readParam = this.imageReader.getDefaultReadParam();
        }
        if (this.rectangle == null) {
            this.rectangle = new Rectangle();
        }
        int subsamplingXOffset = sourceOffsetX % sourceStepX;
        int subsamplingYOffset = sourceOffsetY % sourceStepY;
        this.readParam.setSourceSubsampling(sourceStepX, sourceStepY, subsamplingXOffset, subsamplingYOffset);
        RenderedImage subsampledImage = this.imageReader.readAsRenderedImage(FIRST_IMAGE, this.readParam);
        try {
            this.rectangle.setBounds(destOffsetX, destOffsetY, destWidth, destHeight);
            if (isGlobalShifted180) {
                if (this.swappedSubsampledImage == null) {
                    this.swappedSubsampledImage = horizontalMosaic(getHalfImages(subsampledImage));
                }
                return this.swappedSubsampledImage.getData(this.rectangle);
            } else {
                return subsampledImage.getData(this.rectangle);
            }
        } finally {
            WeakReference<RenderedImage> referenceImage = new WeakReference<>(subsampledImage);
            referenceImage.clear();
        }
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
        Iterator iter = this.imageReader.getImageTypes(FIRST_IMAGE);
        ImageTypeSpecifier its = (ImageTypeSpecifier)iter.next();
        return its.getSampleModel();
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

    public Dimension validateArea(Rectangle area) throws IOException {
        int imageWidth = getImageWidth();
        if ((area.x + area.width) > imageWidth) {
            throw new IllegalStateException("The coordinates are out of bounds: area.x="+area.x+", area.width="+area.width+", image.width=" + imageWidth);
        }
        int imageHeight = getImageHeight();
        if ((area.y + area.height) > imageHeight) {
            throw new IllegalStateException("The coordinates are out of bounds: area.y="+area.y+", area.height="+area.height+", image.height=" + imageHeight);
        }
        return new Dimension(imageWidth, imageHeight);
    }

    private static TIFFImageReader buildImageReader(Object sourceImage) throws IOException {
        TIFFImageReader imageReader = null;
        ImageInputStream imageInputStream = ImageIO.createImageInputStream(sourceImage);
        if (imageInputStream == null) {
            throw new NullPointerException("The image input stream is null for source image '" + sourceImage + "'.");
        }
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

    private RenderedImage[] getHalfImages(RenderedImage fullImage) {
        int xStart = 0;
        int yStart = 0;
        float width = (float) fullImage.getWidth() / 2;
        float height = fullImage.getHeight();
        final RenderedOp leftImage = CropDescriptor.create(fullImage, (float) xStart, (float) yStart, width, height, null);

        xStart = fullImage.getWidth() / 2;
        width = (float) (fullImage.getWidth() - xStart);
        final RenderedOp rightImage = CropDescriptor.create(fullImage, (float) xStart, (float) yStart, width, height, null);

        return new RenderedImage[]{leftImage, rightImage};
    }

    private static RenderedImage horizontalMosaic(RenderedImage[] halfImages) {
        final RenderedImage leftImage = halfImages[0];
        final RenderedImage rightImage = halfImages[1];
        // Translate the left image to shift it fullWidth/2 pixels to the right, and vice versa
        RenderedImage translatedLeftImage = TranslateDescriptor.create(leftImage, (float) leftImage.getWidth(), 0f, new InterpolationNearest(), null);
        RenderedImage translatedRightImage = TranslateDescriptor.create(rightImage, -1.0f * rightImage.getWidth(), 0f, new InterpolationNearest(), null);
        // Now mosaic the two images.
        return MosaicDescriptor.create(new RenderedImage[]{translatedRightImage, translatedLeftImage}, MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null, null, null);
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
            throw new FileNotFoundException("The zip archive '" + productPath.toString() + "' does not contain the file '" + zipEntryPath + "'.");
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
                    Path tiffImagePath = fileSystem.getPath(filePath);
                    GeoTiffImageReader geoTiffImageReader = buildGeoTiffImageReaderObject(tiffImagePath, fileSystem);
                    success = true;
                    return geoTiffImageReader;
                }
            }
            throw new IllegalArgumentException("The zip archive '" + productPath.toString() + "' does not contain an image. The item count is " + filePaths.size()+".");
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

    public static CrsGeoCoding buildGeoCoding(TIFFImageMetadata metadata, int defaultProductWidth, int defaultProductHeight, Rectangle subsetRegion) throws Exception {
        final GeoTiffIIOMetadataDecoder metadataDecoder = new GeoTiffIIOMetadataDecoder(metadata);
        Hints hints = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, true);
        final GeoTiffMetadata2CRSAdapter geoTiff2CRSAdapter = new GeoTiffMetadata2CRSAdapter(hints);
        // todo reactivate the following line if geotools has fixed the problem. (see BEAM-1510)
        // final MathTransform toModel = GeoTiffMetadata2CRSAdapter.getRasterToModel(metadataDecoder, false);
        final MathTransform toModel = getRasterToModel(metadataDecoder);
        CoordinateReferenceSystem mapCRS;
        try {
            mapCRS = geoTiff2CRSAdapter.createCoordinateSystem(metadataDecoder);
        } catch (UnsupportedOperationException e) {
            if (toModel == null) {
                throw e;
            } else {
                // ENVI falls back to WGS84, if no CRS is given in the GeoTIFF.
                mapCRS = DefaultGeographicCRS.WGS84;
            }
        }
        AffineTransform transform = (AffineTransform)toModel;
        if (metadataDecoder.getModelPixelScales() == null) {
            Rectangle imageBounds;
            if (subsetRegion == null) {
                imageBounds = new Rectangle(0, 0, defaultProductWidth, defaultProductHeight);
            } else {
                imageBounds = subsetRegion;
            }
            return new CrsGeoCoding(mapCRS, imageBounds, transform);
        }
        double stepX = metadataDecoder.getModelPixelScales().getScaleX();
        double stepY = metadataDecoder.getModelPixelScales().getScaleY();
        double originX = transform.getTranslateX();
        double originY = transform.getTranslateY();
        return ImageUtils.buildCrsGeoCoding(originX, originY, stepX, stepY, defaultProductWidth, defaultProductHeight, mapCRS, subsetRegion);
    }

    /*
     * Copied from GeoTools GeoTiffMetadata2CRSAdapter because the given tie-point offset is
     * not correctly interpreted in GeoTools. The tie-point should be placed at the pixel center
     * if RasterPixelIsPoint is set as value for GTRasterTypeGeoKey.
     * See links:
     * http://www.remotesensing.org/geotiff/faq.html#PixelIsPoint
     * http://lists.osgeo.org/pipermail/gdal-dev/2007-November/015040.html
     * http://trac.osgeo.org/gdal/wiki/rfc33_gtiff_pixelispoint
     */
    private static MathTransform getRasterToModel(final GeoTiffIIOMetadataDecoder metadata) throws GeoTiffException {
        //
        // Load initials
        //
        final boolean hasTiePoints = metadata.hasTiePoints();
        final boolean hasPixelScales = metadata.hasPixelScales();
        final boolean hasModelTransformation = metadata.hasModelTrasformation();
        int rasterType = getGeoKeyAsInt(GeoTiffConstants.GTRasterTypeGeoKey, metadata);
        // geotiff spec says that PixelIsArea is the default
        if (rasterType == GeoTiffConstants.UNDEFINED) {
            rasterType = GeoTiffConstants.RasterPixelIsArea;
        }
        MathTransform xform;
        if (hasTiePoints && hasPixelScales) {

            //
            // we use tie points and pixel scales to build the grid to world
            //
            // model space
            final TiePoint[] tiePoints = metadata.getModelTiePoints();
            final PixelScale pixScales = metadata.getModelPixelScales();


            // here is the matrix we need to build
            final GeneralMatrix gm = new GeneralMatrix(3);
            final double scaleRaster2ModelLongitude = pixScales.getScaleX();
            final double scaleRaster2ModelLatitude = -pixScales.getScaleY();
            // "raster" space
            final double tiePointColumn = tiePoints[0].getValueAt(0) + (rasterType == GeoTiffConstants.RasterPixelIsPoint ? 0.5 : 0);
            final double tiePointRow = tiePoints[0].getValueAt(1) + (rasterType == GeoTiffConstants.RasterPixelIsPoint ? 0.5 : 0);

            // compute an "offset and scale" matrix
            gm.setElement(0, 0, scaleRaster2ModelLongitude);
            gm.setElement(1, 1, scaleRaster2ModelLatitude);
            gm.setElement(0, 1, 0);
            gm.setElement(1, 0, 0);

            gm.setElement(0, 2, tiePoints[0].getValueAt(3) - (scaleRaster2ModelLongitude * tiePointColumn));
            gm.setElement(1, 2, tiePoints[0].getValueAt(4) - (scaleRaster2ModelLatitude * tiePointRow));

            // make it a LinearTransform
            xform = ProjectiveTransform.create(gm);

        } else if (hasModelTransformation) {
            if (rasterType == GeoTiffConstants.RasterPixelIsPoint) {
                final AffineTransform tempTransform = new AffineTransform(metadata.getModelTransformation());
                tempTransform.concatenate(AffineTransform.getTranslateInstance(0.5, 0.5));
                xform = ProjectiveTransform.create(tempTransform);
            } else {
                assert rasterType == GeoTiffConstants.RasterPixelIsArea;
                xform = ProjectiveTransform.create(metadata.getModelTransformation());
            }
        } else {
            throw new GeoTiffException(metadata, "Unknown Raster to Model configuration.", null);
        }

        return xform;
    }

    private static int getGeoKeyAsInt(final int key, final GeoTiffIIOMetadataDecoder metadata) {
        try {
            return Integer.parseInt(metadata.getGeoKey(key));
        } catch (NumberFormatException ne) {
            logger.log(Level.FINE, ne.getMessage(), ne);
            return GeoTiffConstants.UNDEFINED;
        }
    }
}
