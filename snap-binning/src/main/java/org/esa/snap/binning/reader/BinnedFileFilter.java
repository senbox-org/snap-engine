package org.esa.snap.binning.reader;

import java.nio.file.Files;
import java.nio.file.Path;
import org.esa.snap.core.util.io.SnapFileFilter;

public class BinnedFileFilter extends SnapFileFilter {

    public BinnedFileFilter() {
        super(BinnedProductReaderPlugin.FORMAT_NAME,
              BinnedProductReaderPlugin.FILE_EXTENSION,
              BinnedProductReaderPlugin.FORMAT_DESCRIPTION);
    }

    @Override
    public boolean accept(Path file) {
        if (Files.isDirectory(file)) {
            return true;    // needed to be able to browse through directories when used from VISAT tb 2013-07-29
        }

        if (super.accept(file)) {
            return isBinnedName(file.getFileName().toString());
        }

        return false;
    }

    /**
     * Checks if the given file name is valid.
     *
     * @param name the file name
     * @return true, if so.
     */
    public static boolean isBinnedName(String name) {
        return name != null
                && ((name.startsWith("ESACCI-OC-")
                && name.contains("L3"))
                || name.contains("-bins"))
                && name.endsWith(BinnedProductReaderPlugin.FILE_EXTENSION);
    }

}
