package org.esa.snap.lib.openjpeg.dataio;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks;
import org.esa.snap.lib.openjpeg.dataio.library.Constants;
import org.esa.snap.lib.openjpeg.dataio.library.Enums;
import org.esa.snap.lib.openjpeg.dataio.library.OpenJp2;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.lib.openjpeg.dataio.struct.*;

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper over OpenJP2 library for jp2 compression
 *
 * @author  Cosmin Cara
 * @since   5.0.0
 */
public class OpenJP2Encoder implements AutoCloseable {

    private PointerByReference pStream;
    private CompressionCodec pCodec;
    private PointerByReference pImage;
    private RenderedImage theImage;
    private Logger logger;

    /**
     * Builds an encoder for the given image.
     * @param image The image to be compressed
     */
    public OpenJP2Encoder(RenderedImage image) {
        this.logger = SystemUtils.LOG;
        this.theImage = image;
    }

    /**
     * Compresses this instance's image to the target file, with the given number of resolutions.
     * @param outFile       The target file
     * @param resolutions   The number of resolutions to be written
     */
    public void write(Path outFile, int resolutions) throws IOException {
        CompressionParams parameters = initEncodeParams(outFile, resolutions);
        Raster rasterData = theImage.getData();
        int numBands = rasterData.getNumBands();
        int bitDepth;
        int signed = 0;
        ImageComponentParams.ByReference refParams = new ImageComponentParams.ByReference();
        ImageComponentParams[] imageCompParams = (ImageComponentParams[]) refParams.toArray(numBands);
        int colorSpace = numBands == 1 ? Enums.ColorSpace.OPJ_CLRSPC_GRAY : numBands >= 3 ? Enums.ColorSpace.OPJ_CLRSPC_SRGB : Enums.ColorSpace.OPJ_CLRSPC_UNKNOWN;
        switch (rasterData.getTransferType()) {
            case DataBuffer.TYPE_BYTE:
                bitDepth = 8;
                break;
            case DataBuffer.TYPE_USHORT:
                signed = 0;
                bitDepth = 16;
                break;
            case DataBuffer.TYPE_SHORT:
                signed = 1;
                bitDepth = 16;
                break;
            case DataBuffer.TYPE_INT:
            case DataBuffer.TYPE_FLOAT:
                bitDepth = 32;
                break;
            case DataBuffer.TYPE_DOUBLE:
                bitDepth = 64;
                break;
            default:
                bitDepth = 16;
                break;
        }

        for (int i = 0; i < numBands; i++) {
            imageCompParams[i].prec = bitDepth;
            imageCompParams[i].bpp = bitDepth;
            imageCompParams[i].sgnd = signed;
            imageCompParams[i].dx = 1;
            imageCompParams[i].dy = 1;
            imageCompParams[i].w = rasterData.getWidth();
            imageCompParams[i].h = rasterData.getHeight();
        }

        pImage = OpenJp2.opj_image_create(numBands, imageCompParams[0], colorSpace);
        Image opjImage = RasterUtils.dereference(Image.class, pImage.getPointer());
        opjImage.x0 = 0;
        opjImage.y0 = 0;
        opjImage.x1 = rasterData.getWidth();
        opjImage.y1 = rasterData.getHeight();
        ImageComponent[] components = (ImageComponent[]) opjImage.comps.toArray(numBands);
        if (bitDepth > 16) {
            OpenJp2.opj_image_destroy(pImage.getPointer());
            pImage = null;
            throw new RuntimeException("OpenJPEG cannot encode raw components with bit depth > 16 bits");
        }

        for (int i = 0; i < numBands; i++) {
            int[] data = RasterUtils.readRasterBandasIntArray(rasterData, i);
            components[i].data.getPointer().write(0, data, 0, data.length);
        }

        parameters.tcp_mct = numBands >= 3 ? (char)1 : (char)0;

        switch (parameters.cod_format) {
            case 0: // JPEG-2000 codestream
                pCodec = OpenJp2.opj_create_compress(Enums.CodecFormat.OPJ_CODEC_J2K);
                break;
            case 1: // JPEG-2000 compressed data
                pCodec = OpenJp2.opj_create_compress(Enums.CodecFormat.OPJ_CODEC_JP2);
                break;
            default:
                throw new RuntimeException("Unsupported codec");
        }
        if (SystemUtils.LOG.getLevel().intValue() <= Level.FINE.intValue()) {
            OpenJp2.opj_set_info_handler(pCodec, new Callbacks.MessageFunction() {
                @Override
                public void invoke(Pointer msg, Pointer client_data) {
                    logger.info(msg.getString(0));
                }

                @Override
                public Pointer invoke(Pointer p_codec) {
                    return p_codec;
                }
            }, null);
            OpenJp2.opj_set_warning_handler(pCodec, new Callbacks.MessageFunction() {
                @Override
                public void invoke(Pointer msg, Pointer client_data) {
                    logger.warning(msg.getString(0));
                }

                @Override
                public Pointer invoke(Pointer p_codec) {
                    return p_codec;
                }
            }, null);
        }
        OpenJp2.opj_set_error_handler(pCodec, new Callbacks.MessageFunction() {
            @Override
            public void invoke(Pointer msg, Pointer client_data) {
                logger.severe(msg.getString(0));
            }

            @Override
            public Pointer invoke(Pointer p_codec) {
                return p_codec;
            }
        }, null);

        // Synchronize memory structure with values set in Java code
        opjImage.write();

        if (OpenJp2.opj_setup_encoder(pCodec, parameters, pImage) == 0) {
            throw new RuntimeException("Failed to setup encoder");
        }

        opjImage = RasterUtils.dereference(Image.class, pImage.getPointer());
        pStream = OpenJp2.opj_stream_create_default_file_stream(outFile.toString(), Constants.OPJ_STREAM_WRITE);
        if (pStream == null || pStream.getValue() == null) {
            throw new RuntimeException("Failed to create the stream to the file");
        }
        if (OpenJp2.opj_start_compress(pCodec, opjImage, pStream) == 0) {
            throw new RuntimeException("Failed to encode image: opj_start_compress");
        }
        if (OpenJp2.opj_encode(pCodec, pStream) == 0) {
            throw new RuntimeException("Failed to encode image: opj_encode");
        }
        if (OpenJp2.opj_end_compress(pCodec, pStream) == 0) {
            throw new RuntimeException("Failed to encode image: opj_end_compress");
        }
    }

