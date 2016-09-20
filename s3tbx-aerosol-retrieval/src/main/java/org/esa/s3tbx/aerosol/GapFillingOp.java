/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.aerosol.util.AerosolUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FlagCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.BitSetter;
import org.esa.snap.core.util.ProductUtils;

import javax.media.jai.BorderExtender;
import java.awt.*;
import java.util.Map;

/**
 * @author akheckel
 */
@OperatorMetadata(alias = "AerosolRetrieval.GapFilling",
        description = "Fills Gaps in Grid",
        authors = "Andreas Heckel, Olaf Danne, Marco Zuehlke",
        version = "1.0",
        internal = true,
        copyright = "(C) 2010, 2016 by University Swansea (a.heckel@swansea.ac.uk) and Brockmann Consult")
public class GapFillingOp extends Operator {

    private static final int F_INTERP = 1;
    private static final int F_CLIM = 0;
    private static final int OFF = 25;
    private static final int BOX = OFF * 2;
    private static final int CLIM_DIST = 10;
    private static final double CLIM_ERR = 0.3;

    @SourceProduct
    private Product aotProduct;

    @TargetProduct
    private Product targetProduct;

    private int rasterHeight;
    private int rasterWidth;


    @Override
    public void initialize() throws OperatorException {
        String pname = aotProduct.getName();
        String ptype = aotProduct.getProductType();
        rasterWidth = aotProduct.getSceneRasterWidth();
        rasterHeight = aotProduct.getSceneRasterHeight();
        targetProduct = new Product(pname, ptype, rasterWidth, rasterHeight);
        FlagCoding aotFlagCoding = new FlagCoding(AotConsts.aotFlags.name);
        aotFlagCoding.addFlag("aot_climatology", BitSetter.setFlag(0, F_CLIM), "aot from climatology only");
        aotFlagCoding.addFlag("aot_interp", BitSetter.setFlag(0, F_INTERP), "aot spatially interpolated");
        targetProduct.getFlagCodingGroup().add(aotFlagCoding);
        AerosolUtils.createFlagMasks(targetProduct);
        Band targetBand = AerosolUtils.createTargetBand(AotConsts.aotFlags, rasterWidth, rasterHeight);
        targetBand.setSampleCoding(aotFlagCoding);
        targetProduct.addBand(targetBand);

        ProductUtils.copyBand(AotConsts.aot.name, aotProduct, targetProduct, false);
        ProductUtils.copyBand(AotConsts.aotErr.name, aotProduct, targetProduct, false);
        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle tarRec, ProgressMonitor pm) throws OperatorException {
        Rectangle srcRec = new Rectangle(tarRec.x - OFF, tarRec.y - OFF, tarRec.width + BOX, tarRec.height + BOX);
        BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

        Tile latTile = getSourceTile(aotProduct.getBand("latitude"), srcRec, borderExtender);
        Tile aotTile = getSourceTile(aotProduct.getBand(AotConsts.aot.name), srcRec, borderExtender);
        Tile aotErrTile = getSourceTile(aotProduct.getBand(AotConsts.aotErr.name), srcRec, borderExtender);

        Tile tarAotTile = targetTiles.get(targetProduct.getBand(AotConsts.aot.name));
        Tile tarAotErrTile = targetTiles.get(targetProduct.getBand(AotConsts.aotErr.name));
        Tile tarAotFlagsTile = targetTiles.get(targetProduct.getBand(AotConsts.aotFlags.name));

        double noDataVal = aotTile.getRasterDataNode().getGeophysicalNoDataValue();
        float aotPixel;
        float aotErrPixel;
        for (int y = tarRec.y; y < tarRec.y + tarRec.height; y++) {
            for (int x = tarRec.x; x < tarRec.x + tarRec.width; x++) {

                double climAot = calcClimAot(latTile.getSampleFloat(x, y));

                aotPixel = aotTile.getSampleFloat(x, y);
                aotErrPixel = aotErrTile.getSampleFloat(x, y);
                if (Double.compare(noDataVal, aotPixel) != 0) {
                    tarAotTile.setSample(x, y, aotPixel);
                    tarAotErrTile.setSample(x, y, aotErrPixel);
                    tarAotFlagsTile.setSample(x, y, 0);
                } else {
                    float[] fillResult = new float[2];
                    int flagPixel = fillPixel(x, y, aotTile, aotErrTile, climAot, noDataVal, fillResult);
                    tarAotTile.setSample(x, y, fillResult[0]);
                    tarAotErrTile.setSample(x, y, fillResult[1]);
                    tarAotFlagsTile.setSample(x, y, flagPixel);
                }
            }
        }
    }

    private int fillPixel(int x, int y, Tile aotTile, Tile aotErrTile, double climAot, double noDataValue, float[] fillResult) {
        double n0 = invDistanceWeight(CLIM_DIST, 0, 4);
        double n = n0;
        double sum = climAot * n;
        double sumErr = CLIM_ERR * n;
        float val;
        double weight;
        int ys = (y - OFF < 0) ? 0 : y - OFF;
        int xs = (x - OFF < 0) ? 0 : x - OFF;
        int ye = (y + OFF >= rasterHeight) ? rasterHeight - 1 : y + OFF;
        int xe = (x + OFF >= rasterWidth) ? rasterWidth - 1 : x + OFF;
        for (int j = ys; j <= ye; j++) {
            for (int i = xs; i <= xe; i++) {
                val = aotTile.getSampleFloat(i, j);
                if (Double.compare(noDataValue, val) != 0) {
                    weight = invDistanceWeight(i - x, j - y, 4);
                    sum += val * weight;
                    n += weight;
                    sumErr += aotErrTile.getSampleFloat(i, j) * weight;
                }
            }
        }
        int flag = 0;
        if (n > n0) {
            fillResult[0] = (float) (sum / n);
            fillResult[1] = (float) (sumErr / n);
            flag = BitSetter.setFlag(flag, F_INTERP);
        } else {
            if (n > 0) {
                fillResult[0] = (float) (sum / n);
                fillResult[1] = (float) (sumErr / n);
            } else {
                fillResult[0] = (float) noDataValue;
                fillResult[1] = (float) noDataValue;
            }
            flag = BitSetter.setFlag(flag, F_CLIM);
        }
        return flag;
    }

    private static double invDistanceWeight(int i, int j, int power) {
        return 1 / (Math.pow(Math.pow(i, 2) + Math.pow(j, 2), power / 2) + 1.0E-5);
    }

    private static double calcClimAot(float lat) {
        double latR = Math.toRadians(lat);
        return 0.2 * (Math.cos(latR) - 0.25)
                * Math.pow(Math.sin(latR + Math.PI / 2), 3) + 0.05;
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
            super(GapFillingOp.class);
        }
    }
}
