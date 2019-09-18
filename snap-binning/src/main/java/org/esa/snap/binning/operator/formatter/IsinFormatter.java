package org.esa.snap.binning.operator.formatter;

import com.vividsolutions.jts.geom.Geometry;
import org.esa.snap.binning.PlanetaryGrid;
import org.esa.snap.binning.TemporalBin;
import org.esa.snap.binning.TemporalBinSource;
import org.esa.snap.binning.support.IsinPlanetaryGrid;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.grid.isin.IsinPoint;
import org.esa.snap.core.util.grid.isin.IsinUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import static org.esa.snap.core.util.grid.isin.IsinAPI.Raster.GRID_500_M;

public class IsinFormatter implements Formatter {

    static String getProductName(Date startTime, Date stopTime, int x_tile, int y_tile) {
        // @todo 1 tb/tb read platform from configuration 2019-09-18
        final String tileIndex = String.format("h%02dv%02d", x_tile, y_tile);
        return IsinUtils.createFileName("S3A", startTime, stopTime, new Date(), tileIndex);
    }

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

        final String outputFormat = formatterConfig.getOutputFormat();
        final File outputFile = new File(formatterConfig.getOutputFile());
        if (outputFile.exists() && outputFile.isDirectory()) {
            // check if exists - overwrite?
        }

        // iterate bin cells (check for tile index increment)
        int x_tile = -1;
        int y_tile = -1;
        String productName = null;
        Product tileProduct = null;

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

                    if (tileProduct != null) {
                        writeTileProduct(outputFormat, outputFile, productName, tileProduct);
                    }

                    // @todo 1 tb/tb read resolution from config 2019-09-18
                    tileProduct = IsinUtils.createMpcVegetationPrototype(x_tile, y_tile, GRID_500_M);
                    productName = getProductName(startTime.getAsDate(), stopTime.getAsDate(), x_tile, y_tile);
                }


            }
        }

        writeTileProduct(outputFormat, outputFile, productName, tileProduct);

        // -- create file name from conventions
        // -- create product template
        // - OR
        // -- fill in values (check from feature list)

        //throw new RuntimeException("not implemented");
    }

    private void writeTileProduct(String outputType, File outputFile, String productName, Product tileProduct) throws IOException {
        final File targetFile = new File(outputFile, productName);
        ProductIO.writeProduct(tileProduct, targetFile.getAbsolutePath(), outputType);
        tileProduct.dispose();
    }
}
