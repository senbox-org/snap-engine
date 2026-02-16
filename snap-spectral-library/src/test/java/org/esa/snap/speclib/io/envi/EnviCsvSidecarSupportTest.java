package org.esa.snap.speclib.io.envi;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.dataio.envi.EnviConstants;
import org.esa.snap.speclib.model.*;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;


public class EnviCsvSidecarSupportTest {


    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();


    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_resolveCsvPath_null_throwsNpe() {
        EnviCsvSidecarSupport.resolveCsvPath(null);
    }

    @Test
    @STTM("SNAP-4129")
    public void test_resolveCsvPath_hdrInput_returnsSiblingCsvSameBase() throws Exception {
        Path hdr = tmp.newFile("lib" + EnviConstants.HDR_EXTENSION).toPath();
        Path csv = EnviCsvSidecarSupport.resolveCsvPath(hdr);

        assertEquals(hdr.getParent(), csv.getParent());
        assertEquals("lib.csv", csv.getFileName().toString());
    }

    @Test
    @STTM("SNAP-4129")
    public void test_resolveCsvPath_nonHdrInput_resolvesViaHdrExtension() throws Exception {
        Path sli = tmp.newFile("lib.sli").toPath();
        Path csv = EnviCsvSidecarSupport.resolveCsvPath(sli);

        assertEquals(sli.getParent(), csv.getParent());
        assertEquals("lib.csv", csv.getFileName().toString());
    }

    @Test
    @STTM("SNAP-4129")
    public void test_resolveCsvPath_noExtension_addsHdrThenCsv() throws Exception {
        Path base = tmp.newFile("lib").toPath();
        Path csv = EnviCsvSidecarSupport.resolveCsvPath(base);
        assertEquals("lib.csv", csv.getFileName().toString());
    }


