/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.s3tbx.aerosol;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s3tbx.aerosol.lut.Luts;
import org.esa.s3tbx.aerosol.lut.MomoLut;
import org.esa.s3tbx.aerosol.math.BrentFitFunction;
import org.esa.s3tbx.aerosol.util.AerosolUtils;
import org.esa.s3tbx.aerosol.util.PixelGeometry;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.math.RsMathUtils;

import javax.imageio.stream.ImageInputStream;
import javax.media.jai.BorderExtender;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates currently 2 bands: aot and aot_err on lower spatial resolution
 * rasterSize of target is 1/scale of source
 *
 * @author akheckel
 */
@OperatorMetadata(alias = "AerosolRetrieval.Aerosol",
        description = "Computes aerosol optical thickness",
        authors = "Andreas Heckel, Olaf Danne, Marco Zuehlke",
        version = "1.0",
        internal = true,
        copyright = "(C) 2010, 2016 by University Swansea (a.heckel@swansea.ac.uk) and Brockmann Consult")
public class AerosolOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "2")
    private int vegSpecId;

    @Parameter(defaultValue = "1")
    private int soilSpecId;

    @Parameter(defaultValue = "0.2")
    private float ndviThreshold;

    @Parameter(defaultValue = "9")
    private int scale;

    private static final float WV_COL = 2.5f;

    private String SurfaceSpecName = "surface_reflectance_spec.asc";

    private String productName;
    private String productType;

    private InstrumentConsts instrC;
    private String instrument;
    private String[] specBandNames;
    private String[] geomBandNames;
    private String[] auxBandNames;
    private String surfPresName;
    private String ozoneName;

    private String validName;

    private String ndviName;

    private int srcRasterWidth;
    private int srcRasterHeight;
    private int tarRasterWidth;
    private int tarRasterHeight;
    private int nSpecWvl;
    private float[][] specWvl;
    private double[] soilSurfSpec;
    private double[] vegSurfSpec;
    private double[] specWeights;
    private MomoLut momo;
    private Band validBand;
    private BorderExtender borderExt;
    private Rectangle pixelWindow;
    private Band aotBand;
    private Band aotErrorBand;
    private Band latBand;


    private boolean addFitBands = false;
    private Map<String, Double> sourceNoDataValues;

    @Override
    public void initialize() throws OperatorException {
        productName = sourceProduct.getName() + "_AOT";
        productType = sourceProduct.getProductType() + "_AOT";
        initRasterDimensions(sourceProduct, scale);
        instrC = InstrumentConsts.getInstance();
        instrument = instrC.getInstrument(sourceProduct);

        geomBandNames = instrC.getGeomBandNames(instrument);
        specBandNames = instrC.getSpecBandNames(instrument);
        ozoneName = instrC.getOzoneName(instrument);
        surfPresName = instrC.getSurfPressureName(instrument);
        ndviName = instrC.getNdviName();

        final String validExpression = instrC.getValidRetrievalExpression(instrument);
        validBand = AerosolUtils.createBooleanExpressionBand(validExpression, sourceProduct);
        validName = validBand.getName();

        auxBandNames = new String[]{surfPresName, ozoneName, validName, ndviName};

        specWeights = instrC.getSpectralFitWeights(instrument);
        specWvl = getSpectralWvl(specBandNames);
        nSpecWvl = specWvl[0].length;
        readSurfaceSpectra(SurfaceSpecName);

        if (!sourceProduct.containsRasterDataNode(ozoneName)) {
            createConstOzoneBand(0.35f);
        }
        if (!sourceProduct.containsBand(ndviName)) {
            createNdviBand();
        }

        sourceNoDataValues = getSourceNoDataValues(geomBandNames);
        sourceNoDataValues.putAll(getSourceNoDataValues(specBandNames));
        sourceNoDataValues.putAll(getSourceNoDataValues(auxBandNames));

        try {
            readLookupTable();
        } catch (IOException e) {
            throw new OperatorException("Failed to read LUTs. " + e.getMessage(), e);
        }

        borderExt = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
        pixelWindow = new Rectangle(0, 0, scale, scale);

        createTargetProduct();
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Rectangle srcRec = getSourceRectangle(targetRectangle, pixelWindow);

        if (!containsTileValidData(srcRec)) {
            setInvalidTargetSamples(targetTiles);
            return;
        }

        Map<String, Tile> sourceTiles = getSourceTiles(geomBandNames, srcRec, borderExt);
        sourceTiles.putAll(getSourceTiles(specBandNames, srcRec, borderExt));
        sourceTiles.putAll(getSourceTiles(auxBandNames, srcRec, borderExt));

        int x0 = (int) targetRectangle.getX();
        int y0 = (int) targetRectangle.getY();
        int width = (int) targetRectangle.getWidth() + x0 - 1;
        int height = (int) targetRectangle.getHeight() + y0 - 1;

        for (int iY = y0; iY <= height; iY++) {
            checkForCancellation();
            for (int iX = x0; iX <= width; iX++) {
                processSuperPixel(sourceTiles, iX, iY, targetTiles);
            }
            pm.worked(1);
        }
        pm.done();
    }

    private void processSuperPixel(Map<String, Tile> sourceTiles, int iX, int iY, Map<Band, Tile> targetTiles) {
        // read pixel data and init brent fit
        InputPixelData[] inPixField;
        BrentFitFunction brentFitFunction = null;
        if (instrument.equals("AATSR")) {
            inPixField = readAveragePixel(sourceTiles, iX, iY, pixelWindow);
            if (inPixField != null) {
                brentFitFunction = new BrentFitFunction(BrentFitFunction.ANGULAR_MODEL, inPixField, momo, specWeights);
            }
        } else {
            inPixField = readDarkestNPixels(sourceTiles, iX, iY, pixelWindow);
            if (inPixField != null) {
                brentFitFunction = new BrentFitFunction(BrentFitFunction.SPECTRAL_MODEL, inPixField, momo, specWeights, soilSurfSpec, vegSurfSpec);
            }
        }
        retrieveAndSetTarget(inPixField, brentFitFunction, targetTiles, iX, iY);
    }

    private void retrieveAndSetTarget(InputPixelData[] inPixField, BrentFitFunction brentFitFunction, Map<Band, Tile> targetTiles, int iX, int iY) {
        // run retrieval and set target samples
        if (inPixField != null) {
            RetrievalResults result = executeRetrieval(brentFitFunction);
            if (!result.isRetrievalFailed()) {
                setTargetSamples(targetTiles, iX, iY, result);
            } else {
                setInvalidTargetSamples(targetTiles, iX, iY);
            }
        } else {
            setInvalidTargetSamples(targetTiles, iX, iY);
        }
    }

    private RetrievalResults executeRetrieval(BrentFitFunction brentFitFunction) {
        final double maxAOT = brentFitFunction.getMaxAOT();
        final PointRetrieval pR = new PointRetrieval(brentFitFunction);
        return pR.runRetrieval(maxAOT);
    }

    private InputPixelData createInPixelData(double[] tileValues) {
        final PixelGeometry geomNadir = new PixelGeometry(tileValues[0], tileValues[1], tileValues[2], tileValues[3]);

        double[][] toaRefl = new double[2][nSpecWvl];
        int skip = 4;

        for (int i = 0; i < nSpecWvl; i++) {
            toaRefl[0][i] = tileValues[skip + i];
            toaRefl[1][i] = tileValues[skip + i];
        }
        skip += nSpecWvl;
        if (instrument.equals("AATSR")) {
            for (int i = 0; i < nSpecWvl; i++) {
                toaRefl[0][i] = RsMathUtils.radianceToReflectance((float) toaRefl[0][i], geomNadir.sza, (float) Math.PI) * 0.01f;
                toaRefl[1][i] = tileValues[skip + i];
                toaRefl[1][i] = RsMathUtils.radianceToReflectance((float) toaRefl[1][i], geomNadir.sza, (float) Math.PI) * 0.01f;
            }
            skip += nSpecWvl;
        }
        double surfP = Math.min(tileValues[skip], 1013.25);
        double o3DU = ensureO3DobsonUnits(tileValues[skip + 1]);
        return new InputPixelData(geomNadir, geomNadir, surfP, o3DU, WV_COL, specWvl[0], toaRefl[0], toaRefl[1]);
    }

    private void createTargetProduct() {
        targetProduct = new Product(productName, productType, tarRasterWidth, tarRasterHeight);
        createTargetProductBands();
        setTargetProduct(targetProduct);
    }

    private void createTargetProductBands() {
        aotBand = AerosolUtils.createTargetBand(AotConsts.aot, tarRasterWidth, tarRasterHeight);
        targetProduct.addBand(aotBand);

        aotErrorBand = AerosolUtils.createTargetBand(AotConsts.aotErr, tarRasterWidth, tarRasterHeight);
        aotErrorBand.setValidPixelExpression(instrC.getValidRetrievalExpression(instrument));
        targetProduct.addBand(aotErrorBand);
        latBand = new Band("latitude", ProductData.TYPE_FLOAT32, tarRasterWidth, tarRasterHeight);
        targetProduct.addBand(latBand);

        if (addFitBands) {
            Band targetBand = new Band("fit_err", ProductData.TYPE_FLOAT32, tarRasterWidth, tarRasterHeight);
            targetBand.setDescription("aot uncertainty");
            targetBand.setNoDataValue(-1);
            targetBand.setNoDataValueUsed(true);
            targetBand.setValidPixelExpression("");
            targetBand.setUnit("dl");
            targetProduct.addBand(targetBand);

            targetBand = new Band("fit_curv", ProductData.TYPE_FLOAT32, tarRasterWidth, tarRasterHeight);
            targetBand.setDescription("aot uncertainty");
            targetBand.setNoDataValue(-1);
            targetBand.setNoDataValueUsed(true);
            targetBand.setValidPixelExpression("");
            targetBand.setUnit("dl");
            targetProduct.addBand(targetBand);
        }
    }

    private Rectangle getSourceRectangle(Rectangle targetRectangle, Rectangle pixelWindow) {
        return new Rectangle(targetRectangle.x * pixelWindow.width + pixelWindow.x,
                             targetRectangle.y * pixelWindow.height + pixelWindow.y,
                             targetRectangle.width * pixelWindow.width,
                             targetRectangle.height * pixelWindow.height);
    }

    private void initRasterDimensions(Product sourceProduct, int scale) {
        srcRasterHeight = sourceProduct.getSceneRasterHeight();
        srcRasterWidth = sourceProduct.getSceneRasterWidth();
        tarRasterHeight = srcRasterHeight / scale;
        tarRasterWidth = srcRasterWidth / scale;
    }

    private Map<String, Tile> getSourceTiles(String[] bandNames, Rectangle srcRec, BorderExtender borderExt) {
        Map<String, Tile> tileMap = new HashMap<>(bandNames.length);
        for (String name : bandNames) {
            RasterDataNode b = (name.equals(validName)) ? validBand : sourceProduct.getRasterDataNode(name);
            tileMap.put(name, getSourceTile(b, srcRec, borderExt));
        }
        return tileMap;
    }

    private Map<String, Double> getSourceNoDataValues(String[] bandNames) {
        Map<String, Double> noDataMap = new HashMap<>(bandNames.length);
        for (String name : bandNames) {
            RasterDataNode b = (name.equals(validName)) ? validBand : sourceProduct.getRasterDataNode(name);
            noDataMap.put(name, b.getGeophysicalNoDataValue());
        }
        return noDataMap;
    }

    private InputPixelData[] readAveragePixel(Map<String, Tile> sourceTiles, int iX, int iY, Rectangle pixelWindow) {
        InputPixelData[] inPixField = null;

        boolean valid = uniformityTest(sourceTiles, iX, iY);
        if (!valid) return null;

        double[] tileValues = new double[sourceTiles.size()];
        double[] sumVal = new double[sourceTiles.size()];
        for (int k = 0; k < sumVal.length; k++) sumVal[k] = 0;
        int nAve = 0;
        int xOffset = iX * pixelWindow.width + pixelWindow.x;
        int yOffset = iY * pixelWindow.height + pixelWindow.y;
        for (int y = yOffset; y < yOffset + pixelWindow.height; y++) {
            for (int x = xOffset; x < xOffset + pixelWindow.width; x++) {
                valid = valid && sourceTiles.get(validName).getSampleBoolean(x, y);
                if (valid) {
                    valid = readAllValues(x, y, sourceTiles, tileValues);
                    if (valid) {
                        addToSumArray(tileValues, sumVal);
                        nAve++;
                    }
                }
            }
        }
        if (nAve > 0.95 * pixelWindow.width * pixelWindow.height) {
            for (int i = 0; i < sumVal.length; i++) sumVal[i] /= nAve;
            InputPixelData ipd = createInPixelData(sumVal);
            if (momo.isInsideLut(ipd)) {
                inPixField = new InputPixelData[]{ipd};
            }
        }
        return inPixField;
    }

    private InputPixelData[] readDarkestNPixels(Map<String, Tile> sourceTiles, int iX, int iY, Rectangle pixelWindow) {
        boolean valid = uniformityTest(sourceTiles, iX, iY);
        if (!valid) return null;

        int NPixel = 10;
        ArrayList<InputPixelData> inPixelList = new ArrayList<>(pixelWindow.height * pixelWindow.width);
        InputPixelData[] inPixField = null;
        float ndvi;
        float[] ndviArr = new float[pixelWindow.height * pixelWindow.width];

        double[] tileValues = new double[sourceTiles.size()];

        int nValid = 0;
        int xOffset = iX * pixelWindow.width + pixelWindow.x;
        int yOffset = iY * pixelWindow.height + pixelWindow.y;
        for (int y = yOffset; y < yOffset + pixelWindow.height; y++) {
            for (int x = xOffset; x < xOffset + pixelWindow.width; x++) {
                valid = sourceTiles.get(validName).getSampleBoolean(x, y);
                ndviArr[(y - yOffset) * pixelWindow.width + (x - xOffset)] = (valid) ? sourceTiles.get(ndviName).getSampleFloat(x, y) : -1;
                if (valid) nValid++;
            }
        }

        // return null if not enough valid pixels
        if (nValid < 0.95 * pixelWindow.width * pixelWindow.height) return null;

        Arrays.sort(ndviArr);
        if (ndviArr[ndviArr.length - 10 - NPixel] > ndviThreshold) {
            for (int y = yOffset; y < yOffset + pixelWindow.height; y++) {
                for (int x = xOffset; x < xOffset + pixelWindow.width; x++) {
                    valid = sourceTiles.get(validName).getSampleBoolean(x, y);
                    ndvi = sourceTiles.get(ndviName).getSampleFloat(x, y);
                    if (valid && (ndvi >= ndviArr[ndviArr.length - 10 - NPixel])
                            && (ndvi <= ndviArr[ndviArr.length - 1 - NPixel])) {
                        valid = readAllValues(x, y, sourceTiles, tileValues);
                        InputPixelData ipd = createInPixelData(tileValues);
                        if (valid && momo.isInsideLut(ipd)) {
                            inPixelList.add(ipd);
                        }
                    }
                }
            }
            if (inPixelList.size() > 3) {
                inPixField = new InputPixelData[inPixelList.size()];
                inPixelList.toArray(inPixField);
            }
        }
        return inPixField;
    }

    private void setTargetSamples(Map<Band, Tile> targetTiles, int iX, int iY, RetrievalResults result) {

        float[] latLon = getLatLon(iX, iY, pixelWindow, sourceProduct);
        targetTiles.get(latBand).setSample(iX, iY, latLon[0]);

        targetTiles.get(aotBand).setSample(iX, iY, result.getOptAOT());
        targetTiles.get(aotErrorBand).setSample(iX, iY, result.getRetrievalErr());
        if (addFitBands) {
            targetTiles.get(targetProduct.getBand("fit_err")).setSample(iX, iY, result.getOptErr());
            targetTiles.get(targetProduct.getBand("fit_curv")).setSample(iX, iY, result.getCurvature());
        }
    }

    private void setInvalidTargetSamples(Map<Band, Tile> targetTiles, int iX, int iY) {
        float[] latLon = getLatLon(iX, iY, pixelWindow, sourceProduct);
        for (Tile t : targetTiles.values()) {
            if (t.getRasterDataNode() == latBand) {
                targetTiles.get(targetProduct.getBand("latitude")).setSample(iX, iY, latLon[0]);
            } else {
                t.setSample(iX, iY, t.getRasterDataNode().getNoDataValue());
            }
        }
    }

    private void setInvalidTargetSamples(Map<Band, Tile> targetTiles) {
        for (Tile.Pos pos : targetTiles.get(targetProduct.getBandAt(0))) {
            setInvalidTargetSamples(targetTiles, pos.x, pos.y);
        }
    }

    private void createConstOzoneBand(float ozonAtmCm) {
        String ozoneExpr = String.format("%5.3f", ozonAtmCm);
        VirtualBand ozoneBand = new VirtualBand(ozoneName, ProductData.TYPE_FLOAT32, srcRasterWidth, srcRasterHeight, ozoneExpr);
        ozoneBand.setDescription("constant ozone band");
        ozoneBand.setNoDataValue(0);
        ozoneBand.setNoDataValueUsed(true);
        ozoneBand.setUnit("atm.cm");
        sourceProduct.addBand(ozoneBand);
    }

    private void createNdviBand() {
        VirtualBand ndviBand = new VirtualBand(ndviName, ProductData.TYPE_FLOAT32,
                                               srcRasterWidth, srcRasterHeight,
                                               instrC.getNdviExpression(instrument));
        sourceProduct.addBand(ndviBand);
    }

    private void readSurfaceSpectra(String fname) {
        Guardian.assertNotNull("specWvl", specWvl);
        final InputStream inputStream = AerosolOp.class.getResourceAsStream(fname);
        Guardian.assertNotNull("surface spectra InputStream", inputStream);
        BufferedReader reader;
        reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        float[] fullWvl = new float[10000];
        float[] fullSoil = new float[10000];
        float[] fullVeg = new float[10000];
        int nWvl = 0;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!(line.isEmpty() || line.startsWith("#") || line.startsWith("*"))) {
                    String[] stmp = line.split("[ \t]+");
                    fullWvl[nWvl] = Float.valueOf(stmp[0]);
                    if (fullWvl[nWvl] < 100) fullWvl[nWvl] *= 1000; // conversion from um to nm
                    fullSoil[nWvl] = Float.valueOf(stmp[this.soilSpecId]);
                    fullVeg[nWvl] = Float.valueOf(stmp[this.vegSpecId]);
                    nWvl++;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AerosolOp.class.getName()).log(Level.SEVERE, null, ex);
            throw new OperatorException(ex.getMessage(), ex.getCause());
        }

        soilSurfSpec = new double[nSpecWvl];
        vegSurfSpec = new double[nSpecWvl];
        int j = 0;
        for (int i = 0; i < nSpecWvl; i++) {
            float wvl = specWvl[0][i];
            float width = specWvl[1][i];
            int count = 0;
            while (j < nWvl && fullWvl[j] < wvl - width / 2) j++;
            if (j == nWvl) throw new OperatorException("wavelength not found reading surface spectra");
            while (fullWvl[j] < wvl + width / 2) {
                soilSurfSpec[i] += fullSoil[j];
                vegSurfSpec[i] += fullVeg[j];
                count++;
                j++;
            }
            if (j == nWvl) throw new OperatorException("wavelength window exceeds surface spectra range");
            if (count > 0) {
                soilSurfSpec[i] /= count;
                vegSurfSpec[i] /= count;
            }
        }
    }

    private void readLookupTable() throws IOException {
        final String lutInstrument = instrument.equals("PROBAV") ? "VGT" : instrument;
        ImageInputStream aotIis = Luts.getAotLutData(lutInstrument);
        ImageInputStream gasIis = Luts.getCwvLutData(lutInstrument);
        int nLutBands = InstrumentConsts.getInstance().getnLutBands(instrument);
        momo = new MomoLut(aotIis, gasIis, nLutBands);
    }

    private float[][] getSpectralWvl(String[] bandNames) {
        int nBands = (instrument.equals("AATSR")) ? 4 : bandNames.length;
        float[][] wvl = new float[2][nBands];
        for (int i = 0; i < nBands; i++) {
            wvl[0][i] = sourceProduct.getBand(bandNames[i]).getSpectralWavelength();
            wvl[1][i] = sourceProduct.getBand(bandNames[i]).getSpectralBandwidth();
        }
        return wvl;
    }

    private boolean readAllValues(int x, int y, Map<String, Tile> sourceTiles, double[] tileValues) {
        boolean valid = true;
        for (int i = 0; i < geomBandNames.length; i++) {
            tileValues[i] = sourceTiles.get(geomBandNames[i]).getSampleDouble(x, y);
            valid = valid && (sourceNoDataValues.get(geomBandNames[i]).compareTo(tileValues[i]) != 0);

        }
        int skip = geomBandNames.length;
        for (int i = 0; i < specBandNames.length; i++) {
            tileValues[i + skip] = sourceTiles.get(specBandNames[i]).getSampleDouble(x, y);
            valid = valid && (sourceNoDataValues.get(specBandNames[i]).compareTo(tileValues[i + skip]) != 0);
        }
        skip += specBandNames.length;

        // surface pressure data
        tileValues[skip] = sourceTiles.get(surfPresName).getSampleDouble(x, y);
        valid = valid && (sourceNoDataValues.get(surfPresName).compareTo(tileValues[skip]) != 0);

        // ozone data
        tileValues[skip + 1] = sourceTiles.get(ozoneName).getSampleDouble(x, y);
        valid = valid && (sourceNoDataValues.get(ozoneName).compareTo(tileValues[skip + 1]) != 0);

        return valid;
    }

    private void addToSumArray(double[] tileValues, double[] sumVal) {
        for (int i = 0; i < sumVal.length; i++) {
            sumVal[i] += tileValues[i];
        }
    }

    //
