package org.esa.snap.engine_utilities.dataio;

import com.bc.ceres.annotation.STTM;
import org.esa.snap.engine_utilities.commons.AbstractVirtualPath;
import org.junit.Test;

import static org.mockito.Mockito.*;


public class VirtualDirWrapperTest {


    @Test
    @STTM("SNAP-4105")
    public void test_close_closesWrappedOnlyOnce() {
        AbstractVirtualPath wrapped = mock(AbstractVirtualPath.class);

        VirtualDirWrapper wrapper = new VirtualDirWrapper(wrapped);
        wrapper.close();
        wrapper.close();

        verify(wrapped, times(1)).close();
    }
}