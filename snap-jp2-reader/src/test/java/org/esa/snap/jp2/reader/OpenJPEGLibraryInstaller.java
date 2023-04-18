package org.esa.snap.jp2.reader;

import org.esa.snap.lib.openjpeg.activator.OpenJPEGInstaller;

import java.util.concurrent.atomic.AtomicBoolean;

public final class OpenJPEGLibraryInstaller {

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private OpenJPEGLibraryInstaller() {
    }

    public static void install() {
        if (!INSTALLED.getAndSet(true)) {
            OpenJPEGInstaller.install();
        }
    }
}
