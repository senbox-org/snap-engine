package org.esa.s3tbx.idepix.algorithms.avhrr;

import org.esa.s3tbx.idepix.core.util.IdepixIO;

/**
 * IDEPIX instrument-specific pixel identification algorithm for GlobAlbedo: abstract superclass
 *
 * @author olafd
 */
public class AvhrrAlgorithm implements AvhrrPixelProperties {

    float waterFraction;
    double[] radiance;
    double[] nnOutput;

    double avhrracSchillerNNCloudAmbiguousLowerBoundaryValue;
    double avhrracSchillerNNCloudAmbiguousSureSeparationValue;
    double avhrracSchillerNNCloudSureSnowSeparationValue;

    double reflCh1;
    double reflCh2;
    double reflCh3;
    double btCh3;
    double btCh4;
    double btCh5;

    double rho3b;
    double emissivity3b;
    double ndsi;

    String noaaId;
    double distanceCorr;
    double sza;

    private double latitude;
    private double longitude;
    private double elevation;

    @Override
    public boolean isInvalid() {
        return !IdepixIO.areAllReflectancesValid(radiance);
    }

    @Override
    public boolean isCloud() {
        return isCloudAmbiguous() || isCloudSure();
    }

    @Override
    public boolean isSnowIce() {

//        boolean isSnowIce = isCloudSureSchiller() && emissivity3b < AvhrrAcConstants.EMISSIVITY_THRESH;
        // todo: also consider NDSI?!
        boolean isSnowIce = !isCloudTgct() && ndsi > 0.8;

        // for AVHRR, nnOutput has one element:
        // nnOutput[0] =
        // 0 < x < 2.15 : clear
        // 2.15 < x < 3.45 : noncl / semitransparent cloud --> cloud ambiguous
        // 3.45 < x < 4.45 : cloudy --> cloud sure
        // 4.45 < x : clear snow/ice
        if (!isSnowIce && nnOutput != null) {
            // separation numbers from HS, 20140923
            isSnowIce = nnOutput[0] > avhrracSchillerNNCloudSureSnowSeparationValue && nnOutput[0] <= 5.0;
        }

        // forget all the old stuff, completely new test now (GK/JM, 20151028):
        isSnowIce = false;
        final double btCh3Celsius = btCh3 - 273.15;
        final double btCh4Celsius = btCh4 - 273.15;
        final double btCh5Celsius = btCh5 - 273.15;
        final double ratio21 = reflCh2 / reflCh1;
        final double diffbt53 = btCh5Celsius - btCh3Celsius;
        final double ratiobt53 = btCh5Celsius/btCh3Celsius;
        final double diffrefl1rt3 = reflCh1 - reflCh3;
        final double sumrefl2rt3 = reflCh2 + reflCh3;

        if (latitude > 62.0) {
            // NORTH (lat > 62N)
            // shall be:
//        (( ((-15.0) < bt_4 and bt_4 < (1.35)) and refl_1>0.4 and (0.95 <= (refl_2/ refl_1) and (refl_2/ refl_1) <1.15)
//                           and rt_3<0.054 and elevation > 300 and bt_5-bt_3 > -14 and (bt_5/bt_3) > 1.7) or
//           ((-15.0) < bt_4 and bt_4 < (1.35)) and refl_1>0.4 and (0.80 <= (refl_2/ refl_1) and (refl_2/ refl_1) <1.15)
//                           and (rt_3<0.054 and rt_3 > 0.026) and elevation > 300
//                           and bt_5-bt_3 > -13 and (bt_5/bt_3) <= -0.98 or
//           ((-15.0) < bt_4 and bt_4 < (1.35)) and refl_1>0.4 and (0.80 <= (refl_2/ refl_1) and (refl_2/ refl_1) <1.15)
//                           and rt_3<0.054 and elevation > 300 and bt_5-bt_3 > -11
//                           and ((bt_5/bt_3) <= 1.7 and (bt_5/bt_3) > -0.98) or
//           ((refl_1-rt_3)/(refl_2+rt_3)) > 1.1 and bt_5-bt_3 > -8)
//
            final boolean condBtCh4 = -15.0 < btCh4Celsius && btCh4Celsius < 1.35;
            final boolean condElevation = elevation > 300.0;

            final boolean cond1 = condBtCh4 && reflCh1 > 0.4 && 0.95 <= ratio21 && ratio21 < 1.15
                    && reflCh3 < 0.054 && condElevation && diffbt53 > -14.0 && ratiobt53 > 1.7;
            final boolean cond2 = condBtCh4 && reflCh1 > 0.4 && 0.8 <= ratio21 && ratio21 < 1.15
                    && reflCh3 < 0.054 && reflCh3 > 0.026 && condElevation && diffbt53 > -13.0 && ratiobt53 <= -0.98;
            final boolean cond3 = condBtCh4 && reflCh1 > 0.4 && 0.8 <= ratio21 && ratio21 < 1.15
                    && reflCh3 < 0.054 && condElevation && diffbt53 > -11.0 && ratiobt53 <= 1.7 && ratiobt53 > -0.98;
            final boolean cond4 = diffrefl1rt3/sumrefl2rt3 > 1.1 && diffbt53 > -8.0;

            final boolean snowIceCondition = (cond1 || cond2 ||cond3 || cond4);

            isSnowIce = isLand() && snowIceCondition;

        } else if (latitude < -62.0) {
            // SOUTH (lat < -62S)
            // shall be:
//      (((-27.0) < bt_4 and bt_4 < (1.35)) and refl_1>0.75 and (0.85 <= (refl_2/ refl_1) and (refl_2/ refl_1) <1.15)
//                       and rt_3 < 0.03 and bt_5-bt_3 > -13 or (refl_1- rt_3) / (refl_2+ rt_3) > 1.05
//                       and refl_1 > 0.55) and lat < -62
            isSnowIce = -27.0 < btCh4Celsius && btCh4Celsius < 1.35 && reflCh1 > 0.75 &&
                    ratio21 > 0.85 && ratio21 < 1.15 && reflCh3 < 0.03 &&
                    diffbt53 > -13.0 || (reflCh1-reflCh3)/(reflCh2+reflCh3) > 1.05 && reflCh1 > 0.55;

        } else {
            // CENTER (-62S < lat < 62N)
            // shall be:
//        ((-15.0) < bt_4 and bt_4 < (1.35)) and refl_1>0.4 and (0.80 <= (refl_2/ refl_1) and (refl_2/ refl_1) <1.15)
//                        and rt_3<0.054 and bt_5-bt_3 > -14 and elevation > 1000
//                        and pixel_classif_flags.F_LAND and (lat <= 62 and lat >= -62)
            isSnowIce = isLand() && -15.0 < btCh4Celsius && btCh4Celsius < 1.35 && reflCh1 > 0.4 &&
                    ratio21 > 0.8 && ratio21 < 1.15 && reflCh3 < 0.054 && diffbt53 > -14.0 && elevation > 1000.0;
        }

        return isSnowIce;
    }

