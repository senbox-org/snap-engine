package org.esa.snap.jp2.reader;

import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.file.AbstractFile;
import org.esa.snap.engine_utilities.util.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
        ModuleMetadata moduleMetadata = SystemUtils.loadModuleMetadata(clazz);
        if (moduleMetadata == null) {
            throw new IOException("Unable to load version from module metadata");
        }
        String version = moduleMetadata.getVersion();
        String md5sum = getMD5sum(inputFile.toString());
        if (md5sum == null) {
            throw new IOException("Unable to get md5sum of path " + inputFile);
        }

        Path localCacheFolder = PathUtils.get(SystemUtils.getCacheDir(), "snap", "jp2-reader", version, md5sum);//, PathUtils.getFileNameWithoutExtension(inputFile).toLowerCase() + "_cached");
        if (!Files.exists(localCacheFolder)) {
            Files.createDirectories(localCacheFolder);
        }
        return localCacheFolder;
    }
}
