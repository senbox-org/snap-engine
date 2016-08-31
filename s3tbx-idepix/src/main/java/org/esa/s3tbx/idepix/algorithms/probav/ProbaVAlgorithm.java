package org.esa.s3tbx.idepix.algorithms.probav;

import org.esa.s3tbx.idepix.core.IdepixConstants;
import org.esa.s3tbx.idepix.core.pixel.AbstractPixelProperties;
import org.esa.s3tbx.idepix.core.util.IdepixUtils;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.math.MathUtils;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 17.08.2016
 * Time: 18:18
 *
 * @author olafd
 */
public class ProbaVAlgorithm extends AbstractPixelProperties {

    static final float LAND_THRESH = 0.9f;
    static final float WATER_THRESH = 0.9f;


    private static final float BRIGHTWHITE_THRESH = 0.65f;
    private static final float NDSI_THRESH = 0.50f;
    private static final float PRESSURE_THRESH = 0.9f;
    private static final float CLOUD_THRESH = 1.65f;
    private static final float UNCERTAINTY_VALUE = 0.5f;
    private static final float BRIGHT_THRESH = 0.3f;
    private static final float WHITE_THRESH = 0.5f;
    private static final float BRIGHT_FOR_WHITE_THRESH = 0.2f;
    private static final float NDVI_THRESH = 0.4f;
    private static final float REFL835_WATER_THRESH = 0.1f;
    private static final float REFL835_LAND_THRESH = 0.15f;

    private boolean l1bLand;
    private boolean processingLand;  // land + buffer

    private boolean isBlueGood;
    private boolean isRedGood;
    private boolean isNirGood;
    private boolean isSwirGood;

    private double elevation;

    private double[] nnOutput;
    float[] refl;

    public boolean isInvalid() {
        // GK 20151126;
        return !(isBlueGood && isRedGood && isNirGood && isSwirGood && processingLand);
    }

    public boolean isClearSnow() {
        // GK 20151126;
        // return !isInvalid() && (ndsiValue() > 0.4 && refl[3] < 0.13 && elevation > 600.0) || (ndsiValue() > 0.7);

        // JM, 20160630:
        return !isInvalid() && ((ndsiValue() > 0.4 && refl[3] < 0.13 && tc1Value() > 0.3) ||
                (ndsiValue() > 0.7 && tc1Value() > 0.5)) && elevation > 650.0;
    }

    public boolean isCloud() {
        // JM, 20160630:
        // Combine the previous 4 cloud masks
        if (!isInvalid()) {
            if (isGeneralCloud() || isHaze() || isComplexHaze() || isBorderCloud()) {
                return true;
            }
        }

        return false;
    }

    public boolean isSeaIce() {
        // no algorithm available yet for PROBA-V
        return false;
    }

    public boolean isBrightWhite() {
        return !isInvalid() && (whiteValue() + brightValue() > getBrightWhiteThreshold());
    }

    public boolean isGlintRisk() {
        return false;
    }

    public boolean isClearLand() {
        if (isInvalid()) {
            return false;
        }
        float landValue;

        if (!MathUtils.equalValues(radiometricLandValue(), UNCERTAINTY_VALUE)) {
            landValue = radiometricLandValue();
        } else if (aPrioriLandValue() > UNCERTAINTY_VALUE) {
            landValue = aPrioriLandValue();
        } else {
            return false; // this means: if we have no information about land, we return isClearLand = false
        }
        return (!isWater && !isCloud() && landValue > LAND_THRESH);
    }

    public boolean isClearWater() {
        if (isInvalid()) {
            return false;
        }
        float waterValue;
        if (!MathUtils.equalValues(radiometricWaterValue(), UNCERTAINTY_VALUE)) {
            waterValue = radiometricWaterValue();
        } else if (aPrioriWaterValue() > UNCERTAINTY_VALUE) {
            waterValue = aPrioriWaterValue();
        } else {
            return false; // this means: if we have no information about water, we return isClearWater = false
        }
        return (isWater && !isCloud() && waterValue > WATER_THRESH);
    }

    public boolean isBright() {
        return (!isInvalid() && brightValue() > getBrightThreshold());
    }

    public boolean isWhite() {
        return (!isInvalid() && whiteValue() > getWhiteThreshold());
    }

    public boolean isLand() {
        final boolean isLand1 = !usel1bLandWaterFlag && !isWater;
        return !isInvalid() && (isLand1 || (aPrioriLandValue() > LAND_THRESH));
    }

    public boolean isL1Water() {
        return (!isInvalid() && aPrioriWaterValue() > WATER_THRESH);
    }

