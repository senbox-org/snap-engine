package org.esa.snap.jp2.reader;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 5/11/2019.
 */
public interface JP2LocalFile {

    public Path getLocalFile() throws IOException;
}
