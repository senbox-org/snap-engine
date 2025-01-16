package eu.esa.snap.core.datamodel.band;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductVisitor;
import org.esa.snap.core.util.ImageUtils;
import org.esa.snap.core.util.SystemUtils;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Arrays;

public class SparseDataBand extends Band {

    private final SparseDataProvider dataProvider;
    private DataPoint[] data;

    public SparseDataBand(String name, int dataType, int width, int height, SparseDataProvider dataProvider) {
        super(name, dataType, width, height);
        this.dataProvider = dataProvider;
        data = null;
    }

    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        if (isPartOfSubset(subsetDef)) {
            int rasterWidth = getRasterWidth();
            int rasterHeight = getRasterHeight();
            int numDataElems;

            final int elementSize = ProductData.getElemSize(getDataType());

            if (subsetDef != null) {
                final String nodeName = getName();
                if (subsetDef.getRegionMap() != null && subsetDef.getRegionMap().containsKey(nodeName)) {
                    final Rectangle rectangle = subsetDef.getRegionMap().get(nodeName).getSubsetExtent();
                    rasterWidth = rectangle.width;
                    rasterHeight = rectangle.height;
                } else {
                    final Rectangle region = subsetDef.getRegion();
                    if (region != null) {
                        rasterWidth = region.width;
                        rasterHeight = region.height;
                    }
                }

                rasterWidth = (rasterWidth - 1) / subsetDef.getSubSamplingX() + 1;
                rasterHeight = (rasterHeight - 1) / subsetDef.getSubSamplingY() + 1;
            }

            numDataElems = rasterWidth * rasterHeight;

            return (long) numDataElems * elementSize;
        }

