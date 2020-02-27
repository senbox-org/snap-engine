package org.esa.snap.jp2.reader;

import org.esa.snap.engine_utilities.file.AbstractFile;
import org.esa.snap.engine_utilities.util.PathUtils;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.esa.snap.lib.openjpeg.dataio.Utils.getMD5sum;

/**
 * Created by jcoravu on 8/5/2019.
 */
public class VirtualJP2File extends AbstractFile implements JP2LocalFile {

    private final Path localCacheFolder;

    public VirtualJP2File(Path file, Class clazz) throws IOException {
        super(file);

        this.localCacheFolder = buildLocalCacheFolder(file, clazz);
    }

    @Override
    protected Path getLocalTempFolder() throws IOException {
        return this.localCacheFolder;
    }

    @Override
    public Path getLocalFile() throws IOException {
        return super.getLocalFile();
    }

    public Path getLocalCacheFolder() {
        return localCacheFolder;
    }

    public void deleteLocalFilesOnExit() throws IOException {
        List<Path> files = PathUtils.listFiles(this.localCacheFolder);
        this.localCacheFolder.toFile().deleteOnExit();
        if (files != null) {
            for (Path file : files) {
                file.toFile().deleteOnExit();
            }
        }
    }

    private static Path buildLocalCacheFolder(Path inputFile, Class clazz) throws IOException {
        Path versionFile = ResourceInstaller.findModuleCodeBasePath(clazz).resolve("version/version.properties");
        Properties versionProp = new Properties();

        try (InputStream inputStream = Files.newInputStream(versionFile)) {
            versionProp.load(inputStream);
        }

        String version = versionProp.getProperty("project.version");
        if (version == null) {
            throw new IOException("Unable to get project.version property from " + versionFile);
        }

        String md5sum = getMD5sum(inputFile.toString());
        if (md5sum == null) {
            throw new IOException("Unable to get md5sum of path " + inputFile.toString());
        }

        Path localCacheFolder = PathUtils.get(SystemUtils.getCacheDir(), "snap", "jp2-reader", version, md5sum, PathUtils.getFileNameWithoutExtension(inputFile).toLowerCase() + "_cached");
        if (!Files.exists(localCacheFolder)) {
            Files.createDirectories(localCacheFolder);
        }
        return localCacheFolder;
    }
}
