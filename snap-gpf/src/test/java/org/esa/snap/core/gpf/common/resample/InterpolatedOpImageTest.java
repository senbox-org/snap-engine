package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.image.ImageManager;
import org.junit.Before;
import org.junit.Test;

import javax.media.jai.ImageLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNotNull;

/**
 * @author Tonio Fincke
 */
public class InterpolatedOpImageTest {

    private Band referenceBand;

    @Before
    public void setUp() {
        int referenceWidth = 3;
        int referenceHeight = 3;
        referenceBand = new Band("referenceBand", ProductData.TYPE_INT8, 3, 3);
        final AffineTransform imageToModelTransform = new AffineTransform(3, 0, 0, 3, 2, 2);
        final DefaultMultiLevelModel referenceModel = new DefaultMultiLevelModel(imageToModelTransform, 3, 3);
        referenceBand.setSourceImage(new DefaultMultiLevelImage(new AbstractMultiLevelSource(referenceModel) {
            @Override
            protected RenderedImage createImage(int level) {
                return new BufferedImage(referenceWidth / (1 + level), referenceHeight / (1 + level), ProductData.TYPE_INT8);
            }
        }));
        final Product product = new Product("product", "type", 9, 9);
        product.addBand(referenceBand);
    }

    @Test
    public void testInterpolate_Double_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testInterpolate_Float_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_FLOAT32);
    }

    @Test
    public void testInterpolate_Byte_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_INT8);
    }

    @Test
    public void testInterpolate_Short_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_INT16);
    }

    @Test
    public void testInterpolate_UShort_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_UINT16);
    }

    @Test
    public void testInterpolate_Int_NearestNeighbour() throws NoninvertibleTransformException {
        testNearestNeighbour(ProductData.TYPE_INT32);
    }

    @Test
    public void testInterpolate_Double_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testInterpolate_Float_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_FLOAT32);
    }

    @Test
    public void testInterpolate_Byte_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_INT8);
    }

    @Test
    public void testInterpolate_Short_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_INT16);
    }

    @Test
    public void testInterpolate_UShort_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_UINT16);
    }

    @Test
    public void testInterpolate_Int_Bilinear() throws NoninvertibleTransformException {
        testBilinear(ProductData.TYPE_INT32);
    }

    @Test
    public void testBilinear_FirstAndLastPixelValid() throws NoninvertibleTransformException {
        String expression = "(X%2 == 0.5) ^ (Y%2 == 0.5) ? 123 :(X + 0.5) + ((Y + 0.5) * 2)";
        final Band sourceBand = createSourceBand(expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(3.059999942779541, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(4.5, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(4.5, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(5.940000057220459, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    public void testBilinear_MiddlePixelsValid() throws NoninvertibleTransformException {
        String expression = "(X%2 == 0.5) ^ (Y%2 == 0.5) ? (X + 0.5) + ((Y + 0.5) * 2) : 123";
        final Band sourceBand = createSourceBand(expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(4.5, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(4.019999980926514, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(4.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(4.980000019073486, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(4.5, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(4.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(5.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(5.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(123.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    public void testBilinear_FirstPixelIsInvalid() throws NoninvertibleTransformException {
        String expression = "(X == 0.5) && (Y == 0.5) ? 123 : (X + 0.5) + ((Y + 0.5) * 2)";
        final Band sourceBand = createSourceBand(expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(4.5234375, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(4.2890625, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(4.25, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(5.0390625, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(5.6484375, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(5.75, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(5.125, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(5.875, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    public void testBilinear_SecondPixelIsInvalid() throws NoninvertibleTransformException {
        String expression = "(X == 1.5) && (Y == 0.5) ? 123 : (X + 0.5) + ((Y + 0.5) * 2)";
        final Band sourceBand = createSourceBand(expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(3.4296875, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(4.5078125, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(4.8828125, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(5.6796875, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(5.125, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(5.875, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    public void testBilinear_ThirdPixelIsInvalid() throws NoninvertibleTransformException {
        String expression = "(X == 0.5) && (Y == 1.5) ? 123 : (X + 0.5) + ((Y + 0.5) * 2)";
        final Band sourceBand = createSourceBand(expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(3.3203125, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(4.1171875, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(4.25, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(4.4921875, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(5.5703125, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(5.75, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(6.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    public void testBilinear_FourthPixelIsInvalid() throws NoninvertibleTransformException {
        String expression = "(X == 1.5) && (Y == 1.5) ? 123 : (X + 0.5) + ((Y + 0.5) * 2)";
        final Band sourceBand = createSourceBand(expression);
        sourceBand.getSourceImage().getData().createCompatibleWritableRaster();
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(3.3515625, targetData.getSampleDouble(0, 0, 0), 1e-6);
        assertEquals(3.9609375, targetData.getSampleDouble(1, 0, 0), 1e-6);
        assertEquals(4.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
        assertEquals(4.7109375, targetData.getSampleDouble(0, 1, 0), 1e-6);
        assertEquals(4.4765625, targetData.getSampleDouble(1, 1, 0), 1e-6);
        assertEquals(4.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
        assertEquals(5.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
        assertEquals(5.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
        assertEquals(123.0, targetData.getSampleDouble(2, 2, 0));
    }

    @Test
    public void testInterpolate_Double_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_FLOAT64);
    }

    @Test
    public void testInterpolate_Float_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_FLOAT32);
    }

    @Test
    public void testInterpolate_Byte_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_INT8);
    }

    @Test
    public void testInterpolate_Short_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_INT16);
    }

    @Test
    public void testInterpolate_UShort_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_UINT16);
    }

    @Test
    public void testInterpolate_Int_CubicConvolution() throws NoninvertibleTransformException {
        testCubicConvolution(ProductData.TYPE_INT32);
    }

    private void testCubicConvolution(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Cubic_Convolution),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        if ((dataType == ProductData.TYPE_FLOAT32) || (dataType == ProductData.TYPE_FLOAT64)) {
            assertEquals(3.333984375, targetData.getSampleDouble(0, 0, 0), 1e-6);
            assertEquals(4.669921875, targetData.getSampleDouble(1, 0, 0), 1e-6);
            assertEquals(5.1640625, targetData.getSampleDouble(2, 0, 0), 1e-6);
            assertEquals(4.001953125, targetData.getSampleDouble(0, 1, 0), 1e-6);
            assertEquals(5.337890625, targetData.getSampleDouble(1, 1, 0), 1e-6);
            assertEquals(5.75, targetData.getSampleDouble(2, 1, 0), 1e-6);
            assertEquals(5.892578125, targetData.getSampleDouble(0, 2, 0), 1e-6);
            assertEquals(7.064453125, targetData.getSampleDouble(1, 2, 0), 1e-6);
            assertEquals(7.69921875, targetData.getSampleDouble(2, 2, 0), 1e-6);
        } else {
            assertEquals(3.0, targetData.getSampleDouble(0, 0, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(1, 0, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
            assertEquals(4.0, targetData.getSampleDouble(0, 1, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(1, 1, 0), 1e-6);
            assertEquals(6.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
            assertEquals(6.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
            assertEquals(7.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
            assertEquals(8.0, targetData.getSampleDouble(2, 2, 0), 1e-6);
        }
    }

    private void testBilinear(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);
        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Bilinear),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        if (!(dataType == ProductData.TYPE_FLOAT32) && !(dataType == ProductData.TYPE_FLOAT64)) {
            assertEquals(3.0, targetData.getSampleDouble(0, 0, 0), 1e-6);
            assertEquals(4.0, targetData.getSampleDouble(1, 0, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(2, 0, 0), 1e-6);
            assertEquals(5.0, targetData.getSampleDouble(0, 1, 0), 1e-6);
            assertEquals(6.0, targetData.getSampleDouble(1, 1, 0), 1e-6);
            assertEquals(6.0, targetData.getSampleDouble(2, 1, 0), 1e-6);
            assertEquals(6.0, targetData.getSampleDouble(0, 2, 0), 1e-6);
            assertEquals(7.0, targetData.getSampleDouble(1, 2, 0), 1e-6);
            assertEquals(8.0, targetData.getSampleDouble(2, 2, 0), 1e-6);
        } else {
            assertEquals(3.375, targetData.getSampleDouble(0, 0, 0), 1e-6);
            assertEquals(4.125, targetData.getSampleDouble(1, 0, 0), 1e-6);
            assertEquals(4.875, targetData.getSampleDouble(2, 0, 0), 1e-6);
            assertEquals(4.875, targetData.getSampleDouble(0, 1, 0), 1e-6);
            assertEquals(5.625, targetData.getSampleDouble(1, 1, 0), 1e-6);
            assertEquals(6.375, targetData.getSampleDouble(2, 1, 0), 1e-6);
            assertEquals(6.375, targetData.getSampleDouble(0, 2, 0), 1e-6);
            assertEquals(7.125, targetData.getSampleDouble(1, 2, 0), 1e-6);
            assertEquals(7.875, targetData.getSampleDouble(2, 2, 0), 1e-6);
        }
    }

    private void testNearestNeighbour(int dataType) throws NoninvertibleTransformException {
        final Band sourceBand = createSourceBand(dataType);
        final int dataBufferType = sourceBand.getSourceImage().getSampleModel().getDataType();
        final ImageLayout imageLayout = ImageManager.createSingleBandedImageLayout(referenceBand, dataBufferType);

        final InterpolatedOpImage image = new InterpolatedOpImage(sourceBand, sourceBand.getSourceImage(), imageLayout,
                                                                  sourceBand.getNoDataValue(), dataBufferType,
                                                                  ResampleUtils.getUpsamplingFromInterpolationType(InterpolationType.Nearest),
                                                                  sourceBand.getImageToModelTransform(),
                                                                  referenceBand.getImageToModelTransform());

        assertNotNull(image);
        assertEquals(referenceBand.getRasterWidth(), image.getWidth());
        assertEquals(referenceBand.getRasterHeight(), image.getHeight());
        assertEquals(dataBufferType, image.getSampleModel().getDataType());
        final Raster targetData = image.getData();
        assertEquals(3.0, targetData.getSampleDouble(0, 0, 0), 1e-8);
        assertEquals(4.0, targetData.getSampleDouble(1, 0, 0), 1e-8);
        assertEquals(5.0, targetData.getSampleDouble(2, 0, 0), 1e-8);
        assertEquals(5.0, targetData.getSampleDouble(0, 1, 0), 1e-8);
        assertEquals(6.0, targetData.getSampleDouble(1, 1, 0), 1e-8);
        assertEquals(7.0, targetData.getSampleDouble(2, 1, 0), 1e-8);
        assertEquals(7.0, targetData.getSampleDouble(0, 2, 0), 1e-8);
        assertEquals(8.0, targetData.getSampleDouble(1, 2, 0), 1e-8);
        assertEquals(9.0, targetData.getSampleDouble(2, 2, 0), 1e-8);
    }

    private Band createSourceBand(int dataType) {
        return createSourceBand(dataType, "(X + 0.5) + ((Y + 0.5) * 2)", 4);
    }

    private Band createSourceBand(String expression) {
        return createSourceBand(ProductData.TYPE_FLOAT32, expression, 2);
    }

    private Band createSourceBand(int dataType, String expression, int widthAndHeight) {
        int sourceScaleX = 4;
        int sourceScaleY = 4;
        int sourceTranslateX = 1;
        int sourceTranslateY = 1;
        final AffineTransform imageToModelTransform = new AffineTransform(sourceScaleX, 0, 0, sourceScaleY, sourceTranslateX, sourceTranslateY);
        final Product sourceProduct = new Product("dummy", "dummy", widthAndHeight, widthAndHeight);
        final Band sourceBand = sourceProduct.addBand("sourceBand", expression, dataType);
        sourceBand.setNoDataValue(123);
        sourceBand.setImageToModelTransform(imageToModelTransform);
        return sourceBand;
    }

}