package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class CodestreamIndex extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("main_head_start", "main_head_end", "codestream_size", "marknum", "marker", "maxmarknum", "nb_of_tiles", "tile_index");
    /**
     * main header start position (SOC position)
     */
    public /*OPJ_OFF_T*/ long main_head_start;
    /**
     * main header end position (first SOT position)
     */
    public /*OPJ_OFF_T*/ long main_head_end;
    /**
     * codestream's size
     */
    public /*OPJ_UINT64*/ long codestream_size;
    /**
     * number of markers
     */
    public /* OPJ_UINT32 */ int marknum;
    /**
     * list of markers
     */
    public /* opj_marker_info_t* */ MarkerInfo.ByReference marker;
    /**
     * actual size of markers array
     */
    public /* OPJ_UINT32 */ int maxmarknum;

    public /* OPJ_UINT32 */ int nb_of_tiles;

    public /* opj_tile_index_t* */ TileIndex.ByReference tile_index;

    public CodestreamIndex() {
        super();
    }

    public CodestreamIndex(Pointer peer) {
        super(peer);
    }

    public CodestreamIndex(long main_head_start, long main_head_end, long codestream_size, int marknum, MarkerInfo.ByReference marker, int maxmarknum, int nb_of_tiles, TileIndex.ByReference tile_index) {
        super();
        this.main_head_start = main_head_start;
        this.main_head_end = main_head_end;
        this.codestream_size = codestream_size;
        this.marknum = marknum;
        this.marker = marker;
        this.maxmarknum = maxmarknum;
        this.nb_of_tiles = nb_of_tiles;
        this.tile_index = tile_index;
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends CodestreamIndex implements Structure.ByReference {
    }

    public static class ByValue extends CodestreamIndex implements Structure.ByValue {
    }
}
