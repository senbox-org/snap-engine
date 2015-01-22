package org.esa.beam.dataio.bigtiff;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFImageWriteParam;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFLZWCompressor;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.FileUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.media.jai.JAI;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

public class BigGeoTiffProductWriter extends AbstractProductWriter {

    private static String PARAM_COMPRESSION_TYPE = "org.esa.beam.dataio.bigtiff.compression.type";   // value must be "LZW"

    private static String PARAM_COMPRESSION_QUALITY = "org.esa.beam.dataio.bigtiff.compression.quality";   // value float 0 ... 1, default 0.75
    private static String PARAM_COMPRESSION_QUALITY_DEFAULT = "0.75";

    private static String PARAM_TILING_WIDTH = "org.esa.beam.dataio.bigtiff.tiling.width";   // integer value
    private static String PARAM_TILING_HEIGHT = "org.esa.beam.dataio.bigtiff.tiling.height";   // integer value

    private static String PARAM_FORCE_BIGTIFF = "org.esa.beam.dataio.bigtiff.force.bigtiff";   // boolean

    private File outputFile;
    private TIFFImageWriter imageWriter;
    private boolean isWritten;
    private FileImageOutputStream outputStream;
    private TIFFImageWriteParam writeParam;

    public BigGeoTiffProductWriter(ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);

