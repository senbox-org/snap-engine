package org.esa.snap.jp2.reader;

import org.esa.snap.lib.openjpeg.activator.OpenJPEGInstaller;
import org.junit.Before;

public class AbstractJP2Test {

    private static boolean openJPEGInstalled = false;

    @Before
    public final void setUp(){
        if(!openJPEGInstalled) {
            OpenJPEGInstaller.install();
            openJPEGInstalled = true;
        }
    }
}
