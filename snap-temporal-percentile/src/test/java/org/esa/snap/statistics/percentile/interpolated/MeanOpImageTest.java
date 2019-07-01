package org.esa.snap.statistics.percentile.interpolated;

import org.esa.snap.core.util.SystemUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.Vector;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

public class MeanOpImageTest {

    @BeforeClass
    public static void setUp() {
        // Triggering init of JAI/GeoTools
        // See GeoTools mailing list - https://sourceforge.net/p/geotools/mailman/message/36694323/
        SystemUtils.init3rdPartyLibs(MeanOpImageTest.class);
    }

    @Test
    public void testWithThreeFloatValues() {
        final Vector<RenderedImage> sources = new Vector<>();
        sources.add(ConstantDescriptor.create(2f, 2f, new Float[]{2f}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Float[]{4f}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Float[]{6f}, null));

        //execution
        final MeanOpImage meanOpImage = new MeanOpImage(sources);
        final Raster data = meanOpImage.getAsBufferedImage().getData();
        //verification
        assertArrayEquals(new float[]{4, 4, 4, 4}, data.getPixels(0, 0, 2, 2, new float[4]), 1e-6f);
    }

    @Test
    public void testWithThreeDoubleValues() {
        final Vector<RenderedImage> sources = new Vector<>();
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{2d}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{4d}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{6d}, null));

        //execution
        final MeanOpImage meanOpImage = new MeanOpImage(sources);
        final Raster data = meanOpImage.getAsBufferedImage().getData();
        //verification
        assertArrayEquals(new double[]{4, 4, 4, 4}, data.getPixels(0, 0, 2, 2, new double[4]), 1e-6f);
    }

    @Test
    public void testWithThreeFloatValues_OneIsNaN() {
        final Vector<RenderedImage> sources = new Vector<>();
        sources.add(ConstantDescriptor.create(2f, 2f, new Float[]{Float.NaN}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Float[]{4f}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Float[]{6f}, null));

        //execution
        final MeanOpImage meanOpImage = new MeanOpImage(sources);
        final Raster data = meanOpImage.getAsBufferedImage().getData();
        //verification
        assertArrayEquals(new float[]{5, 5, 5, 5}, data.getPixels(0, 0, 2, 2, new float[4]), 1e-6f);
    }

    @Test
    public void testWithThreeDoubleValues_OneIsNaN() {
        final Vector<RenderedImage> sources = new Vector<>();
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{2d}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{4d}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{Double.NaN}, null));

        //execution
        final MeanOpImage meanOpImage = new MeanOpImage(sources);
        final Raster data = meanOpImage.getAsBufferedImage().getData();
        //verification
        assertArrayEquals(new double[]{3, 3, 3, 3}, data.getPixels(0, 0, 2, 2, new double[4]), 1e-6f);
    }

    @Test
    public void testWithThreeDoubleValues_TwoAreNaN() {
        final Vector<RenderedImage> sources = new Vector<>();
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{2d}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{Double.NaN}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{Double.NaN}, null));

        //execution
        final MeanOpImage meanOpImage = new MeanOpImage(sources);
        final Raster data = meanOpImage.getAsBufferedImage().getData();
        //verification
        assertArrayEquals(new double[]{2, 2, 2, 2}, data.getPixels(0, 0, 2, 2, new double[4]), 1e-6f);
    }

    @Test
    public void testWithThreeFloatValues_AllAreNaN() {
        final float n = Float.NaN;

        final Vector<RenderedImage> sources = new Vector<>();
        sources.add(ConstantDescriptor.create(2f, 2f, new Float[]{n}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Float[]{n}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Float[]{n}, null));

        //execution
        final MeanOpImage meanOpImage = new MeanOpImage(sources);
        final Raster data = meanOpImage.getAsBufferedImage().getData();
        //verification
        assertArrayEquals(new float[]{n, n, n, n}, data.getPixels(0, 0, 2, 2, new float[4]), 1e-6f);
    }

    @Test
    public void testWithThreeDoubleValues_AllAreNaN() {
        final double n = Double.NaN;

        final Vector<RenderedImage> sources = new Vector<>();
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{n}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{n}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{n}, null));

        //execution
        final MeanOpImage meanOpImage = new MeanOpImage(sources);
        final Raster data = meanOpImage.getAsBufferedImage().getData();
        //verification
        assertArrayEquals(new double[]{n, n, n, n}, data.getPixels(0, 0, 2, 2, new double[4]), 1e-6f);
    }


    // Actually UnsupportedOperationException expected. See comment in MeanOpImageTest#setUp for explanation
    @Test(expected = UnsupportedOperationException.class)
    public void testThatOperatorExceptionOccursWhenNoFloatingPointImagesAreProvided() {
        final Vector<RenderedImage> sources = new Vector<>();
        sources.add(ConstantDescriptor.create(2f, 2f, new Integer[]{3}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Integer[]{2}, null));

        //execution
        final MeanOpImage meanOpImage = new MeanOpImage(sources);
        meanOpImage.getAsBufferedImage();

        fail("Should not reach this line");
    }

    @Test
    public void testThatNoOperatorExceptionOccursWhenAtLeastOneFloatingPointImageIsProvided() {
        final Vector<RenderedImage> sources = new Vector<>();
        sources.add(ConstantDescriptor.create(2f, 2f, new Integer[]{3}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Integer[]{2}, null));
        sources.add(ConstantDescriptor.create(2f, 2f, new Double[]{4d}, null));

        //execution
        final MeanOpImage meanOpImage = new MeanOpImage(sources);
        final Raster data = meanOpImage.getAsBufferedImage().getData();

        assertArrayEquals(new double[]{3, 3, 3, 3}, data.getPixels(0, 0, 2, 2, new double[4]), 1e-6f);
    }
}
