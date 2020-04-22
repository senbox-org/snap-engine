package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class PoC extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("resno0", "compno0", "layno1", "resno1", "compno1", "layno0", "precno0", "precno1", "prg1",
                    "prg", "progorder", "tile", "tx0", "tx1", "ty0", "ty1", "layS", "resS", "compS", "prcS", "layE",
                    "resE", "compE", "prcE", "txS", "txE", "tyS", "tyE", "dx", "dy", "lay_t", "res_t", "comp_t",
                    "prc_t", "tx0_t", "ty0_t");

    /**
     * Resolution num start, Component num start, given by POC
     * C type : OPJ_UINT32
     */
    public int resno0;
    /**
     * Resolution num start, Component num start, given by POC
     * C type : OPJ_UINT32
     */
    public int compno0;
    /**
     * Layer num end,Resolution num end, Component num end, given by POC
     * C type : OPJ_UINT32
     */
    public int layno1;
    /**
     * Layer num end,Resolution num end, Component num end, given by POC
     * C type : OPJ_UINT32
     */
    public int resno1;
    /**
     * Layer num end,Resolution num end, Component num end, given by POC
     * C type : OPJ_UINT32
     */
    public int compno1;
    /**
     * Layer num start,Precinct num start, Precinct num end
     * C type : OPJ_UINT32
     */
    public int layno0;
    /**
     * Layer num start,Precinct num start, Precinct num end
     * C type : OPJ_UINT32
     */
    public int precno0;
    /**
     * Layer num start,Precinct num start, Precinct num end
     * C type : OPJ_UINT32
     */
    public int precno1;
    /**
     * Progression order enum
     * C type : OPJ_PROG_ORDER
     */
    public int prg1;
    /**
     * Progression order enum
     * C type : OPJ_PROG_ORDER
     */
    public int prg;
    /**
     * Progression order string
     * C type : OPJ_CHAR[5]
     */
    public byte[] progorder = new byte[5];
    /**
     * Tile number
     * C type : OPJ_UINT32
     */
    public int tile;
    /**
     * Start and end values for Tile width and height
     * C type : OPJ_INT32
     */
    public int tx0;
    /**
     * Start and end values for Tile width and height
     * C type : OPJ_INT32
     */
    public int tx1;
    /**
     * Start and end values for Tile width and height
     * C type : OPJ_INT32
     */
    public int ty0;
    /**
     * Start and end values for Tile width and height
     * C type : OPJ_INT32
     */
    public int ty1;
    /**
     * Start value, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int layS;
    /**
     * Start value, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int resS;
    /**
     * Start value, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int compS;
    /**
     * Start value, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int prcS;
    /**
     * End value, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int layE;
    /**
     * End value, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int resE;
    /**
     * End value, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int compE;
    /**
     * End value, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int prcE;
    /**
     * Start and end values of Tile width and height, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int txS;
    /**
     * Start and end values of Tile width and height, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int txE;
    /**
     * Start and end values of Tile width and height, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int tyS;
    /**
     * Start and end values of Tile width and height, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int tyE;
    /**
     * Start and end values of Tile width and height, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int dx;
    /**
     * Start and end values of Tile width and height, initialised in pi_initialise_encode
     * C type : OPJ_UINT32
     */
    public int dy;
    /**
     * Temporary values for Tile parts, initialised in pi_create_encode
     * C type : OPJ_UINT32
     */
    public int lay_t;
    /**
     * Temporary values for Tile parts, initialised in pi_create_encode
     * C type : OPJ_UINT32
     */
    public int res_t;
    /**
     * Temporary values for Tile parts, initialised in pi_create_encode
     * C type : OPJ_UINT32
     */
    public int comp_t;
    /**
     * Temporary values for Tile parts, initialised in pi_create_encode
     * C type : OPJ_UINT32
     */
    public int prc_t;
    /**
     * Temporary values for Tile parts, initialised in pi_create_encode
     * C type : OPJ_UINT32
     */
    public int tx0_t;
    /**
     * Temporary values for Tile parts, initialised in pi_create_encode
     * C type : OPJ_UINT32
     */
    public int ty0_t;

    public PoC() {
        super();
    }

    public PoC(Pointer peer) {
        super(peer);
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends PoC implements Structure.ByReference {
    }

    public static class ByValue extends PoC implements Structure.ByValue {
    }
}
