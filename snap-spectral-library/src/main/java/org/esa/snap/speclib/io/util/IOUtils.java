package org.esa.snap.speclib.io.util;

import java.nio.file.Path;
import java.util.Locale;

public class IOUtils {

    public static boolean hasExtension(Path path, String ext) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith("." + ext);
    }

    public static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
