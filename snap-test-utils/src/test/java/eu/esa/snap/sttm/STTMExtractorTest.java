package eu.esa.snap.sttm;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class STTMExtractorTest {

    private static String getValidPath() {
        final String validPath = STTMExtractor.class.getClassLoader().getResource("").getPath();
        return validPath.substring(1);
    }

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_no_args() {
        try {
            STTMExtractor.validateInput(new String[0]);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_not_existing_dir() {
        try {
            STTMExtractor.validateInput(new String[]{"/not/existing/directory"});
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_file_not_dir() throws IOException {
        final File tempFile = File.createTempFile("whatever", "dont_care");
        try {
            STTMExtractor.validateInput(new String[]{tempFile.getAbsolutePath()});
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        } finally {
            if (!tempFile.delete()) {
                fail("unable to delete temp file: " + tempFile.getAbsolutePath());
            }
        }
    }

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_valid_dir() {
        final String path = getValidPath();

        final CmdLineArgs args = STTMExtractor.validateInput(new String[]{"-o", "./", path});
        assertEquals(1, args.inputPaths.length);
    }

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_two_valid_dirs() {
        final String path = getValidPath();

        final CmdLineArgs args = STTMExtractor.validateInput(new String[]{"-o", "./", path, path});
        assertEquals(2, args.inputPaths.length);
    }

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_valid_dir_and_output_dir() {
        final String path = getValidPath();

        final CmdLineArgs args = STTMExtractor.validateInput(new String[]{"-o", path, path});

        assertTrue(args.outputPath.contains("snap-test-utils"));
        assertEquals(1, args.inputPaths.length);
    }

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_output_dir_invalid() {
        final String path = getValidPath();

        try {
            STTMExtractor.validateInput(new String[]{"-o", "output-path", path});
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }

        try {
            STTMExtractor.validateInput(new String[]{"-o"});
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_baseUrl() {
        final String path = getValidPath();

        final CmdLineArgs args = STTMExtractor.validateInput(new String[]{"-o", path, "-u", "whatever/dont_care", path});
        assertEquals("whatever/dont_care", args.jiraBaseUrl);
    }

    @Test
    @STTM("SNAP-3506")
    public void testGetJiraBaseUrl() {
        final CmdLineArgs args = new CmdLineArgs();

        String url = STTMExtractor.getJiraBaseUrl(args);
        assertEquals(url, "https://senbox.atlassian.net/jira/software/c/projects/SNAP/issues/");

        args.jiraBaseUrl = "http://wherever/we/go/";
        url = STTMExtractor.getJiraBaseUrl(args);
        assertEquals(url, args.jiraBaseUrl);
    }

    @Test
    @STTM("SNAP-3506")
    public void testPrintUsageTo() {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final String ls = System.lineSeparator();

        STTMExtractor.printUsageTo(outputStream);

        assertEquals("STTMExtractor usage:" + ls + ls +
                        "-o <outdir>       Defines the output directory to write the report to." + ls +
                        "-u <jiraUrl>      Defines base url for Jira ticket reference (optional)." + ls +
                        "<path> ... <path> Defines code paths to be parsed as blank separated list." + ls,
                outputStream.toString());
    }
}