    @Override
    public boolean isCloudAmbiguous() {
        return isCloudSure(); // todo: discuss if we need specific ambiguous flag
//        if (isCloudSure()) {   // this check has priority
//            return false;
//        }
//
//        // for AVHRR, nnOutput has one element:
//        // nnOutput[0] =
//        // 0 < x < 2.15 : clear
//        // 2.15 < x < 3.45 : noncl / semitransparent cloud --> cloud ambiguous
//        // 3.45 < x < 4.45 : cloudy --> cloud sure
//        // 4.45 < x : clear snow/ice
//        if (nnOutput != null) {
//            return nnOutput[0] >= avhrracSchillerNNCloudAmbiguousLowerBoundaryValue &&
//                    nnOutput[0] < avhrracSchillerNNCloudAmbiguousSureSeparationValue;    // separation numbers from report HS, 20141112 for NN Nr.2
////            return nnOutput[0] >= 0.48 && nnOutput[0] < 0.48;      // CB: cloud sure gives enough clouds, no ambiguous needed, 20141111
//        } else {
//            return false;
//        }
    }

    @Override
    public boolean isCloudSure() {
        if (isSnowIce()) {   // this check has priority
            return false;
        }

        final boolean isCloudAdditional = isCloudSnowIceFromDecisionTree();
        if (isCloudAdditional) {
            return true;
        }
        final boolean isCloudSureSchiller = isCloudSureSchiller();
        if (isCloudSureSchiller) {
            return true;
        }

        // Test (GK/JM 20151029): use 'old' snow/ice test as additional cloud criterion:
        boolean isCloudFromOldSnowIce = !isCloudTgct() && ndsi > 0.8;
        // for AVHRR, nnOutput has one element:
        // nnOutput[0] =
        // 0 < x < 2.15 : clear
        // 2.15 < x < 3.45 : noncl / semitransparent cloud --> cloud ambiguous
        // 3.45 < x < 4.45 : cloudy --> cloud sure
        // 4.45 < x : clear snow/ice
        if (!isCloudFromOldSnowIce && nnOutput != null) {
            // separation numbers from HS, 20140923
            isCloudFromOldSnowIce = nnOutput[0] > avhrracSchillerNNCloudSureSnowSeparationValue && nnOutput[0] <= 5.0;
        }
        if (isCloudFromOldSnowIce) {
            return true;
        }

        final boolean isResidualCloud = isResidualCloud();
        if (isResidualCloud) {
            return true;
        }
        return false;
//        return isCloudSureSchiller || isCloudAdditional;
//        return isCloudSureSchiller || isCloudAdditional || isCloudFromOldSnowIce || isResidualCloud;
    }