    public boolean isVegRisk() {
        return (!isInvalid() && ndviValue() > getNdviThreshold());
    }

    public boolean isHigh() {
        return (!isInvalid() && pressureValue() > getPressureThreshold());
    }

    public float brightValue() {
        double value;

        // do not make a difference any more
        // (changed for LC VGT processing because of clouds in rivers with new water mask, 20130227)
        value = (refl[0] + refl[1]) / 2.0f;

        value = Math.min(value, 1.0);
        value = Math.max(value, 0.0);
        return (float) value;
    }

    public float temperatureValue() {
        return UNCERTAINTY_VALUE;
    }

    public float spectralFlatnessValue() {
        final double slope0 = IdepixUtils.spectralSlope(refl[0], refl[1],
                                                        IdepixConstants.PROBAV_WAVELENGTHS[0],
                                                        IdepixConstants.PROBAV_WAVELENGTHS[1]);
        final double slope1 = IdepixUtils.spectralSlope(refl[1], refl[2],
                                                        IdepixConstants.PROBAV_WAVELENGTHS[1],
                                                        IdepixConstants.PROBAV_WAVELENGTHS[2]);
        final double flatness = 1.0f - Math.abs(2000.0 * (slope0 + slope1) / 2.0);
        return (float) Math.max(0.0f, flatness);
    }

    public float whiteValue() {
        if (brightValue() > BRIGHT_FOR_WHITE_THRESH) {
            return spectralFlatnessValue();
        } else {
            return 0f;
        }
    }

    public float ndsiValue() {
        // (RED - SWIR)/(SWIR + RED), GK 20151201
        double value = (refl[1] - refl[3]) / (refl[1] + refl[3]);
        value = Math.min(value, 1.0);
        value = Math.max(value, 0.0);
        return (float) value;
    }

    public float ndviValue() {
        double value = (refl[2] - refl[1]) / (refl[2] + refl[1]);
        value = Math.min(value, 1.0);
        value = Math.max(value, 0.0);
        return (float) value;
    }

    public float pressureValue() {
        return UNCERTAINTY_VALUE;
    }

    public float glintRiskValue() {
        return IdepixUtils.spectralSlope(refl[0], refl[1], IdepixConstants.PROBAV_WAVELENGTHS[0],
                                         IdepixConstants.PROBAV_WAVELENGTHS[1]);
    }

    public float aPrioriLandValue() {
        if (isInvalid()) {
            return UNCERTAINTY_VALUE;
        } else if (l1bLand) {
            return 1.0f;
        } else {
            return 0.0f;
        }
    }

    public float aPrioriWaterValue() {
        if (isInvalid()) {
            return UNCERTAINTY_VALUE;
        } else if (!l1bLand) {
            return 1.0f;
        } else {
            return 0.0f;
        }
    }

    public float radiometricLandValue() {
        if (isInvalid() || isCloud()) {
            return UNCERTAINTY_VALUE;
        } else if (refl[2] > refl[1] && refl[2] > REFL835_LAND_THRESH) {
            return 1.0f;
        } else if (refl[2] > REFL835_LAND_THRESH) {
            return 0.75f;
        } else {
            return 0.25f;
        }
    }

    public float radiometricWaterValue() {
        if (isInvalid() || isCloud()) {
            return UNCERTAINTY_VALUE;
        } else if (refl[0] > refl[1] && refl[1] > refl[2] && refl[2] < REFL835_WATER_THRESH) {
            return 1.0f;
        } else {
            return 0.25f;
        }
    }

    public float getBrightWhiteThreshold() {
        return BRIGHTWHITE_THRESH;
    }

    public float getNdsiThreshold() {
        return NDSI_THRESH;
    }

    public float getNdviThreshold() {
        return NDVI_THRESH;
    }

    public float getBrightThreshold() {
        return BRIGHT_THRESH;
    }

    public float getWhiteThreshold() {
        return WHITE_THRESH;
    }

    public float getPressureThreshold() {
        return PRESSURE_THRESH;
    }

    // new features (JM 20160630):
    public boolean isGeneralCloud() {
        // ((TC4-TC3) < -0.2) and not ((TC3-TC2) < 0)
        if (!isInvalid()) {
            if ((tcSlope1Value() < -0.2 && !(tcSlope2Value() < 0)) && !isClearSnow()) {
                return true;
            }
        }
        return false;
    }

    public boolean isHaze() {
        // (((TC4-TC3) < -0.07) and not ((TC3-TC2) < -0.01) and (TC1 > 0.3) and (BLUE > 0.21)
        if (!isInvalid()) {
            if (((tcSlope1Value() < -0.07 && !(tcSlope2Value() < -0.01)) && tc1Value() > 0.3 && refl[0] > 0.21) &&
                    !isClearSnow() && !isGeneralCloud()) {
                return true;
            }
        }
        return false;
    }

