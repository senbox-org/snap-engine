package org.esa.beam.dataio.bigtiff;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFImageWriteParam;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFLZWCompressor;
import org.esa.beam.dataio.bigtiff.internal.TiffHeader;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.util.io.FileUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

public class BigGeoTiffProductWriter extends AbstractProductWriter {

    private File outputFile;
    private ImageOutputStream outputStream;
    private BigGeoTiffBandWriter bandWriter;
    private TIFFImageWriter imageWriter;
    private NewBandWriter newBandWriter;

    public BigGeoTiffProductWriter(ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
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

        final Product sourceProduct = getSourceProduct();
        final MultiLevelImage firstSourceImage = sourceProduct.getBandAt(0).getSourceImage();
        final int tileWidth = firstSourceImage.getTileWidth();
        final int tileHeight = firstSourceImage.getTileHeight();

        final TIFFImageWriteParam writeParam = new TIFFImageWriteParam(Locale.ENGLISH);
        writeParam.setCompressionMode(TIFFImageWriteParam.MODE_EXPLICIT);                               // @todo 2 tb/tb parse
        final TIFFLZWCompressor compressor = new TIFFLZWCompressor(BaselineTIFFTagSet.PREDICTOR_NONE);  // @todo 2 tb/tb parse
        writeParam.setTIFFCompressor(compressor);
        writeParam.setCompressionType(compressor.getCompressionType());
        writeParam.setCompressionQuality(0.75f);                                                        // @todo 2 tb/tb parse
        writeParam.setTilingMode(TIFFImageWriteParam.MODE_EXPLICIT);
        writeParam.setTiling(tileWidth, tileHeight, 0, 0);                                                           // @todo 2 tb/tb parse
//        writeParam.setForceToBigTIFF(true);                                                             // @todo 2 tb/tb parse

        // @todo 1 tb/tb continue here 2015-01-14
//        writeGeoTiffNewIplementation(writeParam);

        writeGeoTIFFProduct(new FileImageOutputStream(outputFile), sourceProduct);
    }

//    private void writeGeoTiffNewIplementation(TIFFImageWriteParam writeParam) throws IOException {
//        imageWriter = getTiffImageWriter();
//        final File parallelOut = new File(outputFile.getParent(), "test_imageiio_ext.tif");
//        imageWriter.setOutput(new FileImageOutputStream(parallelOut));
//        newBandWriter = new NewBandWriter(imageWriter, writeParam);
//    }

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

    void writeGeoTIFFProduct(ImageOutputStream outputStream, Product sourceProduct) throws IOException {
        this.outputStream = outputStream;
        final TiffHeader tiffHeader = new TiffHeader(new Product[]{sourceProduct});
        tiffHeader.write(outputStream);
        bandWriter = new BigGeoTiffBandWriter(tiffHeader.getIfdAt(0), outputStream, sourceProduct);
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws IOException {
        bandWriter.writeBandRasterData(sourceBand,
                sourceOffsetX, sourceOffsetY,
                sourceWidth, sourceHeight,
                sourceBuffer, pm);

        //newBandWriter.writeBandRasterData(sourceBand, sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight, sourceBuffer, pm);
    }

    @Override
    public void flush() throws IOException {
        if (outputStream != null) {
            outputStream.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (bandWriter != null) {
            bandWriter.dispose();
            bandWriter = null;
        }

        if (newBandWriter != null) {
            newBandWriter.dispose();
            newBandWriter = null;
        }

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
}
