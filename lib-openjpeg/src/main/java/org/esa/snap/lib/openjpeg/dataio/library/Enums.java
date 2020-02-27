package org.esa.snap.lib.openjpeg.dataio.library;

/**
 * Holder class for enum structures
 */
public class Enums {
    /**
     * CinemaMode enum equivalent
     */
    public interface CinemaMode{
        /**
         * Not Digital Cinema
         */
        int OPJ_OFF = 0;
        /**
         * 2K Digital Cinema at 24 fps
         */
        int OPJ_CINEMA2K_24 = 1;
        /**
         * 2K Digital Cinema at 48 fps
         */
        int OPJ_CINEMA2K_48 = 2;
        /**
         * 4K Digital Cinema at 24 fps
         */
        int OPJ_CINEMA4K_24 = 3;
    }

    /**
     * CodecFormat enum equivalent
     */
    public interface CodecFormat {
        /**
         * place-holder
         */
        int OPJ_CODEC_UNKNOWN = -1;
        /**
         * JPEG-2000 codestream : read/write
         */
        int OPJ_CODEC_J2K = 0;
        /**
         * JPT-stream (JPEG 2000, JPIP) : read only
         */
        int OPJ_CODEC_JPT = 1;
        /**
         * JP2 file format : read/write
         */
        int OPJ_CODEC_JP2 = 2;
        /**
         * JPP-stream (JPEG 2000, JPIP) : to be coded
         */
        int OPJ_CODEC_JPP = 3;
        /**
         * JPX file format (JPEG 2000 Part-2) : to be coded
         */
        int OPJ_CODEC_JPX = 4;
    }

    /**
     * ColorSpace enum equivalent
     */
    public interface ColorSpace {
        /**
         * not supported by the library
         */
        int OPJ_CLRSPC_UNKNOWN = -1;
        /**
         * not specified in the codestream
         */
        int OPJ_CLRSPC_UNSPECIFIED = 0;
        /**
         * sRGB
         */
        int OPJ_CLRSPC_SRGB = 1;
        /**
         * grayscale
         */
        int OPJ_CLRSPC_GRAY = 2;
        /**
         * YUV
         */
        int OPJ_CLRSPC_SYCC = 3;
        /**
         * e-YCC
         */
        int OPJ_CLRSPC_EYCC = 4;
        /**
         * CMYK
         */
        int OPJ_CLRSPC_CMYK = 5;
    }

    /**
     * ProgOrder enum equivalent
     */
    public interface ProgOrder {
        /**
         * place-holder
         */
        int OPJ_PROG_UNKNOWN = -1;
        /**
         * layer-resolution-component-precinct order
         */
        int OPJ_LRCP = 0;
        /**
         * resolution-layer-component-precinct order
         */
        int OPJ_RLCP = 1;
        /**
         * resolution-precinct-component-layer order
         */
        int OPJ_RPCL = 2;
        /**
         * precinct-component-resolution-layer order
         */
        int OPJ_PCRL = 3;
        /**
         * component-precinct-resolution-layer order
         */
        int OPJ_CPRL = 4;
    }

    /**
     * RsizCapabilities enum equivalent
     */
    public interface RsizCapabilities {
        /**
         * Standard JPEG2000 profile
         */
        int OPJ_STD_RSIZ = 0;
        /**
         * Profile name for a 2K image
         */
        int OPJ_CINEMA2K = 3;
        /**
         * Profile name for a 4K image
         */
        int OPJ_CINEMA4K = 4;

        int OPJ_MCT = 0x8100;
    }
}
