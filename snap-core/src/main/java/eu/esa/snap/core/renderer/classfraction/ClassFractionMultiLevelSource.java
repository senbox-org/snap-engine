package eu.esa.snap.core.renderer.classfraction;

import eu.esa.snap.core.renderer.TileRenderer;

import com.bc.ceres.multilevel.MultiLevelModel;
import com.bc.ceres.multilevel.support.AbstractMultiLevelSource;
import org.esa.snap.core.datamodel.RasterDataNode;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


/**
 * Internal multi-level source for the class renderer. AbstractMultiLevelSource owns level caching/reset lifecycle.
 */
public final class ClassFractionMultiLevelSource extends AbstractMultiLevelSource {


    private final List<RasterDataNode> sources;
    private final ClassFractionTileRenderer tileRenderer;
    private final double[] noDataValues;
    private final boolean[] hasValidMask;


    ClassFractionMultiLevelSource(ClassFractionRendererConfig config, List<RasterDataNode> sources) {
        super(model(config, sources));
        this.sources = new ArrayList<>(sources);
        this.tileRenderer = new ClassFractionTileRenderer(config);
        this.noDataValues = new double[sources.size()];
        this.hasValidMask = new boolean[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            noDataValues[i] = sources.get(i).isNoDataValueUsed() ? sources.get(i).getGeophysicalNoDataValue() : Double.NaN;
            hasValidMask[i] = sources.get(i).getValidMaskImage() != null;
        }
    }


    private static MultiLevelModel model(ClassFractionRendererConfig config, List<RasterDataNode> sources) {
        ClassFractionRendererValidator.validate(config, sources);
        return sources.get(0).getMultiLevelModel();
    }

    @Override
    public RenderedImage createImage(int level) {
        Vector<RenderedImage> images = new Vector<>();
        for (RasterDataNode source : sources) {
            images.add(source.getGeophysicalImage().getImage(level));
        }

        int[] maskIndexes = new int[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            maskIndexes[i] = -1;
            if (hasValidMask[i]) {
                maskIndexes[i] = images.size();
                images.add(sources.get(i).getValidMaskImage().getImage(level));
            }
        }
        return new ClassFractionOpImage(images, sources.size(), maskIndexes, tileRenderer, noDataValues);
    }

    private static final class ClassFractionOpImage extends PointOpImage {

        private final int sourceCount;
        private final int[] maskIndexes;
        private final TileRenderer renderer;
        private final double[] noDataValues;

        ClassFractionOpImage(Vector<RenderedImage> images, int sourceCount, int[] maskIndexes, TileRenderer renderer, double[] noDataValues) {
            super(images, layout(images.get(0)), null, true);
            this.sourceCount = sourceCount;
            this.maskIndexes = maskIndexes.clone();
            this.renderer = renderer;
            this.noDataValues = noDataValues.clone();
        }

        private static ImageLayout layout(RenderedImage source) {
            DirectColorModel colorModel = new DirectColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB), 32, 0x00FF0000, 0x0000FF00,
                    0x000000FF, 0xFF000000, false, DataBuffer.TYPE_INT
            );
            SinglePixelPackedSampleModel sampleModel = new SinglePixelPackedSampleModel(
                    DataBuffer.TYPE_INT, source.getTileWidth(), source.getTileHeight(),
                    new int[]{0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000}
            );

            ImageLayout layout = new ImageLayout(source);
            layout.setColorModel(colorModel);
            layout.setSampleModel(sampleModel);

            return layout;
        }

        @Override
        protected void computeRect(Raster[] input, WritableRaster destination, Rectangle rectangle) {
            double[] values = new double[sourceCount];
            boolean[] valid = new boolean[sourceCount];

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    for (int i = 0; i < sourceCount; i++) {
                        values[i] = input[i].getSampleDouble(x, y, 0);
                        valid[i] = maskIndexes[i] < 0 || input[maskIndexes[i]].getSample(x, y, 0) != 0;
                    }
                    destination.setDataElements(x, y, new int[]{renderer.compose(values, valid, true, noDataValues)});
                }
            }
        }
    }
}