    @Test
    @STTM("SNAP-4129")
    public void test_hasAnyAttributes_nullLibrary_returnsFalse() {
        assertFalse(EnviCsvSidecarSupport.hasAnyAttributes(null));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_hasAnyAttributes_schemaNonEmpty_returnsTrue() {
        SpectralAxis axis = new SpectralAxis(new double[]{1.0}, "nm");
        SpectralSignature sig = SpectralSignature.of(new double[]{0.1});

        SpectralProfile p = SpectralProfile.create("P", sig);
        AttributeSchema schema = new AttributeSchema();
        schema.inferFromAttributes(Map.of("a", AttributeValue.ofInt(1)));
        SpectralLibrary lib = new SpectralLibrary(UUID.randomUUID(), "L", axis, null, List.of(p), schema);

        assertTrue(EnviCsvSidecarSupport.hasAnyAttributes(lib));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_hasAnyAttributes_schemaEmpty_butProfileAttributesPresent_returnsTrue() {
        SpectralAxis axis = new SpectralAxis(new double[]{1.0}, "nm");
        SpectralSignature sig = SpectralSignature.of(new double[]{0.1});

        SpectralProfile p = SpectralProfile.create("P", sig).withAttribute("x", AttributeValue.ofString("v"));
        SpectralLibrary lib = new SpectralLibrary(UUID.randomUUID(), "L", axis, null, List.of(p), new AttributeSchema());
        assertTrue(EnviCsvSidecarSupport.hasAnyAttributes(lib));
    }



    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_writeIfNeeded_nullLib_throwsNpe() throws Exception {
        Path p = tmp.newFile("lib" + EnviConstants.HDR_EXTENSION).toPath();
        EnviCsvSidecarSupport.writeIfNeeded(null, p);
    }

    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_writeIfNeeded_nullPath_throwsNpe() throws Exception {
        SpectralLibrary lib = SpectralLibrary.create("L", new SpectralAxis(new double[]{1.0}, "nm"), null);
        EnviCsvSidecarSupport.writeIfNeeded(lib, null);
    }

    @Test
    @STTM("SNAP-4129")
    public void test_writeIfNeeded_noAttributes_doesNotCreateCsv() throws Exception {
        Path hdr = tmp.newFile("lib" + EnviConstants.HDR_EXTENSION).toPath();

        SpectralAxis axis = new SpectralAxis(new double[]{1.0}, "nm");
        SpectralSignature sig = SpectralSignature.of(new double[]{0.1});
        SpectralProfile p = SpectralProfile.create("P", sig);
        SpectralLibrary lib = new SpectralLibrary(UUID.randomUUID(), "L", axis, null, List.of(p), new AttributeSchema());

        Path csv = EnviCsvSidecarSupport.resolveCsvPath(hdr);
        Files.deleteIfExists(csv);
        EnviCsvSidecarSupport.writeIfNeeded(lib, hdr);

        assertFalse(Files.exists(csv));
    }

    @Test
    @STTM("SNAP-4129")
    public void test_writeIfNeeded_withAttributes_createsCsv() throws Exception {
        Path hdr = tmp.newFile("lib" + EnviConstants.HDR_EXTENSION).toPath();

        SpectralAxis axis = new SpectralAxis(new double[]{1.0}, "nm");
        SpectralSignature sig = SpectralSignature.of(new double[]{0.1});
        SpectralProfile p = SpectralProfile.create("P", sig).withAttribute("a", AttributeValue.ofInt(7));

        AttributeSchema schema = new AttributeSchema();
        schema.inferFromAttributes(Map.of("a", AttributeValue.ofInt(0)));

        SpectralLibrary lib = new SpectralLibrary(UUID.randomUUID(), "L", axis, null, List.of(p), schema);

        Path csv = EnviCsvSidecarSupport.resolveCsvPath(hdr);
        Files.deleteIfExists(csv);

        EnviCsvSidecarSupport.writeIfNeeded(lib, hdr);

        assertTrue(Files.exists(csv));

        String content = Files.readString(csv, StandardCharsets.UTF_8);
        assertTrue(content.contains("spectra names"));
        assertTrue(content.contains("a"));
        assertTrue(content.contains("P"));
        assertTrue(content.contains("7"));
    }


    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_mergeIfPresent_nullLib_throwsNpe() throws Exception {
        Path hdr = tmp.newFile("lib" + EnviConstants.HDR_EXTENSION).toPath();
        EnviCsvSidecarSupport.mergeIfPresent(null, hdr);
    }

    @Test(expected = NullPointerException.class)
    @STTM("SNAP-4129")
    public void test_mergeIfPresent_nullPath_throwsNpe() throws Exception {
        SpectralLibrary lib = SpectralLibrary.create("L", new SpectralAxis(new double[]{1.0}, "nm"), null);
        EnviCsvSidecarSupport.mergeIfPresent(lib, null);
    }

    @Test
    @STTM("SNAP-4129")
    public void test_mergeIfPresent_csvMissing_returnsSameInstance() throws Exception {
        Path hdr = tmp.newFile("lib" + EnviConstants.HDR_EXTENSION).toPath();
        Path csv = EnviCsvSidecarSupport.resolveCsvPath(hdr);
        Files.deleteIfExists(csv);

        SpectralAxis axis = new SpectralAxis(new double[]{1.0}, "nm");
        SpectralSignature sig = SpectralSignature.of(new double[]{0.1});
        SpectralProfile p = SpectralProfile.create("P", sig);
        SpectralLibrary enviLib = new SpectralLibrary(UUID.randomUUID(), "L", axis, null, List.of(p), new AttributeSchema());

        SpectralLibrary merged = EnviCsvSidecarSupport.mergeIfPresent(enviLib, hdr);
        assertSame(enviLib, merged);
    }

    @Test
    @STTM("SNAP-4129")
    public void test_mergeIfPresent_mergesAttributesByName_andKeepsMeta() throws Exception {
        Path hdr = tmp.newFile("lib" + EnviConstants.HDR_EXTENSION).toPath();
        Path csv = EnviCsvSidecarSupport.resolveCsvPath(hdr);

        SpectralAxis axis = new SpectralAxis(new double[]{1.0}, "nm");
        SpectralSignature sig = SpectralSignature.of(new double[]{0.1});
        SpectralProfile p1 = SpectralProfile.create("P1", sig);
        SpectralProfile p2 = SpectralProfile.create("P2", sig);

        UUID id = UUID.randomUUID();
        SpectralLibrary enviLib = new SpectralLibrary(id, "L", axis, "yUnit", List.of(p1, p2), new AttributeSchema());

        String csvContent = ""
                + "spectra names,a\n"
                + "P1,5\n"
                + ",9\n"
                + "P3,7\n";
        Files.writeString(csv, csvContent, StandardCharsets.UTF_8);

        SpectralLibrary merged = EnviCsvSidecarSupport.mergeIfPresent(enviLib, hdr);

        assertEquals(id, merged.getId());
        assertEquals("L", merged.getName());
        assertEquals(axis.size(), merged.getAxis().size());
        assertEquals("yUnit", merged.getDefaultYUnit().orElse(null));

        assertEquals(2, merged.size());

        SpectralProfile mp1 = merged.getProfiles().get(0);
        SpectralProfile mp2 = merged.getProfiles().get(1);

        assertEquals("P1", mp1.getName());
        assertEquals("P2", mp2.getName());

        assertTrue(mp1.getAttributes().containsKey("a"));
        assertEquals(AttributeType.INT, mp1.getAttributes().get("a").getType());
        assertEquals(5, mp1.getAttributes().get("a").asInt());

        assertTrue(mp2.getAttributes().isEmpty());
        assertTrue(merged.getSchema().find("a").isPresent());
    }

    @Test
    @STTM("SNAP-4129")
    public void test_mergeIfPresent_ignoresNullProfileAndNullNameInCsvLibRows() throws Exception {
        Path hdr = tmp.newFile("lib" + EnviConstants.HDR_EXTENSION).toPath();
        Path csv = EnviCsvSidecarSupport.resolveCsvPath(hdr);

        Files.writeString(csv, "spectra names,a\n\"\",1\nP1,2\n", StandardCharsets.UTF_8);

        SpectralAxis axis = new SpectralAxis(new double[]{1.0}, "nm");
        SpectralSignature sig = SpectralSignature.of(new double[]{0.1});
        SpectralLibrary enviLib = new SpectralLibrary(UUID.randomUUID(), "L", axis, null, List.of(SpectralProfile.create("P1", sig)), new AttributeSchema());

        SpectralLibrary merged = EnviCsvSidecarSupport.mergeIfPresent(enviLib, hdr);
        assertEquals(1, merged.size());
        assertEquals(2, merged.getProfiles().get(0).getAttributes().get("a").asInt());
    }
}