        return 0L;
    }

    @Override
    public int getPixelInt(int x, int y) {
        return (int) Math.round(getPixelDouble(x, y));
    }

    @Override
    public float getPixelFloat(int x, int y) {
        return (float) getPixelDouble(x, y);
    }

    @Override
    public double getPixelDouble(int x, int y) {
        ensureData();
        for (DataPoint point : data) {
            if (point.getX() == x && point.getY() == y) {
                double value = point.getValue();
                if (isScalingApplied()) {
                    return scale(value);
                } else {
                    return value;
                }
            }
        }
        return getNoDataValue();
    }

    @Override
    public int[] readPixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) throws IOException {
        pm.beginTask("readPixels", 100);

        final int noDataValue = (int) Math.round(getNoDataValue());
        Arrays.fill(pixels, noDataValue);

        ensureData();

        pm.worked(50);

        final Rectangle dataRegion = new Rectangle(x, y, w, h);
        for (final DataPoint dataPoint : data) {
            final int dataX = dataPoint.getX();
            final int dataY = dataPoint.getY();
            if (dataRegion.contains(dataX, dataY)) {
                final int offset = dataX - x + (dataY - y) * w;
                double value = dataPoint.getValue();
                if (isScalingApplied()) {
                    value = scale(value);
                }
                pixels[offset] = (int) Math.round(value);
            }
        }

        pm.done();

        return pixels;
    }

    @Override
    public int[] getPixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) {
        if (data == null) {
            throw new IllegalStateException("data not loaded yet");
        }

        int[] result;
        try {
            result = readPixels(x, y, w, h, pixels, pm);
        } catch (IOException e) {
            throw new IllegalStateException("data not loaded yet");
        }

        return result;
    }

    @Override
    public float[] readPixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) throws IOException {
        pm.beginTask("readPixels", 100);

        final float noDataValue = (float) getNoDataValue();
        Arrays.fill(pixels, noDataValue);

        ensureData();

        pm.worked(50);

        final Rectangle dataRegion = new Rectangle(x, y, w, h);
        for (final DataPoint dataPoint : data) {
            final int dataX = dataPoint.getX();
            final int dataY = dataPoint.getY();
            if (dataRegion.contains(dataX, dataY)) {
                final int offset = dataX - x + (dataY - y) * w;
                double value = dataPoint.getValue();
                if (isScalingApplied()) {
                    value = scale(value);
                }
                pixels[offset] = (float) value;
            }
        }

        pm.done();

        return pixels;
    }

    @Override
    public float[] getPixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) {
        if (data == null) {
            throw new IllegalStateException("data not loaded yet");
        }

        float[] result;
        try {
            result = readPixels(x, y, w, h, pixels, pm);
        } catch (IOException e) {
            throw new IllegalStateException("data not loaded yet");
        }

        return result;
    }

    @Override
    public double[] readPixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) throws IOException {
        pm.beginTask("readPixels", 100);

        final double noDataValue = getNoDataValue();
        Arrays.fill(pixels, noDataValue);

        ensureData();

        pm.worked(50);

        final Rectangle dataRegion = new Rectangle(x, y, w, h);
        for (final DataPoint dataPoint : data) {
            final int dataX = dataPoint.getX();
            final int dataY = dataPoint.getY();
            if (dataRegion.contains(dataX, dataY)) {
                final int offset = dataX - x + (dataY - y) * w;
                double value = dataPoint.getValue();
                if (isScalingApplied()) {
                    value = scale(value);
                }
                pixels[offset] = value;
            }
        }

        pm.done();

        return pixels;
    }

    @Override
    public double[] getPixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) {
        if (data == null) {
            throw new IllegalStateException("data not loaded yet");
        }

        double[] result;
        try {
            result = readPixels(x, y, w, h, pixels, pm);
        } catch (IOException e) {
            throw new IllegalStateException("data not loaded yet");
        }

        return result;
    }

    @Override
    public void readRasterDataFully(ProgressMonitor pm) throws IOException {
        ensureData();
    }

    @Override
    public void readRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData, ProgressMonitor pm) throws IOException {
        int type = rasterData.getType();
        switch (type) {
            case ProductData.TYPE_INT32:
                final int[] intData = (int[]) rasterData.getElems();
                readPixels(offsetX, offsetY, width, height, intData, pm);
                break;

            case ProductData.TYPE_FLOAT32:
                final float[] floatData = (float[]) rasterData.getElems();
                readPixels(offsetX, offsetY, width, height, floatData, pm);
                break;

            case ProductData.TYPE_FLOAT64:
                final double[] doubleData = (double[]) rasterData.getElems();
                readPixels(offsetX, offsetY, width, height, doubleData, pm);
                break;

            default:
                throw new IllegalStateException("not implemented for the requested data type");
        }
    }

    @Override
    public void writeRasterDataFully(ProgressMonitor pm) throws IOException {
        // @todo 3 tb/** could be implemented if required 2025-05-16 tb
        throw new RuntimeException("not implemented");
    }

    @Override
    public void writeRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData, ProgressMonitor pm) throws IOException {
        // @todo 3 tb/** could be implemented if required 2025-05-16 tb
        throw new RuntimeException("not implemented");
    }

    @Override
    public void writePixels(int x, int y, int w, int h, int[] pixels, ProgressMonitor pm) throws IOException {
        // @todo 3 tb/** could be implemented if required 2025-05-16 tb
        throw new IllegalStateException("operation not supported for sparse data band");
    }

    @Override
    public synchronized void writePixels(int x, int y, int w, int h, float[] pixels, ProgressMonitor pm) throws IOException {
        // @todo 3 tb/** could be implemented if required 2025-05-16 tb
        throw new IllegalStateException("operation not supported for sparse data band");
    }

    @Override
    public void writePixels(int x, int y, int w, int h, double[] pixels, ProgressMonitor pm) throws IOException {
        // @todo 3 tb/** could be implemented if required 2025-05-16 tb
        throw new IllegalStateException("operation not supported for sparse data band");
    }

    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        // do nothing 2025-05-16 tb
    }

    @Override
    protected RenderedImage createSourceImage() {
        final MultiLevelModel model = createMultiLevelModel();
        final RenderedImage image = ImageUtils.createRenderedImage(getRasterWidth(),
                getRasterHeight(),
                getRasterData());
        return new DefaultMultiLevelImage(new DefaultMultiLevelSource(image, model));
    }

    @Override
    public void ensureRasterData() {
        ensureData();
    }

    @Override
    public ProductData getRasterData() {
        ensureData();

        final int rasterWidth = getRasterWidth();
        final int rasterHeight = getRasterHeight();
        final int numElements = rasterWidth * rasterHeight;
        final ProductData productData = ProductData.createInstance(getDataType(), numElements);

        try {
            readRasterData(0, 0, rasterWidth, rasterHeight, productData, ProgressMonitor.NULL);
        } catch (IOException e) {
            SystemUtils.LOG.severe(e.getMessage());
        }

        return productData;
    }

    @Override
    public void unloadRasterData() {
        data = null;
    }

    private void ensureData() {
        if (data == null) {
            data = dataProvider.get();
        }
    }
}
