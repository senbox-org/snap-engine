package org.esa.snap.speclib.io.csv.util;

import com.bc.ceres.annotation.STTM;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
}