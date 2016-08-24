package org.esa.s3tbx.idepix.operators;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.idepix.algorithms.CloudBuffer;
import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.RectangleExtender;

import java.awt.*;

/**
 * Idepix cloud buffer operator.
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Idepix.Cloudbuffer",
        category = "Optical/Pre-Processing",
        internal = true, // todo: remove when activated
        version = "1.0",
        authors = "Olaf Danne",
        copyright = "(c) 2016 by Brockmann Consult",
        description = "Idepix cloud buffer operator.")
public class CloudBufferOp extends Operator {
    @Parameter(defaultValue = "2", label = "Width of cloud buffer (# of pixels)")
    private int cloudBufferWidth;

    @Parameter(defaultValue = "false", label = " Use the LandCover advanced cloud buffer algorithm")
    private boolean gaLcCloudBuffer;


    @SourceProduct(alias = "classifiedProduct")
    private Product classifiedProduct;

    private Band origClassifFlagBand;

    private RectangleExtender rectCalculator;


    @Override
    public void initialize() throws OperatorException {

        Product cloudBufferProduct = createTargetProduct(classifiedProduct,
                                                         "postProcessedCloudBuffer", "postProcessedCloudBuffer");

        rectCalculator = new RectangleExtender(new Rectangle(classifiedProduct.getSceneRasterWidth(),
                                                             classifiedProduct.getSceneRasterHeight()),
                                               cloudBufferWidth, cloudBufferWidth);

        origClassifFlagBand = classifiedProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
        if (origClassifFlagBand != null) {
            ProductUtils.copyBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS, classifiedProduct, cloudBufferProduct, false);
        } else {
            origClassifFlagBand = classifiedProduct.getBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS);
            ProductUtils.copyBand(IdepixUtils.IDEPIX_CLASSIF_FLAGS, classifiedProduct, cloudBufferProduct, false);
        }
        setTargetProduct(cloudBufferProduct);
    }

    private Product createTargetProduct(Product sourceProduct, String name, String type) {
        final int sceneWidth = sourceProduct.getSceneRasterWidth();
        final int sceneHeight = sourceProduct.getSceneRasterHeight();

        Product targetProduct = new Product(name, type, sceneWidth, sceneHeight);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        return targetProduct;
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        Rectangle targetRectangle = targetTile.getRectangle();
        final Rectangle srcRectangle = rectCalculator.extend(targetRectangle);

        final Tile sourceFlagTile = getSourceTile(origClassifFlagBand, srcRectangle);

//        for (int y = srcRectangle.y; y < srcRectangle.y + srcRectangle.height; y++) {
//            checkForCancellation();
//            for (int x = srcRectangle.x; x < srcRectangle.x + srcRectangle.width; x++) {
//
//                if (targetRectangle.contains(x, y)) {
//                    IdepixUtils.combineFlags(x, y, sourceFlagTile, targetTile);
//                }
//                boolean isCloud = sourceFlagTile.getSampleBit(x, y, IdepixConstants.F_CLOUD);
//                if (isCloud) {
//                    if (gaLcCloudBuffer) {
//                        CloudBuffer.computeCloudBufferLC(targetTile, IdepixConstants.F_CLOUD, IdepixConstants.F_CLOUD_BUFFER);
//                    } else {
//                        CloudBuffer.computeSimpleCloudBuffer(x, y,
//                                                             targetTile,
//                                                             srcRectangle,
//                                                             cloudBufferWidth,
//                                                             IdepixConstants.F_CLOUD_BUFFER);
//                    }
//                }
//            }
//        }

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {

                IdepixUtils.combineFlags(x, y, sourceFlagTile, targetTile);
                boolean isCloud = sourceFlagTile.getSampleBit(x, y, IdepixConstants.F_CLOUD);
                if (isCloud) {
                    if (gaLcCloudBuffer) {
                        CloudBuffer.computeCloudBufferLC(targetTile, IdepixConstants.F_CLOUD, IdepixConstants.F_CLOUD_BUFFER);
                    } else {
                        CloudBuffer.computeSimpleCloudBuffer(x, y,
                                                             targetTile,
                                                             srcRectangle,
                                                             cloudBufferWidth,
                                                             IdepixConstants.F_CLOUD_BUFFER);
                    }
                }
            }
        }


        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
            checkForCancellation();
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                IdepixUtils.consolidateCloudAndBuffer(targetTile, x, y);
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(CloudBufferOp.class);
        }
    }
}
