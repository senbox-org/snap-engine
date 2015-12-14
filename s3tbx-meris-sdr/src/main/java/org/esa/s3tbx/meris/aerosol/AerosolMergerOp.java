/*
 * $Id: AerosolMergerOp.java,v 1.1 2007/03/27 12:52:21 marcoz Exp $
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.s3tbx.meris.aerosol;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;

import java.awt.*;
import java.util.Map;


@OperatorMetadata(alias = "Meris.AerosolMerger", internal = true)
public class AerosolMergerOp extends MerisBasisOp {

    private static final int LARS_FLAG = 1;
    private static final int MOD08_FLAG = 2;
    private static final int DEFAULT_FLAG = 4;

    private static final float AOT_DEFAULT = 0.1f;
    private static final float ANG_DEFAULT = 1.0f;

    private Band aot470Band;
    private Band angstrBand;
    private Band flagBand;

    @SourceProduct(alias="l2")
    private Product l2Product;
    @SourceProduct(alias="mod08")
    private Product mod08Product;
    @TargetProduct
    private Product targetProduct;
    

    @Override
    public void initialize() throws OperatorException {
        targetProduct = createCompatibleProduct(mod08Product, "AEROSOL", "AEROSOL");
        aot470Band = targetProduct.addBand("aot_470", ProductData.TYPE_FLOAT32);
        angstrBand = targetProduct.addBand("ang", ProductData.TYPE_FLOAT32);

        FlagCoding cloudFlagCoding = createFlagCoding(mod08Product);
        mod08Product.getFlagCodingGroup().add(cloudFlagCoding);

        flagBand = targetProduct.addBand("aerosol_flags", ProductData.TYPE_UINT8);
        flagBand.setDescription("Aerosol specific flags");
        flagBand.setSampleCoding(cloudFlagCoding);
    }

    private FlagCoding createFlagCoding(Product outputProduct) {
        MetadataAttribute cloudAttr;
        final FlagCoding flagCoding = new FlagCoding("aerosol_flags");
        flagCoding.setDescription("Cloud Flag Coding");

        int index = 0;
        int w = outputProduct.getSceneRasterWidth();
        int h = outputProduct.getSceneRasterHeight();
        Mask mask;

        cloudAttr = new MetadataAttribute("lars", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(LARS_FLAG);
        flagCoding.addAttribute(cloudAttr);
        mask = Mask.BandMathsType.create(cloudAttr.getName(),
                                         cloudAttr.getDescription(), w, h,
                                         flagCoding.getName() + "." + cloudAttr.getName(),
                                         Color.BLUE, 0.5f);
        outputProduct.getMaskGroup().add(index++, mask);

        cloudAttr = new MetadataAttribute("mod08", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(MOD08_FLAG);
        flagCoding.addAttribute(cloudAttr);
        mask = Mask.BandMathsType.create(cloudAttr.getName(),
                                         cloudAttr.getDescription(), w, h,
                                         flagCoding.getName() + "." + cloudAttr.getName(),
                                         Color.RED, 0.5f);
        outputProduct.getMaskGroup().add(index++, mask);

        cloudAttr = new MetadataAttribute("default", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(DEFAULT_FLAG);
        flagCoding.addAttribute(cloudAttr);
        mask = Mask.BandMathsType.create(cloudAttr.getName(),
                                         cloudAttr.getDescription(), w, h,
                                         flagCoding.getName() + "." + cloudAttr.getName(),
                                         Color.GREEN, 0.5f);
        outputProduct.getMaskGroup().add(index, mask);

        return flagCoding;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {

        final int size = rectangle.height * rectangle.width;
        pm.beginTask("Processing frame...", 1 + size);
        try {
            float[] modAot = (float[]) getSourceTile(mod08Product.getBand(ModisAerosolOp.BAND_NAME_AOT_470), rectangle).getRawSamples().getElems();
			float[] modAng = (float[]) getSourceTile(mod08Product.getBand(ModisAerosolOp.BAND_NAME_ANG), rectangle).getRawSamples().getElems();
			byte[] modFlag = (byte[]) getSourceTile(mod08Product.getBand(ModisAerosolOp.BAND_NAME_FLAGS), rectangle).getRawSamples().getElems();
			
			float[] l2Aot = (float[]) getSourceTile(l2Product.getBand("aero_opt_thick_443"), rectangle).getRawSamples().getElems();
			float[] l2Ang = (float[]) getSourceTile(l2Product.getBand("aero_alpha"), rectangle).getRawSamples().getElems();

            ProductData rawSampleDataAot470 = targetTiles.get(aot470Band).getRawSamples();
            float[] aot470 = (float[]) rawSampleDataAot470.getElems();
            ProductData rawSampleDataAng = targetTiles.get(aot470Band).getRawSamples();
            float[] ang = (float[]) rawSampleDataAng.getElems();
            ProductData rawSampleDataFlag = targetTiles.get(flagBand).getRawSamples();
            byte[] flag = (byte[]) rawSampleDataFlag.getElems();

            for (int i = 0; i < size; i++) {
                if (l2Aot[i] >= 0 && l2Ang[i] >= 0) {
                    aot470[i] = l2Aot[i];
                    ang[i] = l2Ang[i];
                    flag[i] = LARS_FLAG;
                } else if (modFlag[i] == 0) {
                    aot470[i] = modAot[i];
                    ang[i] = modAng[i];
                    flag[i] = MOD08_FLAG;
                } else {
                    aot470[i] = AOT_DEFAULT;
                    ang[i] = ANG_DEFAULT;
                    flag[i] = DEFAULT_FLAG;
                }
                pm.worked(1);
            }
            targetTiles.get(aot470Band).setRawSamples(rawSampleDataAot470);
            targetTiles.get(aot470Band).setRawSamples(rawSampleDataAng);
            targetTiles.get(flagBand).setRawSamples(rawSampleDataFlag);
        } finally {
            pm.done();
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {super(AerosolMergerOp.class);
        }
    }
}
