package org.esa.snap.dataio.bigtiff;


import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.dataio.geotiff.Utils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.Locale;

public class BigGeoTiffProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "GeoTIFF-BigTIFF";

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        try {
            final Object imageIOInput;
            if (input instanceof String) {
                imageIOInput = new File((String) input);
            } else if (input instanceof File || input instanceof InputStream) {
                imageIOInput = input;
            } else {
                return DecodeQualification.UNABLE;
            }

            try (ImageInputStream stream = ImageIO.createImageInputStream(imageIOInput)) {
                return getDecodeQualificationImpl(stream);
            }
        } catch (Exception ignore) {
            // nothing to do, return value is already UNABLE
        }

        return DecodeQualification.UNABLE;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new BigGeoTiffProductReader(this);
    }

    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class, InputStream.class,};
    }

    @Override
    public String[] getFormatNames() {
        return Constants.FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return Constants.FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return Constants.DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(Constants.FORMAT_NAMES[0], getDefaultFileExtensions(), getDescription(null));
    }

    static DecodeQualification getDecodeQualificationImpl(ImageInputStream stream) {
        try {
            String mode = getTiffMode(stream);
            if ("BigTiff".equals(mode)) {
                if (getTiffImageReader(stream) != null) {
                    return DecodeQualification.SUITABLE;
                }
            }
        } catch (Exception ignore) {
            return DecodeQualification.UNABLE;
        }
        return DecodeQualification.UNABLE;
    }

    static TIFFImageReader getTiffImageReader(ImageInputStream stream) throws IOException {
        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        TIFFImageReader imageReader = null;

        while (imageReaders.hasNext()) {
            final ImageReader reader = imageReaders.next();
            if (reader instanceof TIFFImageReader) {
                TIFFImageReader tiffReader = (TIFFImageReader) reader;
                tiffReader.setInput(stream);
                if (!Utils.isCOGGeoTIFF(tiffReader)) {
                    imageReader = tiffReader;
                    break;
                }
            }
        }

        return imageReader;
    }

    static String getTiffMode(ImageInputStream stream) throws IOException {
        try {
            stream.mark();
            int byteOrder = stream.readUnsignedShort();
            switch (byteOrder) {
                case 0x4d4d:
                    stream.setByteOrder(ByteOrder.BIG_ENDIAN);
                    break;
                case 0x4949:
                    stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                    break;
                default:
                    // Fallback
                    stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                    break;
            }

            int magic = stream.readUnsignedShort();
            switch (magic) {
                case 43:
                    // BIG-TIFF
                    return "BigTiff";
                case 42:
                    // normal TIFF
                    return "Tiff";
                default:
                    return "Unknown";
            }
        } finally {
            stream.reset();
        }
    }
}
