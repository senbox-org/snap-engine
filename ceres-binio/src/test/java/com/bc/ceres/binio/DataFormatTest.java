package com.bc.ceres.binio;

import com.bc.ceres.annotation.STTM;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.Assert.assertNotNull;


public class DataFormatTest {

    @Test
    @STTM("SNAP-4105")
    public void test_createContext_fileMode_registersValidCleanupState() throws Exception {
        DataFormat format = new DataFormat();
        Path file = IOHelper.createTempFile("dataformat", ".bin");
        try {
            DataContext context = format.createContext(file, StandardOpenOption.READ, StandardOpenOption.WRITE);
            assertNotNull(context);
            context.dispose();
        } finally {
            IOHelper.delete(file);
        }
    }
}