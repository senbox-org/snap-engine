package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.DestroyCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.EncodeCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.EndCompressCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.SetupEncoderCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.StartCompressCallback;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks.WriteTileCallback;

import java.util.Arrays;
import java.util.List;

/**
 * Header source:   opj_codec.h
 * C structure:     opj_codec_private_t.opj_compression
 *
 * Structure for holding compression-related function pointers
 */
public class CompressionHandler extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("opj_start_compress", "opj_encode", "opj_write_tile", "opj_end_compress", "opj_destroy", "opj_setup_encoder");

    /**
     * OPJ_BOOL (* opj_start_compress) (void *p_codec, struct opj_stream_private *cio, struct opj_image *p_image, struct opj_event_mgr *p_manager)
     */
    public StartCompressCallback opj_start_compress;
    /**
     * OPJ_BOOL (* opj_encode) (void *p_codec, struct opj_stream_private *p_cio, struct opj_event_mgr *p_manager)
     * */
    public EncodeCallback opj_encode;
    /**
     * OPJ_BOOL (* opj_write_tile) (void *p_codec, OPJ_UINT32 p_tile_index, OPJ_BYTE *p_data, OPJ_UINT32 p_data_size, struct opj_stream_private *p_cio, struct opj_event_mgr *p_manager)
     */
    public WriteTileCallback opj_write_tile;
    /**
     * OPJ_BOOL (* opj_end_compress) (void *p_codec, struct opj_stream_private *p_cio, struct opj_event_mgr *p_manager) 
     */
    public EndCompressCallback opj_end_compress;
    /**
     * void (* opj_destroy) (void *p_codec)
     */
    public DestroyCallback opj_destroy;
    /**
     * OPJ_BOOL (* opj_setup_encoder) ( void *p_codec, opj_cparameters_t *p_param, struct opj_image *p_image, struct opj_event_mgr *p_manager)
     */
    public SetupEncoderCallback opj_setup_encoder;

    public CompressionHandler() {
        super();
    }

    public CompressionHandler(Pointer peer) {
        super(peer);
    }

    public CompressionHandler(StartCompressCallback opj_start_compress, EncodeCallback opj_encode, WriteTileCallback opj_write_tile, EndCompressCallback opj_end_compress, DestroyCallback opj_destroy, SetupEncoderCallback opj_setup_encoder) {
        super();
        this.opj_start_compress = opj_start_compress;
        this.opj_encode = opj_encode;
        this.opj_write_tile = opj_write_tile;
        this.opj_end_compress = opj_end_compress;
        this.opj_destroy = opj_destroy;
        this.opj_setup_encoder = opj_setup_encoder;
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends CompressionHandler implements Structure.ByReference {
    }

    public static class ByValue extends CompressionHandler implements Structure.ByValue {
    }
}
