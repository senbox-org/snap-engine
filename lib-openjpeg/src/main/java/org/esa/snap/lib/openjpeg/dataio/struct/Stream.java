package org.esa.snap.lib.openjpeg.dataio.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.ByteByReference;
import com.sun.jna.ptr.LongByReference;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks;

import java.util.Arrays;
import java.util.List;

public class Stream extends Structure {

    private static final List<String> fieldNames =
            Arrays.asList("m_user_data", "m_free_user_data_fn", "m_user_data_length", "m_read_fn", "m_write_fn",
                    "m_skip_fn", "m_seek_fn", "m_stored_data", "m_current_data", "m_bytes_in_buffer", "m_byte_offset",
                    "m_buffer_size", "m_status");

    /**
     * User data, be it files, ... The actual data depends on the type of the stream.
     * C type : void*
     */
    public Pointer m_user_data;
    /**
     * Pointer to function to free m_user_data (NULL at initialization)
     * when destroying the stream. If pointer is NULL the function is not
     * called and the m_user_data is not freed (even if non-NULL).
     * C type : StreamFreeUserDataFunction
     */
    public Callbacks.StreamFreeUserDataFunction m_free_user_data_fn;
    /**
     * User data length
     * C type : OPJ_UINT64
     */
    public LongByReference m_user_data_length;
    /**
     * Pointer to actual read function (NULL at the initialization of the cio.
     * C type : StreamReadFunction
     */
    public Callbacks.StreamReadWriteFunction m_read_fn;
    /**
     * Pointer to actual write function (NULL at the initialization of the cio.
     * C type : StreamWriteFunction
     */
    public Callbacks.StreamReadWriteFunction m_write_fn;
    /**
     * Pointer to actual skip function (NULL at the initialization of the cio.
     * There is no seek function to prevent from back and forth slow procedures.
     * C type : StreamSkipFunction
     */
    public Callbacks.StreamSkipSeekFunction m_skip_fn;
    /**
     * Pointer to actual seek function (if available).
     * C type : StreamSeekFunction
     */
    public Callbacks.StreamSkipSeekFunction m_seek_fn;
    /**
     * Actual data stored into the stream if readed from. Data is read by chunk of fixed size.
     * you should never access this data directly.
     * C type : OPJ_BYTE*
     */
    public ByteByReference m_stored_data;
    /**
     * Pointer to the current read data.
     * C type : OPJ_BYTE*
     */
    public ByteByReference m_current_data;
    /**
     * number of bytes containing in the buffer.
     * C type : OPJ_SIZE_T
     */
    public long m_bytes_in_buffer;
    /**
     * The number of bytes read/written from the beginning of the stream
     * C type : OPJ_OFF_T
     */
    public long m_byte_offset;
    /**
     * The size of the buffer.
     * C type : OPJ_SIZE_T
     */
    public long m_buffer_size;
    /**
     * Flags to tell the status of the stream.
     * Used with OPJ_STREAM_STATUS_* defines.
     * C type : OPJ_UINT32
     */
    public int m_status;

    public Stream() {
        super();
    }

    public Stream(Pointer peer) {
        super(peer);
    }

    @Override
    protected List<String> getFieldOrder() {
        return fieldNames;
    }

    public static class ByReference extends Stream implements Structure.ByReference {
    }

    public static class ByValue extends Stream implements Structure.ByValue {
    }
}
