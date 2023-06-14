package org.esa.snap.engine_utilities.commons;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.engine_utilities.file.AbstractFile;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 14/5/2019.
 */
public class VirtualFile extends AbstractFile implements Closeable {

    private File localTempFolder;

    public VirtualFile(Path file) {
        super(file);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        cleanup();
    }

    @Override
    protected Path getLocalTempFolder() throws IOException {
        if (this.localTempFolder == null) {
            this.localTempFolder = VirtualDir.createUniqueTempDir();
        }
        return this.localTempFolder.toPath();
    }

    @Override
    public void close() {
        cleanup();
    }

    private void cleanup() {
        if (this.localTempFolder != null) {
            VirtualDir.deleteFileTree(this.localTempFolder);
            this.localTempFolder = null;
        }
    }
}
