package org.esa.snap.dem.dataio.copernicus90m;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;


//For reading Copernicus GeoTIFF DEM products stored in a tarball

public class CopernicusGeoTIFFReaderPlugIn implements ProductReaderPlugIn {

    public final static String[] FORMAT_NAMES = {"DTED"};
    private final static String[] FORMAT_FILE_EXTENSIONS = {".tif",".tar"};
    private final static String PLUGIN_DESCRIPTION = "Copernicus Europe DEM";
    private final Class[] VALID_INPUT_TYPES = new Class[]{Path.class, File.class, String.class};

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final Path path = ReaderUtils.getPathFromInput(input);
        if (path != null) {
            String name = path.getFileName().toString().toLowerCase();
            for (String prodExt : FORMAT_FILE_EXTENSIONS) {
                if (name.endsWith(prodExt)) {
                    return DecodeQualification.INTENDED;
                }
            }
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return VALID_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new CopernicusGeoTIFFReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".tif", ".TIF",  ".tar", ".TAR"};
    }

    @Override
    public String getDescription(Locale locale)  {
        return PLUGIN_DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new FileFilter();
    }



    public static class FileFilter extends SnapFileFilter {

        public FileFilter() {
            super(FORMAT_NAMES[0], FORMAT_FILE_EXTENSIONS, PLUGIN_DESCRIPTION);
        }

        /**
         * Tests whether or not the given file is accepted by this filter. The default implementation returns
         * <code>true</code> if the given file is a directory or the path string ends with one of the registered extensions.
         * if no extension are defined, the method always returns <code>true</code>
         *
         * @param file the file to be or not be accepted.
         * @return <code>true</code> if given file is accepted by this filter
         */
        @Override
        public boolean accept(final File file) {
            if (super.accept(file)) {
                if (file.isDirectory()) {
                    return true;
                }
                String name = file.getName().toLowerCase();
                if(name.endsWith(FORMAT_FILE_EXTENSIONS[0]) || name.endsWith(FORMAT_FILE_EXTENSIONS[1])) {
                    return true;
                }
            }
            return false;
        }
    }
}