    private CompressionParams initEncodeParams(Path file, int numResolutions) {
        CompressionParams params = new CompressionParams();
        OpenJp2.opj_set_default_encoder_parameters(params);
        params.tcp_mct = (char)1;
        params.decod_format = RasterUtils.getFormat("raw");
        params.cod_format = RasterUtils.getFileFormat(file);
        params.cp_tx0 = 0;
        params.cp_ty0 = 0;
        params.cp_tdx = 1024;
        params.cp_tdy = 1024;
        params.tile_size_on = 1;
        params.numresolution = numResolutions;
        params.outfile = file.toAbsolutePath().toString().getBytes();
        params.cp_disto_alloc = 0;
        params.cp_fixed_quality = 1;
        params.tcp_numlayers = 1;
        params.tcp_distoratio[0] = 100;
        params.roi_compno = -1;
        params.subsampling_dx = 1;
        params.subsampling_dy = 1;
        return params;
    }
    /**
     * Cleans up the native memory resources if allocated.
     */
    @Override
    public void close() throws Exception {
        if (pCodec != null) {
            OpenJp2.opj_destroy_codec(pCodec.getPointer());
        }
        if (pStream != null && pStream.getValue() != null) {
            OpenJp2.opj_stream_destroy(pStream);
        }
        if (pImage != null && pImage.getValue() != null) {
            OpenJp2.opj_image_destroy(pImage.getPointer());
        }
    }
}