    private boolean isResidualCloud() {

        if (isDesertArea()) {
            return (reflCh3 < 0.18 && reflCh1>0.15 && (btCh4 / btCh3) < 0.927 && btCh4<280)
                || ((reflCh1 + reflCh2 + reflCh3) / 3 > 0.23 && btCh4 < 302 && (btCh4 / btCh3) < 0.95 && reflCh3 < 0.38 && reflCh3 > 0.219);
        } else {
            return (reflCh3 < 0.18 && reflCh1>0.15 && (btCh4 / btCh3) < 0.927)
                || ((reflCh1 + reflCh2 + reflCh3) / 3 > 0.2 && btCh4 < 302 && (btCh4 / btCh3) < 0.95 && reflCh3 < 0.4);
        }
    }

    private boolean isCloudSureSchiller() {
        boolean isCloudSureSchiller;
        // for AVHRR, nnOutput has one element:
        // nnOutput[0] =
        // 0 < x < 2.15 : clear
        // 2.15 < x < 3.45 : noncl / semitransparent cloud --> cloud ambiguous
        // 3.45 < x < 4.45 : cloudy --> cloud sure
        // 4.45 < x : clear snow/ice
        if (nnOutput != null) {
            isCloudSureSchiller = nnOutput[0] >= avhrracSchillerNNCloudAmbiguousSureSeparationValue &&
                    nnOutput[0] < avhrracSchillerNNCloudSureSnowSeparationValue;   // separation numbers from report HS, 0141112 for NN Nr.2
        } else {
            isCloudSureSchiller = false;
        }
        return isCloudSureSchiller;
    }

