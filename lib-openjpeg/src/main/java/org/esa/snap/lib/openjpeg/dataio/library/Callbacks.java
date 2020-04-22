package org.esa.snap.lib.openjpeg.dataio.library;

import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.esa.snap.lib.openjpeg.dataio.types.EventManagerPointer;
import org.esa.snap.lib.openjpeg.dataio.types.FilePointer;
import org.esa.snap.lib.openjpeg.dataio.types.NativeSize;
import org.esa.snap.lib.openjpeg.dataio.struct.*;

/**
 * Callbacks (i.e. function pointers) holder class.
 */
public class Callbacks {

    public interface MessageFunction extends GenericCallback {
        void invoke(Pointer msg, Pointer client_data);
    }

    public interface StreamReadWriteFunction extends Callback {
        NativeSize invoke(Pointer p_buffer, NativeSize p_nb_bytes, Pointer p_user_data);
    }

    public interface StreamSkipSeekFunction extends Callback {
        long invoke(long p_nb_bytes, Pointer p_user_data);
    }

    public interface StreamFreeUserDataFunction extends Callback {
        void invoke(Pointer p_user_data);
    }

    public interface ReadHeaderCallback extends Callback {
        int invoke(Stream cio, Pointer p_codec, PointerByReference p_image, EventManagerPointer p_manager);
    }
    public interface DecodeCallback extends Callback {
        int invoke(Pointer p_codec, Stream p_cio, Image p_image, EventManagerPointer p_manager);
    }
    public interface ReadTileHeaderCallback extends Callback {
        int invoke(Pointer p_codec, int p_tile_index, int p_data_size, int p_tile_x0, int p_tile_y0, int p_tile_x1, int p_tile_y1, int p_nb_comps, int p_should_go_on, Stream p_cio, EventManagerPointer p_manager);
    }
    public interface DecodeTileDataCallback extends Callback {
        int invoke(Pointer p_codec, int p_tile_index, byte p_data, int p_data_size, Stream p_cio, EventManagerPointer p_manager);
    }
    public interface EndDecompressCallback extends Callback {
        int invoke(Pointer p_codec, Stream cio, EventManagerPointer p_manager);
    }
    public interface DestroyCallback extends Callback {
        void invoke(Pointer p_codec);
    }
    public interface SetupDecoderCallback extends Callback {
        void invoke(Pointer p_codec, DecompressCoreParams p_param);
    }
    public interface SetDecodeAreaCallback extends Callback {
        int invoke(Pointer p_codec, Image p_image, int p_start_x, int p_end_x, int p_start_y, int p_end_y, EventManagerPointer p_manager);
    }
    public interface GetDecodedTileCallback extends Callback {
        int invoke(Pointer p_codec, Stream p_cio, Image p_image, EventManagerPointer p_manager, int tile_index);
    }
    public interface SetDecodedResolutionFactorCallback extends Callback {
        int invoke(Pointer p_codec, int res_factor, EventManagerPointer p_manager);
    }

    public interface StartCompressCallback extends Callback {
        int invoke(Pointer p_codec, Stream cio, Image p_image, EventManagerPointer p_manager);
    }
    public interface EncodeCallback extends Callback {
        int invoke(Pointer p_codec, Stream p_cio, EventManagerPointer p_manager);
    }
    public interface WriteTileCallback extends Callback {
        int invoke(Pointer p_codec, int p_tile_index, byte p_data, int p_data_size, Stream p_cio, EventManagerPointer p_manager);
    }
    public interface EndCompressCallback extends Callback {
        int invoke(Pointer p_codec, Stream p_cio, EventManagerPointer p_manager);
    }
    
    public interface SetupEncoderCallback extends Callback {
        int invoke(Pointer p_codec, CompressionParams p_param, Image p_image, EventManagerPointer p_manager);
    }

    public interface DumpCodecCallback extends Callback {
        void invoke(Pointer p_codec, int info_flag, FilePointer output_stream);
    }
    public interface GetCodecInfoCallback extends Callback {
        CodestreamInfo2 invoke(Pointer p_codec);
    }
    public interface GetCodecIndexCallback extends Callback {
        CodestreamIndex invoke(Pointer p_codec);
    }
    public interface GenericCallback extends Callback {
        Pointer invoke(Pointer p_codec);
    }
}
