package org.esa.s3tbx.idepix.algorithms.landsat8;

/**
 * IDEPIX instrument-specific pixel identification algorithm for Landsat 8
 *
 * @author olafd
 */
public class Landsat8Algorithm implements Landsat8PixelProperties {

    private static final int NN_CATEGORY_CLEAR_SKY = 1;
    private static final int NN_CATEGORY_NON_CLEAR_SKY = 3;
    private static final int NN_CATEGORY_CLOUD = 4;
    private static final int NN_CATEGORY_CLEAR_SKY_SNOW_ICE = 5;

    private float[] l8SpectralBandData;

    private int brightnessBandLand;
    private float brightnessThreshLand;
    private int brightnessBand1Water;
    private float brightnessWeightBand1Water;
    private int brightnessBand2Water;
    private float brightnessWeightBand2Water;
    private float brightnessThreshWater;
    private int whitenessBand1Land;
    private int whitenessBand2Land;
    private float whitenessThreshLand;
    private int whitenessBand1Water;
    private int whitenessBand2Water;
    private float whitenessThreshWater;

    private boolean isLand;
    private boolean isInvalid;
    private boolean applyShimezCloudTest;
    private float shimezDiffThresh;
    private float shimezMeanThresh;
    private boolean applyHotCloudTest;
    private float hotThresh;
    private double clostThresh;
    private boolean applyClostCloudTest;
    private float clostValue;
    private float otsuValue;
    private boolean applyOtsuCloudTest;
    private double[] nnResult;
    private double darkGlintThresholdTest1;
    private double darkGlintThresholdTest2;
    private int darkGlintThresholdTest1Wvl;
    private int darkGlintThresholdTest2Wvl;

    double nnCloudAmbiguousLowerBoundaryValue;
    double nnCloudAmbiguousSureSeparationValue;
    double nnCloudSureSnowSeparationValue;

    @Override
    public boolean isInvalid() {
        return isInvalid;
    }

    @Override
    public boolean isCloud() {
        return isCloudSure();
    }

    @Override
    public boolean isCloudAmbiguous() {
        // todo: discuss logic, then apply separation values from new NNs, 20151119:
        // for the moment, just return the 'non clear sky' from the new NN, no other tests
        return !isInvalid() && getNnClassification()[0] == Landsat8Algorithm.NN_CATEGORY_NON_CLEAR_SKY;
    }

    @Override
    public boolean isCloudSure() {
        // todo: discuss logic of cloudSure
        // todo: also discuss if Michael's tests shall be fully replaced by Schiller NN when available
        final boolean isCloudShimez = applyShimezCloudTest && isCloudShimez();
//        final boolean isCloudHot = applyHotCloudTest && isCloudHot();
        final boolean isCloudClost = applyClostCloudTest && isCloudClost();
        final boolean isCloudOtsu = applyOtsuCloudTest && isCloudOtsu();
//        return !isInvalid() && isBright() && isWhite() && (isCloudShimez || isCloudHot);
        boolean nnCloud = nnResult[0] == NN_CATEGORY_CLOUD;
        return !isInvalid() && (isCloudShimez || isCloudClost || isCloudOtsu || (nnCloud && !isLand()));
    }

    public void setNnResult(double[] nnResult) {
        this.nnResult = nnResult;
    }

    public double[] getNnResult() {
        return nnResult;
    }

    public int[] getNnClassification() {
        return classifyNNResult(nnResult);
        // todo: discuss logic, then apply separation values from new NNs, 20151119
    }

    public boolean isCloudShimez() {
        // make sure we have reflectances here!!

        // this is the latest correction from MPa , 20150330:
//        abs(blue/red-1)<A &&
//                abs(blue/green-1)<A &&
//                abs(red/green-1)<A &&
//                (red+green+blue)/3 > 0.35
//                  A = 0.1 over the day
//                  A = 0.2 if twilight

        final double blueGreenRatio = l8SpectralBandData[1] / l8SpectralBandData[2];
        final double redGreenRatio = l8SpectralBandData[3] / l8SpectralBandData[2];
        final double mean = (l8SpectralBandData[1] + l8SpectralBandData[2] + l8SpectralBandData[3]) / 3.0;

        return Math.abs(blueGreenRatio - 1.0) < shimezDiffThresh &&
                Math.abs(redGreenRatio - 1.0) < shimezDiffThresh &&
                mean > shimezMeanThresh;
    }

    public boolean isCloudHot() {
        final double hot = l8SpectralBandData[1] - 0.5 * l8SpectralBandData[3];
        return hot > hotThresh;
    }

    public boolean isCloudClost() {
        if (applyOtsuCloudTest) {
            return clostValue > clostThresh;
        } else {
            final double clost = l8SpectralBandData[0] * l8SpectralBandData[1] * l8SpectralBandData[7] * l8SpectralBandData[8];
            return clost > clostThresh;
        }
    }

