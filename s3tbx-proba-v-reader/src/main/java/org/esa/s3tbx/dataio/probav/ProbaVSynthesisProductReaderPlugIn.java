package org.esa.s3tbx.dataio.probav;

import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.RGBImageProfile;
import org.esa.snap.framework.datamodel.RGBImageProfileManager;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.SnapFileFilter;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * Reader plug-in for Proba-V products
 *
 * @author olafd
 */
public class ProbaVSynthesisProductReaderPlugIn implements ProductReaderPlugIn {

    private static final String _H5_CLASS_NAME = "ncsa.hdf.hdf5lib.H5";

    public static final String FORMAT_NAME_PROBA_V = "PROBA-V-Synthesis";

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String DESCRIPTION = "PROBA-V Format";
    private static final String FILE_EXTENSION = "";
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{FILE_EXTENSION};
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_PROBA_V};

    private static boolean hdf5LibAvailable = false;

    static {
        hdf5LibAvailable = loadHdf5Lib(ProbaVSynthesisProductReaderPlugIn.class) != null;
    }

    public ProbaVSynthesisProductReaderPlugIn() {
        RGBImageProfile toaProfile = new RGBImageProfile("PROBA-V TOA RGB",
                                               new String[] {"TOA_REFL_NIR", "TOA_REFL_RED", "TOA_REFL_BLUE"});
        RGBImageProfile tocProfile = new RGBImageProfile("PROBA-V TOC RGB",
                                               new String[] {"TOC_REFL_NIR", "TOC_REFL_RED", "TOC_REFL_BLUE"});
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(toaProfile);
        manager.addProfile(tocProfile);
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
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAMES[0], FILE_EXTENSION, DESCRIPTION);
    }

    static boolean isInputValid(Object input) {
        File inputFile = new File(input.toString());
        return isInputProbaVFileNameValid(inputFile.getName());
    }

    static boolean isInputProbaVFileNameValid(String fileName) {
        return fileName.toUpperCase().endsWith(".HDF5") &&
                (fileName.startsWith("PROBAV_S1_") ||
                        fileName.startsWith("PROBAV_S5_") ||
                        fileName.startsWith("PROBAV_S10_"));
    }

    static boolean isProbaSynthesisToaProduct(String fileName) {
        return isInputProbaVFileNameValid(fileName) &&
                fileName.contains("_TOA_") && !isProbaSynthesisNdviProduct(fileName);
    }

    static boolean isProbaSynthesisTocProduct(String fileName) {
        return isInputProbaVFileNameValid(fileName) &&
                fileName.contains("_TOC_") && !isProbaSynthesisNdviProduct(fileName);
    }

    static boolean isProbaSynthesisNdviProduct(String fileName) {
        return isInputProbaVFileNameValid(fileName) && fileName.contains("_NDVI_");
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