    private boolean isCloudSnowIceFromDecisionTree() {
        // first apply additional tests (GK, 20150313):

        // 1. RGCT test:
        final double ndvi = (reflCh2 - reflCh1) / (reflCh2 + reflCh1);
        final double rgctThresh = getRgctThreshold(ndvi);
//        final boolean isCloudRGCT = isLand() && reflCh1/100.0 > rgctThresh;
        final boolean isCloudRGCT = isLand() && reflCh1 > rgctThresh;  // reflCh1 should not be divided by 100?!

        // 2. RRCT test:
        final double rrctThresh = 1.1;
        final boolean isCloudRRCT = isLand() && !isDesertArea() && reflCh2 / reflCh1 < rrctThresh;

        // 3. C3AT test:
        final double c3atThresh = 0.06;
        final boolean isCloudC3AT = isLand() && !isDesertArea() && rho3b > c3atThresh;

        // 4. TGCT test
        final boolean isCloudTGCT = btCh4 < AvhrrConstants.TGCT_THRESH;

        // 5. FMFT test
        final double fmftThresh = getFmftThreshold();
        final boolean isCloudFMFT = (btCh4 - btCh5) > fmftThresh;

        // 6. TMFT test
        final double bt34 = btCh3 - btCh4;
        final double tmftMinThresh = getTmftMinThreshold(bt34);
        final double tmftMaxThresh = getTmftMaxThreshold(bt34);
        final boolean isClearTMFT = bt34 > tmftMinThresh && bt34 < tmftMaxThresh;

        // 7. Emissivity test
        final boolean isCloudEmissivity = emissivity3b > AvhrrConstants.EMISSIVITY_THRESH;

        // now use combinations:
        //
        // if (RGCT AND FMFT) then cloud
        // if [ NOT RGCT BUT desert AND (FMFT OR (TGCT AND lat<latMaxThresh)) ] then cloud
        // if [ NOT RGCT BUT (RRCT AND FMFT) ] then cloud
        // if [ NOT RGCT BUT (RRCT AND C3AT) ] then cloud
        // if TMFT: clear pixels must not become cloud in any case!
        // cloud ONLY if cloudEmissivity
        //
        // apply Schiller AFTER these test for pixels not yet cloud!!
        boolean isCloudAdditional = false;
        if (!isClearTMFT && isCloudEmissivity) {
            // first branch of condition tree:
            if (isCloudRGCT && isCloudFMFT) {
                isCloudAdditional = true;
            }
            // second branch of condition tree:
            if (isDesertArea() && (isCloudFMFT || (Math.abs(latitude) < AvhrrConstants.LAT_MAX_THRESH && isCloudTGCT)) ||
                    (!isDesertArea() && isCloudRRCT && isCloudFMFT) ||
                    (!isDesertArea() && isCloudRRCT && (Math.abs(latitude) < AvhrrConstants.LAT_MAX_THRESH && isCloudTGCT)) ||
                    (!isDesertArea() && isCloudRRCT && isCloudC3AT)) {
                isCloudAdditional = true;
            }
        }
        return isCloudAdditional;
    }

    private boolean isCloudTgct() {
        return btCh4 < AvhrrConstants.TGCT_THRESH;
    }

    private double getTmftMinThreshold(double bt34) {
        int tmftMinThresholdIndexRow = (int) ((btCh4 - 190.0) / 10.0);
        tmftMinThresholdIndexRow = Math.max(0, tmftMinThresholdIndexRow);
        tmftMinThresholdIndexRow = Math.min(13, tmftMinThresholdIndexRow);
        int tmftMinThresholdIndexColumn = (int) ((bt34 - 7.5) / 15.0) + 1;
        tmftMinThresholdIndexColumn = Math.max(0, tmftMinThresholdIndexColumn);
        tmftMinThresholdIndexColumn = Math.min(3, tmftMinThresholdIndexColumn);

        final int tmftMinThresholdIndex = 4 * tmftMinThresholdIndexRow + tmftMinThresholdIndexColumn;

        return AvhrrConstants.tmftTestMinThresholds[tmftMinThresholdIndex];
    }

    private double getTmftMaxThreshold(double bt34) {
        int tmftMaxThresholdIndexRow = (int) ((btCh4 - 190.0) / 10.0);
        tmftMaxThresholdIndexRow = Math.max(0, tmftMaxThresholdIndexRow);
        tmftMaxThresholdIndexRow = Math.min(13, tmftMaxThresholdIndexRow);
        int tmftMaxThresholdIndexColumn = (int) ((bt34 - 7.5) / 15.0) + 1;
        tmftMaxThresholdIndexColumn = Math.max(0, tmftMaxThresholdIndexColumn);
        tmftMaxThresholdIndexColumn = Math.min(3, tmftMaxThresholdIndexColumn);

        final int tmftMaxThresholdIndex = 4 * tmftMaxThresholdIndexRow + tmftMaxThresholdIndexColumn;

        return AvhrrConstants.tmftTestMaxThresholds[tmftMaxThresholdIndex];
    }

    private double getFmftThreshold() {
        int fmftThresholdIndex = (int) (btCh4 - 200.0);
        fmftThresholdIndex = Math.max(0, fmftThresholdIndex);
        fmftThresholdIndex = Math.min(120, fmftThresholdIndex);
        return AvhrrConstants.fmftTestThresholds[fmftThresholdIndex];
    }

