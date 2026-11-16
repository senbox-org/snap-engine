package org.esa.snap.engine_utilities.commons;

import com.bc.ceres.core.VirtualDir;
import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.util.CleanUpState;
import com.bc.ceres.util.CleanerRegistry;
import org.esa.snap.engine_utilities.file.AbstractFile;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 14/5/2019.
 */
public class VirtualFile extends AbstractFile implements Closeable {

    private final VirtualFileState fileState;

    public VirtualFile(Path file) {
        super(file);
        this.fileState = new VirtualFileState();
        CleanerRegistry.getInstance().register(this, fileState);
    }

    @Override
    protected Path getLocalTempFolder() throws IOException {
        if (fileState.localTempFolder == null) {
            fileState.localTempFolder = VirtualDir.createUniqueTempDir();
        }
        return fileState.localTempFolder;
    }

    @Override
    public void close() {
        CleanerRegistry.getInstance().cleanup(this);
    }


    private static final class VirtualFileState implements CleanUpState {
        private Path localTempFolder;

        @Override
        public synchronized void run() {
            if (localTempFolder != null) {
                IOHelper.deleteFileTree(localTempFolder);
                localTempFolder = null;
            }
        }
    }
}
