package org.esa.snap.lib.openjpeg.dataio.library;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import org.esa.snap.lib.openjpeg.dataio.types.NativeSize;
import org.esa.snap.lib.openjpeg.dataio.struct.*;
import org.esa.snap.lib.openjpeg.utils.OpenJpegExecRetriever;

/**
 * JNA Wrapper for library <b>OpenJP2</b>.
 * This file was created after the version generated with the help of <a href="http://jnaerator.googlecode.com/">JNAerator</a>.
 *
 * @author Cosmin Cara
 */
public class OpenJp2 implements StdCallLibrary {
    private static final String JNA_LIBRARY_NAME = OpenJpegExecRetriever.getOpenJp2();

    static {
        Native.register(OpenJp2.class, JNA_LIBRARY_NAME);
        /*if (!Native.isProtected()) {
            Native.setProtected(true);
        }*/
    }

    /**
     * Get the version of the struct library
     * Original signature : <code>char* opj_version()</code>
     */
    public static native Pointer opj_version();
    /**
     * Create an image
     * * @param numcmpts      number of components
     * @param cmptparms     components parameters
     * @param clrspc        image color space
     * @return returns      a new image structure if successful, returns NULL otherwise
     * Original signature : <code>opj_image_t* opj_image_create(OPJ_UINT32, opj_image_cmptparm_t*, OPJ_COLOR_SPACE)</code>
     */
    public static native PointerByReference opj_image_create(int numcmpts, ImageComponentParams cmptparms, int clrspc);
    /**
     * Deallocate any resources associated with an image
     * * @param image         image to be destroyed
     * Original signature : <code>void opj_image_destroy(opj_image_t*)</code>
     * <i>native declaration : line 1097</i>
     */
    public static native void opj_image_destroy(Pointer image);
    /**
     * Creates an image without allocating memory for the image (used in the new version of the library).
     * * @param	numcmpts    the number of components
     * @param	cmptparms   the components parameters
     * @param	clrspc      the image color space
     * * @return	a new image structure if successful, NULL otherwise.
     * Original signature : <code>opj_image_t* opj_image_tile_create(OPJ_UINT32, opj_image_cmptparm_t*, OPJ_COLOR_SPACE)</code>
     */
    public static native Image opj_image_tile_create(int numcmpts, ImageComponentParams cmptparms, int clrspc);
    /**
     * Creates an abstract stream. This function does nothing except allocating memory and initializing the abstract stream.
     * * @param	p_is_input		if set to true then the stream will be an input stream, an output stream else.
     * * @return	a stream object.
     * Original signature : <code>opj_stream_t* opj_stream_default_create(OPJ_BOOL)</code>
     */
    public static native PointerByReference opj_stream_default_create(int p_is_input);
    /**
     * Creates an abstract stream. This function does nothing except allocating memory and initializing the abstract stream.
     * * @param	p_buffer_size  
     * @param	p_is_input		if set to true then the stream will be an input stream, an output stream else.
     * * @return	a stream object.
     * Original signature : <code>opj_stream_t* opj_stream_create(OPJ_SIZE_T, OPJ_BOOL)</code>
     */
    public static native PointerByReference opj_stream_create(NativeSize p_buffer_size, int p_is_input);
    /**
     * Destroys a stream created by opj_create_stream. This function does NOT close the abstract stream. If needed the user must
     * close its own implementation of the stream.
     * * @param	p_stream	the stream to destroy.
     * Original signature : <code>void opj_stream_destroy(opj_stream_t*)</code>
     */
    public static native void opj_stream_destroy(PointerByReference p_stream);
    /**
     * Sets the given function to be used as a read function.
     * @param		p_stream	the stream to modify
     * @param		p_function	the function to use a read function.
     * Original signature : <code>void opj_stream_set_read_function(opj_stream_t*, StreamReadFunction)</code>
     */
    public static native void opj_stream_set_read_function(PointerByReference p_stream, Callbacks.StreamReadWriteFunction p_function);
    /**
     * Sets the given function to be used as a write function.
     * @param		p_stream	the stream to modify
     * @param		p_function	the function to use a write function.
     * Original signature : <code>void opj_stream_set_write_function(opj_stream_t*, StreamWriteFunction)</code>
     */
    public static native void opj_stream_set_write_function(PointerByReference p_stream, Callbacks.StreamReadWriteFunction p_function);
    /**
     * Sets the given function to be used as a skip function.
     * @param		p_stream	the stream to modify
     * @param		p_function	the function to use a skip function.
     * Original signature : <code>void opj_stream_set_skip_function(opj_stream_t*, StreamSkipFunction)</code>
     */
    public static native void opj_stream_set_skip_function(PointerByReference p_stream, Callbacks.StreamSkipSeekFunction p_function);
    /**
     * Sets the given function to be used as a seek function, the stream is then seekable.
     * @param		p_stream	the stream to modify
     * @param		p_function	the function to use a skip function.
     * Original signature : <code>void opj_stream_set_seek_function(opj_stream_t*, StreamSeekFunction)</code>
     */
    public static native void opj_stream_set_seek_function(PointerByReference p_stream, Callbacks.StreamSkipSeekFunction p_function);
    /**
     * Sets the given data to be used as a user data for the stream.
     * @param		p_stream	the stream to modify
     * @param		p_data		the data to set.
     * @param		p_function	the function to free p_data when opj_stream_destroy() is called.
     * Original signature : <code>void opj_stream_set_user_data(opj_stream_t*, void*, StreamFreeUserDataFunction)</code>
     */
    public static native void opj_stream_set_user_data(PointerByReference p_stream, Pointer p_data, Callbacks.StreamFreeUserDataFunction p_function);
    /**
     * Sets the length of the user data for the stream.
     * * @param p_stream    the stream to modify
     * @param data_length length of the user_data.
     * Original signature : <code>void opj_stream_set_user_data_length(opj_stream_t*, OPJ_UINT64)</code>
     */
    public static native void opj_stream_set_user_data_length(PointerByReference p_stream, long data_length);
    /**
     * Create a stream from a file identified with its filename with default parameters (helper function)
     * @param fname             the filename of the file to stream
     * @param p_is_read_stream  whether the stream is a read stream (true) or not (false)
     * Original signature : <code>opj_stream_t* opj_stream_create_default_file_stream(const char*, OPJ_BOOL)</code>
     * @deprecated use the safer methods {@link #opj_stream_create_default_file_stream(java.lang.String, int)} and {@link #opj_stream_create_default_file_stream(com.sun.jna.Pointer, int)} instead
     */
    @Deprecated
    public static native PointerByReference opj_stream_create_default_file_stream(Pointer fname, int p_is_read_stream);
    /**
     * Create a stream from a file identified with its filename with default parameters (helper function)
     * @param fname             the filename of the file to stream
     * @param p_is_read_stream  whether the stream is a read stream (true) or not (false)
     * Original signature : <code>opj_stream_t* opj_stream_create_default_file_stream(const char*, OPJ_BOOL)</code>
     */
    public static native PointerByReference opj_stream_create_default_file_stream(String fname, int p_is_read_stream);
    /**
     * Create a stream from a file identified with its filename with a specific buffer size
     * @param fname             the filename of the file to stream
     * @param p_buffer_size     size of the chunk used to stream
     * @param p_is_read_stream  whether the stream is a read stream (true) or not (false)
     * Original signature : <code>opj_stream_t* opj_stream_create_file_stream(const char*, OPJ_SIZE_T, OPJ_BOOL)</code>
     * @deprecated use the safer methods {@link #opj_stream_create_file_stream(java.lang.String, NativeSize, int)} and {@link #opj_stream_create_file_stream(com.sun.jna.Pointer, NativeSize, int)} instead
     */
    @Deprecated
    public static native PointerByReference opj_stream_create_file_stream(Pointer fname, NativeSize p_buffer_size, int p_is_read_stream);
    /**
     * Create a stream from a file identified with its filename with a specific buffer size
     * @param fname             the filename of the file to stream
     * @param p_buffer_size     size of the chunk used to stream
     * @param p_is_read_stream  whether the stream is a read stream (true) or not (false)
     * Original signature : <code>opj_stream_t* opj_stream_create_file_stream(const char*, OPJ_SIZE_T, OPJ_BOOL)</code>
     */
    public static native PointerByReference opj_stream_create_file_stream(String fname, NativeSize p_buffer_size, int p_is_read_stream);
    /**
     * Set the info handler use by struct.
     * @param p_codec       the codec previously initialise
     * @param p_callback    the callback function which will be used
     * @param p_user_data   client object where will be returned the message
     * Original signature : <code>OPJ_BOOL opj_set_info_handler(opj_codec_t*, MessageFunction, void*)</code>
     */
    public static native int opj_set_info_handler(DecompressionCodec p_codec, Callbacks.MessageFunction p_callback, Pointer p_user_data);
    public static native int opj_set_info_handler(CompressionCodec p_codec, Callbacks.MessageFunction p_callback, Pointer p_user_data);
    /**
     * Set the warning handler use by struct.
     * @param p_codec       the codec previously initialise
     * @param p_callback    the callback function which will be used
     * @param p_user_data   client object where will be returned the message
     * Original signature : <code>OPJ_BOOL opj_set_warning_handler(opj_codec_t*, MessageFunction, void*)</code>
     */
    public static native int opj_set_warning_handler(DecompressionCodec p_codec, Callbacks.MessageFunction p_callback, Pointer p_user_data);
    public static native int opj_set_warning_handler(CompressionCodec p_codec, Callbacks.MessageFunction p_callback, Pointer p_user_data);
    /**
     * Set the error handler use by struct.
     * @param p_codec       the codec previously initialise
     * @param p_callback    the callback function which will be used
     * @param p_user_data   client object where will be returned the message
     * Original signature : <code>OPJ_BOOL opj_set_error_handler(opj_codec_t*, MessageFunction, void*)</code>
     */
    public static native int opj_set_error_handler(DecompressionCodec p_codec, Callbacks.MessageFunction p_callback, Pointer p_user_data);
    public static native int opj_set_error_handler(CompressionCodec p_codec, Callbacks.MessageFunction p_callback, Pointer p_user_data);
    /**
     * Creates a J2K/JP2 decompression structure
     * @param format 		Decoder to select
     * * @return Returns a handle to a decompressor if successful, returns NULL otherwise
     * Original signature : <code>opj_codec_t* opj_create_decompress(OPJ_CODEC_FORMAT)</code>
     */
    public static native DecompressionCodec.ByReference opj_create_decompress(int format);
    /**
     * Destroy a decompressor handle
     * * @param	p_codec			decompressor handle to destroy
     * Original signature : <code>void opj_destroy_codec(opj_codec_t*)</code>
     */
    public static native void opj_destroy_codec(Pointer p_codec);
    //public static native void opj_destroy_codec(Pointer p_codec);
    /**
     * Read after the codestream if necessary
     * @param	p_codec			the JPEG2000 codec to read.
     * @param	p_stream		the JPEG2000 stream.
     * Original signature : <code>OPJ_BOOL opj_end_decompress(opj_codec_t*, opj_stream_t*)</code>
     */
    public static native int opj_end_decompress(DecompressionCodec p_codec, PointerByReference p_stream);
    /**
     * Set decoding parameters to default values
     * @param parameters DecompressionHandler parameters
     * Original signature : <code>void opj_set_default_decoder_parameters(opj_dparameters_t*)</code>
     */
    public static native void opj_set_default_decoder_parameters(DecompressCoreParams parameters);
    /**
     * Setup the decoder with decompression parameters provided by the user and with the message handler
     * provided by the user.
     * * @param p_codec 		decompressor handler
     * @param parameters 	decompression parameters
     * * @return true			if the decoder is correctly set
     * Original signature : <code>OPJ_BOOL opj_setup_decoder(opj_codec_t*, opj_dparameters_t*)</code>
     */
    public static native int opj_setup_decoder(DecompressionCodec p_codec, DecompressCoreParams parameters);
    /**
     * Decodes an image header.
     * * @param	p_stream		the jpeg2000 stream.
     * @param	p_codec			the jpeg2000 codec to read.
     * @param	p_image			the image structure initialized with the characteristics of encoded image.
     * * @return true				if the main header of the codestream and the JP2 header is correctly read.
     * Original signature : <code>OPJ_BOOL opj_read_header(opj_stream_t*, opj_codec_t*, opj_image_t**)</code>
     */
    public static native int opj_read_header(PointerByReference p_stream, DecompressionCodec p_codec, PointerByReference p_image);
    /**
     * Sets the given area to be decoded. This function should be called right after opj_read_header and before any tile header reading.
     * * @param	p_codec			the jpeg2000 codec.
     * @param	p_image         the decoded image previously setted by opj_read_header
     * @param	p_start_x		the left position of the rectangle to decode (in image coordinates).
     * @param	p_end_x			the right position of the rectangle to decode (in image coordinates).
     * @param	p_start_y		the up position of the rectangle to decode (in image coordinates).
     * @param	p_end_y			the bottom position of the rectangle to decode (in image coordinates).
     * * @return	true			if the area could be set.
     * Original signature : <code>OPJ_BOOL opj_set_decode_area(opj_codec_t*, opj_image_t*, OPJ_INT32, OPJ_INT32, OPJ_INT32, OPJ_INT32)</code>
     */
    public static native int opj_set_decode_area(DecompressionCodec p_codec, Image p_image, int p_start_x, int p_start_y, int p_end_x, int p_end_y);
    /**
     * Decode an image from a JPEG-2000 codestream
     * * @param p_decompressor 	decompressor handle
     * @param p_stream			Input buffer stream
     * @param p_image 			the decoded image
     * @return 					true if success, otherwise false
     * Original signature : <code>OPJ_BOOL opj_decode(opj_codec_t*, opj_stream_t*, opj_image_t*)</code>
     */
    public static native int opj_decode(DecompressionCodec p_decompressor, PointerByReference p_stream, Image p_image);
    /**
     * Get the decoded tile from the codec
     * * @param	p_codec			the jpeg2000 codec.
     * @param	p_stream		input streamm
     * @param	p_image			output image
     * @param	tile_index		index of the tile which will be decode
     * * @return					true if success, otherwise false
     * Original signature : <code>OPJ_BOOL opj_get_decoded_tile(opj_codec_t*, opj_stream_t*, opj_image_t*, OPJ_UINT32)</code>
     */
    public static native int opj_get_decoded_tile(DecompressionCodec p_codec, PointerByReference p_stream, Image p_image, int tile_index);

    public static native void opj_set_default_encoder_parameters(CompressionParams parameters);

    public static native CompressionCodec.ByReference opj_create_compress(int format);

    public static native int opj_setup_encoder(CompressionCodec p_codec, CompressionParams parameters, PointerByReference p_image);

    public static native int opj_start_compress(CompressionCodec p_codec, Image p_image, PointerByReference p_stream);

    public static native int opj_write_tile(CompressionCodec p_codec, int p_tile_index, byte[] data, int p_data_size, PointerByReference p_stream);

    public static native int opj_encode(CompressionCodec p_codec, PointerByReference p_stream);

    public static native int opj_end_compress(CompressionCodec p_codec, PointerByReference p_stream);
}
