package org.esa.s3tbx.idepix.algorithms.olci;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.algorithms.olci.OlciConstants;
import org.esa.s3tbx.idepix.algorithms.olci.OlciUtils;
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
import org.esa.snap.core.gpf.annotations.SourceProduct;

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

        if (targetBand == mergedClassifBand) {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                checkForCancellation();
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    boolean isLand = landClassifTile.getSampleBit(x, y, OlciConstants.F_LAND);
                    final int sample = isLand ? landClassifTile.getSampleInt(x, y) : waterClassifTile.getSampleInt(x, y);
                    targetTile.setSample(x, y, sample);
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
