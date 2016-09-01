package org.esa.s3tbx.idepix.algorithms.olci;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.algorithms.CloudBuffer;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.RectangleExtender;

import java.awt.*;

/**
 * Idepix water/land merge operator for OLCI.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Olci.Merge.Landwater",
        version = "1.0",
        internal = true,
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Idepix water/land merge operator for OLCI.")
public class OlciMergeLandWaterOp extends Operator {

    @Parameter(defaultValue = "true", label = " Compute a cloud buffer")
    private boolean computeCloudBuffer;

    @Parameter(defaultValue = "2", interval = "[0,100]",
            description = "The width of a cloud 'safety buffer' around a pixel which was classified as cloudy.",
            label = "Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @SourceProduct(alias = "landClassif")
    private Product landClassifProduct;

    @SourceProduct(alias = "waterClassif")
    private Product waterClassifProduct;

    private Band waterClassifBand;
    private Band landClassifBand;
    private Band landNNBand;
    private Band waterNNBand;

    private Band mergedClassifBand;
    private Band mergedNNBand;

    private RectangleExtender rectCalculator;

    private boolean hasNNOutput;

    @Override
    public void initialize() throws OperatorException {
        Product mergedClassifProduct = IdepixUtils.createCompatibleTargetProduct(landClassifProduct,
                                                                                 "mergedClassif", "mergedClassif", true);

        landClassifBand = landClassifProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
        waterClassifBand = waterClassifProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);

        mergedClassifBand = mergedClassifProduct.addBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS, ProductData.TYPE_INT16);
        FlagCoding flagCoding = OlciUtils.createOlciFlagCoding(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
        mergedClassifBand.setSampleCoding(flagCoding);
        mergedClassifProduct.getFlagCodingGroup().add(flagCoding);

        hasNNOutput = landClassifProduct.containsBand(OlciConstants.SCHILLER_NN_OUTPUT_BAND_NAME) &&
                waterClassifProduct.containsBand(OlciConstants.SCHILLER_NN_OUTPUT_BAND_NAME);
        if (hasNNOutput) {
            landNNBand = landClassifProduct.getBand(OlciConstants.SCHILLER_NN_OUTPUT_BAND_NAME);
            waterNNBand = waterClassifProduct.getBand(OlciConstants.SCHILLER_NN_OUTPUT_BAND_NAME);
            mergedNNBand = mergedClassifProduct.addBand(OlciConstants.SCHILLER_NN_OUTPUT_BAND_NAME, ProductData.TYPE_FLOAT32);
        }

        setTargetProduct(mergedClassifProduct);

        if (computeCloudBuffer) {
            rectCalculator = new RectangleExtender(new Rectangle(mergedClassifProduct.getSceneRasterWidth(),
                                                                 mergedClassifProduct.getSceneRasterHeight()),
                                                   cloudBufferWidth, cloudBufferWidth);
        }
    }

    @Override
    public void computeTile(Band targetBand, final Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final Rectangle rectangle = targetTile.getRectangle();

        final Tile waterClassifTile = getSourceTile(waterClassifBand, rectangle);
        final Tile landClassifTile = getSourceTile(landClassifBand, rectangle);

        Tile waterNNTile = null;
        Tile landNNTile = null;
        if (hasNNOutput) {
            waterNNTile = getSourceTile(waterNNBand, rectangle);
            landNNTile = getSourceTile(landNNBand, rectangle);
        }

        Rectangle extendedRectangle = null;
        if (computeCloudBuffer) {
            extendedRectangle = rectCalculator.extend(rectangle);
        }

        if (targetBand == mergedClassifBand) {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    final boolean isLand = landClassifTile.getSampleBit(x, y, OlciConstants.F_LAND);
                    final Tile classifTile = isLand ? landClassifTile : waterClassifTile;
                    final int sample = classifTile.getSampleInt(x, y);
                    targetTile.setSample(x, y, sample);
                }
            }

            // potential post processing after merge, e.g. cloud buffer:
            if (computeCloudBuffer) {
                for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                    checkForCancellation();
                    for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {

                        final boolean isCloud = targetTile.getSampleBit(x, y, OlciConstants.F_CLOUD);
                        if (isCloud) {
                            CloudBuffer.computeSimpleCloudBuffer(x, y,
                                                                 targetTile,
                                                                 extendedRectangle,
                                                                 cloudBufferWidth,
                                                                 IdepixConstants.F_CLOUD_BUFFER);
                        }
                    }
                }

                for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                    checkForCancellation();
                    for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                        IdepixUtils.consolidateCloudAndBuffer(targetTile, x, y);
                    }
                }
            }
        } else if (hasNNOutput && targetBand == mergedNNBand) {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    boolean isLand = landClassifTile.getSampleBit(x, y, OlciConstants.F_LAND);
                    final float sample = isLand ? landNNTile.getSampleFloat(x, y) : waterNNTile.getSampleFloat(x, y);
                    targetTile.setSample(x, y, sample);
                }
            }
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(OlciMergeLandWaterOp.class);
        }
    }

}
