package org.esa.s3tbx.aatsr.regrid;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.Rectangle;

/**
 * @author Alasdhair Beaton (Telespazio VEGA)
 * @author Philip Beavis (Telespazio VEGA)
 */
@OperatorMetadata(description = "Ungrids ATSR L1B products and extracts geolocation and pixel field of view data.",
        alias = "AATSR.Ungrid.2", authors = "Alasdhair Beaton, Philip Beavis",
        category = "Raster", label = "AATSR Ungridding"
)
public class AatsrUngriddingOp2 extends Operator {

    @SourceProduct(description = "The source product")
    Product sourceProduct;

    @TargetProduct
    Product targetProduct;

    @Parameter
    boolean trimProductEndWhereNoADS;

    @Parameter(description = "Values will be divided by this", defaultValue = "2")
    int quotient;

    @Override
    public void initialize() throws OperatorException {
        if (!sourceProduct.getProductType().equals("ATS_TOA_1P")) {
            throw new OperatorException("Product does not have correct type");
        }
        targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        for (Band band : sourceProduct.getBands()) {
            ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
        }
        targetProduct.setAutoGrouping("nadir:fward");
        targetProduct.addBand("nadir view latitude", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("nadir view longitude", ProductData.TYPE_FLOAT32);
        //check source product type
        //read and prepare metadata
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        String sourceBandName = "";
        if (targetBand.getName().equals("nadir view latitude")) {
            sourceBandName = "btemp_nadir_1200";
        } else if (targetBand.getName().equals("nadir view longitude")) {
            sourceBandName = "btemp_nadir_1100";
        }
        if (sourceProduct.containsBand(sourceBandName)) {
            final Rectangle targetRectangle = targetTile.getRectangle();
            final Tile sourceTile = getSourceTile(sourceProduct.getBand(sourceBandName), targetRectangle);
            for (int y = targetRectangle.y; y < targetTile.getHeight(); y++) {
                for (int x = targetRectangle.x; x < targetTile.getWidth(); x++) {
                    targetTile.setSample(x, y, sourceTile.getSampleDouble(x, y) / quotient);
                }
            }
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(AatsrUngriddingOp2.class);
        }
    }
}
