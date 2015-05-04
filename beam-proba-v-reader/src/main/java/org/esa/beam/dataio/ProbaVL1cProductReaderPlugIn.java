package org.esa.beam.dataio;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.RGBImageProfile;
import org.esa.beam.framework.datamodel.RGBImageProfileManager;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * Reader plug-in for Proba-V products
 *
 * @author olafd
 */
public class ProbaVL1cProductReaderPlugIn implements ProductReaderPlugIn {

    private static final String _H5_CLASS_NAME = "ncsa.hdf.hdf5lib.H5";

    public static final String FORMAT_NAME_PROBA_V = "PROBA-V-L1C";

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String DESCRIPTION = "PROBA-V Format";
    private static final String FILE_EXTENSION = "";
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{FILE_EXTENSION};
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_PROBA_V};

    private static final String PROBAV_L1C_FILENAME_REGEXP =
            "PROBAV_L1C_[0-9]{8}_[0-9]{6}_[0-2]{1}_V[0-9]{3}.(?i)(hdf5)";

    private static boolean hdf5LibAvailable = false;

    static {
        hdf5LibAvailable = loadHdf5Lib(ProbaVL1cProductReaderPlugIn.class) != null;
    }

    public ProbaVL1cProductReaderPlugIn() {
        // todo
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (isInputValid(input)) {
            return DecodeQualification.INTENDED;
        } else {
            return DecodeQualification.UNABLE;
        }
    }

    static File getFileInput(Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }

    /**
     * Returns whether or not the HDF5 library is available.
     */
    static boolean isHdf5LibAvailable() {
        return hdf5LibAvailable;
    }

    @Override
    public Class[] getInputTypes() {
        return SUPPORTED_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new ProbaVSynthesisProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAMES[0], FILE_EXTENSION, DESCRIPTION);
    }

    static boolean isInputValid(Object input) {
        File inputFile = new File(input.toString());
        return isInputProbaVFileNameValid(inputFile.getName());
    }

    static boolean isInputProbaVFileNameValid(String fileName) {
        // todo: for L1C we need more detailed specifications first!
        return false;
    }

    static boolean isProbaVL1cProduct(String fileName) {
        return fileName.matches(PROBAV_L1C_FILENAME_REGEXP);
    }


    static Class<?> loadHdf5Lib(Class<?> callerClass) {
        return loadClassWithNativeDependencies(callerClass, _H5_CLASS_NAME, "{0}: HDF-5 library not available: {1}: {2}");
    }

    private static Class<?> loadClassWithNativeDependencies(Class<?> callerClass, String className, String warningPattern) {
        ClassLoader classLoader = callerClass.getClassLoader();

        String classResourceName = "/" + className.replace('.', '/') + ".class";
        SystemUtils.class.getResource(classResourceName);
        if (callerClass.getResource(classResourceName) != null) {
            try {
                return Class.forName(className, true, classLoader);
            } catch (Throwable error) {
                BeamLogManager.getSystemLogger().warning(MessageFormat.format(warningPattern, callerClass, error.getClass(), error.getMessage()));
                return null;
            }
        } else {
            return null;
        }
    }
}
