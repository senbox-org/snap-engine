package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class PacketInfo extends Structure {

    private static final List<String> fieldNames = Arrays.asList("start_pos", "end_ph_pos", "end_pos", "disto");

    /**
     * packet start position (including SOP marker if it exists)<br>
     * C type : OPJ_OFF_T
     */
    public long start_pos;
    /**
     * end of packet header position (including EPH marker if it exists)<br>
     * C type : OPJ_OFF_T
     */
    public long end_ph_pos;
    /**
     * packet end position<br>
     * C type : OPJ_OFF_T
     */
    public long end_pos;
    /** packet distorsion */
    public double disto;

    public PacketInfo() {
        super();
    }
    /**
     * @param start_pos packet start position (including SOP marker if it exists)<br>
     * C type : OPJ_OFF_T<br>
     * @param end_ph_pos end of packet header position (including EPH marker if it exists)<br>
     * C type : OPJ_OFF_T<br>
     * @param end_pos packet end position<br>
     * C type : OPJ_OFF_T<br>
     * @param disto packet distorsion
     */
    public PacketInfo(long start_pos, long end_ph_pos, long end_pos, double disto) {
        super();
        this.start_pos = start_pos;
        this.end_ph_pos = end_ph_pos;
        this.end_pos = end_pos;
        this.disto = disto;
    }

    public PacketInfo(Pointer peer) {
        super(peer);
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends PacketInfo implements Structure.ByReference {
    }

    public static class ByValue extends PacketInfo implements Structure.ByValue {
    }
}
