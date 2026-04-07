package org.esa.snap.speclib.io.envi;

import org.esa.snap.dataio.envi.DataTypeUtils;
import org.esa.snap.dataio.envi.EnviConstants;
import org.esa.snap.dataio.envi.EnviProductReader;
import org.esa.snap.dataio.envi.Header;
import org.esa.snap.dataio.envi.HeaderParser;
import org.esa.snap.speclib.io.SpectralLibraryIODelegate;
import org.esa.snap.speclib.model.AttributeSchema;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;


public class EnviSpectralLibraryIO implements SpectralLibraryIODelegate {


    private static final List<String> EXTENSIONS = List.of("hdr", "sli");
    private static final String FILE_TYPE_SLI = "ENVI Spectral Library";
    private static final String KEY_SPECTRA_NAMES = "spectra names";
    private static final double DEFAULT_NODATA = -9999.0;


    @Override
    public SpectralLibrary read(Path path) throws IOException {
        Objects.requireNonNull(path, "path must not be null");

        Path hdrPath = resolveHdrPath(path);
        File hdrFile = hdrPath.toFile();
        if (!Files.exists(hdrPath)) {
            throw new IOException("ENVI header not found: " + hdrPath);
        }

        Header header;
        HeaderParser parser;
        try (BufferedReader br = Files.newBufferedReader(hdrPath, StandardCharsets.UTF_8)) {
            header = new Header(br);
        }
        try (BufferedReader br = Files.newBufferedReader(hdrPath, StandardCharsets.UTF_8)) {
            parser = HeaderParser.parse(br);
        }

        String fileType = header.getFileType();
        if (fileType == null || !FILE_TYPE_SLI.equalsIgnoreCase(fileType.trim())) {
            throw new IOException("Not an ENVI Spectral Library (file type): " + fileType);
        }

        int samples = header.getNumSamples();
        int lines = header.getNumLines();
        int bands = header.getNumBands();
        if (bands != 1) {
            throw new IOException("Invalid SLI header: bands must be 1 but was " + bands);
        }

        String[] wlStr = header.getWavelengths();
        if (wlStr == null || wlStr.length != samples) {
            throw new IOException("Invalid SLI header: wavelength count must match samples");
        }
        double[] wavelengths = new double[samples];
        for (int i = 0; i < samples; i++) {
            wavelengths[i] = Double.parseDouble(wlStr[i]);
        }
        String wavelengthUnit = header.getWavelengthsUnit();
        if (wavelengthUnit == null) {
            wavelengthUnit = "";
        }
        SpectralAxis axis = new SpectralAxis(wavelengths, wavelengthUnit);

        String[] spectraNames = parser.getStrings(KEY_SPECTRA_NAMES);
        if (spectraNames.length == 0) {
            spectraNames = new String[lines];
            for (int i = 0; i < lines; i++) {
                spectraNames[i] = "Spectrum_" + (i + 1);
            }
        } else if (spectraNames.length != lines) {
            throw new IOException("Invalid SLI header: spectra names count must match lines");
        }

        int enviTypeId = header.getDataType();
        int elemSize = DataTypeUtils.getSizeInBytes(enviTypeId);
        if (elemSize <= 0) {
            throw new IOException("Unsupported ENVI data type: " + enviTypeId);
        }

        Double nodata = header.getDataIgnoreValue();
        double nodataVal = nodata != null ? nodata : DEFAULT_NODATA;

        ByteOrder order = header.getJavaByteOrder();
        int offset = header.getHeaderOffset();

        File dataFile = resolveDataFile(hdrFile);
        if (!dataFile.exists()) {
            throw new IOException("ENVI data file not found for header: " + hdrFile.getPath());
        }

        List<SpectralProfile> profiles = new ArrayList<>(lines);
        try (ImageInputStream in = new FileImageInputStream(dataFile)) {
            in.setByteOrder(order);
            if (offset > 0) {
                in.seek(offset);
            }

            for (int y = 0; y < lines; y++) {
                double[] values = new double[samples];
                for (int x = 0; x < samples; x++) {
                    double v = readValueAsDouble(in, enviTypeId);
                    if (Double.compare(v, nodataVal) == 0) {
                        v = Double.NaN;
                    }
                    values[x] = v;
                }
                SpectralSignature sig = SpectralSignature.of(values);
                SpectralProfile p = SpectralProfile.create(spectraNames[y], sig);
                profiles.add(p);
            }
        }

        String libName = stripExtension(hdrPath.getFileName().toString());
        SpectralLibrary lib = new SpectralLibrary(UUID.randomUUID(), libName, axis, null, profiles, new AttributeSchema());

        return EnviCsvSidecarSupport.mergeIfPresent(lib, hdrPath);
    }

