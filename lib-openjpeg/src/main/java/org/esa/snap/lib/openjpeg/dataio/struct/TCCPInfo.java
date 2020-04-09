package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TCCPInfo extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("compno", "csty", "numresolutions", "cblkw", "cblkh", "cblksty", "qmfbid", "qntsty",
                    "stepsizes_mant", "stepsizes_expn", "numgbits", "roishift", "prcw", "prch");

    /**
     * component index
     * C type : OPJ_UINT32
     */
    public int compno;
    /**
     * coding style
     * C type : OPJ_UINT32
     */
    public int csty;
    /**
     * number of resolutions
     * C type : OPJ_UINT32
     */
    public int numresolutions;
    /**
     * code-blocks width
     * C type : OPJ_UINT32
     */
    public int cblkw;
    /**
     * code-blocks height
     * C type : OPJ_UINT32
     */
    public int cblkh;
    /**
     * code-block coding style
     * C type : OPJ_UINT32
     */
    public int cblksty;
    /**
     * discrete wavelet transform identifier
     * C type : OPJ_UINT32
     */
    public int qmfbid;
    /**
     * quantisation style
     * C type : OPJ_UINT32
     */
    public int qntsty;
    /**
     * stepsizes used for quantization
     * C type : OPJ_UINT32[(3 * 33 - 2)]
     */
    public int[] stepsizes_mant = new int[3 * 33 - 2];
    /**
     * stepsizes used for quantization
     * C type : OPJ_UINT32[(3 * 33 - 2)]
     */
    public int[] stepsizes_expn = new int[3 * 33 - 2];
    /**
     * number of guard bits
     * C type : OPJ_UINT32
     */
    public int numgbits;
    /**
     * Region Of Interest shift
     * C type : OPJ_INT32
     */
    public int roishift;
    /**
     * precinct width
     * C type : OPJ_UINT32[33]
     */
    public int[] prcw = new int[33];
    /**
     * precinct height
     * C type : OPJ_UINT32[33]
     */
    public int[] prch = new int[33];

    public TCCPInfo() {
        super();
    }

    public TCCPInfo(Pointer peer) {
        super(peer);
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends TCCPInfo implements Structure.ByReference {
    }

    public static class ByValue extends TCCPInfo implements Structure.ByValue {
    }
}