        createWriterParams();
    }

    private void createWriterParams() {
        writeParam = new TIFFImageWriteParam(Locale.ENGLISH);

        final String compressionType = System.getProperty(PARAM_COMPRESSION_TYPE);
        if (StringUtils.isNotNullAndNotEmpty(compressionType)) {
            if (compressionType.equals("LZW")) {
                writeParam.setCompressionMode(TIFFImageWriteParam.MODE_EXPLICIT);

                final TIFFLZWCompressor compressor = new TIFFLZWCompressor(BaselineTIFFTagSet.PREDICTOR_NONE);
                writeParam.setTIFFCompressor(compressor);
                writeParam.setCompressionType(compressor.getCompressionType());

                final String compressionQualityProperty = System.getProperty(PARAM_COMPRESSION_QUALITY, PARAM_COMPRESSION_QUALITY_DEFAULT);
                final float compressionQuality = Float.parseFloat(compressionQualityProperty);
                writeParam.setCompressionQuality(compressionQuality);
            } else {
                throw new IllegalArgumentException("Compression type '" + compressionType + "' is not supported");
            }
        }

        final String tilingWidthProperty = System.getProperty(PARAM_TILING_WIDTH);
        final String tilingHeightProperty = System.getProperty(PARAM_TILING_HEIGHT);
        if (StringUtils.isNotNullAndNotEmpty(tilingWidthProperty) && StringUtils.isNotNullAndNotEmpty(tilingHeightProperty)) {
            final int tileWidth = Integer.parseInt(tilingWidthProperty);
            final int tileHeight = Integer.parseInt(tilingHeightProperty);

            writeParam.setTilingMode(TIFFImageWriteParam.MODE_EXPLICIT);
            writeParam.setTiling(tileWidth, tileHeight, 0, 0);
        }

        final String bigTiffProperty = System.getProperty(PARAM_FORCE_BIGTIFF);
        if (StringUtils.isNotNullAndNotEmpty(bigTiffProperty)) {
            final boolean forceBigTiff = Boolean.parseBoolean(bigTiffProperty);
            writeParam.setForceToBigTIFF(forceBigTiff);
        }
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        outputFile = null;

        final File file;
        if (getOutput() instanceof String) {
            file = new File((String) getOutput());
        } else {
            file = (File) getOutput();
        }

        outputFile = FileUtils.ensureExtension(file, Constants.FILE_EXTENSIONS[0]);

        deleteOutput();
        updateProductName();
        updateTilingParameter();

        imageWriter = getTiffImageWriter();

        outputStream = new FileImageOutputStream(outputFile);
        imageWriter.setOutput(outputStream);
    }

    private void updateTilingParameter() {
        if (writeParam.getTilingMode() != TIFFImageWriteParam.MODE_EXPLICIT) {
            final Product sourceProduct = getSourceProduct();
            final MultiLevelImage firstSourceImage = sourceProduct.getBandAt(0).getSourceImage();
            final int tileWidth = firstSourceImage.getTileWidth();
            final int tileHeight = firstSourceImage.getTileHeight();

            writeParam.setTilingMode(TIFFImageWriteParam.MODE_EXPLICIT);
            writeParam.setTiling(tileWidth, tileHeight, 0, 0);
        }
    }

    private TIFFImageWriter getTiffImageWriter() {
        final Iterator<ImageWriter> writerIterator = ImageIO.getImageWritersByFormatName("TIFF");
        while (writerIterator.hasNext()) {
            final ImageWriter writer = writerIterator.next();
            if (writer instanceof TIFFImageWriter) {
                return (TIFFImageWriter) writer;
            }
        }
        throw new IllegalStateException("No appropriate image writer for format BigGeoTiff found.");
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
        if (isWritten) {
            return;
        }

        final ParameterBlock parameterBlock = new ParameterBlock();
        final Product sourceProduct = sourceBand.getProduct();

        RenderedImage writeImage;

        final int nodeCount = sourceProduct.getNumBands();
        if (nodeCount > 1) {
            for (int i = 0; i < nodeCount; i++) {
                parameterBlock.setSource(sourceProduct.getBandAt(i).getSourceImage(), i);
            }
            writeImage = JAI.create("bandmerge", parameterBlock, null);
        } else {
            writeImage = sourceBand.getSourceImage();
        }

        final SampleModel sampleModel = writeImage.getSampleModel();
        writeParam.setDestinationType(new ImageTypeSpecifier(new BogusAndCheatingColorModel(sampleModel), sampleModel));

        final IIOImage iioImage = new IIOImage(writeImage, null, null);
        imageWriter.write(null, iioImage, writeParam);

        isWritten = true;
    }

    @Override
    public void flush() throws IOException {
        if (outputStream != null) {
            outputStream.flush();
        }
    }

    @Override
    public void close() throws IOException {

        if (outputStream != null) {
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        }

        if (imageWriter != null) {
            imageWriter.dispose();
            imageWriter = null;
        }
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        return BigGeoTiffBandWriter.shouldWriteNode(node);
    }

    @Override
    public void deleteOutput() throws IOException {
        if (outputFile != null && outputFile.isFile()) {
            if (!outputFile.delete()) {
                throw new IOException("Unable to delete file: " + outputFile.getAbsolutePath());
            }
        }
    }

    private void updateProductName() {
        if (outputFile != null) {
            getSourceProduct().setName(FileUtils.getFilenameWithoutExtension(outputFile));
        }
    }

    private static class BogusAndCheatingColorModel extends ColorModel {
        private SampleModel sampleModel;

        public BogusAndCheatingColorModel(SampleModel sampleModel) {
            super(8, new int[]{8}, ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false, OPAQUE, DataBuffer.TYPE_BYTE);
            this.sampleModel = sampleModel;
        }

        @Override
        public boolean isCompatibleRaster(Raster raster) {
            return isCompatibleSampleModel(raster.getSampleModel());
        }

        @Override
        public boolean isCompatibleSampleModel(SampleModel sm) {
            return sampleModel.getNumBands() == sm.getNumBands() && sampleModel.getDataType() == sm.getDataType();
        }

        @Override
        public int getNumComponents() {
            return sampleModel.getNumBands();
        }

        @Override
        public int getRed(int pixel) {
            return 0;
        }

        @Override
        public int getGreen(int pixel) {
            return 0;
        }

        @Override
        public int getBlue(int pixel) {
            return 0;
        }

        @Override
        public int getAlpha(int pixel) {
            return 0;
        }
    }
}
