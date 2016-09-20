/*
 * Copyright (C) 2002-2007 by ?
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.s3tbx.aerosol;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.dem.ElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModelRegistry;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;

import java.awt.*;

/**
 * Operator that creates the one and only elevation band
 * based on sourceProduct and BEAM GETASSE30
 */
@OperatorMetadata(alias = "AerosolRetrieval.CreateElevationBand",
        description = "creates a single band with elevation from getasse",
        authors = "A.Heckel, Olaf Danne, Marco Zuehlke",
        version = "1.0",
        internal = true,
        copyright = "(C) 2010, 2016 by University Swansea (a.heckel@swansea.ac.uk) and Brockmann Consult")
public class CreateElevationBandOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    private ElevationModel dem;
    private float noDataValue;
    private GeoCoding geoCoding;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public CreateElevationBandOp() {
    }

    @Override
    public void initialize() throws OperatorException {
        final int rasterWidth = sourceProduct.getSceneRasterWidth();
        final int rasterHeight = sourceProduct.getSceneRasterHeight();
        geoCoding = sourceProduct.getSceneGeoCoding();
        targetProduct = new Product("Elevation Product", "Elevation", rasterWidth, rasterHeight);
        targetProduct.setDescription("Elevation for " + sourceProduct.getName());
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setPointingFactory(sourceProduct.getPointingFactory());

        final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
        ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor("GMTED2010_30");
        if (demDescriptor == null || !demDescriptor.canBeDownloaded()) {
            demDescriptor = elevationModelRegistry.getDescriptor("GETASSE30");
            if (demDescriptor == null || !demDescriptor.canBeDownloaded()) {
                throw new OperatorException(" No DEM installed (neither GETASSE30 nor GMTED2010_30).");
            }
        }
        getLogger().info("Dsing DEM: " + demDescriptor.getName());
        dem = demDescriptor.createDem(Resampling.BILINEAR_INTERPOLATION);
        noDataValue = dem.getDescriptor().getNoDataValue();
        String elevName = "elevation";
        Band elevBand = targetProduct.addBand(elevName, ProductData.TYPE_INT16);
        elevBand.setNoDataValue(noDataValue);
        elevBand.setNoDataValueUsed(true);
        elevBand.setUnit("meters");
        elevBand.setDescription(demDescriptor.getName());
        setTargetProduct(targetProduct);
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.core.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        Rectangle targetTileRectangle = targetTile.getRectangle();
        int x0 = targetTileRectangle.x;
        int y0 = targetTileRectangle.y;
        int w = targetTileRectangle.width;
        int h = targetTileRectangle.height;

        pm.beginTask("Computing elevations", h);
        try {
            final GeoPos geoPos = new GeoPos();
            final PixelPos pixelPos = new PixelPos();
            float elevation;
            for (int y = y0; y < y0 + h; y++) {
                for (int x = x0; x < x0 + w; x++) {
                    pixelPos.setLocation(x + 0.5f, y + 0.5f);
                    geoCoding.getGeoPos(pixelPos, geoPos);
                    try {
                        elevation = (float) dem.getElevation(geoPos);
                    } catch (Exception e) {
                        elevation = noDataValue;
                    }
                    targetTile.setSample(x, y, (short) Math.round(elevation));
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(CreateElevationBandOp.class);
        }
    }
}