    public boolean isComplexHaze() {
        // ((BNIR_diff > -0.192 and BNIR_diff<0.1) and (TC1 > 0.42) and (NIRSWIR_diff < -0.03) and (NIRR_diff>RB_diff) and (BLUE >0.194))
        if (!isInvalid()) {
            if (((bnDiffValue() > -0.192 && bnDiffValue()<0.1) && tc1Value() >0.42 && nswirDiffValue() < 0.03 &&
                    (refl[2]-refl[1] > refl[1]-refl[0]) && refl[0] > 0.194) && !isClearSnow() && !isGeneralCloud() && !isHaze()) {
                return true;
            }
        }
        return false;
    }

    public boolean isBorderCloud() {
        // (RED-BLUE > 0) and (NIR-RED > 0) and (SWIR-NIR >0) and (BLUE > 0.25) and ((RED-BUE)+(NIR-RED)+(SWIR-NIR) < 0.326)
        if (!isInvalid()) {
            if (((refl[1]-refl[0] > 0) && (refl[2]-refl[1] > 0) && (refl[3]-refl[2] > 0) && refl[0] > 0.25 &&
                    (((refl[1]-refl[0])+ (refl[2]-refl[1]) + (refl[3]-refl[2])) < 0.326)) && !isClearSnow() &&
                    !isGeneralCloud() && !isHaze() && !isComplexHaze()) {
                return true;
            }
        }
        return false;
    }

    // new feature values (JM 20160630):
    public float tc1Value() {
        // 0.332*BLUE + 0.603*RED + 0.676*NIR + 0.263*SWIR
        double value = 0.332 * refl[0] + 0.603 * refl[1] + 0.676 * refl[2] + 0.263 * refl[3];
        return (float) value;
    }

    public float tcSlope1Value() {
        // TC4-TC3
        // (0.016 * blue + 0.428 * red + -0.452 * nir + 0.882 * swir) – (0.9 * blue + 0.428 * red + 0.0759 * nir + -0.041 * swir)
        double value = (0.016 * refl[0] + 0.428 * refl[1] - 0.452 * refl[2] + 0.882 * refl[3]) -
                (0.9 * refl[0] + 0.428 * refl[1] + 0.0759 * refl[2] - 0.041 * refl[3]);
        return (float) value;
    }

    public float tcSlope2Value() {
        // TC3-TC2
        // (0.9 * blue + 0.428 * red + 0.0759 * nir + -0.041 * swir) – (0.283 * blue + -0.66 * red + 0.577 * nir + 0.388 * swir)
        double value = (0.9 * refl[0] + 0.428 * refl[1] + 0.0759 * refl[2] - 0.041 * refl[3]) -
                (0.283 * refl[0] - 0.66 * refl[1] + 0.577 * refl[2] + 0.388 * refl[3]);
        return (float) value;
    }

    public float bnDiffValue() {
        // (BLUE – NIR)/(BLUE+NIR)
        double value = (refl[0] - refl[2]) / (refl[0] + refl[2]);
        return (float) value;
    }

    public float nswirDiffValue() {
        // (NIR – SWIR)/(NIR+SWIR)
        double value = (refl[2] - refl[3]) / (refl[2] + refl[3]);
        return (float) value;
    }

    // setters for PROBA-V specific quantities

    public void setL1bLand(boolean l1bLand) {
        this.l1bLand = l1bLand;
    }

    public void setProcessingLand(boolean processingLand) {
        this.processingLand = processingLand;
    }

    public void setIsBlueGood(boolean isBlueGood) {
        this.isBlueGood = isBlueGood;
    }

    public void setIsRedGood(boolean isRedGood) {
        this.isRedGood = isRedGood;
    }

    public void setIsNirGood(boolean isNirGood) {
        this.isNirGood = isNirGood;
    }

    public void setIsSwirGood(boolean isSwirGood) {
        this.isSwirGood = isSwirGood;
    }

    public void setRefl(float[] refl) {
        if (refl.length != IdepixConstants.PROBAV_WAVELENGTHS.length) {
            throw new OperatorException("PROBA-V pixel processing: Invalid number of wavelengths [" + refl.length +
                                                "] - must be " + IdepixConstants.PROBAV_WAVELENGTHS.length);
        }
        this.refl = refl;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public void setNnOutput(double[] nnOutput) {
        this.nnOutput = nnOutput;
    }

    public double[] getNnOutput() {
        return nnOutput;
    }
}
