package org.esa.snap.speclib.io.csv;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.io.csv.util.CsvTable;
import org.esa.snap.speclib.io.csv.util.CsvUtils;
import org.esa.snap.speclib.model.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;


public class CsvSpectralLibrarySidecarIOTest {


    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();


    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_read_nullPath_throwsNpe() throws Exception {
        new CsvSpectralLibrarySidecarIO().read(null);
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4129")
    public void test_read_emptyHeader_throwsIOException() throws Exception {
        Path p = tmp.newFile("empty.csv").toPath();
        Files.writeString(p, "", StandardCharsets.UTF_8);
        new CsvSpectralLibrarySidecarIO().read(p);
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_nameColMissing_fallsBackToFirstColumn_andSkipsEmptyNameRow() throws Exception {
        Path p = tmp.newFile("no_spectra_names.csv").toPath();

        String csv = "name,attr\n,123\nA, 7\n";
        Files.writeString(p, csv, StandardCharsets.UTF_8);

        SpectralLibrary lib = new CsvSpectralLibrarySidecarIO().read(p);
        assertEquals("no_spectra_names", lib.getName());
        assertEquals(1, lib.size());
        assertEquals("A", lib.getProfiles().get(0).getName());

        AttributeValue v = lib.getProfiles().get(0).getAttributes().get("attr");
        assertNotNull(v);
        assertEquals(AttributeType.INT, v.getType());
        assertEquals(7, v.asInt());
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_findNameColumn_skipsNullHeaderEntry_andUsesSpectraNames() throws Exception {
        Path p = tmp.newFile("with_spectra_names.csv").toPath();

        String csv = "spectra names, ,a\nS1,ignored,42\nS2,ignored,\n";
        Files.writeString(p, csv, StandardCharsets.UTF_8);

        SpectralLibrary lib = new CsvSpectralLibrarySidecarIO().read(p);
        assertEquals(2, lib.size());

        assertEquals("S1", lib.getProfiles().get(0).getName());
        assertTrue(lib.getProfiles().get(0).getAttributes().containsKey("a"));

        AttributeValue a1 = lib.getProfiles().get(0).getAttributes().get("a");
        assertEquals(AttributeType.INT, a1.getType());
        assertEquals(42, a1.asInt());

        assertFalse(lib.getProfiles().get(1).getAttributes().containsKey("a"));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_infersArraysAndMapsAndFallsBackToStringWhenParseFails() throws Exception {
        Path p = tmp.newFile("types.csv").toPath();
        String csv = "spectra names,ia,da,sl,mp,badInt\n"
                + "P1,\"[1,2,3]\",\"[1.0,2.5]\",\"[a,b]\",\"k=v,k2=v2\",abc\n";
        Files.writeString(p, csv, StandardCharsets.UTF_8);

        SpectralLibrary lib = new CsvSpectralLibrarySidecarIO().read(p);
        SpectralProfile p1 = lib.getProfiles().get(0);

        assertEquals(AttributeType.INT_ARRAY, p1.getAttributes().get("ia").getType());
        assertArrayEquals(new int[]{1,2,3}, p1.getAttributes().get("ia").asIntArray());

        assertEquals(AttributeType.DOUBLE_ARRAY, p1.getAttributes().get("da").getType());
        assertArrayEquals(new double[]{1.0,2.5}, p1.getAttributes().get("da").asDoubleArray(), 0.0);

        assertEquals(AttributeType.STRING_LIST, p1.getAttributes().get("sl").getType());
        assertEquals(Arrays.asList("a","b"), p1.getAttributes().get("sl").asStringList());

        assertEquals(AttributeType.STRING_MAP, p1.getAttributes().get("mp").getType());
        assertEquals("v", p1.getAttributes().get("mp").asStringMap().get("k"));
        assertEquals("v2", p1.getAttributes().get("mp").asStringMap().get("k2"));

        AttributeValue bad = p1.getAttributes().get("badInt");
        assertNotNull(bad);
        assertEquals(AttributeType.STRING, bad.getType());
        assertEquals("abc", bad.asString());
    }

    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_write_nullLibrary_throwsNpe() throws Exception {
        Path p = tmp.newFile("x.csv").toPath();
        new CsvSpectralLibrarySidecarIO().write(null, p);
    }

    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_write_nullPath_throwsNpe() throws Exception {
        SpectralLibrary lib = SpectralLibrary.create("L", new SpectralAxis(new double[]{1.0}, "nm"), null);
        new CsvSpectralLibrarySidecarIO().write(lib, null);
    }


    @Test
    @STTM("SNAP-4129")
    public void test_write_buildsHeaderWithSchemaKeysAndExtras_sorted_andWritesRows() throws Exception {
        Path p = tmp.newFile("out.csv").toPath();

        SpectralAxis axis = new SpectralAxis(new double[]{1.0}, "nm");
        SpectralSignature sig = SpectralSignature.of(new double[]{0.5});

        SpectralProfile p1 = SpectralProfile.create("P1", sig)
                .withAttribute("a", AttributeValue.ofInt(1))
                .withAttribute("zExtra", AttributeValue.ofString("Z"))
                .withAttribute("bExtra", AttributeValue.ofString("B"));

        SpectralProfile p2 = SpectralProfile.create("P2", sig).withAttribute("a", AttributeValue.ofInt(2));

        AttributeSchema schema = new AttributeSchema();
        schema.inferFromAttributes(Map.of("a", AttributeValue.ofInt(0)));

        SpectralLibrary lib = new SpectralLibrary(UUID.randomUUID(), "L", axis, null, Arrays.asList(p1, p2), schema);
        new CsvSpectralLibrarySidecarIO().write(lib, p);
        CsvTable t = CsvUtils.read(p);

        assertEquals(Arrays.asList("spectra names", "a", "bExtra", "zExtra", "wkt"), t.header());
        assertEquals(2, t.rows().size());

        assertEquals("P1", t.rows().get(0).get(0));
        assertEquals("1", t.rows().get(0).get(1));
        assertEquals("B", t.rows().get(0).get(2));
        assertEquals("Z", t.rows().get(0).get(3));
        assertEquals("", t.rows().get(0).get(4));

        assertEquals("P2", t.rows().get(1).get(0));
        assertEquals("2", t.rows().get(1).get(1));
        assertEquals("", t.rows().get(1).get(2));
        assertEquals("", t.rows().get(1).get(3));
        assertEquals("", t.rows().get(1).get(4));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_write_infersTypeFromProfiles_whenSchemaMissingKey_andFormatsArrayBracketed() throws Exception {
        Path p = tmp.newFile("infer.csv").toPath();

        SpectralAxis axis = new SpectralAxis(new double[]{1.0}, "nm");
        SpectralSignature sig = SpectralSignature.of(new double[]{0.5});
        SpectralProfile prof = SpectralProfile.create("P", sig).withAttribute("arr", AttributeValue.ofIntArray(new int[]{1, 2}));
        SpectralLibrary lib = new SpectralLibrary(UUID.randomUUID(), "L2", axis, null, List.of(prof), new AttributeSchema());

        new CsvSpectralLibrarySidecarIO().write(lib, p);

        CsvTable t = CsvUtils.read(p);
        assertEquals(Arrays.asList("spectra names", "arr", "wkt"), t.header());
        assertEquals(1, t.rows().size());

        assertEquals("P", t.rows().get(0).get(0));
        assertEquals("[1,2]", t.rows().get(0).get(1));
        assertEquals("", t.rows().get(0).get(2));
    }


    @Test
    @STTM("SNAP-4129")
    public void test_read_rowShorterThanHeader_safeCellOutOfRange_returnsEmpty_andAttributeSkipped() throws Exception {
        Path p = tmp.newFile("shortrow.csv").toPath();

        String csv = "spectra names,a,b\nP1\n";
        Files.writeString(p, csv, StandardCharsets.UTF_8);
        SpectralLibrary lib = new CsvSpectralLibrarySidecarIO().read(p);

        assertEquals(1, lib.size());

        SpectralProfile sp = lib.getProfiles().get(0);
        assertEquals("P1", sp.getName());
        assertTrue(sp.getAttributes().isEmpty());
    }


    @Test
    @STTM("SNAP-4129")
    public void test_ensureWktInSchema_coversNullAlreadyPresentAndMissing() throws Exception {
        var m = CsvSpectralLibrarySidecarIO.class.getDeclaredMethod("ensureWktInSchema", AttributeSchema.class);
        m.setAccessible(true);

        AttributeSchema outNull = (AttributeSchema) m.invoke(null, new Object[]{null});
        assertNotNull(outNull);
        assertTrue(outNull.asMap().isEmpty());
        assertTrue(outNull.find(CsvSpectralLibrarySidecarIO.COL_WKT).isEmpty());

        Map<String, AttributeDef> withWkt = new LinkedHashMap<>();
        withWkt.put(CsvSpectralLibrarySidecarIO.COL_WKT, AttributeDef.optional(CsvSpectralLibrarySidecarIO.COL_WKT, AttributeType.STRING));
        AttributeSchema hasWkt = new AttributeSchema(withWkt);

        AttributeSchema outHasWkt = (AttributeSchema) m.invoke(null, hasWkt);
        assertSame(hasWkt, outHasWkt);

        AttributeSchema missingWkt = new AttributeSchema();
        missingWkt.inferFromAttributes(Map.of("a", AttributeValue.ofInt(1)));
        assertTrue(missingWkt.find(CsvSpectralLibrarySidecarIO.COL_WKT).isEmpty());

        AttributeSchema outAdded = (AttributeSchema) m.invoke(null, missingWkt);
        assertNotSame(missingWkt, outAdded);
        assertTrue(outAdded.find("a").isPresent());
        assertTrue(outAdded.find(CsvSpectralLibrarySidecarIO.COL_WKT).isPresent());
    }
}