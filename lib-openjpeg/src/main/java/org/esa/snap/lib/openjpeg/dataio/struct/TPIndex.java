package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TPIndex extends Structure {

    private static final List<String> fieldNames = Arrays.asList("start_pos", "end_header", "end_pos");

    /**
     * start position<br>
     * C type : OPJ_OFF_T
     */
    public long start_pos;
    /**
     * end position of the header<br>
     * C type : OPJ_OFF_T
     */
    public long end_header;
    /**
     * end position<br>
     * C type : OPJ_OFF_T
     */
    public long end_pos;
    public TPIndex() {
        super();
    }
    /**
     * @param start_pos start position<br>
     * C type : OPJ_OFF_T<br>
     * @param end_header end position of the header<br>
     * C type : OPJ_OFF_T<br>
     * @param end_pos end position<br>
     * C type : OPJ_OFF_T
     */
    public TPIndex(long start_pos, long end_header, long end_pos) {
        super();
        this.start_pos = start_pos;
        this.end_header = end_header;
        this.end_pos = end_pos;
    }

    public TPIndex(Pointer peer) {
        super(peer);
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends TPIndex implements Structure.ByReference {
    }

    public static class ByValue extends TPIndex implements Structure.ByValue {
    }
}
