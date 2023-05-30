package org.esa.snap.dataio.gdal.reader;

import org.esa.snap.core.image.AbstractSubsetTileOpImage;
import org.esa.snap.core.image.ImageReadBoundsSupport;
import org.esa.snap.dataio.gdal.drivers.*;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A JAI operator for handling the tiles of the products imported with the GDAL library.
 *
 * @author Jean Coravu
 * @author Adrian Draghici
 */
class GDALTileOpImage extends AbstractSubsetTileOpImage {
    private ImageReader imageReader;

    GDALTileOpImage(GDALBandSource bandSource, int dataBufferType, int tileWidth, int tileHeight, int tileOffsetFromReadBoundsX, int tileOffsetFromReadBoundsY,
                    ImageReadBoundsSupport imageReadBoundsSupport, Dimension defaultJAIReadTileSize) {

        super(dataBufferType, tileWidth, tileHeight, tileOffsetFromReadBoundsX, tileOffsetFromReadBoundsY, imageReadBoundsSupport, defaultJAIReadTileSize);

        this.imageReader = new ImageReader(bandSource) {
            @Override
            protected int getLevel() {
                return GDALTileOpImage.this.getLevel();
            }

            @Override
            protected int getDataBufferType() {
                return GDALTileOpImage.this.getSampleModel().getDataType();
            }
        };
    }

    @Override
    public synchronized void dispose() {
        super.dispose();

        if (this.imageReader != null) {
            this.imageReader.close();
            this.imageReader = null;
        }
    }

    @Override
    protected synchronized void computeRect(PlanarImage[] sources, WritableRaster levelDestinationRaster, Rectangle levelDestinationRectangle) {
        Rectangle normalBoundsIntersection = computeIntersectionOnNormalBounds(levelDestinationRectangle);
        if (!normalBoundsIntersection.isEmpty()) {
            final int level = getLevel();
            int levelDestinationX = normalBoundsIntersection.x >> level;
            int levelDestinationY = normalBoundsIntersection.y >> level;
            int levelDestinationWidth = normalBoundsIntersection.width >> level;
            int levelDestinationHeight = normalBoundsIntersection.height >> level;
            try {
                Raster imageRaster = this.imageReader.read(levelDestinationX, levelDestinationY, levelDestinationWidth, levelDestinationHeight);
                levelDestinationRaster.setDataElements(levelDestinationRaster.getMinX(), levelDestinationRaster.getMinY(), imageRaster);
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to read the data for level " + level + " and rectangle " + levelDestinationRectangle + ".", ex);
            }
        }
    }

    private static abstract class ImageReader {
        private final String path;
        private final int bandIndex;
        final int[] index = new int[] { 0 };
        private int width;
        private int height;

        private ImageReader(GDALBandSource bandSource) {
            this.path = bandSource.getSourceLocalFile().toString();
            // bands are not 0-base indexed, so we must add 1
            this.bandIndex = bandSource.getBandIndex() + 1;
            this.width = -1;
            this.height = -1;
        }

        protected abstract int getLevel();

        protected abstract int getDataBufferType();

        int getBandWidth() {
            if (this.width == -1) {
                try (Dataset dataset = openDataset();
                     Band band = openBand(dataset)) {
                    this.width = band.getXSize();
                    this.height = band.getYSize();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return this.width;
        }

        int getBandHeight() {
            if (this.height == -1) {
                try (Dataset dataset = openDataset();
                     Band band = openBand(dataset)) {
                    this.width = band.getXSize();
                    this.height = band.getYSize();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return this.height;
        }

        void close() {
        }

        Raster read(int areaX, int areaY, int areaWidth, int areaHeight) throws IOException {
            try (Dataset dataset = openDataset();
                 Band band = openBand(dataset)) {
                final int pixels = areaWidth * areaHeight;
                final int gdalBufferDataType = band.getDataType();
                final int bufferSize = pixels * GDAL.getDataTypeSize(gdalBufferDataType);
                final ByteBuffer data = ByteBuffer.allocateDirect(bufferSize);
                data.order(ByteOrder.nativeOrder());

                final int dataBufferType = getDataBufferType();
                final int xSizeToRead = Math.min(areaWidth, getBandWidth() -  areaX);
                final int ySizeToRead = Math.min(areaHeight, getBandHeight() -  areaY);
                final int returnVal = band.readRasterDirect(areaX, areaY, xSizeToRead, ySizeToRead, areaWidth, areaHeight, gdalBufferDataType, data);
                if (returnVal == GDALConstConstants.ceNone()) {
                    DataBuffer imageDataBuffer;
                    switch (dataBufferType) {
                        case DataBuffer.TYPE_BYTE:
                            byte[] bytes = new byte[pixels];
                            data.get(bytes);
                            imageDataBuffer = new DataBufferByte(bytes, pixels);
                            break;
                        case DataBuffer.TYPE_SHORT:
                            short[] shorts = new short[pixels];
                            data.asShortBuffer().get(shorts);
                            imageDataBuffer = new DataBufferShort(shorts, shorts.length);
                            break;
                        case DataBuffer.TYPE_USHORT:
                            short[] ushorts = new short[pixels];
                            data.asShortBuffer().get(ushorts);
                            imageDataBuffer = new DataBufferUShort(ushorts, ushorts.length);
                            break;
                        case DataBuffer.TYPE_INT:
                            int[] ints = new int[pixels];
                            data.asIntBuffer().get(ints);
                            imageDataBuffer = new DataBufferInt(ints, ints.length);
                            break;
                        case DataBuffer.TYPE_FLOAT:
                            float[] floats = new float[pixels];
                            data.asFloatBuffer().get(floats);
                            imageDataBuffer = new DataBufferFloat(floats, floats.length);
                            break;
                        case DataBuffer.TYPE_DOUBLE:
                            double[] doubles = new double[pixels];
                            data.asDoubleBuffer().get(doubles);
                            imageDataBuffer = new DataBufferDouble(doubles, doubles.length);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown data buffer type " + dataBufferType + ".");
                    }
                    final SampleModel sampleModel = new ComponentSampleModel(imageDataBuffer.getDataType(), areaWidth, areaHeight, 1, areaWidth, index);
                    return Raster.createWritableRaster(sampleModel, imageDataBuffer, null);
                } else {
                    throw new IOException("Failed to read the product band data: rectangle=[" + areaX + ", " + areaY + ", " + areaWidth + ", " + areaHeight + "]" + " returnVal=" + returnVal + ".");
                }
            }
        }

        private Dataset openDataset() {
            return GDAL.open(this.path, GDALConst.gaReadonly());
        }

        private Band openBand(Dataset dataset) {
            if (dataset == null) {
                throw new IllegalStateException("Failed to open the GDAL dataset for file '" + this.path + "'.");
            }
            final Band band;
            Band gdalRasterBand = dataset.getRasterBand(this.bandIndex);
            int level = getLevel();
            if (level > 0 && gdalRasterBand.getOverviewCount() > 0) {
                band = gdalRasterBand.getOverview(level - 1);
                gdalRasterBand.delete();
            } else {
                band = gdalRasterBand;
            }
            return band;
        }
    }
}