    private boolean isDesertArea() {
        return (latitude >= 10.0 && latitude < 35.0 && longitude >= -20.0 && longitude < 30.0) ||
                (latitude >= 5.0 && latitude < 50.0 && longitude >= 30.0 && longitude < 60.0) ||
                (latitude >= 25.0 && latitude < 50.0 && longitude >= 60.0 && longitude < 110.0) ||
                (latitude >= -31.0 && latitude < 19.0 && longitude >= 121.0 && longitude < 141.0) ||
                (latitude >= 35.0 && latitude < 50.0 && longitude >= 110.0 && longitude < 127.0);
    }

    private double getRgctThreshold(double ndvi) {
        double rgctThresh = Double.MAX_VALUE;
        if (ndvi < -0.05) {
            rgctThresh = 0.8;
        } else if (ndvi >= -0.05 && ndvi < 0.0) {
            rgctThresh = 0.6;
        } else if (ndvi >= 0.0 && ndvi < 0.05) {
            rgctThresh = 0.5;
        } else if (ndvi >= 0.05 && ndvi < 0.1) {
            rgctThresh = 0.4;
        } else if (ndvi >= 0.1 && ndvi < 0.15) {
            rgctThresh = 0.35;
        } else if (ndvi >= 0.15 && ndvi < 0.25) {
            rgctThresh = 0.3;
        } else if (ndvi >= 0.25) {
            rgctThresh = 0.25;
        }
        return rgctThresh;
    }

    @Override
    public boolean isCloudBuffer() {
        // is applied in post processing!
        return false;
    }

    @Override
    public boolean isCloudShadow() {
        // is applied in post processing!
        return false;
    }

    @Override
    public boolean isCoastline() {
        // NOTE that this does not work if we have a PixelGeocoding. In that case, waterFraction
        // is always 0 or 100!! (TS, OD, 20140502). If so, get a coastline in post processing approach.
        return waterFraction < 100 && waterFraction > 0;
    }

    @Override
    public boolean isLand() {
        return waterFraction == 0;
    }

    @Override
    public boolean isGlintRisk() {
        return false;
    }


    public void setReflCh1(double reflCh1) {
        this.reflCh1 = reflCh1;
    }

    public void setReflCh2(double reflCh2) {
        this.reflCh2 = reflCh2;
    }

    public void setReflCh3(double reflCh3) {
        this.reflCh3 = reflCh3;
    }

    public void setBtCh3(double btCh3) {
        this.btCh3 = btCh3;
    }

    public void setBtCh4(double btCh4) {
        this.btCh4 = btCh4;
    }

    public void setBtCh5(double btCh5) {
        this.btCh5 = btCh5;
    }

    public void setRadiance(double[] rad) {
        this.radiance = rad;
    }

    public void setWaterFraction(float waterFraction) {
        this.waterFraction = waterFraction;
    }

    public void setNnOutput(double[] nnOutput) {
        this.nnOutput = nnOutput;
    }

    public void setAmbiguousLowerBoundaryValue(double avhrracSchillerNNCloudAmbiguousLowerBoundaryValue) {
        this.avhrracSchillerNNCloudAmbiguousLowerBoundaryValue = avhrracSchillerNNCloudAmbiguousLowerBoundaryValue;
    }

    public void setAmbiguousSureSeparationValue(double avhrracSchillerNNCloudAmbiguousSureSeparationValue) {
        this.avhrracSchillerNNCloudAmbiguousSureSeparationValue = avhrracSchillerNNCloudAmbiguousSureSeparationValue;
    }

    public void setSureSnowSeparationValue(double avhrracSchillerNNCloudSureSnowSeparationValue) {
        this.avhrracSchillerNNCloudSureSnowSeparationValue = avhrracSchillerNNCloudSureSnowSeparationValue;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public void setSza(double sza) {
        this.sza = sza;
    }

    public void setDistanceCorr(double distanceCorr) {
        this.distanceCorr = distanceCorr;
    }

    public void setNoaaId(String noaaId) {
        this.noaaId = noaaId;
    }

    public float getWaterFraction() {
        return waterFraction;
    }

}
