package org.esa.snap.lib.openjpeg.dataio;

import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.lib.openjpeg.dataio.library.Callbacks;
import org.esa.snap.lib.openjpeg.dataio.library.Constants;
import org.esa.snap.lib.openjpeg.dataio.library.Enums;
import org.esa.snap.lib.openjpeg.dataio.library.OpenJp2;
import org.esa.snap.lib.openjpeg.dataio.struct.DecompressParams;
import org.esa.snap.lib.openjpeg.dataio.struct.DecompressionCodec;
import org.esa.snap.lib.openjpeg.dataio.struct.Image;
import org.esa.snap.lib.openjpeg.dataio.struct.ImageComponent;
import sun.awt.image.SunWritableRaster;

import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper over OpenJP2 library for decompression from a given .jp2 file
 *
 * @author  Cosmin Cara
 * @since   5.0.0
 */
public class OpenJP2Decoder implements AutoCloseable {
    private static final ExecutorService executor;

    private PointerByReference pStream;
    private DecompressParams parameters;
    private DecompressionCodec pCodec;
    private PointerByReference pImage;
    private int width;
    private int height;
    private final Path tileFile;
    private int resolution;
    private int layer;
    private int dataType;
    private int tileIndex;
    private int bandIndex;
    private int numBands;
    private Logger logger;
    private final Set<Path> pendingWrites;
    private Function<Path, Void> writeCompletedCallback;

    static {
        executor = Executors.newFixedThreadPool(Math.min(Runtime.getRuntime().availableProcessors() / 2, 4));
    }

    /**
     * The only constructor of this class.
     *
     * @param cacheDir      The cache directory (where extracted info will be saved)
     * @param file          The source jp2 file
     * @param bandIndex     The index of the raster band to be handled by this instance
     * @param dataType      The data type (@see DataBuffer types) of the raster
     * @param resolution    The resolution to be extracted
     * @param layer         The quality layers to extract
     * @param tileIndex     The index of the jp2 tile to be handled
     */
    public OpenJP2Decoder(Path cacheDir, Path file, int bandIndex, int dataType, int resolution, int layer, int tileIndex) {
        this.logger = SystemUtils.LOG;
        this.dataType = dataType;
        this.resolution = resolution;
        this.layer = layer;
        this.tileIndex = tileIndex;
        this.bandIndex = bandIndex == -1 ? 0 : bandIndex;
        /*this.tileFile = cacheDir.resolve(file.getFileName().toString().replace(".", "_").toLowerCase()
                + "_" + String.valueOf(tileIndex)
                + "_" + String.valueOf(resolution)
                + "_" + String.valueOf(this.bandIndex) + ".raw");*/
        this.tileFile = cacheDir.resolve(Utils.getChecksum(file.getFileName().toString())
                + "_" + tileIndex + "_" + resolution + "_" + this.bandIndex + ".raw");
        pStream = OpenJp2.opj_stream_create_default_file_stream(file.toAbsolutePath().toString(), Constants.OPJ_STREAM_READ);
        if (pStream == null || pStream.getValue() == null)
            throw new RuntimeException("Failed to create the stream from the file");
        this.parameters = initDecodeParams(file);
        pCodec = setupDecoder(parameters);
        pImage = new PointerByReference();
        OpenJp2.opj_read_header(pStream, pCodec, pImage);
        Image jImage = RasterUtils.dereference(Image.class, pImage.getValue());
        this.numBands = jImage.numcomps;
        ImageComponent component = ((ImageComponent[]) jImage.comps.toArray(jImage.numcomps))[this.bandIndex];
        width = component.w;
        height = component.h;
        this.pendingWrites = Collections.synchronizedSet(new HashSet<>());
        this.writeCompletedCallback = value -> {
            if (value != null) {
                pendingWrites.remove(value);
            }
            return null;
        };
    }
    /**
     * Returns the extracted image bands number
     */
    public int getBandNumber() throws IOException {
        return this.numBands;
    }

    /**
     * Returns the extracted image dimensions
     */
    public Dimension getImageDimensions() throws IOException {
        return new Dimension(width, height);
    }

    /**
     * Extracts the full image.
     */
    public Raster read() throws IOException {
        return decompress(null);
    }

