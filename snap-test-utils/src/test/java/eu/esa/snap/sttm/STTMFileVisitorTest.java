package eu.esa.snap.sttm;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class STTMFileVisitorTest {

    private final String sep = System.getProperty("line.separator");

    private static BufferedReader getReader(String fileContent) {
        final StringReader stringReader = new StringReader(fileContent);
        return new BufferedReader(stringReader);
    }

    @Test
    @STTM("SNAP-3506")
    public void testExtractSTTMInfo_none_old_junit_format() throws IOException {
        final String fileContent = "package com.bc.ceres.binding.converters;" + sep + "public abstract class AbstractConverterTest extends TestCase {" + sep + "protected void testValueType(Class<?> expectedType) {" + sep + "        assertEquals(expectedType, getConverter().getValueType());" + sep + "    }" + sep + "}";
        final BufferedReader bufferedReader = getReader(fileContent);

        final List<STTMInfo> sttmInfos = STTMFileVisitor.extractSTTMInfo(bufferedReader);
        assertEquals(0, sttmInfos.size());
    }

    @Test
    @STTM("SNAP-3506")
    public void testExtractSTTMInfo_one_sttm_before_test() throws IOException {
        final String fileContent = "package eu.esa.opt.dataio.s3.olci;" + sep +
                "public class OlciLevel1ProductFactoryTest {" + sep +
                "@STTM(\"SNAP-3519\")" + sep +
                "@Test" + sep +
                "    public void testBandGroupingPattern() {" + sep +
                "    }" + sep +
                "}";

        final BufferedReader bufferedReader = getReader(fileContent);

        final List<STTMInfo> sttmInfos = STTMFileVisitor.extractSTTMInfo(bufferedReader);
        assertEquals(1, sttmInfos.size());

        final STTMInfo sttmInfo = sttmInfos.get(0);
        assertEquals("SNAP-3519", sttmInfo.jiraIssue);
        assertEquals("eu.esa.opt.dataio.s3.olci", sttmInfo.pckg);
        assertEquals("OlciLevel1ProductFactoryTest", sttmInfo.clazz);
        assertEquals("testBandGroupingPattern", sttmInfo.method);
    }

    @Test
    @STTM("SNAP-3506")
    public void testExtractSTTMInfo_two_sttm_after_test() throws IOException {
        final String fileContent = "package org.esa.snap.landcover;" + sep +
                "public class TestAddLandCoverOp {" + sep +
                "@Test" + sep +
                "@STTM(\"SNAP-3520, SNAP-3351\")" + sep + "    public void testGLC2000() throws Exception {" + sep +
                "    }" +
                sep + "}";

        final BufferedReader bufferedReader = getReader(fileContent);

        final List<STTMInfo> sttmInfos = STTMFileVisitor.extractSTTMInfo(bufferedReader);
        assertEquals(2, sttmInfos.size());

        STTMInfo sttmInfo = sttmInfos.get(0);
        assertEquals("SNAP-3520", sttmInfo.jiraIssue);
        assertEquals("org.esa.snap.landcover", sttmInfo.pckg);
        assertEquals("TestAddLandCoverOp", sttmInfo.clazz);
        assertEquals("testGLC2000", sttmInfo.method);

        sttmInfo = sttmInfos.get(1);
        assertEquals("SNAP-3351", sttmInfo.jiraIssue);
        assertEquals("org.esa.snap.landcover", sttmInfo.pckg);
        assertEquals("TestAddLandCoverOp", sttmInfo.clazz);
        assertEquals("testGLC2000", sttmInfo.method);
    }

    @Test
    @STTM("SNAP-3506")
    public void testExtractJiraIssues_singleIssue() {
        final String line = "     @STTM(\"SNAP-3523\")";

        final String[] issues = STTMFileVisitor.extractJiraIssues(line);
        assertEquals(1, issues.length);
        assertEquals("SNAP-3523", issues[0]);
    }

    @Test
    @STTM("SNAP-3506")
    public void testExtractJiraIssues_threeIssues() {
        final String line = "     @STTM(\"SNAP-3523, SNAP-3456,SNAP-3225\")";

        final String[] issues = STTMFileVisitor.extractJiraIssues(line);
        assertEquals(3, issues.length);
        assertEquals("SNAP-3523", issues[0]);
        assertEquals("SNAP-3456", issues[1]);
        assertEquals("SNAP-3225", issues[2]);
    }

    @Test
    @STTM("SNAP-3506")
    public void testExtractPackage() {
        final String line = "package org.esa.snap.dataio.bigtiff;";

        final String pckg = STTMFileVisitor.extractPackage(line);
        assertEquals("org.esa.snap.dataio.bigtiff", pckg);
    }

    @Test
    @STTM("SNAP-3506")
    public void testExtractClass() {
        final String line = "public class ChangeVectorAnalysisOpTest {";

        final String clazz = STTMFileVisitor.extractClass(line);
        assertEquals("ChangeVectorAnalysisOpTest", clazz);
    }

    @Test
    @STTM("SNAP-3506")
    public void testExtractClass_keyWord_in_other_expression() {
        final String line = " return GDALVersion.class.getClassLoader().getResource()";

        final String clazz = STTMFileVisitor.extractClass(line);
        assertNull(clazz);
    }

    @Test
    @STTM("SNAP-3506")
    public void testExtractMethodName() {
        final String line = "public void testGetEnvironmentVariablesFilePathFromSources() {";

        final String methodName = STTMFileVisitor.extractMethodName(line);
        assertEquals("testGetEnvironmentVariablesFilePathFromSources", methodName);
    }
}
