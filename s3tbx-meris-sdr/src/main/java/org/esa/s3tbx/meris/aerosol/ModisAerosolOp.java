/*
 * $Id: ModisAerosolOp.java,v 1.1 2007/03/27 12:52:21 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.aerosol;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdflib.HDFConstants;
import ncsa.hdf.hdflib.HDFException;
import ncsa.hdf.hdflib.HDFLibrary;
import org.esa.s3tbx.meris.MerisBasisOp;
import org.esa.s3tbx.util.math.FractIndex;
import org.esa.s3tbx.util.math.Interp;
import org.esa.s3tbx.util.math.LUT;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.StringUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Map;


@OperatorMetadata(alias = "Meris.Mod08Aerosol", internal = true)
public class ModisAerosolOp extends MerisBasisOp {

    public static final String BAND_NAME_AOT_470 = "aot_470";
    public static final String BAND_NAME_AOT_660 = "aot_660";
    public static final String BAND_NAME_ANG = "ang";
    public static final String BAND_NAME_FLAGS = "aerosol_flags";

    private static final String MOD08_BAND_NAME = "Corrected_Optical_Depth_Land_QA_Mean_Mean";
    private static final int AEROSOL_GAP_VALUE = -9999;
    private static final float SCALE_FACTOR = 0.001f;

    private static final int MOD08_WIDTH = 360;
    private static final int MOD08_HEIGHT = 180;

    private static final double AOT_470_WAVELENGTH = 470.0E-9;
    private static final double AOT_660_WAVELENGTH = 660.0E-9;

    private TemporalFileArray mod08FileArray;

    private LUT _aot470LUT;
    private LUT _aot660LUT;
    private boolean[][][] gapFilled;

    private Band _aot470Band;
    private Band _aot660Band;
    private Band _angstrBand;
    private Band _flagsBand;

    private GapFiller gapFiller;

    @SourceProduct(alias = "input")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
    @Parameter
    private String auxdataDir;

    @Override
    public void initialize() throws OperatorException {
        if (StringUtils.isNullOrEmpty(auxdataDir)) {
            throw new OperatorException("'auxdataDir' not set.");
        }
        File mod08Dir = new File(auxdataDir);
        if (!mod08Dir.exists()) {
            throw new OperatorException("'auxdataDir' does not exist.");
        }

        mod08FileArray = TemporalFileArray.scan(mod08Dir, true, new MOD08FileFactory());
        if (mod08FileArray.getTemporalFiles().length < 2) {
            throw new OperatorException("not enough MOD08 data available");
        }
        gapFiller = new GapFiller();
        gapFiller.setWidthHeight(MOD08_WIDTH, MOD08_HEIGHT);


        try {
            readMod08();
        } catch (IOException e) {
            throw new OperatorException("Could not load MOD08 data files", e);
        }
        createTargetProduct();
    }

    private void createTargetProduct() {
        targetProduct = createCompatibleProduct(sourceProduct, "AEROSOL", "AEROSOL");
        _aot470Band = targetProduct.addBand(BAND_NAME_AOT_470, ProductData.TYPE_FLOAT32);
        _aot660Band = targetProduct.addBand(BAND_NAME_AOT_660, ProductData.TYPE_FLOAT32);
        _angstrBand = targetProduct.addBand(BAND_NAME_ANG, ProductData.TYPE_FLOAT32);

        // create and add the flags coding
        FlagCoding cloudFlagCoding = createFlagCoding(targetProduct);
        targetProduct.getFlagCodingGroup().add(cloudFlagCoding);

        // create and add the SDR flags dataset
        _flagsBand = targetProduct.addBand(BAND_NAME_FLAGS, ProductData.TYPE_UINT8);
        _flagsBand.setDescription("Aerosol specific flags");
        _flagsBand.setSampleCoding(cloudFlagCoding);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        // OLD Beam:
//        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
        // todo: make sure this is ok:
        final GeoCoding geoCoding = sourceProduct.getBandAt(0).getGeoCoding();
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixelPos = new PixelPos();
        final FractIndex[] indexes = FractIndex.createArray(3);

        Tile aot470Tile = targetTiles.get(_aot470Band);
        Tile aot660Tile = targetTiles.get(_aot660Band);
        Tile angTile = targetTiles.get(_angstrBand);
        Tile flagTile = targetTiles.get(_flagsBand);

        final double time = getSceneRasterMeanTime(sourceProduct).getMJD();
        final double logWavelengthDiff = Math.log(AOT_660_WAVELENGTH) - Math.log(AOT_470_WAVELENGTH);
        for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                pixelPos.setLocation(x, y);
                geoCoding.getGeoPos(pixelPos, geoPos);
                float lon = (float) geoPos.getLon();
                float lat = (float) geoPos.getLat();

                Interp.interpCoord(time, _aot470LUT.getTab(0), indexes[0]);
                Interp.interpCoord(lat, _aot470LUT.getTab(1), indexes[1]);
                Interp.interpCoord(lon, _aot470LUT.getTab(2), indexes[2]);

                double aot470 = Interp.interpolate(_aot470LUT.getJavaArray(), indexes);
                aot470Tile.setSample(x, y, (float) aot470);
                double aot660 = Interp.interpolate(_aot660LUT.getJavaArray(), indexes);
                aot660Tile.setSample(x, y, (float) aot660);
                angTile.setSample(x, y, (float) ((Math.log(aot470) - Math.log(aot660)) / logWavelengthDiff));

                int xInt = indexes[2].index;
                int yInt = indexes[1].index;
                byte tFlag = 0;
                for (int prod = 0; prod < 2; prod++) {
                    if (gapFilled[prod][yInt][xInt]) {
                        tFlag += 1 << prod;
                    }
                }
                flagTile.setSample(x, y, tFlag);

            }
        }

    }

    private FlagCoding createFlagCoding(Product outputProduct) {
        MetadataAttribute cloudAttr;
        final FlagCoding flagCoding = new FlagCoding(BAND_NAME_FLAGS);
        flagCoding.setDescription("Cloud Flag Coding");

        int index = 0;
        int w = outputProduct.getSceneRasterWidth();
        int h = outputProduct.getSceneRasterHeight();
        Mask mask;

        cloudAttr = new MetadataAttribute("aerosol_lower_filled", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(1);
        cloudAttr.setDescription("the lower aerosol source has been filled at this position");
        flagCoding.addAttribute(cloudAttr);

        mask = Mask.BandMathsType.create(cloudAttr.getName(),
                                         cloudAttr.getDescription(), w, h,
                                         flagCoding.getName() + "." + cloudAttr.getName(),
                                         createBitmaskColor(1, 3), 0.5f);
        outputProduct.getMaskGroup().add(index++, mask);


        cloudAttr = new MetadataAttribute("aerosol_upper_filled", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(2);
        cloudAttr.setDescription("the upper aerosol source has been filled at this position");
        flagCoding.addAttribute(cloudAttr);
        mask = Mask.BandMathsType.create(cloudAttr.getName(),
                                         cloudAttr.getDescription(), w, h,
                                         flagCoding.getName() + "." + cloudAttr.getName(),
                                         createBitmaskColor(2, 3), 0.5f);
        outputProduct.getMaskGroup().add(index++, mask);


        cloudAttr = new MetadataAttribute("aerosol_both_filled", ProductData.TYPE_UINT8);
        cloudAttr.getData().setElemInt(4);
        cloudAttr.setDescription("the lower and the upper aerosol source have been filled at this position");
        flagCoding.addAttribute(cloudAttr);
        mask = Mask.BandMathsType.create(cloudAttr.getName(),
                                         cloudAttr.getDescription(), w, h,
                                         flagCoding.getName() + "." + cloudAttr.getName(),
                                         createBitmaskColor(3, 3), 0.5f);
        outputProduct.getMaskGroup().add(index, mask);

        return flagCoding;
    }

    /**
     * Creates a new color object to be used in the bitmaskDef.
     * The given indices start with 1.
     */
    private Color createBitmaskColor(int index, int maxIndex) {
        final double rf1 = 0.3;
        final double gf1 = 0.5;
        final double bf1 = 1.0;

        final double a = 2 * Math.PI * index / maxIndex;

        return new Color((float) (0.5 + 0.5 * Math.sin(a + rf1 * Math.PI)),
                         (float) (0.5 + 0.5 * Math.sin(a + gf1 * Math.PI)),
                         (float) (0.5 + 0.5 * Math.sin(a + bf1 * Math.PI)));
    }

    private void readMod08() throws IOException {
        final ProductData.UTC meanTime = getSceneRasterMeanTime(sourceProduct);
        if (meanTime == null) {
            throw new IOException("failed to retrieve sensing time information from input product");
        }
        final Date sensingDate = meanTime.getAsDate();

        final TemporalFile[] mod08Files = mod08FileArray.getTemporalFilesSorted(sensingDate, 2);
        gapFilled = new boolean[4][MOD08_HEIGHT][MOD08_WIDTH];
        final float[][][] aot470Data = createFromAuxData(mod08Files, 0);
        final float[][][] aot660Data = createFromAuxData(mod08Files, 2);

        final double[] timeTab = new double[2];
        timeTab[0] = ProductData.UTC.create(mod08Files[1].getMeanDate(), 0).getMJD();
        timeTab[1] = ProductData.UTC.create(mod08Files[0].getMeanDate(), 0).getMJD();

        final double[] latTab = new double[MOD08_HEIGHT];
        for (int i = 0; i < latTab.length; i++) {
            latTab[i] = 90.0 - i - 0.5;
        }
        final double[] lonTab = new double[MOD08_WIDTH];
        for (int i = 0; i < lonTab.length; i++) {
            lonTab[i] = i - 180.0 + 0.5;
        }

        _aot470LUT = new LUT(aot470Data);
        _aot470LUT.setTab(0, timeTab);
        _aot470LUT.setTab(1, latTab);
        _aot470LUT.setTab(2, lonTab);

        _aot660LUT = new LUT(aot660Data);
        _aot660LUT.setTab(0, timeTab);
        _aot660LUT.setTab(1, latTab);
        _aot660LUT.setTab(2, lonTab);
    }

    private static ProductData.UTC getSceneRasterMeanTime(Product product) {
        final ProductData.UTC startTime = product.getStartTime();
        final ProductData.UTC stopTime = product.getEndTime();
        final ProductData.UTC meanTime;
        if (startTime != null && stopTime != null) {
            meanTime = new ProductData.UTC(0.5 * (startTime.getMJD() + stopTime.getMJD()));
        } else if (startTime != null) {
            meanTime = startTime;
        } else if (stopTime != null) {
            meanTime = stopTime;
        } else {
            meanTime = null;
        }
        return meanTime;
    }

    private float[][][] createFromAuxData(TemporalFile[] modFiles, int index) throws IOException {
        final short[][][] mod08Data = new short[modFiles.length][MOD08_HEIGHT][MOD08_WIDTH];
        final float[][][] mod08DataFloat = new float[modFiles.length][MOD08_HEIGHT][MOD08_WIDTH];
        for (int i = 0; i < modFiles.length; i++) {
            readHDFdata(modFiles[i].getFile(), MOD08_BAND_NAME, index, mod08Data[i]);
            for (int y = 0; y < MOD08_HEIGHT; y++) {
                for (int x = 0; x < MOD08_WIDTH; x++) {
                    short unscaled = mod08Data[i][y][x];
                    if (unscaled != AEROSOL_GAP_VALUE) {
                        mod08DataFloat[i][y][x] = unscaled * SCALE_FACTOR;
                    } else {
                        mod08DataFloat[i][y][x] = 0.0f;
                    }
                }
            }
            gapFiller.findGaps(mod08DataFloat[i], 0.0f);
            gapFiller.fillGaps(mod08DataFloat[i], gapFilled[index + i]);
        }
        return mod08DataFloat;
    }


    private static void readHDFdata(final File file, final String datasetName, final int index, final short[][] array) throws IOException {
        final int sdId;
        try {
            sdId = HDFLibrary.SDstart(file.getPath(), HDFConstants.DFACC_RDONLY);
        } catch (HDFException e) {
            throw new IOException("File '" + file + "': HDF I/O exception: " + e.getMessage());
        }
        try {
            int sdIndex = HDFLibrary.SDnametoindex(sdId, datasetName);
            if (sdIndex == -1) {
                sdIndex = HDFLibrary.SDnametoindex(sdId, datasetName.toUpperCase());
                if (sdIndex == -1) {
                    throw new IOException("Couldn't find dataset: " + datasetName);
                }
            }
            int sdsId = HDFLibrary.SDselect(sdId, sdIndex);
            int[] sdDimensions = new int[]{1, MOD08_HEIGHT, MOD08_WIDTH};
            int[] start = new int[]{index, 0, 0};
            int[] stride = new int[]{1, 1, 1};
            HDFLibrary.SDreaddata(sdsId, start, stride, sdDimensions, array);
            HDFLibrary.SDendaccess(sdsId);
        } catch (HDFException e) {
            throw new IOException("File '" + file + "': HDF I/O exception: " + e.getMessage());
        } finally {
            try {
                HDFLibrary.SDend(sdId);
            } catch (HDFException ignored) {
            }
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ModisAerosolOp.class);
        }
    }
}
