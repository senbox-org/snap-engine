package org.esa.snap.speclib.io;

import org.esa.snap.speclib.model.SpectralLibrary;

import java.io.IOException;
import java.nio.file.Path;


public interface SpectralLibraryIO {


    SpectralLibrary read(Path path) throws IOException;
    void write(SpectralLibrary library, Path path) throws IOException;
}
