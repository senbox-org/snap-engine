package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * Header source:   openjpeg.h
 * C structure:     opj_codestream_info_v2
 */
public class CodestreamInfo2 extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("tx0", "ty0", "tdx", "tdy", "tw", "th", "nbcomps", "m_default_tile_info", "tile_info");
    /**
     * tile origin in x = XTOsiz
     */
    public int tx0; /* OPJ_UINT32 */
    /**
     * tile origin in y = YTOsiz
     */
    public int ty0; /* OPJ_UINT32 */
    /**
     * tile size in x = XTsiz
     */
    public int tdx; /* OPJ_UINT32 */
    /**
     * tile size in y = YTsiz
     */
    public int tdy; /* OPJ_UINT32 */
    /**
     * number of tiles in X
     */
    public int tw; /* OPJ_UINT32 */
    /**
     * number of tiles in Y
     */
    public int th; /* OPJ_UINT32 */
    /**
     * number of components
     */
    public int nbcomps; /* OPJ_UINT32 */
    /**
     * Default information regarding tiles inside image
     */
    public TileInfo2 m_default_tile_info; /* opj_tile_info_v2_t */
    /**
     * information regarding tiles inside image
     */
    public TileInfo2.ByReference tile_info; /* opj_tile_info_v2_t* */
    
    public CodestreamInfo2() {
        super();
    }

    public CodestreamInfo2(Pointer peer) {
        super(peer);
    }

    public CodestreamInfo2(int tx0, int ty0, int tdx, int tdy, int tw, int th, int nbcomps, TileInfo2 m_default_tile_info, TileInfo2.ByReference tile_info) {
        super();
        this.tx0 = tx0;
        this.ty0 = ty0;
        this.tdx = tdx;
        this.tdy = tdy;
        this.tw = tw;
        this.th = th;
        this.nbcomps = nbcomps;
        this.m_default_tile_info = m_default_tile_info;
        this.tile_info = tile_info;
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends CodestreamInfo2 implements Structure.ByReference {
    }

    public static class ByValue extends CodestreamInfo2 implements Structure.ByValue {
    }
}
