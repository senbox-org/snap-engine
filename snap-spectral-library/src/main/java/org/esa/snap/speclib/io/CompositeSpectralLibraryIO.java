package org.esa.snap.speclib.io;

import org.esa.snap.speclib.model.SpectralLibrary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;


public class CompositeSpectralLibraryIO implements SpectralLibraryIO {


    private final List<SpectralLibraryIODelegate> delegates;


    public CompositeSpectralLibraryIO() {
        this.delegates = loadDelegates();
    }

    CompositeSpectralLibraryIO(List<SpectralLibraryIODelegate> delegates) {
        this.delegates = List.copyOf(delegates);
    }


    @Override
    public SpectralLibrary read(Path path) throws IOException {
        SpectralLibraryIODelegate delegate = findReadDelegate(path);
        if (delegate == null) {
            throw new IOException("No reader found for: " + path.getFileName());
        }
        return delegate.read(path);
    }

    @Override
    public void write(SpectralLibrary library, Path path) throws IOException {
        SpectralLibraryIODelegate delegate = findWriteDelegate(path);
        if (delegate == null) {
            throw new IOException("No writer found for: " + path.getFileName());
        }
        delegate.write(library, path);
    }

    @Override
    public boolean canRead(Path path) {
        return findReadDelegate(path) != null;
    }

    @Override
    public boolean canWrite(Path path) {
        return findWriteDelegate(path) != null;
    }

    @Override
    public List<String> getFileExtensions() {
        List<String> all = new ArrayList<>();
        for (SpectralLibraryIODelegate d : delegates) {
            all.addAll(d.getFileExtensions());
        }
        return Collections.unmodifiableList(all);
    }

    public List<SpectralLibraryIODelegate> getDelegates() {
        return Collections.unmodifiableList(delegates);
    }


    private SpectralLibraryIODelegate findReadDelegate(Path path) {
        for (SpectralLibraryIODelegate d : delegates) {
            if (d.canRead(path)) {
                return d;
            }
        }
        return null;
    }

    private SpectralLibraryIODelegate findWriteDelegate(Path path) {
        for (SpectralLibraryIODelegate d : delegates) {
            if (d.canWrite(path)) {
                return d;
            }
        }
        return null;
    }

    private static List<SpectralLibraryIODelegate> loadDelegates() {
        List<SpectralLibraryIODelegate> list = new ArrayList<>();
        for (SpectralLibraryIODelegate d : ServiceLoader.load(SpectralLibraryIODelegate.class)) {
            if (d != null) {
                list.add(d);
            }
        }
        return Collections.unmodifiableList(list);
    }
}
