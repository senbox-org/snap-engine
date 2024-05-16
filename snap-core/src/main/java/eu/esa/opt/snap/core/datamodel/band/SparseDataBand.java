package eu.esa.opt.snap.core.datamodel.band;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.AbstractBand;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductVisitor;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class SparseDataBand extends AbstractBand {

    private final SparseDataProvider dataProvider;
    private DataPoint[] data;

    public SparseDataBand(String name, int dataType, int rasterWidth, int rasterHeight, SparseDataProvider dataProvider) {
        super(name, dataType, rasterWidth, rasterHeight);
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
                    final Rectangle rectangle = subsetDef.getRegionMap().get(nodeName);
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
        return (int) getPixelDouble(x, y);
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
                return point.getValue();
            }
        }
        return getNoDataValue();
    }

    @Override
    public void readRasterDataFully(ProgressMonitor pm) throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void readRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData, ProgressMonitor pm) throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void writeRasterDataFully(ProgressMonitor pm) throws IOException {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void writeRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData, ProgressMonitor pm) throws IOException {
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
        throw new RuntimeException("not implemented");
    }

    @Override
    protected RenderedImage createSourceImage() {
        throw new RuntimeException("not implemented");
    }

    private void ensureData() {
        if (data == null) {
            data = dataProvider.get();
        }
    }
}
