package org.esa.beam.dataio.bigtiff;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.bigtiff.internal.*;
import org.esa.beam.framework.datamodel.*;

import javax.imageio.stream.ImageOutputStream;
import java.io.IOException;
import java.util.ArrayList;

class BigGeoTiffBandWriter {

    private ImageOutputStream ios;
    private TiffIFD ifd;
    private Product tempProduct;
    private ArrayList<Band> bandsList;

    BigGeoTiffBandWriter(final TiffIFD ifd, final ImageOutputStream ios, final Product product) {
        this.ifd = ifd;
        this.ios = ios;
        tempProduct = product;
        final Band[] bands = tempProduct.getBands();
        bandsList = new ArrayList<>(bands.length);
        for (Band band : bands) {
            if(shouldWriteNode(band)) {
                bandsList.add(band);
            }
        }
    }

    void dispose() {
        ifd = null;
        ios = null;
        tempProduct = null;
    }

    public void writeBandRasterData(final Band sourceBand,
                                    final int regionX,
                                    final int regionY,
                                    final int regionWidth,
                                    final int regionHeight,
                                    final ProductData regionData,
                                    ProgressMonitor pm) throws IOException {
        if (!tempProduct.containsBand(sourceBand.getName())) {
            throw new IllegalArgumentException("'" + sourceBand.getName() + "' is not a band of the product");
        }
        final int bandDataType = ifd.getBandDataType();
        final int stripIndex = getStripIndex(sourceBand);
        final TiffValue[] offsetValues = ifd.getEntry(TiffTag.STRIP_OFFSETS).getValues();
        final long stripOffset = ((TiffLong) offsetValues[stripIndex]).getValue();
        final TiffValue[] bitsPerSampleValues = ifd.getEntry(TiffTag.BITS_PER_SAMPLE).getValues();
        final long elemSize = ((TiffShort) bitsPerSampleValues[stripIndex]).getValue() / 8;
        final long sourceWidthBytes = sourceBand.getSceneRasterWidth() * elemSize;
        final long regionOffsetXInBytes = regionX * elemSize;
        final long pixelOffset = sourceWidthBytes * regionY + regionOffsetXInBytes;
        final long startOffset = stripOffset + pixelOffset;

        pm.beginTask("Writing band '" + sourceBand.getName() + "'...", regionHeight);
        try {
            for (int y = 0; y < regionHeight; y++) {
                ios.seek(startOffset + y * sourceWidthBytes);
                final int stride = y * regionWidth;
                if (bandDataType == ProductData.TYPE_UINT8) {
                    final byte[] data = new byte[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (byte) regionData.getElemUIntAt(stride + x);
                    }
                    ios.write(data);
                } else if (bandDataType == ProductData.TYPE_INT8) {
                    final byte[] data = new byte[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (byte) regionData.getElemIntAt(stride + x);
                    }
                    ios.write(data);
                } else if (bandDataType == ProductData.TYPE_UINT16) {
                    final short[] data = new short[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (short) regionData.getElemUIntAt(stride + x);
                    }
                    ios.writeShorts(data, 0, regionWidth);
                } else if (bandDataType == ProductData.TYPE_INT16) {
                    final short[] data = new short[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (short) regionData.getElemIntAt(stride + x);
                    }
                    ios.writeShorts(data, 0, regionWidth);
                } else if (bandDataType == ProductData.TYPE_UINT32) {
                    final int[] data = new int[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = (int) regionData.getElemUIntAt(stride + x);
                    }
                    ios.writeInts(data, 0, regionWidth);
                } else if (bandDataType == ProductData.TYPE_INT32) {
                    final int[] data = new int[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = regionData.getElemIntAt(stride + x);
                    }
                    ios.writeInts(data, 0, regionWidth);
                } else if (bandDataType == ProductData.TYPE_FLOAT32) {
                    final float[] data = new float[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = regionData.getElemFloatAt(stride + x);
                    }
                    ios.writeFloats(data, 0, regionWidth);
                } else if (bandDataType == ProductData.TYPE_FLOAT64) {
                    final double[] data = new double[regionWidth];
                    for (int x = 0; x < regionWidth; x++) {
                        data[x] = regionData.getElemDoubleAt(stride + x);
                    }
                    ios.writeDoubles(data, 0, regionWidth);
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private int getStripIndex(Band sourceBand) {
        return bandsList.indexOf(sourceBand);
    }

    static boolean shouldWriteNode(ProductNode node) {
        if (node instanceof VirtualBand) {
            return false;
        } else if (node instanceof FilterBand) {
            return false;
        }
        return true;
    }
}
