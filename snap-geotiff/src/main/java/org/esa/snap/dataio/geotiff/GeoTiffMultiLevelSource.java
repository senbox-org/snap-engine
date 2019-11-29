package org.esa.snap.dataio.geotiff;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.image.TileImageDisposer;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by jcoravu on 22/11/2019.
 */
public class GeoTiffMultiLevelSource extends AbstractMultiLevelSource {

    private static final Logger logger = Logger.getLogger(GeoTiffMultiLevelSource.class.getName());

    private final GeoTiffImageReader geoTiffImageReader;
    private final int dataBufferType;
    private Dimension tileSize;
    private Rectangle imageBounds;
    private final int bandIndex;
    private final TileImageDisposer tileImageDisposer;
    private final boolean isGlobalShifted180;

    public GeoTiffMultiLevelSource(GeoTiffImageReader geoTiffImageReader, int dataBufferType, Rectangle imageBounds, Dimension tileSize,
                                   int bandIndex, GeoCoding geoCoding, boolean isGlobalShifted180) {

        super(new DefaultMultiLevelModel(Product.findImageToModelTransform(geoCoding), imageBounds.width, imageBounds.height));

        if (imageBounds.width < tileSize.width) {
            throw new IllegalArgumentException("The visible region width " + imageBounds.width + " cannot be smaller than the tile width " + tileSize.width + ".");
        }
        if (imageBounds.height < tileSize.height) {
            throw new IllegalArgumentException("The visible region height " + imageBounds.height + " cannot be smaller than the tile height " + tileSize.height + ".");
        }

        this.geoTiffImageReader = geoTiffImageReader;
        this.dataBufferType = dataBufferType;
        this.imageBounds = imageBounds;
        this.tileSize = tileSize;
        this.bandIndex = bandIndex;
        this.isGlobalShifted180 = isGlobalShifted180;

        this.tileImageDisposer = new TileImageDisposer();
    }

    @Override
    public synchronized void reset() {
        super.reset();

        this.tileImageDisposer.disposeAll();
        System.gc();
    }

    @Override
    protected RenderedImage createImage(int level) {
        int numXTiles = ImageUtils.computeTileCount(this.imageBounds.width, this.tileSize.width);
        int numYTiles = ImageUtils.computeTileCount(this.imageBounds.height, this.tileSize.height);

        int tileCount = numXTiles * numYTiles;
        List<RenderedImage> tileImages = Collections.synchronizedList(new ArrayList<>(tileCount));
        double factor = 1.0 / Math.pow(2, level);
        for (int tileRowIndex = 0; tileRowIndex < numYTiles; tileRowIndex++) {
            for (int tileColumnIndex = 0; tileColumnIndex < numXTiles; tileColumnIndex++) {
                int tileOffsetX = tileColumnIndex * this.tileSize.width;
                int tileOffsetY = tileRowIndex * this.tileSize.height;
                int tileWidth = this.tileSize.width;
                if (tileColumnIndex == numXTiles-1) {
                    tileWidth = this.imageBounds.width - tileOffsetX;
                }
                int tileHeight = this.tileSize.height;
                if (tileRowIndex == numYTiles - 1) {
                    tileHeight = this.imageBounds.height - tileOffsetY;
                }
                Dimension currentTileSize = new Dimension(tileWidth, tileHeight);

                Point tileOffset = new Point(tileOffsetX, tileOffsetY);
                GeoTiffTileOpImage geoTiffTileOpImage = new GeoTiffTileOpImage(this.geoTiffImageReader, getModel(), this.dataBufferType, this.bandIndex,
                                                                                this.imageBounds, currentTileSize, tileOffset, level, this.isGlobalShifted180);
                this.tileImageDisposer.registerForDisposal(geoTiffTileOpImage);

                float xTrans = (float) (tileOffsetX * factor);
                float yTrans = (float) (tileOffsetY * factor);
                RenderedOp opImage = TranslateDescriptor.create(geoTiffTileOpImage, xTrans, yTrans, Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
                tileImages.add(opImage);
            }
        }
        if (tileImages.isEmpty()) {
            logger.warning("No tile images for mosaic.");
            return null;
        }

        Dimension defaultTileSize = JAI.getDefaultTileSize();

        ImageLayout imageLayout = new ImageLayout();
        imageLayout.setMinX(0);
        imageLayout.setMinY(0);
        imageLayout.setTileWidth(defaultTileSize.width);
        imageLayout.setTileHeight(defaultTileSize.height);
        imageLayout.setTileGridXOffset(0);
        imageLayout.setTileGridYOffset(0);

        RenderedImage[] sources = tileImages.toArray(new RenderedImage[tileImages.size()]);
        RenderedOp mosaicOp = MosaicDescriptor.create(sources, MosaicDescriptor.MOSAIC_TYPE_OVERLAY, null, null, null, null, new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));

        int fittingRectWidth = ImageUtils.scaleValue(this.imageBounds.width, level);
        int fittingRectHeight = ImageUtils.scaleValue(this.imageBounds.height, level);

        Rectangle fitRect = new Rectangle(0, 0, fittingRectWidth, fittingRectHeight);
        Rectangle destBounds = DefaultMultiLevelSource.getLevelImageBounds(fitRect, Math.pow(2.0, level));

        BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);

        if (mosaicOp.getWidth() < destBounds.width || mosaicOp.getHeight() < destBounds.height) {
            int rightPad = destBounds.width - mosaicOp.getWidth();
            int bottomPad = destBounds.height - mosaicOp.getHeight();
            mosaicOp = BorderDescriptor.create(mosaicOp, 0, rightPad, 0, bottomPad, borderExtender, null);
        }

        return mosaicOp;
    }
}
