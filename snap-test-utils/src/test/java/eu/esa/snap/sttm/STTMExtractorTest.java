package eu.esa.snap.sttm;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;
//import com.bc.ceres.annotation.STTM;

import java.io.File;
import java.io.IOException;

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
}
