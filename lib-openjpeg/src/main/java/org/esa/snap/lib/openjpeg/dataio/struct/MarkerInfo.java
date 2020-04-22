package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class MarkerInfo extends Structure {

    private final List<String> fieldNames = Arrays.asList("type", "pos", "len");

    /** marker type */
    public short type;
    /**
     * position in codestream<br>
     * C type : OPJ_OFF_T
     */
    public long pos;
    /** length, marker val included */
    public int len;

    public MarkerInfo() {
        super();
    }
    /**
     * @param type marker type<br>
     * @param pos position in codestream<br>
     * C type : OPJ_OFF_T<br>
     * @param len length, marker val included
     */
    public MarkerInfo(short type, long pos, int len) {
        super();
        this.type = type;
        this.pos = pos;
        this.len = len;
    }

    public MarkerInfo(Pointer peer) {
        super(peer);
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends MarkerInfo implements Structure.ByReference {
    }

    public static class ByValue extends MarkerInfo implements Structure.ByValue {
    }
}
