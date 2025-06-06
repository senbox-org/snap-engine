/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.multilevel.support;

import com.bc.ceres.multilevel.MultiLevelModel;
import com.bc.ceres.multilevel.MultiLevelSource;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.image.RenderedImage;

/**
 * An abstract base class for {@link MultiLevelSource} implementations.
 * Level images are cached unless {@link #reset()} is called.
 * Subclasses are asked to implement {@link #createImage(int)}.
 */
public abstract class AbstractMultiLevelSource implements MultiLevelSource {

    private final MultiLevelModel multiLevelModel;
    private final RenderedImage[] levelImages;

    protected AbstractMultiLevelSource(MultiLevelModel multiLevelModel) {
        this.multiLevelModel = multiLevelModel;
        this.levelImages = new RenderedImage[multiLevelModel.getLevelCount()];
    }

    @Override
    public MultiLevelModel getModel() {
        return multiLevelModel;
    }

    /**
     * Gets the {@code RenderedImage} at the given resolution level. Unless {@link #reset()} is called,
     * the method will always return the same image instance at the same resolution level.
     * If a level image is requested for the first time, the method calls
     * {@link #createImage(int)} in order to retrieve the actual image instance.
     *
     * @param level The resolution level.
     * @return The {@code RenderedImage} at the given resolution level.
     */
    @Override
    public synchronized RenderedImage getImage(int level) {
        checkLevel(level);
        RenderedImage levelImage = levelImages[level];
        if (levelImage == null) {
            levelImage = createImage(level);
            levelImages[level] = levelImage;
        }
        return levelImage;
    }

    @Override
    public Shape getImageShape(int level) {
        return null;
    }

    /**
     * Called by {@link #getImage(int)} if a level image is requested for the first time.
     * Note that images created via this method will be {@link PlanarImage#dispose disposed}
     * when {@link #reset} is called on this multi-level image source. See {@link #getImage(int)}.
     * <p>
     * The dimension of the level image created must be the same as that obtained from
     * {DefaultMultiLevelSource.getLevelImageBounds(Rectangle sourceBounds, double scale)} for the scale associated with the
     * given resolution level.
     *
     * @param level The resolution level.
     * @return An instance of a {@code RenderedImage} for the given resolution level.
     */
    protected abstract RenderedImage createImage(int level);


    /**
     * Removes all cached level images and also disposes
     * any {@link PlanarImage PlanarImage}s among them.
     * <p>Overrides should always call {@code super.reset()}.<p>
     */
    @Override
    public synchronized void reset() {
        for (int level = 0; level < levelImages.length; level++) {
            RenderedImage levelImage = levelImages[level];
            if (levelImage instanceof PlanarImage) {
                PlanarImage planarImage = (PlanarImage) levelImage;
                planarImage.dispose();
            }
            levelImages[level] = null;
        }
    }

    /**
     * Utility method which checks if a given level is valid.
     *
     * @param level The resolution level.
     * @throws IllegalArgumentException if {@code level &lt; 0 || level &gt;= getModel().getLevelCount()}
     */
    protected synchronized void checkLevel(int level) {
        if (level < 0 || level >= getModel().getLevelCount()) {
            throw new IllegalArgumentException("level=" + level + " < " + getModel().getLevelCount());
        }
    }
}
