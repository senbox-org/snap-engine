package eu.esa.snap.sttm;

import org.junit.Test;
//import com.bc.ceres.annotation.STTM;

import static org.junit.Assert.fail;

public class STTMExtractorTest {

    @Test
    //@STTM("SNAP-3506")
    public void testValidateInput_no_args()  {
        try {
            STTMExtractor.validateInput(new String[0]);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException expected) {
        }
    }
}
