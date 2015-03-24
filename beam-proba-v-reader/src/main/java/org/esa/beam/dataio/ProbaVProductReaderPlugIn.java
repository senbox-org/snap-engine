package org.esa.beam.dataio;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 11.03.2015
 * Time: 16:21
 *
 * @author olafd
 */
public class ProbaVProductReaderPlugIn implements ProductReaderPlugIn {

    // todo: implement

    public static final String HDF5_FORMAT_NAME = "HDF5";
    public static final String HDF5_FILE_EXTENSION = ".h5";

    private static final String _H5_CLASS_NAME = "ncsa.hdf.hdf5lib.H5";

    public static final String FORMAT_NAME_PROBA_V = "PROBA-V";

    private static final Class[] SUPPORTED_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String DESCRIPTION = "PROBA-V Format";
    private static final String FILE_EXTENSION = "";
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{FILE_EXTENSION};
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME_PROBA_V};

    private static final String PROBAV_L1C_FILENAME_REGEXP =
            "PROBAV_L1C_[0-9]{8}_[0-9]{6}_[0-2]{1}_V[0-9]{3}.(?i)(hdf5)";
    private static final String PROBAV_S1_TOA_333M_FILENAME_REGEXP =
            "PROBAV_S1_TOA_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_V[0-9]{3}.(?i)(hdf5)";
    private static final String PROBAV_S1_TOA_1KM_FILENAME_REGEXP =
            "PROBAV_S1_TOA_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_1KM_V[0-9]{3}.(?i)(hdf5)";
    private static final String PROBAV_S1_TOC_333M_FILENAME_REGEXP =
            "PROBAV_S1_TOC_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_V[0-9]{3}.(?i)(hdf5)";
    private static final String PROBAV_S1_TOC_1KM_FILENAME_REGEXP =
            "PROBAV_S1_TOC_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_1KM_V[0-9]{3}.(?i)(hdf5)";
    private static final String PROBAV_S10_TOC_333M_FILENAME_REGEXP =
            "PROBAV_S10_TOC_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_V[0-9]{3}.(?i)(hdf5)";
    private static final String PROBAV_S10_TOC_1KM_FILENAME_REGEXP =
            "PROBAV_S10_TOC_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_1KM_V[0-9]{3}.(?i)(hdf5)";
    private static final String PROBAV_S10_TOC_NDVI_FILENAME_REGEXP =
            "PROBAV_S1_TOA_X[0-9]{2}Y[0-9]{2}_[0-9]{8}_333M_NDVI_V[0-9]{3}.(?i)(hdf5)";   // todo: shall this be supported?

    private static boolean hdf5LibAvailable = false;

    static {
        hdf5LibAvailable = loadHdf5Lib(ProbaVProductReaderPlugIn.class) != null;
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
        return new ProbaVProductReader(this);
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

    private boolean isInputValid(Object input) {
        File inputFile = new File(input.toString());
        return isInputProbaVFileNameValid(inputFile.getName());
    }

    private boolean isInputProbaVFileNameValid(String fileName) {
        return
//                isProbaL1CProduct(fileName) ||           // todo: for L1C we need more detailed specifications first!
                isProbaS1ToaProduct(fileName) ||
                isProbaS1TocProduct(fileName) ||
                isProbaS10TocProduct(fileName) ||
                isProbaS10TocNdviProduct(fileName);
    }

    static boolean isProbaS10TocProduct(String fileName) {
        return fileName.matches(PROBAV_S10_TOC_333M_FILENAME_REGEXP) || fileName.matches(PROBAV_S10_TOC_1KM_FILENAME_REGEXP);
    }

    static boolean isProbaS1TocProduct(String fileName) {
        return fileName.matches(PROBAV_S1_TOC_333M_FILENAME_REGEXP) || fileName.matches(PROBAV_S1_TOC_1KM_FILENAME_REGEXP);
    }

    static boolean isProbaS1ToaProduct(String fileName) {
        return fileName.matches(PROBAV_S1_TOA_333M_FILENAME_REGEXP) || fileName.matches(PROBAV_S1_TOA_1KM_FILENAME_REGEXP);
    }

    static boolean isProbaSynthesisProduct(String fileName) {
        return isProbaS1ToaProduct(fileName) || isProbaS1TocProduct(fileName) || isProbaS10TocProduct(fileName);
    }

    static boolean isProbaL1CProduct(String fileName) {
        return fileName.matches(PROBAV_L1C_FILENAME_REGEXP);
    }

    static boolean isProbaS10TocNdviProduct(String fileName) {
        return fileName.matches(PROBAV_S10_TOC_NDVI_FILENAME_REGEXP);
    }


    static Class<?> loadHdf5Lib(Class<?> callerClass) {
        return loadClassWithNativeDependencies(callerClass,
                                               _H5_CLASS_NAME,
                                               "{0}: HDF-5 library not available: {1}: {2}");
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
