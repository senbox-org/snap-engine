package org.esa.snap.dataio.geotiff;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFRenderedImage;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Created by jcoravu on 22/11/2019.
 */
public class GeoTiffImageReader implements Closeable {

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
}
