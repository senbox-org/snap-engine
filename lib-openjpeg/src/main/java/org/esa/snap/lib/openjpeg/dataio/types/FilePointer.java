package org.esa.snap.lib.openjpeg.dataio.types;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public class FilePointer extends PointerType {

    public FilePointer(Pointer address) {
        super(address);
    }

    public FilePointer() {
        super();
    }
}
