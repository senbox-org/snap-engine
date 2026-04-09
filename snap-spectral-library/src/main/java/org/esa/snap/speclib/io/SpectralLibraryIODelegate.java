package org.esa.snap.speclib.io;


/**
 * SPI interface for individual spectral library format implementations.
 *
 * <p>Extends {@link SpectralLibraryIO} — a delegate is a full {@link SpectralLibraryIO}
 * for a specific format. Register concrete implementations in
 * {@code META-INF/services/org.esa.snap.speclib.io.SpectralLibraryIODelegate}.
 * The {@link CompositeSpectralLibraryIO} discovers and delegates to them.
 */
public interface SpectralLibraryIODelegate extends SpectralLibraryIO { }
