package org.esa.s3tbx.idepix.algorithms.landsat8;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.image.ImageManager;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.jai.SingleBandedSampleModel;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BandSelectDescriptor;
import javax.media.jai.operator.FormatDescriptor;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Operator to generate grey or binary images with Otsu algorithm
 * (see e.g. http://zerocool.is-a-geek.net/java-image-binarization/)
 * Target product will contain just one band with this image
 *
 * @author olafd
 */
@OperatorMetadata(alias = "idepix.landsat8.otsu",
        version = "2.2.1",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2015 by Brockmann Consult",
        description = "Landsat 8 Otsu binarization: provides product with binarized R, G, and B image.")
public class OtsuBinarizeOp extends Operator {

    public static final String OTSU_BINARY_BAND_NAME = "OTSU_BINARY";
    public static final String OTSU_GREY_BAND_NAME = "OTSU_GREY";

    @SourceProduct(alias = "l8source", description = "The source product.")
    Product sourceProduct;

    @SourceProduct(alias = "clost", description = "The CLOST product.")
    Product clostProduct;

    @TargetProduct(description = "The target product.")
    Product targetProduct;

    @Parameter(defaultValue = "GREY",
            valueSet = {"GREY", "BINARY"},
            description = "OTSU processing mode (grey or binary target image)",
            label = "OTSU processing mode (grey or binary target image)")
    private String otsuMode;


    @Override
    public void initialize() throws OperatorException {
        final Band redBand = sourceProduct.getBand(Landsat8Constants.LANDSAT8_RED_BAND_NAME);
        final Band greenBand = sourceProduct.getBand(Landsat8Constants.LANDSAT8_GREEN_BAND_NAME);
        final Band blueBand = sourceProduct.getBand(Landsat8Constants.LANDSAT8_BLUE_BAND_NAME);
        final Band cirrusBand = sourceProduct.getBand(Landsat8Constants.LANDSAT8_CIRRUS_BAND_NAME);
        final Band aerosolBand = sourceProduct.getBand(Landsat8Constants.LANDSAT8_COASTAL_AEROSOL_BAND_NAME);
        final Band panBand = sourceProduct.getBand(Landsat8Constants.LANDSAT8_PANCHROMATIC_BAND_NAME);

        // MPa: try with clost band:
        Band clostBand = clostProduct.getBand(ClostOp.CLOST_BAND_NAME);
        // MPa: now try with cirrus band...:     // todo: define what we want!!
//        Band clostBand = sourceProduct.getBand(Landsat8Constants.LANDSAT8_CIRRUS_BAND_NAME);

//        final RasterDataNode[] rgbChannelNodes = new RasterDataNode[]{redBand, greenBand, blueBand};
//        final RasterDataNode[] rgbChannelNodes = new RasterDataNode[]{cirrusBand};
        final RasterDataNode[] rgbChannelNodes = new RasterDataNode[]{clostBand};

        try {
            final ImageInfo clostImageInfo = ProductUtils.createImageInfo(rgbChannelNodes, true, ProgressMonitor.NULL);
            BufferedImage clostImageRgb = ProductUtils.createRgbImage(rgbChannelNodes, clostImageInfo, ProgressMonitor.NULL);
            final BufferedImage clostImageGray = OtsuBinarize.toGray(clostImageRgb);
            final BufferedImage clostImageBinarized = OtsuBinarize.binarize(clostImageGray);
            Product otsuProduct;
            if (otsuMode.equals("GREY")) {
                otsuProduct = createGreyProduct(clostImageGray);
            } else {
                otsuProduct = createBinarizedProduct(clostImageBinarized);
            }

            ProductUtils.copyBand(ClostOp.CLOST_BAND_NAME, clostProduct, otsuProduct, true);
            setTargetProduct(otsuProduct);
        } catch (IOException e) {
            throw new OperatorException("Cannot do OTSU binarization: " + e.getMessage());
        }
    }

    private Product createBinarizedProduct(BufferedImage sourceImage) {

        Product product = new Product(sourceProduct.getName() + "_binary",
                sourceProduct.getProductType() + " (binarized)",
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        product.setSceneGeoCoding(sourceProduct.getSceneGeoCoding());
        product.setDescription("Product holding RGB Image transformed to binary");

        final PlanarImage planarImage = PlanarImage.wrapRenderedImage(sourceImage);
        RenderedOp bandImage = getBandSourceImage(planarImage, 0);
        Band band = product.addBand(OTSU_BINARY_BAND_NAME, ImageManager.getProductDataType(bandImage.getSampleModel().getDataType()));
        band.setSourceImage(bandImage);
        band.setUnit("dl");
        band.setDescription("RGB Image transformed to binary");
        final Band sourceProductReferenceBand = sourceProduct.getBand(Landsat8Constants.LANDSAT8_RED_BAND_NAME);
        band.setNoDataValue(sourceProductReferenceBand.getNoDataValue());
        band.setNoDataValueUsed(sourceProductReferenceBand.isNoDataValueUsed());
        product.getBand(OTSU_BINARY_BAND_NAME).setValidPixelExpression(sourceProductReferenceBand.getValidPixelExpression());

        return product;
    }

    private Product createGreyProduct(BufferedImage sourceImage) {

        Product product = new Product(sourceProduct.getName() + "_grey",
                sourceProduct.getProductType() + " (greyscaled)",
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        product.setSceneGeoCoding(sourceProduct.getSceneGeoCoding());
        product.setDescription("Product holding RGB Image transformed to greyscale");

        final PlanarImage planarImage = PlanarImage.wrapRenderedImage(sourceImage);
        RenderedOp bandImage = getBandSourceImage(planarImage, 0);
        Band band = product.addBand(OTSU_GREY_BAND_NAME, ImageManager.getProductDataType(bandImage.getSampleModel().getDataType()));
        band.setSourceImage(bandImage);
        band.setUnit("dl");
        band.setDescription("RGB Image transformed to greyscale");
        final Band sourceProductReferenceBand = sourceProduct.getBand(Landsat8Constants.LANDSAT8_RED_BAND_NAME);
        band.setNoDataValue(sourceProductReferenceBand.getNoDataValue());
        band.setNoDataValueUsed(sourceProductReferenceBand.isNoDataValueUsed());
        product.getBand(OTSU_GREY_BAND_NAME).setValidPixelExpression(sourceProductReferenceBand.getValidPixelExpression());

        return product;
    }

    private RenderedOp getBandSourceImage(PlanarImage planarImage, int i) {
        RenderedOp bandImage = BandSelectDescriptor.create(planarImage, new int[]{i}, null);
        int tileWidth = bandImage.getTileWidth();
        int tileHeight = bandImage.getTileHeight();
        ImageLayout imageLayout = new ImageLayout();
        boolean noSourceImageTiling = tileWidth == bandImage.getWidth() && tileHeight == bandImage.getHeight();
        if (noSourceImageTiling) {
            tileWidth = Math.min(bandImage.getWidth(), 512);
            tileHeight = Math.min(bandImage.getHeight(), 512);
            imageLayout.setTileWidth(tileWidth);
            imageLayout.setTileHeight(tileHeight);
        }
        imageLayout.setSampleModel(new SingleBandedSampleModel(bandImage.getSampleModel().getDataType(), tileWidth, tileHeight));
        bandImage = FormatDescriptor.create(bandImage, bandImage.getSampleModel().getDataType(), new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));
        return bandImage;
    }


    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OtsuBinarizeOp.class);
        }
    }

}
