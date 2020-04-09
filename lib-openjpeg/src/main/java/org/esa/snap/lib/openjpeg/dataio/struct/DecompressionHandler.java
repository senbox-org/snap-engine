package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.DecodeCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.DecodeTileDataCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.DestroyCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.EndDecompressCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.GetDecodedTileCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.ReadHeaderCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.ReadTileHeaderCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.SetDecodeAreaCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.SetDecodedResolutionFactorCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.SetupDecoderCallback;

import java.util.Arrays;
import java.util.List;

/**
 * Header source:   opj_codec.h
 * C structure:     opj_codec_private_t.opj_decompression
 *
 * Structure for holding decompression-related function pointers
 */
public class DecompressionHandler extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("opj_read_header", "opj_decode", "opj_read_tile_header", "opj_decode_tile_data",
                    "opj_end_decompress", "opj_destroy", "opj_setup_decoder", "opj_set_decode_area",
                    "opj_get_decoded_tile", "opj_set_decoded_resolution_factor");

    /**
     * Main header reading function handler
     * OPJ_BOOL (*opj_read_header) ( struct opj_stream_private * cio, void * p_codec, opj_image_t **p_image, struct opj_event_mgr * p_manager)
     * */
    public ReadHeaderCallback opj_read_header;
    /**
     * Decoding function
     * OPJ_BOOL (*opj_decode) ( void * p_codec, struct opj_stream_private * p_cio, opj_image_t * p_image, struct opj_event_mgr * p_manager)
     */
    public DecodeCallback opj_decode;
    /**
     * OPJ_BOOL (*opj_read_tile_header)( void * p_codec, OPJ_UINT32 * p_tile_index, OPJ_UINT32 * p_data_size, OPJ_INT32 * p_tile_x0, OPJ_INT32 * p_tile_y0, OPJ_INT32 * p_tile_x1,
     *                                   OPJ_INT32 * p_tile_y1, OPJ_UINT32 * p_nb_comps, OPJ_BOOL * p_should_go_on, struct opj_stream_private * p_cio, struct opj_event_mgr * p_manager)
     */
    public ReadTileHeaderCallback opj_read_tile_header;
    /**
     * OPJ_BOOL (*opj_decode_tile_data)( void * p_codec, OPJ_UINT32 p_tile_index, OPJ_BYTE * p_data, OPJ_UINT32 p_data_size, struct opj_stream_private * p_cio, struct opj_event_mgr * p_manager)
     */
    public DecodeTileDataCallback opj_decode_tile_data;
    /**
     * Reading function used after codestream if necessary
     * OPJ_BOOL (* opj_end_decompress) ( void *p_codec, struct opj_stream_private * cio, struct opj_event_mgr * p_manager)
     */
    public EndDecompressCallback opj_end_decompress;
    /**
     * Codec destroy function handler
     * void (*opj_destroy) (void * p_codec);
     */
    public DestroyCallback opj_destroy;
    /**
     * Setup decoder function handler
     * void (*opj_setup_decoder) ( void * p_codec, opj_dparameters_t * p_param)
     */
    public SetupDecoderCallback opj_setup_decoder;
    /**
     * Set decode area function handler
     * OPJ_BOOL (*opj_set_decode_area) ( void * p_codec, opj_image_t * p_image, OPJ_INT32 p_start_x, OPJ_INT32 p_end_x, OPJ_INT32 p_start_y, OPJ_INT32 p_end_y, struct opj_event_mgr * p_manager)
     */
    public SetDecodeAreaCallback opj_set_decode_area;
    /**
     * Get tile function
     * OPJ_BOOL (*opj_get_decoded_tile) ( void *p_codec, opj_stream_private_t * p_cio, opj_image_t *p_image, struct opj_event_mgr * p_manager, OPJ_UINT32 tile_index)
     */
    public GetDecodedTileCallback opj_get_decoded_tile;
    /**
     * Set the decoded resolution factor  OPJ_BOOL (*opj_set_decoded_resolution_factor) ( void * p_codec, OPJ_UINT32 res_factor, opj_event_mgr_t * p_manager)
     */
    public SetDecodedResolutionFactorCallback opj_set_decoded_resolution_factor;

    public DecompressionHandler() {
        super();
    }

    public DecompressionHandler(Pointer peer) {
        super(peer);
    }

    public DecompressionHandler(ReadHeaderCallback opj_read_header, DecodeCallback opj_decode, ReadTileHeaderCallback opj_read_tile_header, DecodeTileDataCallback opj_decode_tile_data, EndDecompressCallback opj_end_decompress, DestroyCallback opj_destroy, SetupDecoderCallback opj_setup_decoder, SetDecodeAreaCallback opj_set_decode_area, GetDecodedTileCallback opj_get_decoded_tile, SetDecodedResolutionFactorCallback opj_set_decoded_resolution_factor) {
        super();
        this.opj_read_header = opj_read_header;
        this.opj_decode = opj_decode;
        this.opj_read_tile_header = opj_read_tile_header;
        this.opj_decode_tile_data = opj_decode_tile_data;
        this.opj_end_decompress = opj_end_decompress;
        this.opj_destroy = opj_destroy;
        this.opj_setup_decoder = opj_setup_decoder;
        this.opj_set_decode_area = opj_set_decode_area;
        this.opj_get_decoded_tile = opj_get_decoded_tile;
        this.opj_set_decoded_resolution_factor = opj_set_decoded_resolution_factor;
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends DecompressionHandler implements Structure.ByReference { }
    public static class ByValue extends DecompressionHandler implements Structure.ByValue { }
}