    public boolean isCloudOtsu() {
        // todo
        return otsuValue > 128;
    }

    @Override
    public boolean isCloudBuffer() {
        return false;   // in post-processing
    }

    @Override
    public boolean isCloudShadow() {
        return false;  // in post-processing when defined
    }

    @Override
    public boolean isSnowIce() {
        return nnResult[0] == NN_CATEGORY_CLEAR_SKY_SNOW_ICE;
    }

    @Override
    public boolean isGlintRisk() {
        return false;  // no way to compute?!
    }

    @Override
    public boolean isCoastline() {
        return false;
    }

    @Override
    public boolean isLand() {
        return isLand;
    }

    @Override
    public boolean isBright() {
        if (isLand()) {
            final Integer landBandIndex = Landsat8Constants.LANDSAT8_SPECTRAL_WAVELENGTH_MAP.get(brightnessBandLand);
            return !isInvalid() && (l8SpectralBandData[landBandIndex] > brightnessThreshLand);
        } else {
            final Integer waterBand1Index = Landsat8Constants.LANDSAT8_SPECTRAL_WAVELENGTH_MAP.get(brightnessBand1Water);
            final Integer waterBand2Index = Landsat8Constants.LANDSAT8_SPECTRAL_WAVELENGTH_MAP.get(brightnessBand2Water);
            final float brightnessWaterValue = brightnessWeightBand1Water * l8SpectralBandData[waterBand1Index] +
                    brightnessWeightBand2Water * l8SpectralBandData[waterBand2Index];
            return !isInvalid() && (brightnessWaterValue > brightnessThreshWater);
        }
    }

    @Override
    public boolean isWhite() {
        if (isLand()) {
            final Integer whitenessBand1Index = Landsat8Constants.LANDSAT8_SPECTRAL_WAVELENGTH_MAP.get(whitenessBand1Land);
            final Integer whitenessBand2Index = Landsat8Constants.LANDSAT8_SPECTRAL_WAVELENGTH_MAP.get(whitenessBand2Land);
            final float whiteness = l8SpectralBandData[whitenessBand1Index] / l8SpectralBandData[whitenessBand2Index];
            return !isInvalid() && (whiteness < whitenessThreshLand);
        } else {
            final Integer whitenessBand1Index = Landsat8Constants.LANDSAT8_SPECTRAL_WAVELENGTH_MAP.get(whitenessBand1Water);
            final Integer whitenessBand2Index = Landsat8Constants.LANDSAT8_SPECTRAL_WAVELENGTH_MAP.get(whitenessBand2Water);
            final float whiteness = l8SpectralBandData[whitenessBand1Index] / l8SpectralBandData[whitenessBand2Index];
            return !isInvalid() && (whiteness < whitenessThreshWater);
        }
    }

    public boolean isDarkGlintTest1() {
        Integer darkGlintTest1Index = Landsat8Constants.LANDSAT8_SPECTRAL_WAVELENGTH_MAP.get(darkGlintThresholdTest1Wvl);
        return l8SpectralBandData[darkGlintTest1Index] > darkGlintThresholdTest1;
    }

    public boolean isDarkGlintTest2() {
        Integer darkGlintTest2Index = Landsat8Constants.LANDSAT8_SPECTRAL_WAVELENGTH_MAP.get(darkGlintThresholdTest2Wvl);
        return l8SpectralBandData[darkGlintTest2Index] > darkGlintThresholdTest2;

    }
    // setter methods

    public void setL8SpectralBandData(float[] l8SpectralBandData) {
        this.l8SpectralBandData = l8SpectralBandData;
    }

    public void setIsLand(boolean isLand) {
        this.isLand = isLand;
    }

    public void setInvalid(boolean isInvalid) {
        this.isInvalid = isInvalid;
    }

    public void setBrightnessBandLand(int brightnessBandLand) {
        this.brightnessBandLand = brightnessBandLand;
    }

    public void setBrightnessThreshLand(float brightnessThreshLand) {
        this.brightnessThreshLand = brightnessThreshLand;
    }

    public void setBrightnessBand1Water(int brightnessBand1Water) {
        this.brightnessBand1Water = brightnessBand1Water;
    }

    public void setBrightnessWeightBand1Water(float brightnessWeightBand1Water) {
        this.brightnessWeightBand1Water = brightnessWeightBand1Water;
    }

    public void setBrightnessBand2Water(int brightnessBand2Water) {
        this.brightnessBand2Water = brightnessBand2Water;
    }

    public void setBrightnessWeightBand2Water(float brightnessWeightBand2Water) {
        this.brightnessWeightBand2Water = brightnessWeightBand2Water;
    }

