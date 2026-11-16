package org.esa.snap.core.gpf.main;

import org.esa.snap.core.util.io.FileUtils;

import java.nio.file.Path;
import java.util.function.Predicate;

/**
 * @author Norman Fomferra
 */
class SourceMetadataFilenameFilter implements Predicate<Path> {
    private final String fileName;
    private final String wantedPrefix;

    public SourceMetadataFilenameFilter(String fileName) {
        this.fileName = fileName;
        this.wantedPrefix = FileUtils.getFilenameWithoutExtension(fileName) + "-";
    }

    @Override
    public boolean test(Path dir) {
        final String name = dir.getFileName().toString();
        if (!name.equalsIgnoreCase(fileName)) {
            return name.startsWith(wantedPrefix);
        }
        return false;
    }

    public String getMetadataBaseName(String fileName) {
        return fileName.substring(wantedPrefix.length());
    }
}
