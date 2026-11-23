package eu.esa.snap.core.renderer.classfraction;

import com.bc.ceres.multilevel.MultiLevelModel;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Internal validation before a source is constructed.
 */
public final class ClassFractionRendererValidator {


    private ClassFractionRendererValidator() {
    }


    /**
     * Validates resolved renderer inputs without attempting to align, resample, or reproject them.
     *
     * <p>Equal raster dimensions alone do not establish that identical pixel coordinates describe
     * the same location.  This renderer therefore also requires the same image-to-model transform
     * and pyramid-scale layout.</p>
     */
    public static void validate(ClassFractionRendererConfig config, List<RasterDataNode> sources) {
        if (sources == null || sources.size() != config.getClassBandGroup().getBands().size()) {
            throw new IllegalArgumentException("Configured class sources and resolved rasters must have the same size");
        }

        RasterDataNode anchor = sources.get(0);
        if (anchor == null || anchor.getProduct() == null) {
            throw new IllegalArgumentException("A product-owned anchor raster is required");
        }

        int width = anchor.getRasterWidth();
        int height = anchor.getRasterHeight();
        MultiLevelModel anchorModel = anchor.getMultiLevelModel();
        int levelCount = anchorModel.getLevelCount();
        Set<RasterDataNode> seen = new HashSet<>();

        for (int index = 0; index < sources.size(); index++) {
            RasterDataNode source = sources.get(index);
            if (source == null) {
                throw new IllegalArgumentException("Resolved raster must not be null");
            }
            if (!seen.add(source)) {
                throw new IllegalArgumentException("Duplicate resolved class source: " + source.getName());
            }
            if (source.getProduct() != anchor.getProduct()) {
                throw new IllegalArgumentException("All class sources must belong to one product");
            }

            ClassBandConfig configuredBand = config.getClassBandGroup().getBands().get(index);
            if (!configuredBand.getProductName().equals(source.getProduct().getName()) ||
                    !configuredBand.getBandName().equals(source.getName())) {
                throw new IllegalArgumentException("Configured class source does not match resolved raster: " + source.getName());
            }
            if (width != source.getRasterWidth() || height != source.getRasterHeight()) {
                throw new IllegalArgumentException("Class sources must have equal raster bounds");
            }
            if (levelCount != source.getMultiLevelModel().getLevelCount()) {
                throw new IllegalArgumentException("Class sources must have compatible multi-level models");
            }
            if (!anchor.getImageToModelTransform().equals(source.getImageToModelTransform())) {
                throw new IllegalArgumentException("Class sources must share the same raster grid transform");
            }

            for (int level = 0; level < levelCount; level++) {
                if (Double.compare(anchorModel.getScale(level), source.getMultiLevelModel().getScale(level)) != 0) {
                    throw new IllegalArgumentException("Class sources must have compatible multi-level models");
                }
                if (!anchorModel.getImageToModelTransform(level)
                        .equals(source.getMultiLevelModel().getImageToModelTransform(level))) {
                    throw new IllegalArgumentException("Class sources must share the same raster grid transform");
                }
            }
        }
    }
}
