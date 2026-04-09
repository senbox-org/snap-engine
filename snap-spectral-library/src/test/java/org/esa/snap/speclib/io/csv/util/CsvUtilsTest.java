package org.esa.snap.speclib.io.csv.util;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.speclib.model.AttributeType;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;


public class CsvUtilsTest {


    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();


    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_read_nullPath_throwsNpe() throws Exception {
        CsvUtils.read(null);
    }

    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_write_nullPath_throwsNpe() throws Exception {
        CsvUtils.write(null, List.of("h"), List.of(List.of("v")));
    }

    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_write_nullHeader_throwsNpe() throws Exception {
        Path p = tmp.newFile("x.csv").toPath();
        CsvUtils.write(p, null, List.of(List.of("v")));
    }

    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_write_nullRows_throwsNpe() throws Exception {
        Path p = tmp.newFile("x.csv").toPath();
        CsvUtils.write(p, List.of("h"), null);
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_emptyFile_returnsEmptyTable() throws Exception {
        Path p = tmp.newFile("empty.csv").toPath();
        Files.writeString(p, "", StandardCharsets.UTF_8);

        CsvTable t = CsvUtils.read(p);
        assertNotNull(t);
        assertTrue(t.header().isEmpty());
        assertTrue(t.rows().isEmpty());
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_bomIsRemovedFromFirstHeaderCell() throws Exception {
        Path p = tmp.newFile("bom.csv").toPath();
        Files.writeString(p, "\uFEFFa,b\n1,2\n", StandardCharsets.UTF_8);

        CsvTable t = CsvUtils.read(p);
        assertEquals(List.of("a", "b"), t.header());
        assertEquals(1, t.rows().size());
        assertEquals(List.of("1", "2"), t.rows().get(0));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_eofWithoutNewline_flushesLastRow() throws Exception {
        Path p = tmp.newFile("eof.csv").toPath();
        Files.writeString(p, "h1,h2\nv1,v2", StandardCharsets.UTF_8);

        CsvTable t = CsvUtils.read(p);
        assertEquals(List.of("h1", "h2"), t.header());
        assertEquals(1, t.rows().size());
        assertEquals(List.of("v1", "v2"), t.rows().get(0));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_trailingCommaAtEof_addsEmptyLastCell() throws Exception {
        Path p = tmp.newFile("trail.csv").toPath();
        Files.writeString(p, "h1,h2,\n1,2,", StandardCharsets.UTF_8);

        CsvTable t = CsvUtils.read(p);
        assertEquals(List.of("h1", "h2", ""), t.header());
        assertEquals(1, t.rows().size());
        assertEquals(List.of("1", "2", ""), t.rows().get(0));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_handlesCRLF_andStandaloneCR() throws Exception {
        Path p = tmp.newFile("cr.csv").toPath();
        Files.writeString(p, "h1,h2\r\nv1,v2\ra,b\n", StandardCharsets.UTF_8);

        CsvTable t = CsvUtils.read(p);
        assertEquals(List.of("h1", "h2"), t.header());
        assertEquals(2, t.rows().size());
        assertEquals(List.of("v1", "v2"), t.rows().get(0));
        assertEquals(List.of("a", "b"), t.rows().get(1));
    }

    @Test(expected = IOException.class)
    @STTM("SNAP-4129")
    public void test_read_unclosedQuote_throwsIOException() throws Exception {
        Path p = tmp.newFile("bad.csv").toPath();
        Files.writeString(p, "h1\n\"x", StandardCharsets.UTF_8);
        CsvUtils.read(p);
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_quotesEscapesCommasAndNewlinesInsideQuotes() throws Exception {
        Path p = tmp.newFile("quotes.csv").toPath();

        String csv = ""
                + "c1,c2\n"
                + "\"a,b\",\"line1\nline2\"\n"
                + "\"x\"\"y\",z\n";

        Files.writeString(p, csv, StandardCharsets.UTF_8);

        CsvTable t = CsvUtils.read(p);
        assertEquals(List.of("c1", "c2"), t.header());
        assertEquals(2, t.rows().size());

        assertEquals("a,b", t.rows().get(0).get(0));
        assertEquals("line1\nline2", t.rows().get(0).get(1));

        assertEquals("x\"y", t.rows().get(1).get(0));
        assertEquals("z", t.rows().get(1).get(1));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_read_quoteCloseAtEof_noResetNeeded() throws Exception {
        Path p = tmp.newFile("quote_eof.csv").toPath();

        Files.writeString(p, "h1\n\"x\"", StandardCharsets.UTF_8);

        CsvTable t = CsvUtils.read(p);
        assertEquals(List.of("h1"), t.header());
        assertEquals(1, t.rows().size());
        assertEquals(List.of("x"), t.rows().get(0));
    }


    @Test
    @STTM("SNAP-4129")
    public void test_write_then_read_roundtrip_escapesSpecialCharsAndNulls() throws Exception {
        File f = tmp.newFile("roundtrip.csv");
        Path p = f.toPath();

        List<String> header = new ArrayList<>(Arrays.asList("plain", "a,b", "c\"d", "e\nf", "g\rg", null));

        List<List<String>> rows = Arrays.asList(
                new ArrayList<>(Arrays.asList("v", "1,2", "x\"y", "L1\nL2", "R\rS", null)),
                Arrays.asList("", "", "", "", "", "")
        );

        CsvUtils.write(p, header, rows);

        CsvTable t = CsvUtils.read(p);
        assertEquals(Arrays.asList("plain", "a,b", "c\"d", "e\nf", "g\rg", ""), t.header());
        assertEquals(2, t.rows().size());

        assertEquals(Arrays.asList("v", "1,2", "x\"y", "L1\nL2", "R\rS", ""), t.rows().get(0));
        assertEquals(Arrays.asList("", "", "", "", "", ""), t.rows().get(1));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_isNumeric() {
        assertTrue(CsvUtils.isNumeric("490"));
        assertTrue(CsvUtils.isNumeric("490.0"));
        assertTrue(CsvUtils.isNumeric("-1.5"));
        assertTrue(CsvUtils.isNumeric("1.5e3"));
        assertTrue(CsvUtils.isNumeric("  42.0  "));
        assertFalse(CsvUtils.isNumeric("spectra"));
        assertFalse(CsvUtils.isNumeric(""));
        assertFalse(CsvUtils.isNumeric(null));
        assertFalse(CsvUtils.isNumeric("   "));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_safeCell() {
        assertEquals("hello", CsvUtils.safeCell(List.of("a", "hello", "c"), 1));
        assertEquals("", CsvUtils.safeCell(List.of("a"), 5));
        assertEquals("", CsvUtils.safeCell(List.of("a"), -1));
        assertEquals("", CsvUtils.safeCell(null, 0));

        List<String> row = new ArrayList<>();
        row.add(null);
        assertEquals("", CsvUtils.safeCell(row, 0));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_inferAttributeType() {
        assertEquals(AttributeType.INT, CsvUtils.inferAttributeType("count", "42"));
        assertEquals(AttributeType.DOUBLE, CsvUtils.inferAttributeType("score", "0.95"));
        assertEquals(AttributeType.BOOLEAN, CsvUtils.inferAttributeType("valid", "true"));
        assertEquals(AttributeType.STRING, CsvUtils.inferAttributeType("cls", "vegetation"));
        assertEquals(AttributeType.INSTANT, CsvUtils.inferAttributeType("ts", "2024-01-01T00:00:00Z"));
        assertEquals(AttributeType.STRING, CsvUtils.inferAttributeType("label", "not-a-date"));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_isColumnOriented() {
        CsvTable t = new CsvTable(
                List.of("spectra", "P1", "P2"),
                List.of(List.of("490.0", "1.0", "2.0"), List.of("560.0", "3.0", "4.0"))
        );
        assertTrue(CsvUtils.isColumnOriented(t));

        t = new CsvTable(
                List.of("spectra", "490.0", "560.0"),
                List.of(List.of("P1", "1.0", "2.0"))
        );
        assertFalse(CsvUtils.isColumnOriented(t));

        t = new CsvTable(List.of(), List.of());
        assertFalse(CsvUtils.isColumnOriented(t));

        t = new CsvTable(List.of("spectra", "P1"), List.of());
        assertFalse(CsvUtils.isColumnOriented(t));

        t = new CsvTable(
                List.of("spectra", "P1"),
                List.of(List.of("490.0", "1.0"), List.of("metadata", "something"))
        );
        assertFalse(CsvUtils.isColumnOriented(t));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_transpose_byColumnToByRow() {
        CsvTable byCol = new CsvTable(
                List.of("spectra", "P1", "P2"),
                List.of(
                        List.of("490.0", "1.0", "2.0"),
                        List.of("560.0", "3.0", "4.0")
                )
        );

        CsvTable byRow = CsvUtils.transpose(byCol);

        assertEquals(List.of("spectra", "490.0", "560.0"), byRow.header());
        assertEquals(2, byRow.rows().size());
        assertEquals(List.of("P1", "1.0", "3.0"), byRow.rows().get(0));
        assertEquals(List.of("P2", "2.0", "4.0"), byRow.rows().get(1));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_transpose_singleProfile() {
        CsvTable byCol = new CsvTable(
                List.of("spectra", "OnlyProfile"),
                List.of(List.of("400.0", "0.5"))
        );

        CsvTable byRow = CsvUtils.transpose(byCol);

        assertEquals(List.of("spectra", "400.0"), byRow.header());
        assertEquals(1, byRow.rows().size());
        assertEquals(List.of("OnlyProfile", "0.5"), byRow.rows().get(0));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_transpose_singleWavelength() {
        CsvTable byCol = new CsvTable(
                List.of("spectra", "P1", "P2"),
                List.of(List.of("500.0", "0.1", "0.2"))
        );

        CsvTable byRow = CsvUtils.transpose(byCol);

        assertEquals(List.of("spectra", "500.0"), byRow.header());
        assertEquals(2, byRow.rows().size());
        assertEquals("0.1", byRow.rows().get(0).get(1));
        assertEquals("0.2", byRow.rows().get(1).get(1));
    }

    @Test
    @STTM("SNAP-4177")
    public void test_transpose_missingCells_handledGracefully() {
        CsvTable byCol = new CsvTable(
                List.of("spectra", "P1", "P2"),
                List.of(
                        List.of("490.0", "1.0"),
                        List.of("560.0", "3.0", "4.0")
                )
        );

        CsvTable byRow = CsvUtils.transpose(byCol);

        assertEquals("", byRow.rows().get(1).get(1));
        assertEquals("4.0", byRow.rows().get(1).get(2));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_findColumn() {
        assertEquals(2, CsvUtils.findColumn(List.of("a", "b", "wkt", "d"), "wkt"));
        assertEquals(1, CsvUtils.findColumn(List.of("a", "WKT"), "wkt"));
        assertEquals(-1, CsvUtils.findColumn(List.of("a", "b"), "missing"));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_findWavelengthColumns() {
        List<Integer> cols = CsvUtils.findWavelengthColumns(List.of("spectra", "wkt", "490.0", "560.0", "class"));
        assertEquals(List.of(2, 3), cols);

        assertTrue(CsvUtils.findWavelengthColumns(List.of("spectra", "class")).isEmpty());

        cols = CsvUtils.findWavelengthColumns(List.of("400", "500", "600"));
        assertEquals(List.of(0, 1, 2), cols);
    }


    @Test
    @STTM("SNAP-4177")
    public void test_extractWavelengths_correctValues() {
        List<String> header = List.of("spectra", "490.0", "560.0", "665.0");
        List<Integer> waveCols = List.of(1, 2, 3);

        double[] wl = CsvUtils.extractWavelengths(header, waveCols);
        assertArrayEquals(new double[]{490.0, 560.0, 665.0}, wl, 1e-9);
    }

    @Test
    @STTM("SNAP-4177")
    public void test_extractWavelengths_integers() {
        List<String> header = List.of("spectra", "400", "500");
        List<Integer> waveCols = List.of(1, 2);

        double[] wl = CsvUtils.extractWavelengths(header, waveCols);
        assertArrayEquals(new double[]{400.0, 500.0}, wl, 1e-9);
    }


    @Test
    @STTM("SNAP-4177")
    public void test_formatWavelength() {
        assertEquals("490", CsvUtils.formatWavelength(490.0));
        assertEquals("490.5", CsvUtils.formatWavelength(490.5));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_isWavelengthColumn() {
        assertTrue(CsvUtils.isWavelengthColumn("490.0", new double[]{490.0, 560.0}));
        assertFalse(CsvUtils.isWavelengthColumn("700.0", new double[]{490.0, 560.0}));
        assertFalse(CsvUtils.isWavelengthColumn("spectra", new double[]{490.0}));
    }


    @Test
    @STTM("SNAP-4177")
    public void test_wavelengthIndex() {
        assertEquals(1, CsvUtils.wavelengthIndex("560.0", new double[]{490.0, 560.0, 665.0}));
        assertEquals(-1, CsvUtils.wavelengthIndex("700.0", new double[]{490.0, 560.0}));
        assertEquals(-1, CsvUtils.wavelengthIndex("spectra", new double[]{490.0}));
    }
}