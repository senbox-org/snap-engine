package org.esa.s3tbx.dataio.landsat.geotiff;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public abstract class AbstractLandsat8ScalingGeotiffReaderPlugin implements ProductReaderPlugIn {

    private static final Class[] READER_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{".txt", ".TXT", ".gz"};
    public static final String PRE_COLLECTION_FILENAME_PATTERN = "LT8\\d{13}.{3}\\d{2}_MTL.(txt|TXT)";

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        String filename = new File(input.toString()).getName();
        if (!isLandsat8Filename(filename)) {
            return DecodeQualification.UNABLE;
        }

        VirtualDir virtualDir;
        try {
            virtualDir = LandsatGeotiffReaderPlugin.getInput(input);
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        final DecodeQualification decodeQualification = LandsatGeotiffReaderPlugin.getDecodeQualification(virtualDir);
        if( DecodeQualification.INTENDED.equals(decodeQualification)) {
            return DecodeQualification.SUITABLE;
        }
        return decodeQualification;
    }

    private static boolean isLandsat8Filename(String filename) {
        // TIRS must not be considered, only in OLI and COMBINED products we have different resolutions
        return LandsatTypeInfo.isLandsat8(filename) && !filename.matches(PRE_COLLECTION_FILENAME_PATTERN);
    }

    @Override
    public Class[] getInputTypes() {
        return READER_INPUT_TYPES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], DEFAULT_FILE_EXTENSIONS, getDescription(Locale.getDefault()));
    }
}
