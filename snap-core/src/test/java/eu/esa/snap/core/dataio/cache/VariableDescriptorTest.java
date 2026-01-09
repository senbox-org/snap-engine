package eu.esa.snap.core.dataio.cache;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class VariableDescriptorTest {

    @Test
    @STTM("SNAP-4107")
    public void testConstruction() {
        VariableDescriptor variableDescriptor = new VariableDescriptor();
        assertFalse(variableDescriptor.scaled);
    }
}
