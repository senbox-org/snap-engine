package org.esa.snap.jp2.reader;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 5/11/2019.
 */
public class JP2ImageFile {

    private final JP2LocalFile localFile;

    private Path file;

    public JP2ImageFile(JP2LocalFile localFile) {
        this.localFile = localFile;
    }

    public Path getLocalFile() throws IOException {
        if (this.file == null) {
            this.file = this.localFile.getLocalFile();
            if (this.file == null) {
                throw new NullPointerException("The file is null.");
            }
        }
        return this.file;
    }
}
