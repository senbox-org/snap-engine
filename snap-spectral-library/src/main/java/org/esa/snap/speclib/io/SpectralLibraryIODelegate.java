package org.esa.snap.speclib.io;

import org.esa.snap.speclib.model.SpectralLibrary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface SpectralLibraryIODelegate {

    SpectralLibrary read(Path path) throws IOException;
    void write(SpectralLibrary library, Path path) throws IOException;
    boolean canRead(Path path);
    boolean canWrite(Path path);
    List<String> getFileExtensions();
}
