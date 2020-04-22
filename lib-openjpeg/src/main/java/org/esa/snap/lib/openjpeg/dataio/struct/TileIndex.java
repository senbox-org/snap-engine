package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

public class TileIndex extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("tileno", "nb_tps", "current_nb_tps", "current_tpsno", "tp_index", "marknum", "marker",
                    "maxmarknum", "nb_packet", "packet_index");

    /**
     * tile index<br>
     * C type : OPJ_UINT32
     */
    public int tileno;
    /**
     * number of tile parts<br>
     * C type : OPJ_UINT32
     */
    public int nb_tps;
    /**
     * current nb of tile part (allocated)<br>
     * C type : OPJ_UINT32
     */
    public int current_nb_tps;
    /**
     * current tile-part index<br>
     * C type : OPJ_UINT32
     */
    public int current_tpsno;
    /**
     * information concerning tile parts<br>
     * C type : opj_tp_index_t*
     */
    public TPIndex.ByReference tp_index;
    /**
     * number of markers<br>
     * C type : OPJ_UINT32
     */
    public int marknum;
    /**
     * list of markers<br>
     * C type : opj_marker_info_t*
     */
    public MarkerInfo.ByReference marker;
    /**
     * actual size of markers array<br>
     * C type : OPJ_UINT32
     */
    public int maxmarknum;
    /**
     * packet number<br>
     * C type : OPJ_UINT32
     */
    public int nb_packet;
    /**
     * information concerning packets inside tile<br>
     * C type : opj_packet_info_t*
     */
    public PacketInfo.ByReference packet_index;

    public TileIndex() {
        super();
    }

    public TileIndex(Pointer peer) {
        super(peer);
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends TileIndex implements Structure.ByReference {
    }

    public static class ByValue extends TileIndex implements Structure.ByValue {
    }
}
