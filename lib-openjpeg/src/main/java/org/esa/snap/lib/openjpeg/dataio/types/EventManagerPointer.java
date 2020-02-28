package org.esa.snap.lib.openjpeg.dataio.types;

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

public class EventManagerPointer extends PointerType {

    public EventManagerPointer(Pointer address) {
        super(address);
    }

    public EventManagerPointer() {
        super();
    }
}