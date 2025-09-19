package org.esa.snap.dataio.gdal.reader;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Vector;
import java.util.stream.Collectors;
import javax.media.jai.PlanarImage;
import org.esa.snap.core.image.AbstractSubsetTileOpImage;
import org.esa.snap.core.image.ImageReadBoundsSupport;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dataio.gdal.drivers.Band;
import org.esa.snap.dataio.gdal.drivers.BuildVRTOptions;
import org.esa.snap.dataio.gdal.drivers.Dataset;
import org.esa.snap.dataio.gdal.drivers.GDAL;
import org.esa.snap.dataio.gdal.drivers.GDALConst;
import org.esa.snap.dataio.gdal.drivers.GDALConstConstants;

/**
 * A JAI operator for handling the tiles of the products imported with the GDAL library.
 *
 * @author Jean Coravu
 * @author Adrian Draghici
 */
class GDALTileOpImage extends AbstractSubsetTileOpImage {

    private ImageReader imageReader;

    GDALTileOpImage(GDALBandSource bandSource, int dataBufferType, int tileWidth, int tileHeight,
                    int tileOffsetFromReadBoundsX, int tileOffsetFromReadBoundsY,
                    ImageReadBoundsSupport imageReadBoundsSupport, Dimension defaultJAIReadTileSize) {

        super(dataBufferType, tileWidth, tileHeight, tileOffsetFromReadBoundsX, tileOffsetFromReadBoundsY,
                imageReadBoundsSupport, defaultJAIReadTileSize);

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
    protected void computeRect(PlanarImage[] sources, WritableRaster levelDestinationRaster,
                               Rectangle levelDestinationRectangle) {
        Rectangle normalBoundsIntersection = computeIntersectionOnNormalBounds(levelDestinationRectangle);
        if (!normalBoundsIntersection.isEmpty()) {
            final int level = getLevel();
            int levelDestinationX = normalBoundsIntersection.x >> level;
            int levelDestinationY = normalBoundsIntersection.y >> level;
            int levelDestinationWidth = ensureMinimunDimension(normalBoundsIntersection.width, level);
            int levelDestinationHeight = ensureMinimunDimension(normalBoundsIntersection.height, level);
            try {
                Raster imageRaster = this.imageReader.read(levelDestinationX, levelDestinationY, levelDestinationWidth,
                        levelDestinationHeight);
                levelDestinationRaster.setDataElements(levelDestinationRaster.getMinX(), levelDestinationRaster.getMinY(),
                        imageRaster);
            } catch (ProductClosedException ignore) {
                SystemUtils.LOG.fine("Product was already closed. Leave tile empty");
            } catch (IOException ex) {
                throw new IllegalStateException(
                        "Failed to read the data for level " + level + " and rectangle " + levelDestinationRectangle + ".", ex);
            }
        }
    }

    public int ensureMinimunDimension(int value, int level) {
        return Math.max(1, value >> level);
    }

    private abstract static class ImageReader {

        private final Path[] paths;
        private final int bandIndex;
        final int[] index = new int[]{0};

        private ImageReader(GDALBandSource bandSource) {
            this.paths = bandSource.getSourceLocalFiles();
            // bands are not 0-base indexed, so we must add 1
            this.bandIndex = bandSource.getBandIndex() + 1;
        }

        protected abstract int getLevel();

        protected abstract int getDataBufferType();

        void close() {
        }

        Raster read(int areaX, int areaY, int areaWidth, int areaHeight) throws IOException {
            final int pixels = areaWidth * areaHeight;
            if (!filesExist(this.paths)) {
                throw new ProductClosedException();
            }
            final ByteBuffer data = readRasterFromDataset(areaX, areaY, areaWidth, areaHeight, pixels, true);
            final int dataBufferType = getDataBufferType();
            final DataBuffer imageDataBuffer = buildImageDataBuffer(data, dataBufferType, pixels);
            return buildWritableRaster(imageDataBuffer, areaWidth, areaHeight);
        }

        private boolean filesExist(Path[] paths) {
            if (paths == null || paths.length == 0) {
                return false;
            }
            for (Path p : paths) {
                if (p == null || !Files.exists(p)) {
                    return false;
                }
            }
            return true;
        }

        ByteBuffer readRasterFromDataset(int areaX, int areaY, int areaWidth, int areaHeight, int pixels, boolean retry)
                throws IOException {
            try (Dataset dataset = openDataset(); Band band = openBand(dataset)) {
                final int gdalBufferDataType = band.getDataType();
                final int bufferSize = pixels * (GDAL.getDataTypeSize(gdalBufferDataType) >> 3);
                final ByteBuffer data = ByteBuffer.allocateDirect(bufferSize);
                data.order(ByteOrder.nativeOrder());

                final int bandWidth = band.getXSize();
                final int bandHeight = band.getYSize();
                final int xSizeToRead = Math.min(areaWidth, bandWidth - areaX);
                final int ySizeToRead = Math.min(areaHeight, bandHeight - areaY);
                int returnVal = band.readRasterDirect(areaX, areaY, xSizeToRead, ySizeToRead, areaWidth, areaHeight,
                        gdalBufferDataType, data);
                if (returnVal == GDALConstConstants.cpleFileIO() && retry) {
                    try {
                        Thread.sleep(1000);
                        return readRasterFromDataset(areaX, areaY, areaWidth, areaHeight, pixels, false);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if (returnVal == GDALConstConstants.ceNone()) {
                    return data;
                } else {
                    throw new IOException(
                            "Failed to read the product band data: rectangle=[" + areaX + ", " + areaY + ", " + areaWidth
                                    + ", " + areaHeight + "]" + " returnVal=" + returnVal + ".");
                }
            }
        }

        DataBuffer buildImageDataBuffer(ByteBuffer data, int dataBufferType, int pixels) {
            DataBuffer imageDataBuffer;
            try {
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
            } finally {
                data.clear();
            }
            return imageDataBuffer;
        }

        Raster buildWritableRaster(DataBuffer imageDataBuffer, int areaWidth, int areaHeight) {
            final int imageDataBufferType = imageDataBuffer.getDataType();
            final SampleModel sampleModel = new ComponentSampleModel(imageDataBufferType, areaWidth, areaHeight, 1, areaWidth,
                    index);
            return Raster.createWritableRaster(sampleModel, imageDataBuffer, null);
        }

        private Dataset openDataset() {
            if (this.paths.length == 1) {
                return GDAL.open(this.paths[0].toString(), GDALConst.gaReadonly());
            } else {
                final Dataset[] datasets = new Dataset[this.paths.length];
                for (int i = 0; i < this.paths.length; i++) {
                    datasets[i] = GDAL.open(this.paths[i].toString(), GDALConst.gaReadonly());
                }
                final Dataset vrtDataset = GDAL.buildVRT("/vsimem/" + System.currentTimeMillis() + ".vrt",
                        datasets, new BuildVRTOptions(new Vector<>()));
                return new VRTDataset(vrtDataset, datasets);
            }
        }

        private Band openBand(Dataset dataset) {
            if (dataset == null) {
                throw new IllegalStateException(String.format("Failed to open the GDAL dataset for file%s '%s'.",
                        this.paths.length > 1 ? "s" : "",
                        Arrays.stream(this.paths)
                                .map(Path::toString)
                                .collect(Collectors.joining(","))));
            }
            final Band band;
            Band gdalRasterBand = (dataset instanceof VRTDataset
                    ? ((VRTDataset) dataset).getDataset()
                    : dataset).getRasterBand(this.bandIndex);
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

    private static class VRTDataset extends Dataset {

        private final Dataset vrtDataset;
        private final Dataset[] sourceDatasets;

        public VRTDataset(Dataset vrtDataset, Dataset[] sourceDatasets) {
            super();
            this.vrtDataset = vrtDataset;
            this.sourceDatasets = sourceDatasets;
        }

        public Dataset getDataset() {
            return vrtDataset;
        }

        @Override
        public void close() throws IOException {
            if (this.sourceDatasets != null) {
                for (Dataset source : this.sourceDatasets) {
                    source.close();
                }
            }
            if (this.vrtDataset != null) {
                this.vrtDataset.close();
            }
        }
    }

    static final class ProductClosedException extends IOException {

        ProductClosedException(){
            super("product closed");
        }
    }
}
