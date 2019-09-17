package org.esa.snap.binning.operator.formatter;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.TemporalBin;
import org.esa.snap.binning.TemporalBinSource;
import org.esa.snap.binning.support.IsinPlanetaryGrid;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.grid.isin.IsinPoint;

import java.io.File;
import java.util.Iterator;

public class IsinFormatter implements Formatter {

    @Override
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

        final String outputType = formatterConfig.getOutputType();
        final File outputFile = new File(formatterConfig.getOutputFile());
        if (outputFile.exists() && outputFile.isDirectory()) {
            // check if exists - overwrite?
        }

        // iterate bin cells (check for tile index increment)
        int x_tile = -1;
        int y_tile = -1;

        final int partCount = temporalBinSource.open();
        for (int i = 0; i < partCount; i++) {
            final Iterator<? extends TemporalBin> part = temporalBinSource.getPart(i);

            while (part.hasNext()) {
                final TemporalBin temporalBin = part.next();
                final long index = temporalBin.getIndex();
                final IsinPoint gridIndex = IsinPlanetaryGrid.toIsinPoint(index);
                if (x_tile != gridIndex.getTile_col() || y_tile != gridIndex.getTile_line()) {
                    x_tile = gridIndex.getTile_col();
                    y_tile = gridIndex.getTile_line();

                    // getProductName(formatterConfig, startTime, stopTime, x_tile, y_tile);

                    // write current memory Product to disk
                    // dispose
                    // create new memory Product
                    // create filename from platform, startDate, endDate, timeIndexString
                }

                // write final memory Product, if exists


            }
        }
        // -- create file name from conventions
        // -- create product template
        // - OR
        // -- fill in values (check from feature list)

        //throw new RuntimeException("not implemented");
    }
}
