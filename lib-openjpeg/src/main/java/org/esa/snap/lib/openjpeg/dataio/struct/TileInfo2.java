package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TileInfo2 extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("tileno", "csty", "prg", "numlayers", "mct", "tccp_info");

    /** number (index) of tile */
    public int tileno;
    /**
     * coding style<br>
     * C type : OPJ_UINT32
     */
    public int csty;
    /**
     * progression order<br>
     * C type : OPJ_PROG_ORDER
     */
    public int prg;
    /**
     * number of layers<br>
     * C type : OPJ_UINT32
     */
    public int numlayers;
    /**
     * multi-component transform identifier<br>
     * C type : OPJ_UINT32
     */
    public int mct;
    /**
     * information concerning tile component parameters<br>
     * C type : opj_tccp_info_t*
     */
    public TCCPInfo.ByReference tccp_info;

    public TileInfo2() {
        super();
    }
    /**
     * @param tileno number (index) of tile<br>
     * @param csty coding style<br>
     * C type : OPJ_UINT32<br>
     * @param prg progression order<br>
     * C type : OPJ_PROG_ORDER<br>
     * @param numlayers number of layers<br>
     * C type : OPJ_UINT32<br>
     * @param mct multi-component transform identifier<br>
     * C type : OPJ_UINT32<br>
     * @param tccp_info information concerning tile component parameters<br>
     * C type : opj_tccp_info_t*
     */
    public TileInfo2(int tileno, int csty, int prg, int numlayers, int mct, TCCPInfo.ByReference tccp_info) {
        super();
        this.tileno = tileno;
        this.csty = csty;
        this.prg = prg;
        this.numlayers = numlayers;
        this.mct = mct;
        this.tccp_info = tccp_info;
    }
    public TileInfo2(Pointer peer) {
        super(peer);
    }

    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends TileInfo2 implements Structure.ByReference { }
    public static class ByValue extends TileInfo2 implements Structure.ByValue { }
}
