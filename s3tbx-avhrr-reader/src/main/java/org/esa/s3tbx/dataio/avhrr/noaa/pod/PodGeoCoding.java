/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.s3tbx.dataio.avhrr.noaa.pod;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.GeoApproximation;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.PixelPosEstimator;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.Scene;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;

/**
 * This geo-coding improves the inverse approximations used in the {@code TiePointGeoCoding} in order
 * to facilitate accurate re-projections and graticule drawing.
 * <p/>
 * Limitation: this geo-coding is not transferred when making subsets and is not saved when a product
 * is written to disk.
 *
 * @author Ralf Quast
 */
final class PodGeoCoding extends TiePointGeoCoding {

    private transient PixelPosEstimator pixelPosEstimator;
    private transient PodPixelFinder pixelFinder;
    private transient GeoApproximation[] approximations;
    private transient PlanarImage latImage;

    PodGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid) {
        this(latGrid, lonGrid, createApproximations(lonGrid.getGeophysicalImage(), latGrid.getGeophysicalImage()));
    }

    private PodGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid, GeoApproximation[] approximations) {
        super(latGrid, lonGrid);
        this.approximations = approximations;

        final PlanarImage lonImage = lonGrid.getGeophysicalImage();
        latImage = latGrid.getGeophysicalImage();

        final Rectangle bounds = new Rectangle(0, 0, lonGrid.getRasterWidth(), lonGrid.getRasterHeight());
        pixelPosEstimator = new PixelPosEstimator(approximations, bounds);
        pixelFinder = new PodPixelFinder(lonImage, latImage, null, 0.01);
    }

    @Override
    public boolean canGetPixelPos() {
        return pixelPosEstimator.canGetPixelPos();
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPosEstimator.canGetPixelPos()) {
            if (pixelPos == null) {
                pixelPos = new PixelPos();
            }
            pixelPosEstimator.getPixelPos(geoPos, pixelPos);
            if (pixelPos.isValid()) {
                // check that LAT displacement is less than 0.5
                int x = (int) Math.floor(pixelPos.x);
                int y = (int) Math.floor(pixelPos.y);
                try {
                    double lat = latImage.getData(new Rectangle(x, y, 1, 1)).getSampleDouble(x, y, 0);
                    if (Math.abs(lat - geoPos.getLat()) > 0.5) {
                        pixelPos.setInvalid();
                    }
                } catch (IllegalArgumentException iae) {
                    pixelPosEstimator.getPixelPos(geoPos, pixelPos);
                }

            }
//            if (pixelPos.isValid()) {
//                pixelFinder.findPixelPos(geoPos, pixelPos);
//            }
        } else {
            super.getPixelPos(geoPos, pixelPos);
        }
        return pixelPos;
    }

    private static GeoApproximation[] createApproximations(PlanarImage lonImage, PlanarImage latImage) {
        return GeoApproximation.createApproximations(lonImage, latImage, null, 0.5);
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        final String latGridName = getLatGrid().getName();
        final String lonGridName = getLonGrid().getName();
        final Product destProduct = destScene.getProduct();
        TiePointGrid latGrid = destProduct.getTiePointGrid(latGridName);
        if (latGrid == null) {
            latGrid = TiePointGrid.createSubset(getLatGrid(), subsetDef);
            destProduct.addTiePointGrid(latGrid);
        }
        TiePointGrid lonGrid = destProduct.getTiePointGrid(lonGridName);
        if (lonGrid == null) {
            lonGrid = TiePointGrid.createSubset(getLonGrid(), subsetDef);
            destProduct.addTiePointGrid(lonGrid);
        }
        if (latGrid != null && lonGrid != null) {
            if (subsetDef == null || subsetDef.getRegion() == null) {
                // re-use approximations
                destScene.setGeoCoding(new PodGeoCoding(latGrid, lonGrid, approximations));
            } else {
                destScene.setGeoCoding(new PodGeoCoding(latGrid, lonGrid));
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void dispose() {
        super.dispose();

        pixelFinder = null;
        pixelPosEstimator = null;
        approximations = null;
        latImage = null;
    }
}
