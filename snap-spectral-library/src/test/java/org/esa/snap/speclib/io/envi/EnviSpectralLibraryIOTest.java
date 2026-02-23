package org.esa.snap.speclib.io.envi;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.dataio.envi.EnviConstants;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.junit.Test;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;


public class EnviSpectralLibraryIOTest {


    private static final String FILE_TYPE_SLI = "ENVI Spectral Library";

    @Test
    @STTM("SNAP-4129")
    public void test_nullGuards() {
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();

        assertThrows(NullPointerException.class, () -> io.read(null));
        assertThrows(NullPointerException.class, () -> io.write(null, Path.of("x.hdr")));

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, null);
        assertThrows(NullPointerException.class, () -> io.write(lib, null));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_write_pathWithoutHdrExtension_createsHdrAndSli() throws Exception {
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, null)
                .withProfileAdded(SpectralProfile.create("P", SpectralSignature.of(new double[]{1, 2})));

        Path dir = Files.createTempDirectory("sli-path");
        Path base = dir.resolve("mylib");

        io.write(lib, base);

        assertTrue(Files.exists(dir.resolve("mylib.hdr")));
        assertTrue(Files.exists(dir.resolve("mylib.sli")));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_pathWithoutHdrExtension_resolvesHdr() throws Exception {
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();

        Path dir = Files.createTempDirectory("sli-readpath");
        Path hdr = dir.resolve("x.hdr");
        Path sli = dir.resolve("x.sli");

        writeHdr(hdr,
                "samples", "2",
                "lines", "1",
                "bands", "1",
                "header offset", "0",
                "data type", String.valueOf(EnviConstants.TYPE_ID_FLOAT64),
                "byte order", "0",
                "wavelength units", "nm",
                "wavelength", "{1,2}",
                "spectra names", "{A}",
                "data ignore value", "-9999"
        );
        try (ImageOutputStream out = new FileImageOutputStream(sli.toFile())) {
            out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            out.writeDouble(1.0);
            out.writeDouble(2.0);
        }

        assertEquals(1, io.read(dir.resolve("x")).size());
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_missingHeader_throws() throws Exception {
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        Path dir = Files.createTempDirectory("sli-nohdr");
        Path missing = dir.resolve("missing.hdr");
        assertThrows(IOException.class, () -> io.read(missing));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_rejectsBandsNotOne() throws Exception {
        Path dir = Files.createTempDirectory("sli-bands");
        Path hdr = dir.resolve("x.hdr");
        writeHdr(hdr,
                "samples", "1",
                "lines", "1",
                "bands", "2",
                "data type", String.valueOf(EnviConstants.TYPE_ID_FLOAT64),
                "file type", FILE_TYPE_SLI,
                "wavelength", "{1}",
                "wavelength units", "nm",
                "spectra names", "{A}"
        );
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        assertThrows(IOException.class, () -> io.read(hdr));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_wavelengthCountMismatch_throws() throws Exception {
        Path dir = Files.createTempDirectory("sli-wlm");
        Path hdr = dir.resolve("x.hdr");
        writeHdr(hdr,
                "samples", "2",
                "lines", "1",
                "bands", "1",
                "data type", String.valueOf(EnviConstants.TYPE_ID_FLOAT64),
                "file type", FILE_TYPE_SLI,
                "wavelength", "{1}", // mismatch
                "wavelength units", "nm",
                "spectra names", "{A}"
        );
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        assertThrows(IOException.class, () -> io.read(hdr));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_defaultSpectraNamesWhenMissing() throws Exception {
        Path dir = Files.createTempDirectory("sli-defnames");
        Path hdr = dir.resolve("x.hdr");
        Path sli = dir.resolve("x.sli");

        writeHdr(hdr,
                "samples", "2",
                "lines", "2",
                "bands", "1",
                "data type", String.valueOf(EnviConstants.TYPE_ID_FLOAT64),
                "byte order", "0",
                "header offset", "0",
                "file type", FILE_TYPE_SLI,
                "wavelength", "{1,2}",
                "wavelength units", "nm",
                "data ignore value", "-9999"
        );
        try (ImageOutputStream out = new FileImageOutputStream(sli.toFile())) {
            out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            out.writeDouble(1); out.writeDouble(2);
            out.writeDouble(3); out.writeDouble(4);
        }

        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        SpectralLibrary lib = io.read(hdr);
        assertEquals("Spectrum_1", lib.getProfiles().get(0).getName());
        assertEquals("Spectrum_2", lib.getProfiles().get(1).getName());
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_spectraNamesCountMismatch_throws() throws Exception {
        Path dir = Files.createTempDirectory("sli-nmismatch");
        Path hdr = dir.resolve("x.hdr");
        writeHdr(hdr,
                "samples", "1",
                "lines", "2",
                "bands", "1",
                "data type", String.valueOf(EnviConstants.TYPE_ID_FLOAT64),
                "file type", FILE_TYPE_SLI,
                "wavelength", "{1}",
                "wavelength units", "nm",
                "spectra names", "{OnlyOne}"
        );
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        assertThrows(IOException.class, () -> io.read(hdr));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_missingWavelengthUnits_isToleratedAsEmptyString() throws Exception {
        Path dir = Files.createTempDirectory("sli-wlunit");
        Path hdr = dir.resolve("x.hdr");
        Path sli = dir.resolve("x.sli");

        writeHdr(hdr,
                "samples", "1",
                "lines", "1",
                "bands", "1",
                "data type", String.valueOf(EnviConstants.TYPE_ID_FLOAT64),
                "byte order", "0",
                "header offset", "0",
                "file type", FILE_TYPE_SLI,
                "wavelength", "{1}",
                "spectra names", "{A}"
        );
        try (ImageOutputStream out = new FileImageOutputStream(sli.toFile())) {
            out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            out.writeDouble(7);
        }

        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        SpectralLibrary lib = io.read(hdr);
        assertEquals("", lib.getAxis().getXUnit());
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_missingDataFile_throws() throws Exception {
        Path dir = Files.createTempDirectory("sli-nodata");
        Path hdr = dir.resolve("x.hdr");
        writeHdr(hdr,
                "samples", "1",
                "lines", "1",
                "bands", "1",
                "data type", String.valueOf(EnviConstants.TYPE_ID_FLOAT64),
                "byte order", "0",
                "header offset", "0",
                "file type", FILE_TYPE_SLI,
                "wavelength", "{1}",
                "wavelength units", "nm",
                "spectra names", "{A}"
        );
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        assertThrows(IOException.class, () -> io.read(hdr));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_unsupportedDataType_throws() throws Exception {
        Path dir = Files.createTempDirectory("sli-udt");
        Path hdr = dir.resolve("x.hdr");
        writeHdr(hdr,
                "samples", "1",
                "lines", "1",
                "bands", "1",
                "data type", "999",
                "file type", FILE_TYPE_SLI,
                "wavelength", "{1}",
                "wavelength units", "nm",
                "spectra names", "{A}"
        );
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        assertThrows(IOException.class, () -> io.read(hdr));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_headerOffsetIsRespected() throws Exception {
        Path dir = Files.createTempDirectory("sli-offset");
        Path hdr = dir.resolve("x.hdr");
        Path sli = dir.resolve("x.sli");

        writeHdr(hdr,
                "samples", "1",
                "lines", "1",
                "bands", "1",
                "data type", String.valueOf(EnviConstants.TYPE_ID_FLOAT64),
                "byte order", "0",
                "header offset", "8",
                "file type", FILE_TYPE_SLI,
                "wavelength", "{1}",
                "wavelength units", "nm",
                "spectra names", "{A}"
        );

        try (ImageOutputStream out = new FileImageOutputStream(sli.toFile())) {
            out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            out.writeLong(123456L);
            out.writeDouble(9.0);
        }

        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        SpectralLibrary lib = io.read(hdr);
        assertEquals(9.0, lib.getProfiles().get(0).getSignature().getValues()[0], 1e-12);
    }

    @Test
    @STTM("SNAP-4129")
    public void test_readValueAsDouble_allTypeBranches() throws Exception {
        typeRoundTrip(EnviConstants.TYPE_ID_FLOAT64, ByteOrder.LITTLE_ENDIAN, (out) -> out.writeDouble(1.5), 1.5);
        typeRoundTrip(EnviConstants.TYPE_ID_FLOAT32, ByteOrder.LITTLE_ENDIAN, (out) -> out.writeFloat(2.5f), 2.5);
        typeRoundTrip(EnviConstants.TYPE_ID_INT32,   ByteOrder.LITTLE_ENDIAN, (out) -> out.writeInt(-7), -7.0);
        typeRoundTrip(EnviConstants.TYPE_ID_UINT32,  ByteOrder.LITTLE_ENDIAN, (out) -> out.writeInt(-1), 4294967295.0);
        typeRoundTrip(EnviConstants.TYPE_ID_INT16,   ByteOrder.LITTLE_ENDIAN, (out) -> out.writeShort((short) -3), -3.0);
        typeRoundTrip(EnviConstants.TYPE_ID_UINT16,  ByteOrder.LITTLE_ENDIAN, (out) -> out.writeShort((short) 65535), 65535.0);
        typeRoundTrip(EnviConstants.TYPE_ID_BYTE,    ByteOrder.LITTLE_ENDIAN, (out) -> out.writeByte(255), 255.0);
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_mapsDefaultNoDataWhenHeaderMissingDataIgnoreValue() throws Exception {
        Path dir = Files.createTempDirectory("sli-nodata-default");
        Path hdr = dir.resolve("x.hdr");
        Path sli = dir.resolve("x.sli");

        writeHdr(hdr,
                "samples", "1",
                "lines", "1",
                "bands", "1",
                "data type", String.valueOf(EnviConstants.TYPE_ID_FLOAT64),
                "byte order", "0",
                "header offset", "0",
                "file type", FILE_TYPE_SLI,
                "wavelength", "{1}",
                "wavelength units", "nm",
                "spectra names", "{A}"
        );

        try (ImageOutputStream out = new FileImageOutputStream(sli.toFile())) {
            out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            out.writeDouble(-9999.0);
        }

        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        SpectralLibrary lib = io.read(hdr);
        assertTrue(Double.isNaN(lib.getProfiles().get(0).getSignature().getValues()[0]));
    }


    @Test
    @STTM("SNAP-4129")
    public void test_read_rejectsNullFileType() throws Exception {
        Path dir = Files.createTempDirectory("sli-nullfiletype");
        Path hdr = dir.resolve("x.hdr");
        writeHdr(hdr,
                "samples", "1",
                "lines", "1",
                "bands", "1",
                "data type", String.valueOf(EnviConstants.TYPE_ID_FLOAT64),
                "wavelength", "{1}",
                "wavelength units", "nm",
                "spectra names", "{A}"
        );

        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        assertThrows(IOException.class, () -> io.read(hdr));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_rejectsWrongFileType() throws Exception {
        Path dir = Files.createTempDirectory("sli-wrongfiletype");
        Path hdr = dir.resolve("x.hdr");
        Files.writeString(hdr,
                "ENVI\n" +
                        "file type = ENVI\n" + // wrong
                        "samples = 1\nlines = 1\nbands = 1\n" +
                        "data type = 5\nbyte order = 0\nheader offset = 0\n" +
                        "wavelength = {1}\n" +
                        "wavelength units = nm\n" +
                        "spectra names = {A}\n",
                StandardCharsets.UTF_8);

        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        assertThrows(IOException.class, () -> io.read(hdr));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_write_replacesNaNAndInfinityWithNoData() throws Exception {
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();

        SpectralAxis axis = new SpectralAxis(new double[]{1, 2}, "nm");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, null)
                .withProfileAdded(SpectralProfile.create("P", SpectralSignature.of(new double[]{Double.NaN, Double.POSITIVE_INFINITY})));

        Path dir = Files.createTempDirectory("sli-naninf");
        Path hdr = dir.resolve("x.hdr");
        Path sli = dir.resolve("x.sli");

        io.write(lib, hdr);

        try (var in = new FileImageInputStream(sli.toFile())) {
            in.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            double a = in.readDouble();
            double b = in.readDouble();
            assertEquals(-9999.0, a, 0.0);
            assertEquals(-9999.0, b, 0.0);
        }
    }

    @Test
    @STTM("SNAP-4129")
    public void test_resolveDataPath_whenPathEndsWithSli_returnsSamePath() throws Exception {
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();

        SpectralAxis axis = new SpectralAxis(new double[]{1}, "nm");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, null)
                .withProfileAdded(SpectralProfile.create("P", SpectralSignature.of(new double[]{1})));

        Path dir = Files.createTempDirectory("sli-pathsli");
        Path sliPath = dir.resolve("x.sli"); // ends with .sli

        io.write(lib, sliPath);

        assertTrue(Files.exists(dir.resolve("x.hdr")));
        assertTrue(Files.exists(sliPath));
    }


    // -------- helpers --------


    private interface WriterFn {
        void write(ImageOutputStream out) throws IOException;
    }

    private void typeRoundTrip(int enviType, ByteOrder order, WriterFn writer, double expected) throws Exception {
        EnviSpectralLibraryIO io = new EnviSpectralLibraryIO();
        Path dir = Files.createTempDirectory("sli-type-" + enviType);
        Path hdr = dir.resolve("x.hdr");
        Path sli = dir.resolve("x.sli");

        String bo = (order == ByteOrder.BIG_ENDIAN) ? "1" : "0";

        writeHdr(hdr,
                "samples", "1",
                "lines", "1",
                "bands", "1",
                "data type", String.valueOf(enviType),
                "byte order", bo,
                "header offset", "0",
                "file type", FILE_TYPE_SLI,
                "wavelength", "{1}",
                "wavelength units", "nm",
                "spectra names", "{A}",
                "data ignore value", "-9999"
        );

        try (ImageOutputStream out = new FileImageOutputStream(sli.toFile())) {
            out.setByteOrder(order);
            writer.write(out);
        }

        SpectralLibrary lib = io.read(hdr);
        assertEquals(expected, lib.getProfiles().get(0).getSignature().getValues()[0], 1e-6);
    }

    private static void writeHdr(Path hdr, String... kv) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("ENVI\n");

        boolean hasFileType = false;
        for (int i = 0; i < kv.length; i += 2) {
            if ("file type".equals(kv[i])) hasFileType = true;
        }
        if (!hasFileType) {
            sb.append("file type = ").append(FILE_TYPE_SLI).append("\n");
        }
        for (int i = 0; i < kv.length; i += 2) {
            String key = kv[i];
            String value = kv[i + 1];
            if ("wavelength".equals(key) || "spectra names".equals(key)) {
                sb.append(key).append(" = ").append(value).append("\n");
            } else {
                sb.append(key).append(" = ").append(value).append("\n");
            }
        }
        Files.writeString(hdr, sb.toString(), StandardCharsets.UTF_8);
    }
}