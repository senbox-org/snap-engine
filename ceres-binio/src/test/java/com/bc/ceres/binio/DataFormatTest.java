package com.bc.ceres.binio;

import com.bc.ceres.annotation.STTM;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;


public class DataFormatTest {

    @Test
    @STTM("SNAP-4105")
    public void test_createContext_fileMode_registersValidCleanupState() throws Exception {
        DataFormat format = new DataFormat();
        File file = File.createTempFile("dataformat", ".bin");
        try {
            DataContext context = format.createContext(file, "rw");
            assertNotNull(context);
            context.dispose();
        } finally {
            file.delete();
        }
    }
}