    @Override
    public void write(SpectralLibrary library, Path path) throws IOException {
        Objects.requireNonNull(library, "library must not be null");
        Objects.requireNonNull(path, "path must not be null");

        Path hdrPath = resolveHdrPath(path);
        Path dataPath = resolveDataPath(path);

        Files.createDirectories(hdrPath.toAbsolutePath().getParent());

        int enviTypeId = EnviConstants.TYPE_ID_FLOAT64;
        int samples = library.getAxis().size();
        int lines = library.size();
        int bands = 1;
        int headerOffset = 0;
        int byteOrder = 0;

        double nodataVal = DEFAULT_NODATA;

        try (BufferedWriter w = Files.newBufferedWriter(hdrPath, StandardCharsets.UTF_8)) {
            w.write(EnviConstants.FIRST_LINE);
            w.newLine();

            writeKeyValue(w, EnviConstants.HEADER_KEY_DESCRIPTION, "{Spectral Library (SNAP)}");
            writeKeyValue(w, EnviConstants.HEADER_KEY_SAMPLES, String.valueOf(samples));
            writeKeyValue(w, EnviConstants.HEADER_KEY_LINES, String.valueOf(lines));
            writeKeyValue(w, EnviConstants.HEADER_KEY_BANDS, String.valueOf(bands));
            writeKeyValue(w, EnviConstants.HEADER_KEY_HEADER_OFFSET, String.valueOf(headerOffset));
            writeKeyValue(w, EnviConstants.HEADER_KEY_FILE_TYPE, FILE_TYPE_SLI);
            writeKeyValue(w, EnviConstants.HEADER_KEY_DATA_TYPE, String.valueOf(enviTypeId));
            writeKeyValue(w, EnviConstants.HEADER_KEY_INTERLEAVE, "bsq");
            writeKeyValue(w, EnviConstants.HEADER_KEY_BYTE_ORDER, String.valueOf(byteOrder));
            writeKeyValue(w, EnviConstants.HEADER_KEY_DATA_IGNORE_VALUE, formatDouble(nodataVal));

            writeKeyValue(w, EnviConstants.HEADER_KEY_WAVELENGTH_UNITS, library.getAxis().getXUnit());

            writeBraceList(w, EnviConstants.HEADER_KEY_WAVELENGTH, library.getAxis().getWavelengths());
            writeBraceList(w, KEY_SPECTRA_NAMES, library.getProfiles().stream().map(SpectralProfile::getName).toArray(String[]::new));
        }

        try (ImageOutputStream out = new FileImageOutputStream(dataPath.toFile())) {
            out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            for (SpectralProfile p : library.getProfiles()) {
                double[] v = p.getSignature().getValues();
                if (v.length != samples) {
                    throw new IOException("Profile length does not match library axis: " + p.getName());
                }
                for (int i = 0; i < v.length; i++) {
                    double vv = v[i];
                    if (Double.isNaN(vv) || Double.isInfinite(vv)) {
                        vv = nodataVal;
                    }
                    out.writeDouble(vv);
                }
            }
            out.flush();
        }

        EnviCsvSidecarSupport.writeIfNeeded(library, hdrPath);
    }

    @Override
    public boolean canRead(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(EXTENSIONS.getFirst()) || name.endsWith(EXTENSIONS.get(1));
    }

    @Override
    public boolean canWrite(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(EXTENSIONS.getFirst()) || name.endsWith(EXTENSIONS.get(1));
    }

    @Override
    public List<String> getFileExtensions() {
        return EXTENSIONS;
    }

    private static Path resolveHdrPath(Path path) {
        String name = path.getFileName().toString();
        if (name.toLowerCase(Locale.ROOT).endsWith(EnviConstants.HDR_EXTENSION)) {
            return path;
        }
        return path.resolveSibling(stripExtension(name) + EnviConstants.HDR_EXTENSION);
    }

    private static Path resolveDataPath(Path path) {
        String name = path.getFileName().toString();
        String base = stripExtension(name);

        if (name.toLowerCase(Locale.ROOT).endsWith(".sli")) {
            return path;
        }
        return path.resolveSibling(base + ".sli");
    }

    private static File resolveDataFile(File hdrFile) throws IOException {
        String base = stripExtension(hdrFile.getName());
        File siblingSli = new File(hdrFile.getParentFile(), base + ".sli");
        if (siblingSli.exists()) {
            return siblingSli;
        }

        return EnviProductReader.getEnviImageFile(hdrFile);
    }

    private static double readValueAsDouble(ImageInputStream in, int enviTypeId) throws IOException {
        return switch (enviTypeId) {
            case EnviConstants.TYPE_ID_FLOAT64 -> in.readDouble();
            case EnviConstants.TYPE_ID_FLOAT32 -> in.readFloat();
            case EnviConstants.TYPE_ID_INT32 -> in.readInt();
            case EnviConstants.TYPE_ID_UINT32 -> Integer.toUnsignedLong(in.readInt());
            case EnviConstants.TYPE_ID_INT16 -> in.readShort();
            case EnviConstants.TYPE_ID_UINT16 -> Short.toUnsignedInt(in.readShort());
            case EnviConstants.TYPE_ID_BYTE -> in.readUnsignedByte();
            default -> throw new IOException("Unsupported ENVI data type: " + enviTypeId);
        };
    }

    private static void writeKeyValue(BufferedWriter w, String key, String value) throws IOException {
        w.write(key);
        w.write(" = ");
        w.write(value);
        w.newLine();
    }

    private static void writeBraceList(BufferedWriter w, String key, double[] values) throws IOException {
        w.write(key);
        w.write(" = {");
        w.newLine();
        int perLine = 12;
        for (int i = 0; i < values.length; i++) {
            w.write("  ");
            w.write(formatDouble(values[i]));
            if (i < values.length - 1) {
                w.write(",");
            }
            if ((i + 1) % perLine == 0 || i == values.length - 1) {
                w.newLine();
            }
        }
        w.write("}");
        w.newLine();
    }

    private static void writeBraceList(BufferedWriter w, String key, String[] values) throws IOException {
        w.write(key);
        w.write(" = {");
        w.newLine();
        int perLine = 6;
        for (int i = 0; i < values.length; i++) {
            w.write("  ");
            w.write(values[i]);
            if (i < values.length - 1) {
                w.write(",");
            }
            if ((i + 1) % perLine == 0 || i == values.length - 1) {
                w.newLine();
            }
        }
        w.write("}");
        w.newLine();
    }

    private static String formatDouble(double d) {
        return Double.toString(d);
    }

    private static String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
