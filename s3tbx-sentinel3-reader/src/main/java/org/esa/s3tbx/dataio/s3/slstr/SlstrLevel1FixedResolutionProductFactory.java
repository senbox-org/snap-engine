package org.esa.s3tbx.dataio.s3.slstr;

import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.ProductUtils;

import java.awt.image.RenderedImage;

/**
 * @author Marco Peters
 */
public abstract class SlstrLevel1FixedResolutionProductFactory extends SlstrLevel1ProductFactory {

    public SlstrLevel1FixedResolutionProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected RasterDataNode addSpecialNode(Product masterProduct, Band sourceBand, Product targetProduct) {
        final String sourceBandName = sourceBand.getName();
        final int sourceBandNameLength = sourceBandName.length();
        String gridIndex = sourceBandName;
        if(sourceBandNameLength > 1) {
            gridIndex = sourceBandName.substring(sourceBandNameLength - 2);
        }
        final Double sourceStartOffset = getStartOffset(gridIndex);
        final Double sourceTrackOffset = getTrackOffset(gridIndex);
        if (sourceStartOffset != null && sourceTrackOffset != null) {
            final short[] sourceResolutions = getResolutions(gridIndex);
            if (gridIndex.startsWith("t")) {
                return copyTiePointGrid(sourceBand, targetProduct, sourceStartOffset, sourceTrackOffset, sourceResolutions);
            } else {
                final Band targetBand = new Band(sourceBandName, sourceBand.getDataType(),
                                                 targetProduct.getSceneRasterWidth(), targetProduct.getSceneRasterHeight());
                targetProduct.addBand(targetBand);
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                final float[] offsets = getOffsets(sourceStartOffset, sourceTrackOffset, sourceResolutions);
                final RenderedImage sourceImage = createSourceImage(masterProduct, sourceBand, offsets, targetBand,
                                                                    sourceResolutions);
                targetBand.setSourceImage(sourceImage);
                return targetBand;
            }
        }
        return sourceBand;
    }

    @Override
    protected void setSceneTransforms(Product product) {
    }

    @Override
    protected void setBandGeoCodings(Product product) {
    }
}
