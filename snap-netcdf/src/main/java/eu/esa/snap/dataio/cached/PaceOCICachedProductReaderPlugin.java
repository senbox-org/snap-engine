package eu.esa.snap.dataio.cached;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class PaceOCICachedProductReaderPlugin implements ProductReaderPlugIn {

    private static final String FORMAT_NAME = "PaceOCI_L1B";
    private static final String DEFAULT_FILE_EXTENSION = ".nc";
    private static final String READER_DESCRIPTION = "PACE OCI L1B Products caching reader";

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File inputFile = getInputFile(input);
        if (!inputFile.isFile()) {
            return DecodeQualification.UNABLE;
        }

        try {
            try (NetcdfFile netcdfFile = NetcdfFileOpener.open(inputFile.getPath())) {
                if (netcdfFile == null) {
                    return DecodeQualification.UNABLE;
                }

                final Attribute sceneTitleAttribute = netcdfFile.findGlobalAttribute("title");
                if (sceneTitleAttribute == null) {
                    return DecodeQualification.UNABLE;
                }

                final String sceneTitle = sceneTitleAttribute.toString();
                if (sceneTitle.contains("PACE OCIS Level-1B Data") || sceneTitle.contains("PACE OCI Level-1B Data")) {
                    return DecodeQualification.INTENDED;
                }
            }
        } catch (IOException e) {
            // ignore - is "unable" anyways
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{DEFAULT_FILE_EXTENSION};
    }

    @Override
    public String getDescription(Locale locale) {
        return READER_DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        final String[] formatNames = getFormatNames();
        String formatName = "";
        if (formatNames.length > 0) {
            formatName = formatNames[0];
        }
        return new SnapFileFilter(formatName, getDefaultFileExtensions(), getDescription(null));
    }

    static File getInputFile(Object input) {
        File inputFile;
        if (input instanceof File) {
            inputFile = (File) input;
        } else if (input instanceof String) {
            inputFile = new File((String) input);
        } else {
            return null;
        }
        return inputFile;
    }
}