    /**
     * Extracts the given region of interest from the image.
     * @param rectangle     The region of interest
     */
    public Raster read(Rectangle rectangle) throws IOException {
        return decompress(rectangle);
    }

    /**
     * Cleans up the native memory resources if allocated.
     */
    @Override
    public void close() {
        try {
            if (pImage != null && pImage.getValue() != null) {
                OpenJp2.opj_image_destroy(pImage.getValue());
            }
            if (pCodec != null) {
                OpenJp2.opj_destroy_codec(pCodec.getPointer());
            }
            if (pStream != null && pStream.getValue() != null) {
                OpenJp2.opj_stream_destroy(pStream);
            }
        } catch (Exception ex) {
            logger.warning(ex.getMessage());
        }
    }

    private ImageComponent[] decode() {
        Image jImage = RasterUtils.dereference(Image.class, pImage.getValue());

        if (parameters.nb_tile_to_decode == 0) {
            if (OpenJp2.opj_set_decode_area(pCodec, jImage, parameters.DA_x0, parameters.DA_y0, parameters.DA_x1, parameters.DA_y1) == 0) {
                throw new RuntimeException("Failed to set the decoded area");
            }
            if (OpenJp2.opj_decode(pCodec, pStream, jImage) == 0 &&
                    OpenJp2.opj_end_decompress(pCodec, pStream) != 0) {
                throw new RuntimeException("Failed to decode image");
            }
        } else {
            if (OpenJp2.opj_get_decoded_tile(pCodec, pStream, jImage, parameters.tile_index) == 0) {
                throw new RuntimeException("Failed to decode tile");
            }
        }

        jImage = RasterUtils.dereference(Image.class, pImage.getValue());
        ImageComponent[] comps = (ImageComponent[]) jImage.comps.toArray(jImage.numcomps);
        if (jImage.color_space != Enums.ColorSpace.OPJ_CLRSPC_SYCC &&
                jImage.numcomps == 3 && comps[0].dx == comps[0].dy && comps[1].dx != 1) {
            jImage.color_space = Enums.ColorSpace.OPJ_CLRSPC_SYCC;
        } else if (jImage.numcomps <= 2) {
            jImage.color_space = Enums.ColorSpace.OPJ_CLRSPC_GRAY;
        } else
        {
            jImage.color_space = Enums.ColorSpace.OPJ_CLRSPC_GRAY;
        }
        return comps;
    }

    private DecompressParams initDecodeParams(Path inputFile) {
        DecompressParams params = new DecompressParams();
        params.decod_format = -1;
        params.cod_format = -1;
        OpenJp2.opj_set_default_decoder_parameters(params.core);
        params.decod_format = RasterUtils.getFileFormat(inputFile);
        params.cod_format = RasterUtils.getFormat("jp2");
        params.core.cp_reduce = this.resolution;
        params.core.cp_layer = this.layer;
        params.tile_index = this.tileIndex;
        params.nb_tile_to_decode = params.tile_index >= 0 ? 1 : 0;
        return params;
    }

