/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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


package org.esa.snap.binning.operator.formatter;

import org.esa.snap.binning.*;
import org.esa.snap.binning.operator.ImageTemporalBinRenderer;
import org.esa.snap.binning.operator.ProductTemporalBinRenderer;
import org.esa.snap.binning.support.CrsGrid;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;

import java.awt.*;
import java.io.File;

/**
 * Utility class used to format the results of a binning given by a {@link TemporalBinSource}.
 *
 * @author Norman Fomferra
 */
class DefaultFormatter implements Formatter {

    public void format(PlanetaryGrid planetaryGrid,
                       TemporalBinSource temporalBinSource,
                       String[] featureNames,
                       FormatterConfig formatterConfig,
                       Geometry roiGeometry,
                       ProductData.UTC startTime,
                       ProductData.UTC stopTime,
                       MetadataElement... metadataElements) throws Exception {

        if (featureNames.length == 0) {
            throw new IllegalArgumentException("Illegal binning context: featureNames.length == 0");
        }

        final File outputFile = new File(formatterConfig.getOutputFile());
        final String outputType = formatterConfig.getOutputType();
        final String outputFormat = FormatterFactory.getOutputFormat(formatterConfig, outputFile);

        final Rectangle outputRegion = getOutputRegion(planetaryGrid, roiGeometry);
        final ProductCustomizer productCustomizer = formatterConfig.getProductCustomizer();

        final TemporalBinRenderer temporalBinRenderer;
        if (outputType.equalsIgnoreCase("Product")) {
            GeoCoding geoCoding;
            if (planetaryGrid instanceof MosaickingGrid) {
                MosaickingGrid mosaickingGrid = (MosaickingGrid) planetaryGrid;
                geoCoding = mosaickingGrid.getGeoCoding(outputRegion);
            } else {
                double pixelSize = Reprojector.getRasterPixelSize(planetaryGrid);
                geoCoding = ProductTemporalBinRenderer.createMapGeoCoding(outputRegion, pixelSize);
            }
            temporalBinRenderer = new ProductTemporalBinRenderer(featureNames,
                    outputFile,
                    outputFormat,
                    outputRegion,
                    geoCoding,
                    startTime,
                    stopTime,
                    productCustomizer,
                    metadataElements);
        } else {
            temporalBinRenderer = new ImageTemporalBinRenderer(featureNames,
                    outputFile,
                    outputFormat,
                    outputRegion,
                    formatterConfig.getBandConfigurations(),
                    outputType.equalsIgnoreCase("RGB"));
        }

        Reprojector.reproject(planetaryGrid, temporalBinSource, temporalBinRenderer);
    }

    static Rectangle getOutputRegion(PlanetaryGrid planetaryGrid, Geometry roiGeometry) {
        final Rectangle outputRegion;
        if (planetaryGrid instanceof CrsGrid) {
            if (roiGeometry == null) {
                outputRegion = new Rectangle(planetaryGrid.getNumCols(0), planetaryGrid.getNumRows());
            } else {
                final CrsGrid crsGrid = (CrsGrid) planetaryGrid;
                if (roiGeometry instanceof LinearRing) {
                    final LinearRing linearRing = (LinearRing) roiGeometry;
                    roiGeometry = new GeometryFactory().createPolygon(linearRing);
                }
                final Geometry imageGeometry = crsGrid.getImageGeometry(roiGeometry);
                if (imageGeometry == null) {
                    outputRegion = new Rectangle(planetaryGrid.getNumCols(0), planetaryGrid.getNumRows());
                } else {
                    outputRegion = crsGrid.getBounds(imageGeometry);
                }
            }
        } else {
            outputRegion = Reprojector.computeRasterSubRegion(planetaryGrid, roiGeometry);
        }
        return outputRegion;
    }
}
