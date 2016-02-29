package org.esa.s3tbx.meris.brr.operator;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.meris.brr.dpm.*;
import org.esa.s3tbx.meris.l2auxdata.Constants;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.s3tbx.meris.l2auxdata.L2AuxDataProvider;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.envisat.EnvisatConstants;

import java.awt.*;
import java.util.Map;


@OperatorMetadata(alias = "Meris.Brr",
                  version = "2.4",
                  authors = "R. Santer, M. Zuehlke, T. Block, O. Danne",
                  copyright = "(c) European Space Agency",
                  description = "Performs the Rayleigh correction on a MERIS L1b product.",
                  category = "Optical/Pre-Processing")
public class BrrOp extends BrrBasisOp {

    private static final float NODATA_VALUE = -1.0f;

    // source product
    private RasterDataNode[] tpGrids;
    private RasterDataNode[] l1bRadiance;
    private RasterDataNode detectorIndex;
    private RasterDataNode l1bFlags;

    private final ThreadLocal<DpmPixel[]> frame = new ThreadLocal<DpmPixel[]>() {
        @Override
        protected DpmPixel[] initialValue() {
            return new DpmPixel[0];
        }
    };
    private final ThreadLocal<DpmPixel[][]> block = new ThreadLocal<DpmPixel[][]>() {
        @Override
        protected DpmPixel[][] initialValue() {
            return new DpmPixel[0][0];
        }
    };

    // target product
    protected Band[] brrReflecBands = new Band[Constants.L1_BAND_NUM];
    protected Band[] toaReflecBands = new Band[Constants.L1_BAND_NUM];