//     verify that ozone column is in DU
//     function assumes 2 common possibilities:
//         1. Ozone column being in DU (generally 100 < ozDU < 1000)
//         2. Ozone column being in atm.cm (generally < 1 )
//     conversion factor is 1000
//     return ozone column in [DU]
//
    private double ensureO3DobsonUnits(double ozoneColumn) {
        return (ozoneColumn < 1) ? ozoneColumn * 1000 : ozoneColumn;
    }

    //      Test whether @validBand contains any valid datapoint in the given source rectangle
    private boolean containsTileValidData(Rectangle srcRec) {
        Tile validTile = getSourceTile(validBand, srcRec);
        for (Tile.Pos pos : validTile) {
            if (validTile.getSampleBoolean(pos.x, pos.y)) {
                return true;
            }
        }
        return false;
    }

    //     Tests uniformity on the given bin pixel (e.g. 9x9 block)
//     based on the NIR reflectance (max - min < 0.2)
    private boolean uniformityTest(Map<String, Tile> sourceTiles, int iX, int iY) {
        String nirName = instrC.getNirName(instrument);
        Guardian.assertNotNullOrEmpty("nirName is empty", nirName);
        double nan = sourceNoDataValues.get(nirName);
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        int xOffset = iX * pixelWindow.width + pixelWindow.x;
        int yOffset = iY * pixelWindow.height + pixelWindow.y;
        for (int y = yOffset; y < yOffset + pixelWindow.height; y++) {
            for (int x = xOffset; x < xOffset + pixelWindow.width; x++) {
                boolean valid = sourceTiles.get(validName).getSampleBoolean(x, y);
                double value = sourceTiles.get(nirName).getSampleDouble(x, y);
                if (valid && Double.compare(nan, value) != 0) {
                    if (value < min) min = value;
                    if (value > max) max = value;
                }
            }
        }
        return ((max - min) < 20);
    }

    private float[] getLatLon(int iX, int iY, Rectangle pixelWindow, Product sourceProduct) {
        float xOffset = ((iX + 0.5f) * pixelWindow.width + pixelWindow.x);
        float yOffset = ((iY + 0.5f) * pixelWindow.height + pixelWindow.y);
        GeoCoding geoCoding = sourceProduct.getSceneGeoCoding();
        GeoPos geoPos = geoCoding.getGeoPos(new PixelPos(xOffset, yOffset), null);
        return new float[]{(float) geoPos.lat, (float) geoPos.lon};
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.snap.core.gpf.OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(AerosolOp.class);
        }
    }
}
