package org.esa.snap.engine_utilities.commons;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 30/4/2019.
 */
public class FilePath implements Closeable {

    private final Path path;
    private final Closeable closeable;

    public FilePath(Path path, Closeable closeable) {
        this.path = path;
        this.closeable = closeable;
    }

    @Override
    public void close() throws IOException {
        if (this.closeable != null) {
            this.closeable.close();
        }
    }

    public Path getPath() {
        return path;
    }
}
