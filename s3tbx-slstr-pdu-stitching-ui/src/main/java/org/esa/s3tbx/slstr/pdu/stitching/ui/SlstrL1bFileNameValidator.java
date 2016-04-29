package org.esa.s3tbx.slstr.pdu.stitching.ui;

import java.io.File;
import java.util.regex.Pattern;

/**
 * @author Tonio Fincke
 */
class SlstrL1bFileNameValidator {

    private static final Pattern DIRECTORY_NAME_PATTERN = Pattern.compile("S3.?_SL_1_RBT_.*(.SEN3)?");

    static boolean isValidSlstrL1BFile(File f) {
        return (f.getName().equals("xfdumanifest.xml") && isValidDirectoryName(f.getParentFile().getName()) ||
                f.isDirectory());
    }

    static boolean isValidDirectoryName(String name) {
        return DIRECTORY_NAME_PATTERN.matcher(name).matches();
    }

}
