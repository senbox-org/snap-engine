package org.esa.beam.dataio;

import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.MultiLevelSource;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import ncsa.hdf.hdf5lib.H5;
import ncsa.hdf.hdf5lib.HDF5Constants;
import ncsa.hdf.hdf5lib.exceptions.HDF5Exception;
import ncsa.hdf.hdf5lib.exceptions.HDF5LibraryException;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h5.H5ScalarDS;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.jai.RasterDataNodeOpImage;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.util.ImageUtils;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.List;

public class MultiLevelSourceProductTest {

    static final int L = 6;         // # resolution levels for the image pyramid
    static final int T = 1024;      // Image tile size at highest resolution level (=0)
    static final int W = 5 * T;     // Image width at highest resolution level (=0)
    static final int H = 4 * T;     // Image height at highest resolution level (=0)

    @Test
    public void testCreateMultiLevelSourceImage() throws IOException {
        Product product = createTestProduct(W, H, T, L);
        Band outputBand = product.getBand("magnitude");

        System.out.println("Accuracy test...");
        double[] pixels = new double[6];
        outputBand.readPixels(100, 120, 3, 2, pixels);
        assertEqualDoubles(pixels[0], getExpectedValue(100, 120), 1e-8);
        assertEqualDoubles(pixels[1], getExpectedValue(101, 120), 1e-8);
        assertEqualDoubles(pixels[2], getExpectedValue(102, 120), 1e-8);
        assertEqualDoubles(pixels[3], getExpectedValue(100, 121), 1e-8);
        assertEqualDoubles(pixels[4], getExpectedValue(101, 121), 1e-8);
        assertEqualDoubles(pixels[5], getExpectedValue(102, 121), 1e-8);
        System.out.println("Success.");

        int pixelsCount = W * H;
        double[] allPixels = new double[pixelsCount];

        System.out.println("Performance test...");
        long t0 = System.currentTimeMillis();
        outputBand.readPixels(0, 0, W, H, allPixels);
        long dt = System.currentTimeMillis() - t0;
        System.out.println(pixelsCount + " pixels processed in " + dt + " ms or " + (double) pixelsCount / dt + " pixels/ms");
    }

    private static double getExpectedValue(int x, int y) {
        double X = x + 0.5;
        double Y = y + 0.5;
        double real = X * X - Y * Y;
        double imag = 2 * X * Y;
        return Math.sqrt(real * real + imag * imag);
    }

    private static void assertEqualDoubles(double expected, double actual, double eps) {
        if (Math.abs(expected - actual) > eps) {
            throw new IllegalStateException("Expected " + expected + " but got " + actual);
        }
    }



    static Product createTestProduct(int width, int height, int tileSize, int numResolutionsMax) {
        Product product = new Product("test", "test", width, height);
        product.setPreferredTileSize(tileSize, tileSize);
        product.setNumResolutionsMax(numResolutionsMax);

        final Band realBand = product.addBand("real", "X * X - Y * Y", ProductData.TYPE_FLOAT64);
        final Band imagBand = product.addBand("imag", "2 * X * Y", ProductData.TYPE_FLOAT64);
        final Band magnitudeBand = product.addBand("magnitude", ProductData.TYPE_FLOAT64);

        MultiLevelModel multiLevelModel = ImageManager.getMultiLevelModel(realBand);
        MultiLevelSource multiLevelSource = new AbstractMultiLevelSource(multiLevelModel) {

            @Override
            public void reset() {
                super.reset();
                magnitudeBand.fireProductNodeDataChanged();
            }

            @Override
            public RenderedImage createImage(int level) {
                return new MagnitudeOpImage(magnitudeBand, ResolutionLevel.create(getModel(), level),
                                            realBand, imagBand);
            }
        };

        magnitudeBand.setSourceImage(new DefaultMultiLevelImage(multiLevelSource));
        return product;
    }

    static class MagnitudeOpImage extends RasterDataNodeOpImage {

        private final Band realBand;
        private final Band imagBand;

        public MagnitudeOpImage(Band outputBand, ResolutionLevel level, Band realBand, Band imagBand) {
            super(outputBand, level);
            this.realBand = realBand;
            this.imagBand = imagBand;
        }

        @Override
        protected void computeProductData(ProductData outputData, Rectangle region) throws IOException {
            ProductData realData = getRawProductData(realBand, region);
            ProductData imagData = getRawProductData(imagBand, region);
            int numElems = region.width * region.height;
            for (int i = 0; i < numElems; i++) {
                double real = realData.getElemDoubleAt(i);
                double imag = imagData.getElemDoubleAt(i);
                double result = Math.sqrt(real * real + imag * imag);
                outputData.setElemDoubleAt(i, result);
            }
        }

        // these methods are taken from RasterDataNodeOpImage in SNAP. The BEAM version does not contain them:

        private ProductData getRawProductData(RasterDataNode band, Rectangle region) {
            return getProductData(band.getSourceImage().getImage(getLevel()), band.getDataType(), region);
        }

        private ProductData getProductData(RenderedImage image, int productDataType, Rectangle region) {
            Raster raster = image.getData(region);
            boolean directMode = raster.getDataBuffer().getSize() == region.width * region.height;
            if (directMode) {
                return ProductData.createInstance(productDataType, ImageUtils.getPrimitiveArray(raster.getDataBuffer()));
            } else {
                final ProductData instance = ProductData.createInstance(productDataType, region.width * region.height);
                raster.getDataElements(region.x, region.y, region.width, region.height, instance.getElems());
                return instance;
            }
        }

    }

}
