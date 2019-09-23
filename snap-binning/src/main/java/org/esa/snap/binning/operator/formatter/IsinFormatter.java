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
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.util.grid.isin.IsinPoint;
import org.esa.snap.core.util.grid.isin.IsinUtils;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

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
        final ProductData[] targetVariables = new ProductData[featureNames.length];

        final IsinPlanetaryGrid isinGrid = (IsinPlanetaryGrid) planetaryGrid;

        final String outputFormat = formatterConfig.getOutputFormat();
        final File outputFile = new File(formatterConfig.getOutputFile());

        // iterate bin cells (check for tile index increment)
        int x_tile = -1;
        int y_tile = -1;
        String productName = null;
        Product tileProduct = null;
        int productWidth = 0;

        final int partCount = temporalBinSource.open();
        for (int i = 0; i < partCount; i++) {
            final Iterator<? extends TemporalBin> part = temporalBinSource.getPart(i);

            while (part.hasNext()) {
                final TemporalBin temporalBin = part.next();
                final long index = temporalBin.getIndex();
                final IsinPoint gridIndex = IsinPlanetaryGrid.toIsinPoint(index);

                // make switch to new tile
                // -----------------------
                if (x_tile != gridIndex.getTile_col() || y_tile != gridIndex.getTile_line()) {
                    if (tileProduct != null) {
                        writeTileProduct(outputFormat, outputFile, productName, tileProduct);
                        tileProduct.dispose();
                    }

                    x_tile = gridIndex.getTile_col();
                    y_tile = gridIndex.getTile_line();

                    tileProduct = IsinUtils.createMpcVegetationPrototype(x_tile, y_tile, isinGrid.getRaster());
                    productWidth = tileProduct.getSceneRasterWidth();
                    productName = getProductName(startTime.getAsDate(), stopTime.getAsDate(), x_tile, y_tile);

                    for (int feat = 0; feat < featureNames.length; feat++) {
                        final RasterDataNode rasterDataNode = tileProduct.getRasterDataNode(featureNames[feat]);
                        targetVariables[feat] = rasterDataNode.getRasterData();
                    }
                }

                int writeIndex = (int) (gridIndex.getX() + productWidth * gridIndex.getY());
                final float[] featureValues = temporalBin.getFeatureValues();
                for (int feat = 0; feat < featureValues.length; feat++) {
                    targetVariables[feat].setElemFloatAt(writeIndex, featureValues[feat]);
                }
            }
        }

        if (tileProduct != null) {
            writeTileProduct(outputFormat, outputFile, productName, tileProduct);
            tileProduct.dispose();
        }
    }

    private void writeTileProduct(String outputType, File outputFile, String productName, Product tileProduct) throws IOException {
        final File targetFile = new File(outputFile, productName);
        ProductIO.writeProduct(tileProduct, targetFile.getAbsolutePath(), outputType);
    }
}
