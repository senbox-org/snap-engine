package org.esa.snap.speclib.io;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.*;


public class CompositeSpectralLibraryIOTest {


    @Test
    @STTM("SNAP-4177")
    public void test_defaultConstructor_createsInstanceWithDelegatesFromServiceLoader() {
        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO();

        assertNotNull(composite);
        assertNotNull(composite.getDelegates());
        assertNotNull(composite.getFileExtensions());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_canRead_matchingDelegate_returnsTrue() {
        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO(
                List.of(stubDelegate("geojson", dummyLibrary()))
        );
        assertTrue(composite.canRead(Paths.get("library.geojson")));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_canRead_noMatchingDelegate_returnsFalse() {
        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO(
                List.of(stubDelegate("geojson", dummyLibrary()))
        );
        assertFalse(composite.canRead(Paths.get("library.hdr")));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_canWrite_matchingDelegate_returnsTrue() {
        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO(
                List.of(stubDelegate("hdr", dummyLibrary()))
        );
        assertTrue(composite.canWrite(Paths.get("library.hdr")));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_canWrite_noMatchingDelegate_returnsFalse() {
        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO(
                List.of(stubDelegate("hdr", dummyLibrary()))
        );
        assertFalse(composite.canWrite(Paths.get("library.geojson")));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_read_delegatesToMatchingFormat() throws IOException {
        SpectralLibrary expected = dummyLibrary();
        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO(
                List.of(stubDelegate("geojson", expected))
        );

        SpectralLibrary result = composite.read(Paths.get("lib.geojson"));
        assertSame(expected, result);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_firstMatchingDelegateWins() throws IOException {
        SpectralLibrary first  = dummyLibrary();
        SpectralLibrary second = dummyLibrary();
        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO(
                List.of(stubDelegate("geojson", first), stubDelegate("geojson", second))
        );

        assertSame(first, composite.read(Paths.get("lib.geojson")));
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_noMatchingDelegate_throwsIOException() throws IOException {
        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO(
                List.of(stubDelegate("hdr", dummyLibrary()))
        );
        composite.read(Paths.get("lib.geojson"));
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_noDelegates_throwsIOException() throws IOException {
        new CompositeSpectralLibraryIO(List.of()).read(Paths.get("lib.geojson"));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_write_delegatesToMatchingFormat() throws IOException {
        boolean[] called = {false};
        SpectralLibraryIODelegate delegate = new SpectralLibraryIODelegate() {
            @Override
            public boolean canRead(Path p)  {
                return true;
            }
            @Override
            public boolean canWrite(Path p) {
                return true;
            }
            @Override
            public List<String> getFileExtensions() {
                return List.of("geojson");
            }
            @Override
            public SpectralLibrary read(Path p) {
                return null;
            }
            @Override
            public void write(SpectralLibrary lib, Path p) {
                called[0] = true;
            }
        };

        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO(List.of(delegate));
        composite.write(dummyLibrary(), Paths.get("lib.geojson"));
        assertTrue(called[0]);
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_write_noMatchingDelegate_throwsIOException() throws IOException {
        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO(
                List.of(stubDelegate("hdr", dummyLibrary()))
        );
        composite.write(dummyLibrary(), Paths.get("lib.geojson"));
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_write_noDelegates_throwsIOException() throws IOException {
        new CompositeSpectralLibraryIO(List.of()).write(dummyLibrary(), Paths.get("lib.geojson"));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_getFileExtensions_aggregatesAllDelegates() {
        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO(List.of(
                stubDelegate("hdr", null), stubDelegate("geojson", null))
        );
        List<String> exts = composite.getFileExtensions();

        assertTrue(exts.contains("hdr"));
        assertTrue(exts.contains("geojson"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_getFileExtensions_noDelegates_returnsEmptyList() {
        assertTrue(new CompositeSpectralLibraryIO(List.of()).getFileExtensions().isEmpty());
    }


    @Test
    @STTM("SNAP-4177")
    public void test_getDelegates_returnsAllRegistered() {
        SpectralLibraryIODelegate d1 = stubDelegate("hdr", null);
        SpectralLibraryIODelegate d2 = stubDelegate("geojson", null);

        CompositeSpectralLibraryIO composite = new CompositeSpectralLibraryIO(List.of(d1, d2));
        assertEquals(List.of(d1, d2), composite.getDelegates());
    }


    private static SpectralLibraryIODelegate stubDelegate(String ext, SpectralLibrary returnOnRead) {
        return new SpectralLibraryIODelegate() {
            @Override
            public boolean canRead(Path p)  {
                return name(p).endsWith("." + ext);
            }
            @Override
            public boolean canWrite(Path p) {
                return name(p).endsWith("." + ext);
            }
            @Override
            public List<String> getFileExtensions() {
                return List.of(ext);
            }
            @Override
            public SpectralLibrary read(Path p) {
                return returnOnRead;
            }
            @Override
            public void write(SpectralLibrary lib, Path p) {}
            private String name(Path p) {
                return p.getFileName().toString().toLowerCase();
            }
        };
    }

    private static SpectralLibrary dummyLibrary() {
        SpectralAxis axis = new SpectralAxis(new double[]{400.0}, "nm");
        return SpectralLibrary.create("dummy", axis, null);
    }
}