    private DecompressionCodec setupDecoder(DecompressParams params) {
        DecompressionCodec codec;
        switch (params.decod_format) {
            case 0: // JPEG-2000 codestream
                codec = OpenJp2.opj_create_decompress(Enums.CodecFormat.OPJ_CODEC_J2K);
                break;
            case 1: // JPEG-2000 compressed data
                codec = OpenJp2.opj_create_decompress(Enums.CodecFormat.OPJ_CODEC_JP2);
                break;
            case 2:
                codec = OpenJp2.opj_create_decompress(Enums.CodecFormat.OPJ_CODEC_JPT);
                break;
            default:
                throw new RuntimeException("File is not coded with JPEG-2000");
        }
        if (SystemUtils.LOG.getLevel().intValue() <= Level.FINE.intValue()) {
            OpenJp2.opj_set_info_handler(codec, new Callbacks.MessageFunction() {
                @Override
                public void invoke(Pointer msg, Pointer client_data) {
                    logger.info(msg.getString(0));
                }

                @Override
                public Pointer invoke(Pointer p_codec) {
                    return p_codec;
                }
            }, null);
            OpenJp2.opj_set_warning_handler(codec, new Callbacks.MessageFunction() {
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
        OpenJp2.opj_set_error_handler(codec, new Callbacks.MessageFunction() {
            @Override
            public void invoke(Pointer msg, Pointer client_data) {
                logger.severe(msg.getString(0));
            }

            @Override
            public Pointer invoke(Pointer p_codec) {
                return p_codec;
            }
        }, null);

        int setupDecoder = OpenJp2.opj_setup_decoder(codec, params.core);
        if (setupDecoder == 0) {
            throw new RuntimeException("Failed to setup decoder");
        }
        return codec;
    }

    private Raster decompress(Rectangle roi) throws IOException {
        int width;
        int height;
        int[] pixels;
        // Maybe this tile file is written by other thread, so let's wait if so
        while (this.pendingWrites.contains(this.tileFile)) {
            Thread.yield();
        }
        // int[] bandOffsets = new int[] {0};
        int[] bandOffsets = new int[this.numBands];
        Arrays.fill(bandOffsets,0);
        DataBuffer buffer;
        if (!Files.exists(this.tileFile)) {
            ImageComponent[] components = decode();
            width = components[this.bandIndex].w;
            height = components[this.bandIndex].h;
            pixels = components[this.bandIndex].data.getPointer().getIntArray(0, components[this.bandIndex].w * components[this.bandIndex].h);
            executor.submit(() -> {
                try {
                    this.pendingWrites.add(this.tileFile);
                    RasterUtils.write(components[this.bandIndex].w, components[this.bandIndex].h, pixels, this.dataType, this.tileFile, this.writeCompletedCallback);
                } catch (Exception ex) {
                    logger.warning(ex.getMessage());
                }
            });
            switch (this.dataType) {
                case DataBuffer.TYPE_BYTE:
                    buffer = RasterUtils.extractROIAsByteBuffer(pixels, width, height, roi);
                    break;
                case DataBuffer.TYPE_USHORT:
                    buffer = RasterUtils.extractROIAsUShortBuffer(pixels, width, height, roi);
                    break;
                case DataBuffer.TYPE_SHORT:
                    buffer = RasterUtils.extractROIAsShortBuffer(pixels, width, height, roi);
                    break;
                case DataBuffer.TYPE_INT:
                    buffer = RasterUtils.extractROI(pixels, width, height, roi);
                    break;
                default:
                    throw new UnsupportedOperationException("Source buffer type not supported");
            }
            if (roi != null) {
                width = roi.width;
                height = roi.height;
            }
        } else {
            TileImageDescriptor fileDescriptor;
            switch (this.dataType) {
                case DataBuffer.TYPE_BYTE:
                    fileDescriptor = RasterUtils.readAsByteArray(this.tileFile, roi);
                    width = fileDescriptor.getWidth();
                    height = fileDescriptor.getHeight();
                    buffer = new DataBufferByte((byte[]) fileDescriptor.getDataArray(), width * height);
                    break;
                case DataBuffer.TYPE_USHORT:
                    fileDescriptor = RasterUtils.readAsShortArray(this.tileFile, roi);
                    width = fileDescriptor.getWidth();
                    height = fileDescriptor.getHeight();
                    buffer = new DataBufferUShort((short[]) fileDescriptor.getDataArray(), width * height);
                    break;
                case DataBuffer.TYPE_SHORT:
                    fileDescriptor = RasterUtils.readAsShortArray(this.tileFile, roi);
                    width = fileDescriptor.getWidth();
                    height = fileDescriptor.getHeight();
                    buffer = new DataBufferShort((short[]) fileDescriptor.getDataArray(), width * height);
                    break;
                case DataBuffer.TYPE_INT:
                    fileDescriptor = RasterUtils.readAsIntArray(this.tileFile, roi);
                    width = fileDescriptor.getWidth();
                    height = fileDescriptor.getHeight();
                    buffer = new DataBufferInt((int[]) fileDescriptor.getDataArray(), width * height);
                    break;
                default:
                    throw new UnsupportedOperationException("Source buffer type not supported");
            }
        }
        // Maybe the write operation has not finished
        while (this.pendingWrites.contains(this.tileFile)) {
            Thread.yield();
        }
        SampleModel sampleModel = new PixelInterleavedSampleModel(this.dataType, width, height, 1, width, bandOffsets);
        WritableRaster raster = null;
        try {
            raster = new SunWritableRaster(sampleModel, buffer, new Point(0, 0));
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return raster;
    }
}
