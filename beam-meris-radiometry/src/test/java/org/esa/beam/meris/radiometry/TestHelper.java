package org.esa.beam.meris.radiometry;

import com.bc.jexp.ParseException;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;

import java.awt.Color;

public class TestHelper {

    public static final int SCENE_WIDTH = 10;
    public static final int SCENE_HEIGHT = 10;

    public static Product createL1bProduct(String resolutionString) throws ParseException, java.text.ParseException {
        boolean isFSG = resolutionString.equals("FSG");
        String productType = String.format("MER_%s_1P", resolutionString);
        Product product = new Product("MERIS-TEST-PRODUCT", productType, SCENE_WIDTH, SCENE_HEIGHT);

        final float[] MERIS_WAVELENGTHS = new float[]{
                412.0f, 442.0f, 490.0f, 510.0f, 560.0f, 620.0f, 665.0f,
                681.0f, 705.0f, 753.0f, 760.0f, 775.0f, 865.0f, 890.0f, 900.0f
        };

        for (int i = 0; i < 15; i++) {
            final Band band = product.addBand(String.format("radiance_%d", (i + 1)), "X");
            band.setSpectralBandIndex(i);
            band.setSpectralWavelength(MERIS_WAVELENGTHS[i]);
            band.setData(band.createCompatibleRasterData());
        }
        addBandToProduct(product, "l1_flags", ProductData.TYPE_INT8);
        addBandToProduct(product, "detector_index", ProductData.TYPE_UINT16);
        if (isFSG) {
            product.addBand("corr_longitude", "X/10");
            product.addBand("corr_latitude", "60 - Y/10");
            product.addBand("altitude", "10", ProductData.TYPE_INT16);
        }

        float[] tiePointData = new float[SCENE_WIDTH * SCENE_HEIGHT];
        product.addTiePointGrid(new TiePointGrid("sun_zenith", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("sun_azimuth", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("view_zenith", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("view_azimuth", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("dem_alt", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("atm_press", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("ozone", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("latitude", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("longitude", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("dem_rough", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("lat_corr", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("lon_corr", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("zonal_wind", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("merid_wind", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));
        product.addTiePointGrid(new TiePointGrid("rel_hum", SCENE_WIDTH, SCENE_HEIGHT, 0, 0, 1, 1, tiePointData));

        FlagCoding l1_flags = new FlagCoding("l1_flags");
        l1_flags.addFlag("COSMETIC", 0x01, "No Description.");
        l1_flags.addFlag("DUPLICATED", 0x02, "No Description.");
        l1_flags.addFlag("GLINT_RISK", 0x04, "No Description.");
        l1_flags.addFlag("SUSPECT", 0x08, "No Description.");
        l1_flags.addFlag("LAND_OCEAN", 0x10, "No Description.");
        l1_flags.addFlag("BRIGHT", 0x20, "No Description.");
        l1_flags.addFlag("COASTLINE", 0x40, "No Description.");
        l1_flags.addFlag("INVALID", 0x80, "No Description.");
        product.getBand("l1_flags").setSampleCoding(l1_flags);
        product.getFlagCodingGroup().add(l1_flags);
        ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
        maskGroup.add(mask("coastline", "l1_flags.COASTLINE"));
        maskGroup.add(mask("land_ocean", "l1_flags.LAND_OCEAN"));
        maskGroup.add(mask("water", "NOT l1_flags.LAND_OCEAN"));
        maskGroup.add(mask("cosmetic", "l1_flags.COSMETIC"));
        maskGroup.add(mask("duplicated", "l1_flags.DUPLICATED"));
        maskGroup.add(mask("glint_risk", "l1_flags.GLINT_RISK"));
        maskGroup.add(mask("suspect", "l1_flags.SUSPECT"));
        maskGroup.add(mask("bright", "l1_flags.BRIGHT"));
        maskGroup.add(mask("invalid", "l1_flags.INVALID"));

        product.setStartTime(ProductData.UTC.parse("12-Mar-2003 13:45:36"));
        product.setEndTime(ProductData.UTC.parse("12-Mar-2003 13:48:12"));

        final MetadataElement sph = new MetadataElement("SPH");
        ProductData sphData = ProductData.createInstance(String.format("MER_%s_1P SPECIFIC HEADER", resolutionString));
        final MetadataAttribute sphDescriptor = new MetadataAttribute("SPH_DESCRIPTOR", sphData, true);
        sph.addAttribute(sphDescriptor);
        product.getMetadataRoot().addElement(sph);

        final MetadataElement mph = new MetadataElement("MPH");
        ProductData mphData = ProductData.createInstance(product.getName() + ".N1");
        final MetadataAttribute mphDescriptor = new MetadataAttribute("PRODUCT", mphData, true);
        mph.addAttribute(mphDescriptor);
        product.getMetadataRoot().addElement(mph);

        // adding minimal metadata for detecting reprocessing version
        MetadataElement dsd23 = new MetadataElement("DSD.23");
        dsd23.addAttribute(new MetadataAttribute("DATASET_NAME",
                                                 new ProductData.ASCII("RADIOMETRIC_CALIBRATION_FILE"), true));
        dsd23.addAttribute(new MetadataAttribute("FILE_NAME",
                                                 new ProductData.ASCII("MER_RAC_AXVIEC20050708_135806_20041213_220000_20141213_220000"),
                                                 true));
        MetadataElement dsd = new MetadataElement("DSD");
        dsd.addElement(dsd23);
        product.getMetadataRoot().addElement(dsd);

        if (isFSG) {
            product.setGeoCoding(new PixelGeoCoding(product.getBand("corr_latitude"),
                                                    product.getBand("corr_longitude"), null, 3));
        } else {
            product.setGeoCoding(new TiePointGeoCoding(product.getTiePointGrid("latitude"),
                                                       product.getTiePointGrid("longitude")));

        }

        return product;
    }

    private static Mask mask(String name, String expression) {
        return Mask.BandMathsType.create(name, null, TestHelper.SCENE_WIDTH, TestHelper.SCENE_HEIGHT, expression, Color.green, 0.5f);
    }

    private static Band addBandToProduct(Product product, String name, int dataType) {
        Band band = product.addBand(name, dataType);
        band.setData(band.createCompatibleRasterData());
        return band;
    }

}