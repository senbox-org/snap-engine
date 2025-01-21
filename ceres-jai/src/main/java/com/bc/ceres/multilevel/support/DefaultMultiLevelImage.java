package com.bc.ceres.multilevel.support;

import com.bc.ceres.multilevel.MultiLevelImage;
import com.bc.ceres.multilevel.MultiLevelModel;
import com.bc.ceres.multilevel.MultiLevelSource;

import javax.media.jai.ImageLayout;
import java.awt.Shape;
import java.awt.image.RenderedImage;

/**
 * Adapts a JAI {@link javax.media.jai.PlanarImage PlanarImage} to the {@link MultiLevelSource}
 * interface. The image data provided by this {@code PlanarImage} corresponds to the level zero image of the given
 * {@code MultiLevelSource}.
 *
 * @author Norman Fomferra
 */
public class DefaultMultiLevelImage extends MultiLevelImage {

    private final MultiLevelSource source;

    /**
     * Constructs a new multi-level image from the given source.
     *
     * @param source The multi-level image source.
     */
    public DefaultMultiLevelImage(MultiLevelSource source) {
        this(source, new ImageLayout(source.getImage(0)));
    }

    /**
     * Constructs a new multi-level image from the given source and the image layout.
     *
     * @param source The multi-level image source.
     * @param layout The image layout.
     */
    public DefaultMultiLevelImage(MultiLevelSource source, ImageLayout layout) {
        super(layout, source, null);
        this.source = source;
    }

    /**
     * @return The multi-level image source.
     */
    public final MultiLevelSource getSource() {
        return source;
    }

    /////////////////////////////////////////////////////////////////////////
    // MultiLevelImage interface

    @Override
    public final MultiLevelModel getModel() {
        return source.getModel();
    }

    @Override
    public final RenderedImage getImage(int level) {
        return source.getImage(level);
    }

    @Override
    public Shape getImageShape(int level) {
        return source.getImageShape(level);
    }

    @Override
    public void reset() {
        source.reset();
    }

    @Override
    public void dispose() {
        source.reset();
        super.dispose();
    }
}
