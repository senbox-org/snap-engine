package org.esa.beam.dataio.bigtiff;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.bigtiff.internal.TiffHeader;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.io.FileUtils;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;

public class BigTiffProductWriter extends AbstractProductWriter {

    private File outputFile;
    private ImageOutputStream outputStream;
    private BigGeoTiffBandWriter bandWriter;

    public BigTiffProductWriter(ProductWriterPlugIn writerPlugIn) {
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

        writeGeoTIFFProduct(new FileImageOutputStream(outputFile), getSourceProduct());
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

        if (outputStream != null) {
            outputStream.flush();
            outputStream.close();
            outputStream = null;
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
