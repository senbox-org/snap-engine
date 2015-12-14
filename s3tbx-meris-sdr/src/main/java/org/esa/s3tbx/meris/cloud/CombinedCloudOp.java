package org.esa.s3tbx.meris.cloud;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.BitSetter;

import java.awt.*;

@OperatorMetadata(alias = "Meris.CombinedCloud", internal = true)
public class CombinedCloudOp extends MerisBasisOp {

    public static final String FLAG_BAND_NAME = "combined_cloud";
    public static final int FLAG_INVALID = 0;
    public static final int FLAG_CLEAR = 1;
    public static final int FLAG_CLOUD = 2;
    public static final int FLAG_SNOW = 4;
    public static final int FLAG_CLOUD_EDGE = 8;
    public static final int FLAG_CLOUD_SHADOW = 16;

    @SourceProduct(alias="cloudProb")
    private Product cloudProduct;
    @SourceProduct(alias="blueBand")
    private Product blueBandProduct;
    @TargetProduct
    private Product targetProduct;

    @Override
    public void initialize() throws OperatorException {

        targetProduct = createCompatibleProduct(cloudProduct, "MER_COMBINED_CLOUD", "MER_L2");
        // create and add the flags coding
        FlagCoding flagCoding = createFlagCoding();
        targetProduct.getFlagCodingGroup().add(flagCoding);

        // create and add the flags dataset
        Band combinedCloudBand = targetProduct.addBand(FLAG_BAND_NAME, ProductData.TYPE_UINT8);
        combinedCloudBand.setDescription("combined cloud flags");
        combinedCloudBand.setSampleCoding(flagCoding);
    }

    @Override
    public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {

    	Rectangle rectangle = targetTile.getRectangle();
        final int size = rectangle.height * rectangle.width;
        pm.beginTask("Processing frame...", size + 1);
        try {
        	byte[] cloudProbData = (byte[]) getSourceTile(cloudProduct.getBand(CloudProbabilityOp.CLOUD_FLAG_BAND), rectangle).getRawSamples().getElems();
        	byte[] blueBandData = (byte[]) getSourceTile(blueBandProduct.getBand(BlueBandOp.BLUE_FLAG_BAND), rectangle).getRawSamples().getElems();

            ProductData flagData = targetTile.getRawSamples();
            byte[] combinedCloudData = (byte[]) flagData.getElems();
            pm.worked(1);
            
            for (int i = 0; i < size; i++) {
                final byte cloudProb = cloudProbData[i];
                byte result;
                if (cloudProb == CloudProbabilityOp.FLAG_INVALID) {
                    result = FLAG_INVALID;
                } else {
                    byte combined = FLAG_CLEAR;
                    final byte blueBand = blueBandData[i];
                    if (cloudProb == CloudProbabilityOp.FLAG_CLOUDY
                            || BitSetter.isFlagSet(blueBand, BlueBandOp.DENSE_CLOUD_BIT)
                            || BitSetter.isFlagSet(blueBand, BlueBandOp.THIN_CLOUD_BIT)) {
                        combined = FLAG_CLOUD;
                    }
                    if (BitSetter.isFlagSet(blueBand, BlueBandOp.SNOW_BIT)) {
                        combined = FLAG_SNOW;
                    }
                    
                    boolean snowPlausible = BitSetter.isFlagSet(blueBand, BlueBandOp.SNOW_PLAUSIBLE_BIT);
                    boolean snowIndex = BitSetter.isFlagSet(blueBand, BlueBandOp.SNOW_INDEX_BIT);
                    boolean brightLand = BitSetter.isFlagSet(blueBand, BlueBandOp.BRIGHT_LAND_BIT);

                    if (snowPlausible && (snowIndex || combined == FLAG_SNOW)) {
                        result = FLAG_SNOW;
                    } else if (!snowPlausible && (snowIndex || combined == FLAG_SNOW)) {
                        result = FLAG_CLOUD;
                    } else if (brightLand && !snowPlausible && ((snowIndex && combined != FLAG_CLOUD) || combined == FLAG_SNOW)) {
                        result = FLAG_CLOUD;
                    } else if (combined == FLAG_CLOUD && !snowIndex) {
                        result = FLAG_CLOUD;
                    } else {
                        result = FLAG_CLEAR;
                    }
                    if (combined == FLAG_CLEAR) {
                        result = FLAG_CLEAR;
                    }
                }
                combinedCloudData[i] = result;
                pm.worked(1);
            }
            targetTile.setRawSamples(flagData);
        } finally {
            pm.done();
        }
    }
    
    public static FlagCoding createFlagCoding() {
        MetadataAttribute cloudAttr;
        final FlagCoding flagCoding = new FlagCoding(FLAG_BAND_NAME);
        flagCoding.setDescription("Combined CLoud Band Flag Coding");

        cloudAttr = new MetadataAttribute("clear", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_CLEAR);
        flagCoding.addAttribute(cloudAttr);

        cloudAttr = new MetadataAttribute("cloud", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_CLOUD);
        flagCoding.addAttribute(cloudAttr);

        cloudAttr = new MetadataAttribute("snow", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_SNOW);
        flagCoding.addAttribute(cloudAttr);

        cloudAttr = new MetadataAttribute("cloud_edge", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_CLOUD_EDGE);
        flagCoding.addAttribute(cloudAttr);

        cloudAttr = new MetadataAttribute("cloud_shadow", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(FLAG_CLOUD_SHADOW);
        flagCoding.addAttribute(cloudAttr);

        return flagCoding;
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(CombinedCloudOp.class);
        }
    }
}
