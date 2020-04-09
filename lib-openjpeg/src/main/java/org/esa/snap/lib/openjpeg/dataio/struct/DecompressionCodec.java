package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Union;
import com.sun.jna.ptr.PointerByReference;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks;
import org.esa.snap.lib.openjpeg.dataio.types.EventManagerPointer;

import java.util.Arrays;
import java.util.List;

/**
 * Header source:   opj_codec.h
 * C structure:     opj_codec_private_t
 */
public class DecompressionCodec extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("m_codec_data", "m_codec", "m_event_mgr", "is_decompressor", "opj_dump_codec", "opj_get_codec_info", "opj_get_codec_index");

    /**
     * Union for decompression/compression handlers
     */
    public CodecUnion m_codec_data;
    /**
     * Actual codec pointer
     */
    public PointerByReference m_codec; /* (void *) */
    /**
     * Event handler
     */
    public EventManagerPointer m_event_mgr; /* opj_event_mgr_t */
    /**
     * Flag to indicate if the codec is used to decode or encode
     * */
    public int is_decompressor;
    /**
     * void (*opj_dump_codec) (void * p_codec, OPJ_INT32 info_flag, FILE* output_stream)
     */
    public Callbacks.DumpCodecCallback opj_dump_codec;
    /**
     * opj_codestream_info_v2_t* (*opj_get_codec_info)(void* p_codec)
     */
    public Callbacks.GenericCallback opj_get_codec_info;
    /**
     * opj_codestream_index_t* (*opj_get_codec_index)(void* p_codec)
     */
    public Callbacks.GenericCallback opj_get_codec_index;

    public DecompressionCodec() {
        super();
        m_event_mgr = new EventManagerPointer();
        is_decompressor = 1;
    }

    public DecompressionCodec(Pointer peer) {
        super(peer);
    }

    public DecompressionCodec(CodecUnion m_codec_data, PointerByReference m_codec, EventManagerPointer m_event_mgr, int is_decompressor, Callbacks.DumpCodecCallback opj_dump_codec, Callbacks.GenericCallback opj_get_codec_info, Callbacks.GenericCallback opj_get_codec_index) {
        super();
        this.m_codec_data = m_codec_data;
        this.m_codec = m_codec;
        this.m_event_mgr = m_event_mgr;
        this.is_decompressor = is_decompressor;
        this.opj_dump_codec = opj_dump_codec;
        this.opj_get_codec_info = opj_get_codec_info;
        this.opj_get_codec_index = opj_get_codec_index;
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends DecompressionCodec implements Structure.ByReference { }

    public static class ByValue extends DecompressionCodec implements Structure.ByValue { }

    public static class CodecUnion extends Union {
        public DecompressionHandler m_decompression;
        public CompressionHandler m_compression;

        public CodecUnion() {
            setType(DecompressionHandler.class);
        }

        public CodecUnion(Pointer peer) {
            super(peer);
            setType(DecompressionHandler.class);
        }

        public static class ByReference extends CodecUnion implements Structure.ByReference { }
    }
}