    public void setBrightnessThreshWater(float brightnessThreshWater) {
        this.brightnessThreshWater = brightnessThreshWater;
    }

    public void setWhitenessBand1Land(int whitenessBand1Land) {
        this.whitenessBand1Land = whitenessBand1Land;
    }

    public void setWhitenessBand2Land(int whitenessBand2Land) {
        this.whitenessBand2Land = whitenessBand2Land;
    }

    public void setWhitenessThreshLand(float whitenessThreshLand) {
        this.whitenessThreshLand = whitenessThreshLand;
    }

    public void setWhitenessThreshWater(float whitenessThreshWater) {
        this.whitenessThreshWater = whitenessThreshWater;
    }

    public void setWhitenessBand1Water(int whitenessBand1Water) {
        this.whitenessBand1Water = whitenessBand1Water;
    }

    public void setWhitenessBand2Water(int whitenessBand2Water) {
        this.whitenessBand2Water = whitenessBand2Water;
    }


    public void setApplyShimezCloudTest(boolean applyShimezCloudTest) {
        this.applyShimezCloudTest = applyShimezCloudTest;
    }

    public void setShimezDiffThresh(float shimezDiffThresh) {
        this.shimezDiffThresh = shimezDiffThresh;
    }

    public void setShimezMeanThresh(float shimezMeanThresh) {
        this.shimezMeanThresh = shimezMeanThresh;
    }

    public void setApplyHotCloudTest(boolean applyHotCloudTest) {
        this.applyHotCloudTest = applyHotCloudTest;
    }

    public void setHotThresh(float hotThresh) {
        this.hotThresh = hotThresh;
    }

    public void setClostThresh(double clostThresh) {
        this.clostThresh = clostThresh;
    }

    public void setApplyClostCloudTest(boolean applyClostCloudTest) {
        this.applyClostCloudTest = applyClostCloudTest;
    }

    public void setClostValue(float clostValue) {
        this.clostValue = clostValue;
    }

    public void setOtsuValue(float otsuValue) {
        this.otsuValue = otsuValue;
    }

    public void setApplyOtsuCloudTest(boolean applyOtsuCloudTest) {
        this.applyOtsuCloudTest = applyOtsuCloudTest;
    }

    public void setDarkGlintThresholdTest1(double darkThreshold865) {
        this.darkGlintThresholdTest1 = darkThreshold865;
    }

    public void setDarkGlintThresholdTest1Wvl(int darkGlintThresholdTest1Wvl) {
        this.darkGlintThresholdTest1Wvl = darkGlintThresholdTest1Wvl;
    }

    public void setDarkGlintThresholdTest2(double darkThreshold1610) {
        this.darkGlintThresholdTest2 = darkThreshold1610;
    }

    public void setDarkGlintThresholdTest2Wvl(int darkGlintThresholdTest2Wvl) {
        this.darkGlintThresholdTest2Wvl = darkGlintThresholdTest2Wvl;
    }

    private int[] classifyNNResult(double[] netResult) {
        double netResultValue = netResult[0];
        int[] nnClassification = new int[netResult.length];
        if (netResultValue < nnCloudAmbiguousLowerBoundaryValue) {
            nnClassification[0] = Landsat8Algorithm.NN_CATEGORY_CLEAR_SKY;
        } else if (netResultValue >= nnCloudAmbiguousLowerBoundaryValue &&
                netResultValue < nnCloudAmbiguousSureSeparationValue) {
            nnClassification[0] = Landsat8Algorithm.NN_CATEGORY_NON_CLEAR_SKY;
        } else if (netResultValue >= nnCloudAmbiguousSureSeparationValue &&
                netResultValue < nnCloudSureSnowSeparationValue) {
            nnClassification[0] = Landsat8Algorithm.NN_CATEGORY_CLOUD;
        } else {
            nnClassification[0] = Landsat8Algorithm.NN_CATEGORY_CLEAR_SKY_SNOW_ICE;
        }
        return nnClassification;
    }

    public void setNnCloudAmbiguousLowerBoundaryValue(double nnCloudAmbiguousLowerBoundaryValue) {
        this.nnCloudAmbiguousLowerBoundaryValue = nnCloudAmbiguousLowerBoundaryValue;
    }

    public void setNnCloudAmbiguousSureSeparationValue(double nnCloudAmbiguousSureSeparationValue) {
        this.nnCloudAmbiguousSureSeparationValue = nnCloudAmbiguousSureSeparationValue;
    }

    public void setNnCloudSureSnowSeparationValue(double nnCloudSureSnowSeparationValue) {
        this.nnCloudSureSnowSeparationValue = nnCloudSureSnowSeparationValue;
    }
}
