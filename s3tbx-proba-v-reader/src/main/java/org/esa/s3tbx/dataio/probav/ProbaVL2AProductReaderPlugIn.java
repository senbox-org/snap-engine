package org.esa.s3tbx.dataio.probav;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.RGBImageProfile;
import org.esa.snap.core.datamodel.RGBImageProfileManager;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * Reader plug-in for Proba-V L2A products
 *
 * @author olafd
 */
public class ProbaVL2AProductReaderPlugIn implements ProductReaderPlugIn {

    private static final String _H5_CLASS_NAME = "ncsa.hdf.hdf5lib.H5";

    public static final String FORMAT_NAME_PROBA_V = "PROBA-V-L2A";

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String DESCRIPTION = "PROBA-V Format";
    private static final String FILE_EXTENSION = "";
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{FILE_EXTENSION};
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_PROBA_V};

    private static boolean hdf5LibAvailable = false;

    static {
        hdf5LibAvailable = loadHdf5Lib(ProbaVL2AProductReaderPlugIn.class) != null;
    }

    public ProbaVL2AProductReaderPlugIn() {
        RGBImageProfile toaProfile = new RGBImageProfile("PROBA-V TOA RGB",
                                               new String[] {"TOA_REFL_NIR", "TOA_REFL_RED", "TOA_REFL_BLUE"});

        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(toaProfile);
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
//        return new ProbaVL2AProductReader(this);
        return new ProbaVL2AProductReader(this);   // test!
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
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAMES[0], FILE_EXTENSION, DESCRIPTION);
    }

    static boolean isInputValid(Object input) {
        File inputFile = new File(input.toString());
        return isInputProbaVFileNameValid(inputFile.getName());
    }

    static boolean isInputProbaVFileNameValid(String fileName) {
        return fileName.startsWith("PROBAV_L2A_") && fileName.toUpperCase().endsWith(".HDF5");
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
                SystemUtils.LOG.warning(MessageFormat.format(warningPattern, callerClass, error.getClass(), error.getMessage()));
                return null;
            }
        } else {
            return null;
        }
    }
}
