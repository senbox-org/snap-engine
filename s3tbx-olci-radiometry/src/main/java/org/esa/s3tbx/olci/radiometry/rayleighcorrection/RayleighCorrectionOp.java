package org.esa.s3tbx.olci.radiometry.rayleighcorrection;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Rectangle;

/**
 * @author muhammad.bc.
 */
public class RayleighCorrectionOp extends Operator {
    @SourceProduct
    Product sourceProduct;
    private Product targetProduct;
    private RayleighCorrAlgorithm algorithm;


    @Override
    public void initialize() throws OperatorException {
        algorithm = new RayleighCorrAlgorithm();
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        for (final Band band : sourceProduct.getBands()) {
            Band targetBand = targetProduct.addBand(band.getName(), band.getDataType());
            ProductUtils.copyRasterDataNodeProperties(band,targetBand);
        }
        ProductUtils.copyFlagBands(sourceProduct, targetProduct,true);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        final String targetBandName = targetBand.getName();
        final Band sourceBand = sourceProduct.getBand(targetBandName);
        float spectralWavelength = sourceBand.getSpectralWavelength();

        Rectangle rectangle = targetTile.getRectangle();


        double[] sampleTilesDouble = getSourceTile(sourceProduct.getBand(targetBandName), rectangle).getSamplesDouble();
        double[] szas = getSourceTile(sourceProduct.getTiePointGrid("SZA"), rectangle).getSamplesDouble();
        double[] ozas = getSourceTile(sourceProduct.getTiePointGrid("OZA"), rectangle).getSamplesDouble();
    }

}
