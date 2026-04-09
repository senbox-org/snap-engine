package org.esa.snap.speclib.io;

import org.esa.snap.speclib.model.SpectralLibrary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;


/**
 * Central I/O interface for spectral libraries.
 *
 * <p>Callers work exclusively against this interface and are
 * unaware of which format is actually handling a given file.
 *
 * <p>The single SPI entry registered in
 * {@code META-INF/services/org.esa.snap.speclib.io.SpectralLibraryIO} is
 * {@link CompositeSpectralLibraryIO}, which dispatches to the appropriate
 * {@link SpectralLibraryIODelegate} based on file extension.
 *
 * <p>To add support for a new format, implement {@link SpectralLibraryIODelegate}
 * and register it in {@code META-INF/services/org.esa.snap.speclib.io.SpectralLibraryIODelegate}.
 * No changes to this interface or to {@link CompositeSpectralLibraryIO} are required.
 */
public interface SpectralLibraryIO {

    SpectralLibrary read(Path path) throws IOException;
    void write(SpectralLibrary library, Path path) throws IOException;
    boolean canRead(Path path);
    boolean canWrite(Path path);
    List<String> getFileExtensions();
}
