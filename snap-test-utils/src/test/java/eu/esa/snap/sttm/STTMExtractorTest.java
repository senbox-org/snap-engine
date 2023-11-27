package eu.esa.snap.sttm;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;
//import com.bc.ceres.annotation.STTM;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class STTMExtractorTest {

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_no_args()  {
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
                fail("unabel to delete temp file: " + tempFile.getAbsolutePath());
            }
        }
    }

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_valid_dir() {
        final String path = getValidPath();

        final CmdLineArgs args = STTMExtractor.validateInput(new String[]{path});
        assertEquals(1, args.inputPaths.length);
    }

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_two_valid_dirs() {
        final String path = getValidPath();

        final CmdLineArgs args = STTMExtractor.validateInput(new String[]{path, path});
        assertEquals(2, args.inputPaths.length);
    }

    @Test
    @STTM("SNAP-3506")
    public void testValidateInput_valid_dir_and_output_dir() {
        final String path = getValidPath();

        final CmdLineArgs args = STTMExtractor.validateInput(new String[]{"-o", "output-path", path});

        assertEquals("output-path", args.outputPath);
        assertEquals(1, args.inputPaths.length);

    }

    private static String getValidPath() {
        final String validPath = STTMExtractor.class.getClassLoader().getResource("").getPath();
        return validPath.substring(1);
    }
}
