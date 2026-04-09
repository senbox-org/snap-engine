package org.esa.snap.speclib.io.csv;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.model.AttributeType;
import org.esa.snap.speclib.model.AttributeValue;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralLibrary;
import org.esa.snap.speclib.model.SpectralProfile;
import org.esa.snap.speclib.model.SpectralSignature;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;


public class CsvSpectralLibraryIOTest {


    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final CsvSpectralLibraryIO io = new CsvSpectralLibraryIO();


    @Test
    @STTM("SNAP-4177")
    public void test_canRead() throws Exception {
        Path f1 = write("lib.csv", "spectra,490.0,560.0\nP1,1.0,2.0\n");
        assertTrue(io.canRead(f1));

        Path f2 = write("sidecar.csv", "spectra names,wkt,class\nP1,,veg\n");
        assertFalse(io.canRead(f2));

        Path f3 = write("lib.geojson", "{}");
        assertFalse(io.canRead(f3));

        Path f4 = write("empty.csv", "");
        assertFalse(io.canRead(f4));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_canWrite() {
        assertTrue(io.canWrite(tmp.getRoot().toPath().resolve("out.csv")));
        assertFalse(io.canWrite(tmp.getRoot().toPath().resolve("out.geojson")));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_getFileExtensions_containsCsv() {
        assertTrue(io.getFileExtensions().contains("csv"));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_libraryName() throws Exception {
        Path f = write("my_library.csv", "spectra,490.0,560.0\nP1,1.0,2.0\n");
        assertEquals("my_library", io.read(f).getName());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_axis() throws Exception {
        Path f = write("lib.csv", "spectra,490.0,560.0,665.0\nP1,1.0,2.0,3.0\n");
        SpectralAxis axis = io.read(f).getAxis();

        assertArrayEquals(new double[]{490.0, 560.0, 665.0}, axis.getWavelengths(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_xUnit() throws Exception {
        Path f = write("lib.csv", "spectra,xUnit,490.0,560.0\nP1,nanometers,1.0,2.0\n");
        assertEquals("nanometers", io.read(f).getAxis().getXUnit());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_xUnitMissing_defaultsToEmpty() throws Exception {
        Path f = write("lib.csv", "spectra,490.0,560.0\nP1,1.0,2.0\n");
        assertEquals("", io.read(f).getAxis().getXUnit());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_profileCount() throws Exception {
        Path f = write("lib.csv", "spectra,490.0,560.0\nP1,1.0,2.0\nP2,3.0,4.0\n");
        assertEquals(2, io.read(f).size());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_profileNames() throws Exception {
        Path f = write("lib.csv", "spectra,490.0,560.0\nAlpha,1.0,2.0\nBeta,3.0,4.0\n");
        List<SpectralProfile> profiles = io.read(f).getProfiles();

        assertEquals("Alpha", profiles.get(0).getName());
        assertEquals("Beta",  profiles.get(1).getName());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_yValues() throws Exception {
        Path f = write("lib.csv", "spectra,490.0,560.0\nP1,0.1,0.2\n");
        double[] v = io.read(f).getProfiles().getFirst().getSignature().getValues();

        assertArrayEquals(new double[]{0.1, 0.2}, v, 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_emptyCell_becomesNaN() throws Exception {
        Path f = write("lib.csv", "spectra,490.0,560.0\nP1,,0.2\n");
        double[] v = io.read(f).getProfiles().getFirst().getSignature().getValues();

        assertTrue(Double.isNaN(v[0]));
        assertEquals(0.2, v[1], 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_nameColumnAcceptsNameKey() throws Exception {
        Path f = write("lib.csv", "name,490.0\nP1,1.0\n");
        assertEquals("P1", io.read(f).getProfiles().getFirst().getName());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_nameColumnAcceptsSpectraNamesKey() throws Exception {
        Path f = write("lib.csv", "spectra names,490.0\nP1,1.0\n");
        assertEquals("P1", io.read(f).getProfiles().getFirst().getName());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_noNameColumn_defaultsToSpectrumIndex() throws Exception {
        Path f = write("lib.csv", "class,490.0,560.0\nveg,1.0,2.0\nurban,3.0,4.0\n");
        List<SpectralProfile> profiles = io.read(f).getProfiles();

        assertEquals("Spectrum_1", profiles.get(0).getName());
        assertEquals("Spectrum_2", profiles.get(1).getName());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_wktAttribute() throws Exception {
        Path f = write("lib.csv", "spectra,wkt,490.0\nP1,POINT(13.0 52.0),1.0\n");
        AttributeValue wkt = io.read(f).getProfiles().getFirst().getAttributes().get("wkt");

        assertNotNull(wkt);
        assertEquals("POINT(13.0 52.0)", wkt.asString());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_customStringAttribute() throws Exception {
        Path f = write("lib.csv", "spectra,class,490.0\nP1,vegetation,1.0\n");
        assertEquals("vegetation", io.read(f).getProfiles().getFirst().getAttributes().get("class").asString());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_customIntAttribute() throws Exception {
        Path f = write("lib.csv", "spectra,count,490.0\nP1,42,1.0\n");
        assertEquals(42, io.read(f).getProfiles().getFirst().getAttributes().get("count").asInt());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_customDoubleAttribute() throws Exception {
        Path f = write("lib.csv", "spectra,score,490.0\nP1,0.95,1.0\n");
        assertEquals(0.95, io.read(f).getProfiles().getFirst().getAttributes().get("score").asDouble(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_customBooleanAttribute() throws Exception {
        Path f = write("lib.csv", "spectra,valid,490.0\nP1,true,1.0\n");
        assertTrue(io.read(f).getProfiles().getFirst().getAttributes().get("valid").asBoolean());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_byRow_instantAttribute() throws Exception {
        Path f = write("lib.csv", "spectra,ts,490.0\nP1,2024-01-01T00:00:00Z,1.0\n");
        AttributeValue ts = io.read(f).getProfiles().getFirst().getAttributes().get("ts");

        assertEquals(AttributeType.INSTANT, ts.getType());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_emptyAttributeCell_skipped() throws Exception {
        Path f = write("lib.csv", "spectra,class,490.0\nP1,,1.0\n");
        assertNull(io.read(f).getProfiles().getFirst().getAttributes().get("class"));
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_noWavelengthColumns_throws() throws Exception {
        Path f = write("lib.csv", "spectra,class\nP1,veg\n");
        io.read(f);
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4177")
    public void test_read_emptyHeader_throws() throws Exception {
        Path f = write("lib.csv", "");
        io.read(f);
    }


    @Test
    @STTM("SNAP-4177")
    public void test_read_byColumn_parsedCorrectly() throws Exception {
        String csv = "spectra,P1,P2\n490.0,1.0,2.0\n560.0,3.0,4.0\n";
        Path f = write("ecosis.csv", csv);
        SpectralLibrary lib = io.read(f);

        assertArrayEquals(new double[]{490.0, 560.0}, lib.getAxis().getWavelengths(), 1e-9);
        assertEquals(2, lib.size());
        assertEquals("P1", lib.getProfiles().get(0).getName());

        assertArrayEquals(new double[]{1.0, 3.0}, lib.getProfiles().get(0).getSignature().getValues(), 1e-9);
        assertEquals("P2", lib.getProfiles().get(1).getName());
        assertArrayEquals(new double[]{2.0, 4.0}, lib.getProfiles().get(1).getSignature().getValues(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_read_realEcosisExample() throws Exception {
        String csv = "spectra,AK01_ACRU_B,AK01_ACRU_M\n350,0.054,0.035\n351,0.05,0.052\n";
        Path f = write("ecosis_real.csv", csv);
        SpectralLibrary lib = io.read(f);

        assertArrayEquals(new double[]{350.0, 351.0}, lib.getAxis().getWavelengths(), 1e-9);
        assertEquals(2, lib.size());

        assertEquals(0.054, lib.getProfiles().get(0).getSignature().getValues()[0], 1e-9);
        assertEquals(0.052, lib.getProfiles().get(1).getSignature().getValues()[1], 1e-9);
    }


    @Test
    @STTM("SNAP-4177")
    public void test_write_headerContainsSpectraColumn() throws Exception {
        Path out = outPath("out.csv");
        io.write(libraryWith("P", 1.0, 2.0), out);
        String[] header = Files.readString(out).split("\n")[0].split(",");

        assertEquals("spectra", header[0]);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_wavelengthsInHeader() throws Exception {
        Path out = outPath("out.csv");
        io.write(libraryWith("P", 1.0, 2.0), out);
        String firstLine = Files.readString(out).split("\n")[0];

        assertTrue(firstLine.contains("400"));
        assertTrue(firstLine.contains("500"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_nanWrittenAsEmptyCell() throws Exception {
        Path out = outPath("nan.csv");
        io.write(libraryWith("P", Double.NaN, 2.0), out);

        String content = Files.readString(out);
        String dataRow = content.split("\n")[1];

        assertTrue(dataRow.contains(",,") || dataRow.endsWith(","));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_xUnitInHeader() throws Exception {
        Path out = outPath("unit.csv");
        SpectralAxis axis = new SpectralAxis(new double[]{490.0, 560.0}, "nanometers");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, null)
                .withProfileAdded(SpectralProfile.create("P", SpectralSignature.of(new double[]{1.0, 2.0})));

        io.write(lib, out);
        String firstLine = Files.readString(out).split("\n")[0];

        assertTrue(firstLine.contains("xUnit"));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_createsParentDirectories() throws Exception {
        Path deep = tmp.getRoot().toPath().resolve("a/b/c/out.csv");
        io.write(libraryWith("P", 1.0), deep);

        assertTrue(Files.exists(deep));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_write_wktAsColumn() throws Exception {
        Path out = outPath("wkt.csv");
        SpectralProfile p = SpectralProfile.create("P", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("wkt", AttributeValue.ofString("POINT(10.0 50.0)"));
        SpectralLibrary lib = SpectralLibrary.create("L", axis(), null).withProfileAdded(p);

        io.write(lib, out);
        String content = Files.readString(out);

        assertTrue(content.contains("wkt"));
        assertTrue(content.contains("POINT(10.0 50.0)"));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_profileName() throws Exception {
        Path out = outPath("rt.csv");
        io.write(libraryWith("MyProfile", 1.0, 2.0), out);

        assertEquals("MyProfile", io.read(out).getProfiles().getFirst().getName());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_axis() throws Exception {
        Path out = outPath("rt_axis.csv");
        SpectralAxis axis = new SpectralAxis(new double[]{450.0, 550.0, 650.0}, "nanometers");
        SpectralLibrary lib = SpectralLibrary.create("L", axis, null)
                .withProfileAdded(SpectralProfile.create("P", SpectralSignature.of(new double[]{1.0, 2.0, 3.0})));

        io.write(lib, out);
        SpectralAxis read = io.read(out).getAxis();

        assertArrayEquals(new double[]{450.0, 550.0, 650.0}, read.getWavelengths(), 1e-9);
        assertEquals("nanometers", read.getXUnit());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_goodValues() throws Exception {
        Path out = outPath("rt_vals.csv");

        io.write(libraryWith("P", 0.1, 0.2, 0.3), out);
        double[] v = io.read(out).getProfiles().getFirst().getSignature().getValues();

        assertArrayEquals(new double[]{0.1, 0.2, 0.3}, v, 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_nanPreserved() throws Exception {
        Path out = outPath("rt_nan.csv");

        io.write(libraryWith("P", Double.NaN, 2.0), out);
        double[] v = io.read(out).getProfiles().getFirst().getSignature().getValues();

        assertTrue(Double.isNaN(v[0]));
        assertEquals(2.0, v[1], 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_stringAttribute() throws Exception {
        Path out = outPath("rt_str.csv");
        SpectralProfile p = SpectralProfile.create("P", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("cls", AttributeValue.ofString("water"));

        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(p), out);

        assertEquals("water", io.read(out).getProfiles().getFirst().getAttributes().get("cls").asString());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_intAttribute() throws Exception {
        Path out = outPath("rt_int.csv");
        SpectralProfile p = SpectralProfile.create("P", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("count", AttributeValue.ofInt(7));

        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(p), out);

        assertEquals(7, io.read(out).getProfiles().getFirst().getAttributes().get("count").asInt());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_doubleAttribute() throws Exception {
        Path out = outPath("rt_dbl.csv");
        SpectralProfile p = SpectralProfile.create("P", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("score", AttributeValue.ofDouble(0.75));

        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(p), out);

        assertEquals(0.75, io.read(out).getProfiles().getFirst().getAttributes().get("score").asDouble(), 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_booleanAttribute() throws Exception {
        Path out = outPath("rt_bool.csv");
        SpectralProfile p = SpectralProfile.create("P", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("valid", AttributeValue.ofBoolean(true));

        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(p), out);

        assertTrue(io.read(out).getProfiles().getFirst().getAttributes().get("valid").asBoolean());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_wktAttribute() throws Exception {
        Path out = outPath("rt_wkt.csv");
        SpectralProfile p = SpectralProfile.create("P", SpectralSignature.of(new double[]{1.0, 2.0}))
                .withAttribute("wkt", AttributeValue.ofString("POINT(9.0 48.0)"));

        io.write(SpectralLibrary.create("L", axis(), null).withProfileAdded(p), out);

        assertEquals("POINT(9.0 48.0)", io.read(out).getProfiles().getFirst().getAttributes().get("wkt").asString());
    }

    @Test
    @STTM("SNAP-4177")
    public void test_roundtrip_multipleProfiles() throws Exception {
        Path out = outPath("rt_multi.csv");
        SpectralLibrary lib = SpectralLibrary.create("Multi", axis(), null)
                .withProfileAdded(SpectralProfile.create("A", SpectralSignature.of(new double[]{1.0, 2.0})))
                .withProfileAdded(SpectralProfile.create("B", SpectralSignature.of(new double[]{3.0, 4.0})));

        io.write(lib, out);
        SpectralLibrary read = io.read(out);

        assertEquals(2, read.size());
        assertEquals("A", read.getProfiles().get(0).getName());
        assertEquals("B", read.getProfiles().get(1).getName());
    }



    private SpectralAxis axis() {
        return new SpectralAxis(new double[]{400.0, 500.0}, "nm");
    }

    private SpectralLibrary libraryWith(String profileName, double... yValues) {
        double[] wl = new double[yValues.length];

        for (int i = 0; i < yValues.length; i++) {
            wl[i] = 400.0 + i * 100.0;
        }
        SpectralAxis ax = new SpectralAxis(wl, "nm");

        return SpectralLibrary.create("Lib", ax, null)
                .withProfileAdded(SpectralProfile.create(profileName, SpectralSignature.of(yValues)));
    }

    private Path write(String filename, String content) throws Exception {
        Path path = tmp.newFile(filename).toPath();
        Files.writeString(path, content);
        return path;
    }

    private Path outPath(String filename) {
        return tmp.getRoot().toPath().resolve(filename);
    }
}