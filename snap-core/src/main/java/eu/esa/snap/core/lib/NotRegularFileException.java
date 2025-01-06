package eu.esa.snap.core.lib;

import java.nio.file.FileSystemException;

/**
 * Created by jcoravu on 16/4/2019.
 */
public class NotRegularFileException extends FileSystemException {

    public NotRegularFileException(String file) {
        super(file);
    }
}