    @SourceProduct(alias = "merisL1bProduct",
                   label = "MERIS L1b product",
                   description = "The MERIS L1b source product")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "Write L1 flags to the target product.",
               label = "Write L1 flags to the target product.",
               defaultValue = "true")
    public boolean copyL1Flags = true;
    @Parameter(description = "Write TOA reflectances to the target product.",
               label = "Write TOA reflectances to the target product.",
               defaultValue = "false")
    public boolean outputToar = false;

    @Parameter(defaultValue = "ALL_SURFACES",
               valueSet = {"ALL_SURFACES", "LAND", "WATER"},
               label = "Perform Rayleigh correction over",
               description = "Specify the surface where the Rayleigh correction shall be performed")
    private CorrectionSurfaceEnum correctionSurface;

    private L2AuxData auxData;

    @Override
    public void initialize() throws OperatorException {

        checkInputProduct(sourceProduct);
        prepareSourceProducts();

        targetProduct = createCompatibleProduct(sourceProduct, "BRR", "BRR");
        // set tile-size smaller than the one that GPF might associate. We need to allocate A LOT of memory per tile.
        // preferred tile-size must be odd in x-direction to cope with the 4x4 window required by the algo and the odd
        // line length of a meris product. Tiles of width=1 force exceptions. tb 2014-01-24
        targetProduct.setPreferredTileSize(129, 128);
        if (copyL1Flags) {
            ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        }

        createOutputBands(brrReflecBands, "brr");
        if (outputToar) {
            createOutputBands(toaReflecBands, "toar");
        }

        initAlgorithms(sourceProduct);
    }

    private void initAlgorithms(Product inputProduct) throws OperatorException {
        try {
            auxData = L2AuxDataProvider.getInstance().getAuxdata(inputProduct);
        } catch (Exception e) {
            throw new OperatorException("Cannot initialize L2 Auxdata:" + e.getMessage());
        }
    }

    protected void prepareSourceProducts() {
        final int numTPGrids = EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES.length;
        tpGrids = new RasterDataNode[numTPGrids];
        for (int i = 0; i < numTPGrids; i++) {
            tpGrids[i] = sourceProduct.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[i]);
        }

        l1bRadiance = new RasterDataNode[EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES.length];
        for (int i = 0; i < l1bRadiance.length; i++) {
            l1bRadiance[i] = sourceProduct.getBand(EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES[i]);
        }
        detectorIndex = sourceProduct.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME);
        l1bFlags = sourceProduct.getBand(EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME);
    }

    @SuppressWarnings("MismatchedReadAndWriteOfArray")
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {

        L1bDataExtraction extdatl1 = new L1bDataExtraction(auxData);
        GaseousAbsorptionCorrection gaz_cor = new GaseousAbsorptionCorrection(auxData);
        PixelIdentification pixelid = new PixelIdentification(auxData, gaz_cor);
        RayleighCorrection ray_cor = new RayleighCorrection(auxData);
        CloudClassification classcloud = new CloudClassification(auxData, ray_cor);
        AtmosphericCorrectionLand landac = new AtmosphericCorrectionLand(ray_cor);

        pixelid.setCorrectionSurface(correctionSurface);
        landac.setCorrectionSurface(correctionSurface);

        final FrameAndBlock frameAndBlock = getFrameAndBlock(rectangle);
        final DpmPixel[] frameLocal = frameAndBlock.frame;
        final DpmPixel[][] blockLocal = frameAndBlock.block;

        final int frameSize = rectangle.height * rectangle.width;

        Tile[] l1bTiePoints = new Tile[tpGrids.length];
        for (int i = 0; i < tpGrids.length; i++) {
            l1bTiePoints[i] = getSourceTile(tpGrids[i], rectangle);
        }
        Tile[] l1bRadiances = new Tile[l1bRadiance.length];
        for (int i = 0; i < l1bRadiance.length; i++) {
            l1bRadiances[i] = getSourceTile(l1bRadiance[i], rectangle);
        }
        Tile l1bDetectorIndex = getSourceTile(detectorIndex, rectangle);
        Tile l1bFlagRaster = getSourceTile(l1bFlags, rectangle);

        for (int pixelIndex = 0; pixelIndex < frameSize; pixelIndex++) {
            DpmPixel pixel = frameLocal[pixelIndex];
            extdatl1.l1_extract_pixbloc(pixel,
                                        rectangle.x + pixel.i,
                                        rectangle.y + pixel.j,
                                        l1bTiePoints,
                                        l1bRadiances,
                                        l1bDetectorIndex,
                                        l1bFlagRaster);

            if (!BitSetter.isFlagSet(pixel.l2flags, Constants.F_INVALID)) {
                pixelid.rad2reflect(pixel);
                classcloud.classify_cloud(pixel);
            }
        }

        for (int iPL1 = 0; iPL1 < rectangle.height; iPL1 += Constants.SUBWIN_HEIGHT) {
            for (int iPC1 = 0; iPC1 < rectangle.width; iPC1 += Constants.SUBWIN_WIDTH) {
                final int iPC2 = Math.min(rectangle.width, iPC1 + Constants.SUBWIN_WIDTH) - 1;
                final int iPL2 = Math.min(rectangle.height, iPL1 + Constants.SUBWIN_HEIGHT) - 1;
                pixelid.pixel_classification(blockLocal, iPC1, iPC2, iPL1, iPL2);
                landac.landAtmCor(blockLocal, iPC1, iPC2, iPL1, iPL2);
            }
        }

        for (int bandIndex = 0; bandIndex < brrReflecBands.length; bandIndex++) {
            if (isValidRhoSpectralIndex(bandIndex)) {
                ProductData data = targetTiles.get(brrReflecBands[bandIndex]).getRawSamples();
                float[] dData = (float[]) data.getElems();
                for (int iP = 0; iP < rectangle.width * rectangle.height; iP++) {
                    dData[iP] = (float) frameLocal[iP].rho_top[bandIndex];
                    if (BitSetter.isFlagSet((int) frameLocal[iP].l2flags, Constants.F_INVALID)) {
                        dData[iP] = NODATA_VALUE;
                    }
                }
                targetTiles.get(brrReflecBands[bandIndex]).setSamples(dData);
            }
        }
        if (outputToar) {
            for (int bandIndex = 0; bandIndex < toaReflecBands.length; bandIndex++) {
                ProductData data = targetTiles.get(toaReflecBands[bandIndex]).getRawSamples();
                float[] ddata = (float[]) data.getElems();
                for (int iP = 0; iP < rectangle.width * rectangle.height; iP++) {
                    ddata[iP] = (float) frameLocal[iP].rho_toa[bandIndex];
                }
                targetTiles.get(toaReflecBands[bandIndex]).setRawSamples(data);
            }
        }
    }

    protected void createOutputBands(Band[] bands, final String name) {
        final int sceneWidth = targetProduct.getSceneRasterWidth();
        final int sceneHeight = targetProduct.getSceneRasterHeight();

        for (int bandId = 0; bandId < bands.length; bandId++) {
            if (isValidRhoSpectralIndex(bandId) || name.equals("toar")) {
                Band aNewBand = new Band(name + "_" + (bandId + 1), ProductData.TYPE_FLOAT32, sceneWidth,
                                         sceneHeight);
                aNewBand.setNoDataValueUsed(true);
                aNewBand.setNoDataValue(NODATA_VALUE);
                aNewBand.setSpectralBandIndex(sourceProduct.getBandAt(bandId).getSpectralBandIndex());
                aNewBand.setSpectralWavelength(sourceProduct.getBandAt(bandId).getSpectralWavelength());
                aNewBand.setSpectralBandwidth(sourceProduct.getBandAt(bandId).getSpectralBandwidth());
                targetProduct.addBand(aNewBand);
                bands[bandId] = aNewBand;
            }
        }
    }

    protected void checkInputProduct(Product inputProduct) throws IllegalArgumentException {
        String name;

        for (int i = 0; i < EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES.length; i++) {
            name = EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[i];
            if (inputProduct.getTiePointGrid(name) == null) {
                throw new IllegalArgumentException("Invalid input product. Missing tie point grid '" + name + "'.");
            }
        }

        for (int i = 0; i < EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES.length; i++) {
            name = EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES[i];
            if (inputProduct.getBand(name) == null) {
                throw new IllegalArgumentException("Invalid input product. Missing band '" + name + "'.");
            }
        }

        name = EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME;
        if (inputProduct.getBand(name) == null) {
            throw new IllegalArgumentException("Invalid input product. Missing dataset '" + name + "'.");
        }

        name = EnvisatConstants.MERIS_L1B_FLAGS_DS_NAME;
        if (inputProduct.getBand(name) == null) {
            throw new IllegalArgumentException("Invalid input product. Missing dataset '" + name + "'.");
        }
    }

    static boolean isValidRhoSpectralIndex(int i) {
        return i >= Constants.bb1 && i < Constants.bb15 && i != Constants.bb11;
    }

    private FrameAndBlock getFrameAndBlock(Rectangle rectangle) {
        final FrameAndBlock frameAndBlock = new FrameAndBlock();
        final int frameSize = rectangle.width * rectangle.height;

        DpmPixel[] frameLocal = frame.get();
        DpmPixel[][] blockLocal = block.get();
        if (frameLocal.length != frameSize) {
            // reallocate
            frameLocal = new DpmPixel[frameSize];
            blockLocal = new DpmPixel[rectangle.height][rectangle.width];
            for (int pixelIndex = 0; pixelIndex < frameSize; pixelIndex++) {
                final DpmPixel pixel = new DpmPixel(pixelIndex % rectangle.width, pixelIndex / rectangle.width);
                frameLocal[pixelIndex] = blockLocal[pixel.j][pixel.i] = pixel;
                frame.set(frameLocal);
                block.set(blockLocal);
            }
        } else {
            for (int pixelIndex = 0; pixelIndex < frameSize; pixelIndex++) {
                frameLocal[pixelIndex].reset(pixelIndex % rectangle.width, pixelIndex / rectangle.width);
            }
        }

        frameAndBlock.frame = frameLocal;
        frameAndBlock.block = blockLocal;

        return frameAndBlock;
    }

    private class FrameAndBlock {
        DpmPixel[] frame;
        DpmPixel[][] block;

    }

    public static class Spi extends OperatorSpi
    {
        public Spi() {
            super(BrrOp.class);
        }
    }
}
