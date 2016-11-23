/* AATSR GBT-UBT-Tool - Ungrids AATSR L1B products and extracts geolocation data and field of view extent
 * 
 * Copyright (C) 2015 Telespazio VEGA UK Ltd
 * 
 * This file is part of the AATSR GBT-UBT-Tool.
 * 
 * AATSR GBT-UBT-Tool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * AATSR GBT-UBT-Tool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with AATSR GBT-UBT-Tool.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.esa.s3tbx.aatsr.regrid;

import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductNodeGroup;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ABeaton, Telespazio VEGA UK Ltd 30/10/2013
 *
 * Contact: alasdhair(dot)beaton(at)telespazio(dot)com
 *
 *
 */
class Calculator {
    /* This class controls the execution of the ungridding process for a given subset of pixels
     * This class also computes the pixel field of view using adapted IDL code provided by RAL.
     */

    public Calculator() {
    }

 /**
 *  This is the original top level method from GBT-UBT tool, not used in SNAP since the iteration and threading
 *  is now managed by the pixel operator.
 *
    public static void unGrid(double[][][] tempResult, int startingScanNumber, int rowsPerThread, int minX, int maxX, int s0, ProductNodeGroup<MetadataElement> NADIR_VIEW_SCAN_PIX_NUM_ADS_Records, ProductNodeGroup<MetadataElement> FWARD_VIEW_SCAN_PIX_NUM_ADS_Records, ProductNodeGroup<MetadataElement> SCAN_PIXEL_X_AND_Y_ADS_Records, ProductNodeGroup<MetadataElement> GEOLOCATION_ADS_Records, List<Double> scanYCoords, String threadName, InputParameters parameters, List<List<Double>> pixelProjectionMap, *//*BoundedPropagator ephemeris,*//* Band DEM) {
        for (int i = startingScanNumber; i < startingScanNumber + rowsPerThread; i++) {
            for (int j = minX; j < maxX; j++) {
                int[] pixelRelativeNumbers = {0, 0};
                double[] pixelNewPositionsAndTimes = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
                getPixelPositionsAcquisitionTimes(i, j, s0, NADIR_VIEW_SCAN_PIX_NUM_ADS_Records, FWARD_VIEW_SCAN_PIX_NUM_ADS_Records, SCAN_PIXEL_X_AND_Y_ADS_Records, GEOLOCATION_ADS_Records, scanYCoords, pixelNewPositionsAndTimes, pixelRelativeNumbers, parameters);

                 if (parameters.orthorectify) {
                    // PDB hide this until we import orekit
                    // Orthorectifier.orthorectify(ephemeris, pixelNewPositionsAndTimes,parameters, DEM);
                }
                tempResult[i - startingScanNumber][j - minX][0] = pixelNewPositionsAndTimes[0];
                tempResult[i - startingScanNumber][j - minX][1] = pixelNewPositionsAndTimes[1];
                tempResult[i - startingScanNumber][j - minX][2] = pixelNewPositionsAndTimes[2];
                tempResult[i - startingScanNumber][j - minX][3] = pixelProjectionMap.get(pixelRelativeNumbers[0]).get(0);
                tempResult[i - startingScanNumber][j - minX][4] = pixelProjectionMap.get(pixelRelativeNumbers[0]).get(1);
                tempResult[i - startingScanNumber][j - minX][5] = pixelNewPositionsAndTimes[3];
                tempResult[i - startingScanNumber][j - minX][6] = pixelNewPositionsAndTimes[4];
                tempResult[i - startingScanNumber][j - minX][7] = pixelNewPositionsAndTimes[5];
                tempResult[i - startingScanNumber][j - minX][8] = pixelProjectionMap.get(pixelRelativeNumbers[1]).get(0);
                tempResult[i - startingScanNumber][j - minX][9] = pixelProjectionMap.get(pixelRelativeNumbers[1]).get(1);
            }
        }
        System.out.println(threadName + " complete");
    }
*/
    /**
     * Computes original geolocation (UBT) as in Technical Note by Andrew Birks
     1.	Transform L1B image pixels (i,j) into instrument  scan and pixel number (s,p) using Annotation Data Sets ADS#1 (nadir) and ADS#2 (forward).
     2.	Transform scan and pixel (s,p) into coordinates (x,y) using ADS#4 and the “first pixel” numbers from L1B characterisation file ATS_CH1_AX (e.g. nadir = 213, forward=1305 ?).
     3.	Transform coordinates (x,y) into location (lat,long)  using ADS#3 topographic corrections.
     DISABLED: Optionally orthorectifies UBT using DEM file.
     Computes acquisition times.
     Computes the size of pixel field of view using Dave Smith algorithm.
     *
     * @param iRow
     * @param jPixel
     * @param s0
     * @param nadirViewADS
     * @param forwardViewADS
     * @param scanPixelADS
     * @param geolocationADS
     * @param ADSScanYList
     * @param pixelNewPositionsAndTimes
     * @param pixelRelativeNumbers
     * @param parameters
     */
    public static void getPixelPositionsAcquisitionTimes(int iRow,
                                                         int jPixel,
                                                         int s0,
                                                         ProductNodeGroup<MetadataElement> nadirViewADS,
                                                         ProductNodeGroup<MetadataElement> forwardViewADS,
                                                         ProductNodeGroup<MetadataElement> scanPixelADS,
                                                         ProductNodeGroup<MetadataElement> geolocationADS,
                                                         List<Double> ADSScanYList,
                                                         double[] pixelNewPositionsAndTimes,
                                                         int[] pixelRelativeNumbers,
                                                         InputParameters parameters) {
        /* This function returns the latitude, longitude and acquisition time (for nadir and forward views) for pixel i,j
         units are (degrees*1.0e6) and (mjd2000)
         */

        /* Variable Declaration */
        int[] scanAndPixelIndices = {0, 0};
        double[] pixelCoordinatesAndTime = {0.0, 0.0, 0.0};
        double[] pixelLatsLongs = {0.0, 0.0};
        int firstNadirPixel = parameters.firstNadirPixel;
        int firstForwardPixel = parameters.firstForwardPixel;
        boolean nadirFlag;

        /* Compute the nadir view first
         Find the instrument scan number and instrument pixel number for image pixel i,j
         Note if the returned scan or pixel numbers are 0, (could arise if pixel is absent or cosmetically filled...)
         then return fill flags because original geolocation is undefined.
         Fill flags:
         -999999.0 No data (No ADS or cosmetic pixel)
         -888888.0 For pixels with scan number <=32 (First ADS starts at row 32).
         */

        ScanAndPixelIndicesExtractor.searchScanAndPixelNumberADS(iRow, jPixel, nadirViewADS, scanAndPixelIndices);

        pixelRelativeNumbers[0] = scanAndPixelIndices[1];

        if (scanAndPixelIndices[0] == 0 || scanAndPixelIndices[1] == 0) {

            pixelNewPositionsAndTimes[0] = -999999.0;
            pixelNewPositionsAndTimes[1] = -999999.0;
            pixelNewPositionsAndTimes[2] = -999999.0;

        } else {
            PixelCoordinateInterpolator.searchScanPixelADS(scanAndPixelIndices, s0, scanPixelADS, firstNadirPixel, pixelCoordinatesAndTime);
            if (parameters.cornerReferenceFlag) {
                PixelCoordinateInterpolator.convertCentreLocationToReference(pixelCoordinatesAndTime, iRow, geolocationADS);
            }
            nadirFlag = true;
            GeolocationInterpolator.searchGeolocationADS(pixelCoordinatesAndTime[0], pixelCoordinatesAndTime[1], geolocationADS, ADSScanYList, pixelLatsLongs, parameters.topographicFlag, nadirFlag, parameters.topographyHomogenity);
            pixelNewPositionsAndTimes[0] = pixelLatsLongs[0];
            pixelNewPositionsAndTimes[1] = pixelLatsLongs[1];
            pixelNewPositionsAndTimes[2] = pixelCoordinatesAndTime[2];
        }

        /* Now compute the forward view */

        ScanAndPixelIndicesExtractor.searchScanAndPixelNumberADS(iRow, jPixel, forwardViewADS, scanAndPixelIndices);

        pixelRelativeNumbers[1] = scanAndPixelIndices[1];

        if (scanAndPixelIndices[0] == 0 || scanAndPixelIndices[1] == 0) {

            pixelNewPositionsAndTimes[3] = -999999.0;
            pixelNewPositionsAndTimes[4] = -999999.0;
            pixelNewPositionsAndTimes[5] = -999999.0;

        } else {

            PixelCoordinateInterpolator.searchScanPixelADS(scanAndPixelIndices, s0, scanPixelADS, firstForwardPixel, pixelCoordinatesAndTime);
            if (parameters.cornerReferenceFlag) {
                PixelCoordinateInterpolator.convertCentreLocationToReference(pixelCoordinatesAndTime, iRow, geolocationADS);
            }
            nadirFlag = false;
            GeolocationInterpolator.searchGeolocationADS(pixelCoordinatesAndTime[0], pixelCoordinatesAndTime[1], geolocationADS, ADSScanYList, pixelLatsLongs, parameters.topographicFlag, nadirFlag, parameters.topographyHomogenity);
            pixelNewPositionsAndTimes[3] = pixelLatsLongs[0];
            pixelNewPositionsAndTimes[4] = pixelLatsLongs[1];
            pixelNewPositionsAndTimes[5] = pixelCoordinatesAndTime[2];
        }
    }

    private static void getPixelProjection(InputParameters parameters, double[] pixelDimensions, int[] pixelRelativeNumbers) {
        /* This function takes the regridded IFOV and then computes the nadir & forward pixels FOV */
        /* This code has been translated from IDL code provided by RAL */
        double coneAngle = Math.toRadians(23.45);
        double gridInterval = 20.0; /* Arc seconds */
        double radius = 6371.0;
        double altitude = 800.0;
        double PI = 3.14159265358979323846;
        double sinConeAngle = Math.sin(coneAngle);
        double tanConeAngle = Math.tan(coneAngle);
        pixelDimensions[0] = 0.0;
        pixelDimensions[1] = 0.0;
        pixelDimensions[2] = 0.0;
        pixelDimensions[3] = 0.0;

        double[] rotationAngle = new double[101];
        double[] nadirViewAngle = new double[101];
        double[] SSP2PixelAngle = new double[101];
        double[] acrossFOVAngles = new double[101];
        double[] alongFOVAngles = new double[101];

        /* Compute geometry for both pixels */
        for (int k = 0; k < 2; k++) {

            /* Calculate the viewing angles for a given pixel */
            int pixel = pixelRelativeNumbers[k];


            /* If the pixel number is 0, i.e. data not available, place fill value */
            if (pixel <= 0) {
                if (k == 0) {
                    pixelDimensions[0] = -999999.0;
                    pixelDimensions[1] = -999999.0;
                } else if (k == 1) {
                    pixelDimensions[2] = -999999.0;
                    pixelDimensions[3] = -999999.0;
                }
                break;
            }

            for (int i = 0; i < 101; i++) {
                rotationAngle[i] = i;
                rotationAngle[i] = 2.0 * PI * (pixel - ((rotationAngle[i] - 50.0) / 100.0) - 501.0) / 2000.0;
                nadirViewAngle[i] = 2.0 * Math.asin(sinConeAngle * Math.sin(rotationAngle[i] / 2.0));
                SSP2PixelAngle[i] = Math.acos(Math.tan(nadirViewAngle[i] / 2.0) / tanConeAngle);

                /* If the angle of rotation is greater than pi, reverse sign of sub satellite point to pixel angle */
                if (Math.abs(rotationAngle[i]) > PI) {
                    SSP2PixelAngle[i] = -SSP2PixelAngle[i];
                }

                /* Calculate angles for FOV response */
                acrossFOVAngles[i] = Math.asin(Math.sin(nadirViewAngle[i]) * Math.sin(SSP2PixelAngle[i]));
                alongFOVAngles[i] = Math.acos(Math.cos(nadirViewAngle[i]) / Math.cos(acrossFOVAngles[i]));
                acrossFOVAngles[i] = Math.toDegrees(acrossFOVAngles[i] * 3600.0);
                alongFOVAngles[i] = Math.toDegrees(alongFOVAngles[i] * 3600.0);

            }

            /* Extract geometry for centre of pixel */
            double centrePixelAcrossAngle = acrossFOVAngles[50];
            double centrePixelAlongAngle = alongFOVAngles[50];
            double viewAngle = nadirViewAngle[50];
            double surfaceToPixelAngle = SSP2PixelAngle[50];

            double zenithAngle = Math.asin(Math.sin(viewAngle) * (radius + altitude) / radius);
            double acrossTrackDistance = radius * Math.asin(Math.sin(zenithAngle - viewAngle) * Math.sin(surfaceToPixelAngle));
            double alongTrackDistance = radius * Math.acos(Math.cos(zenithAngle - viewAngle) / Math.cos(acrossTrackDistance / radius));
            double acrossTrackPerpAngle = Math.atan((radius * Math.sin(acrossTrackDistance / radius)) / (radius * (1.0 - Math.cos(acrossTrackDistance / radius)) + altitude));
            double sat2PixelDistance;

            if (viewAngle != 0.0) {
                sat2PixelDistance = radius * Math.sin(zenithAngle - viewAngle) / Math.sin(viewAngle);
            } else {
                sat2PixelDistance = altitude;
            }

            /* Create arrays for output */
            double minAlongTrackPixel = 10000000.0;
            double maxAlongTrackPixel = -10000000.0;
            double minAcrossTrackPixel = 10000000.0;
            double maxAcrossTrackPixel = -10000000.0;
            double minAlongTrackFOV = 10000000.0;
            double maxAlongTrackFOV = -10000000.0;
            double minAcrossTrackFOV = 10000000.0;
            double maxAcrossTrackFOV = -10000000.0;
            for (int i = 0; i < 101; i++) {
                if (alongFOVAngles[i] > maxAlongTrackPixel) {
                    maxAlongTrackPixel = alongFOVAngles[i];
                }
                if (alongFOVAngles[i] < minAlongTrackPixel) {
                    minAlongTrackPixel = alongFOVAngles[i];
                }
                if (acrossFOVAngles[i] > maxAcrossTrackPixel) {
                    maxAcrossTrackPixel = acrossFOVAngles[i];
                }
                if (acrossFOVAngles[i] < minAcrossTrackPixel) {
                    minAcrossTrackPixel = acrossFOVAngles[i];
                }
            }
            double[] alongTrackAngle = parameters.alongTrackAngle;
            double[] acrossTrackAngle = parameters.acrossTrackAngle;

            for (int i = 0; i < 961; i++) {
                if (alongTrackAngle[i] > maxAlongTrackFOV) {
                    maxAlongTrackFOV = alongTrackAngle[i];
                }
                if (alongTrackAngle[i] < minAlongTrackFOV) {
                    minAlongTrackFOV = alongTrackAngle[i];
                }
                if (acrossTrackAngle[i] > maxAcrossTrackFOV) {
                    maxAcrossTrackFOV = acrossTrackAngle[i];
                }
                if (acrossTrackAngle[i] < minAcrossTrackFOV) {
                    minAcrossTrackFOV = acrossTrackAngle[i];
                }
            }

            int minAlongTrackIndx = (int) Math.round((minAlongTrackFOV + minAlongTrackPixel - centrePixelAlongAngle) / gridInterval);
            int maxAlongTrackIndx = (int) Math.round((maxAlongTrackFOV + maxAlongTrackPixel - centrePixelAlongAngle) / gridInterval);
            int minAcrossTrackIndx = (int) Math.round((minAcrossTrackFOV + minAcrossTrackPixel - centrePixelAcrossAngle) / gridInterval);
            int maxAcrossTrackIndx = (int) Math.round((maxAcrossTrackFOV + maxAcrossTrackPixel - centrePixelAcrossAngle) / gridInterval);

            double[] alongTrackAngleArray = new double[maxAlongTrackIndx - minAlongTrackIndx + 1];
            double[] acrossTrackAngleArray = new double[maxAcrossTrackIndx - minAcrossTrackIndx + 1];
            double[] FOVResponse = new double[(maxAlongTrackIndx - minAlongTrackIndx + 1) * (maxAcrossTrackIndx - minAcrossTrackIndx + 1)];


            for (int i = 0; i < (maxAlongTrackIndx - minAlongTrackIndx + 1); i++) {
                alongTrackAngleArray[i] = i * gridInterval + minAlongTrackIndx * gridInterval;
            }
            for (int i = 0; i < (maxAcrossTrackIndx - minAcrossTrackIndx + 1); i++) {
                acrossTrackAngleArray[i] = i * gridInterval + minAcrossTrackIndx * gridInterval;
            }

            double[] ifov1D = parameters.ifov1D;

            /* Calculate the FOV */
            for (int i = 0; i < 961; i++) {
                for (int j = 0; j < 101; j++) {

                    int m = (int) Math.round((alongTrackAngle[i] + alongFOVAngles[j] - centrePixelAlongAngle) / gridInterval);
                    int n = (int) Math.round((acrossTrackAngle[i] + acrossFOVAngles[j] - centrePixelAcrossAngle) / gridInterval);

                    double dm = m * gridInterval;
                    double dn = n * gridInterval;
                    int u;
                    int v;
                    if (dm >= ((alongTrackAngle[i] + alongFOVAngles[j] - centrePixelAlongAngle) - gridInterval / 2.0) && dm < ((alongTrackAngle[i] + alongFOVAngles[j] - centrePixelAlongAngle) + gridInterval / 2.0)) {
                        u = 1;
                    } else {
                        u = 0;
                    }
                    if (dn >= ((acrossTrackAngle[i] + acrossFOVAngles[j] - centrePixelAcrossAngle) - gridInterval / 2.0) && dn < ((acrossTrackAngle[i] + acrossFOVAngles[j] - centrePixelAcrossAngle) + gridInterval / 2.0)) {
                        v = 1;
                    } else {
                        v = 0;
                    }
                    FOVResponse[(n - minAcrossTrackIndx) + ((m - minAlongTrackIndx) * (maxAcrossTrackIndx - minAcrossTrackIndx + 1))]
                            = FOVResponse[(n - minAcrossTrackIndx) + ((m - minAlongTrackIndx) * (maxAcrossTrackIndx - minAcrossTrackIndx + 1))]
                            + u * v * ifov1D[i];
                }
            }

            /* Normalise the FOV */
            double maximum = -1000000.0;
            double minimum = 1000000.0;

            /* The remaining code in this method is an adaption to the RAL code to return a measure of
             the FOV extent from the 2D matrix
             * Note this method is an approximate because when (several times per scan) the position of the 
             * threshold in the 2D matrix changes, large jumps of 20 arcseconds are witnessed in the FOV
             * extent between adjacent pixels. An algorithm to interpolate between adjacent cells in the 
             * 2D FOV array could be written or the RAL code could be modified to have finer resolution 
             * (e.g. 2 arc seconds) but this was outside the scope of the initial development.
             */
            
            /* Find the minimum and maximum values from the ifov data */
            for (int i = 0; i < (maxAcrossTrackIndx - minAcrossTrackIndx + 1); i++) {
                for (int j = 0; j < (maxAlongTrackIndx - minAlongTrackIndx + 1); j++) {
                    if (FOVResponse[i + (j * ((maxAcrossTrackIndx - minAcrossTrackIndx + 1)))] > maximum) {
                        maximum = (FOVResponse[i + (j * ((maxAcrossTrackIndx - minAcrossTrackIndx + 1)))]);
                    }
                    if (FOVResponse[i + (j * ((maxAcrossTrackIndx - minAcrossTrackIndx + 1)))] < minimum) {
                        minimum = (FOVResponse[i + (j * ((maxAcrossTrackIndx - minAcrossTrackIndx + 1)))]);
                    }
                }
            }

            /* Now normalise using min/max/range */
            double range = maximum - minimum;
            for (int i = 0; i < (maxAcrossTrackIndx - minAcrossTrackIndx + 1); i++) {
                for (int j = 0; j < (maxAlongTrackIndx - minAlongTrackIndx + 1); j++) {
                    FOVResponse[i + (j * ((maxAcrossTrackIndx - minAcrossTrackIndx + 1)))] = (FOVResponse[i + (j * ((maxAcrossTrackIndx - minAcrossTrackIndx + 1)))] - minimum) / range;
                }
            }

            for (int i = 0; i < (maxAlongTrackIndx - minAlongTrackIndx + 1); i++) {
                alongTrackAngleArray[i] = ((Math.toRadians(alongTrackAngleArray[i]) / 3600) * sat2PixelDistance) / (Math.cos((Math.toRadians(centrePixelAlongAngle) / 3600) + (alongTrackDistance / radius)));
            }

            for (int i = 0; i < (maxAcrossTrackIndx - minAcrossTrackIndx + 1); i++) {
                acrossTrackAngleArray[i] = ((Math.toRadians(acrossTrackAngleArray[i]) / 3600) * sat2PixelDistance) / (Math.cos(acrossTrackPerpAngle + (acrossTrackDistance / radius)));
            }

            /* Find extents of FOV data in matrix i.e. top, bottom, left, right extent*/
            int left = 0;
            int right = 0;
            int top = 0;
            int bottom = 0;
            int found = 0;

            double top_extent_1 = 0;
            double top_extent_2 = 0;
            double bottom_extent_1 = 0;
            double bottom_extent_2 = 0;
            double right_extent_1 = 0;
            double right_extent_2 = 0;
            double left_extent_1 = 0;
            double left_extent_2 = 0;
            
            double extent = parameters.pixelIFOVReportingExtent;
            for (int i = 0; i < (maxAcrossTrackIndx - minAcrossTrackIndx + 1); i++) {
                if (found == 1) {
                    break;
                }
                for (int j = 0; j < (maxAlongTrackIndx - minAlongTrackIndx + 1); j++) {

                    if (FOVResponse[i + (j * ((maxAcrossTrackIndx - minAcrossTrackIndx + 1)))] >= extent) {
                        top = i;
                        found = 1;
                        top_extent_1 = FOVResponse[i + (j * ((maxAcrossTrackIndx - minAcrossTrackIndx + 1)))];
                        top_extent_2 = FOVResponse[(i-1) + (j * ((maxAcrossTrackIndx - minAcrossTrackIndx + 1)))];
                        break;
                    }
                }
            }

            found = 0;
            for (int i = 0; i < (maxAcrossTrackIndx - minAcrossTrackIndx + 1); i++) {
                if (found == 1) {
                    break;
                }
                for (int j = 0; j < (maxAlongTrackIndx - minAlongTrackIndx + 1); j++) {
                    if (FOVResponse[((maxAcrossTrackIndx - minAcrossTrackIndx) - i) + (j * (maxAcrossTrackIndx - minAcrossTrackIndx + 1))] >= extent) {
                        bottom = i + 1;
                        found = 1;
                        bottom_extent_1 = FOVResponse[((maxAcrossTrackIndx - minAcrossTrackIndx) - i) + (j * (maxAcrossTrackIndx - minAcrossTrackIndx + 1))];
                        bottom_extent_2 = FOVResponse[((maxAcrossTrackIndx - minAcrossTrackIndx) - (i-1)) + (j * (maxAcrossTrackIndx - minAcrossTrackIndx + 1))];
                        break;
                    }
                }
            }

            found = 0;
            for (int j = 0; j < (maxAlongTrackIndx - minAlongTrackIndx + 1); j++) {
                if (found == 1) {
                    break;
                }
                for (int i = 0; i < (maxAcrossTrackIndx - minAcrossTrackIndx + 1); i++) {

                    if (FOVResponse[i + (j * (maxAcrossTrackIndx - minAcrossTrackIndx + 1))] >= extent) {
                        left = j;
                        found = 1;
                        left_extent_1 = FOVResponse[i + (j * (maxAcrossTrackIndx - minAcrossTrackIndx + 1))];
                        left_extent_2 = FOVResponse[i + ((j-1) * (maxAcrossTrackIndx - minAcrossTrackIndx + 1))];
                        break;
                    }
                }
            }

            found = 0;
            for (int j = 0; j < (maxAlongTrackIndx - minAlongTrackIndx + 1); j++) {
                if (found == 1) {
                    break;
                }
                for (int i = 0; i < (maxAcrossTrackIndx - minAcrossTrackIndx + 1); i++) {

                    if (FOVResponse[i + (((maxAlongTrackIndx - minAlongTrackIndx) - j) * (maxAcrossTrackIndx - minAcrossTrackIndx + 1))] >= extent) {
                        right = j + 1;
                        found = 1;
                        right_extent_1 = FOVResponse[i + (((maxAlongTrackIndx - minAlongTrackIndx) - j) * (maxAcrossTrackIndx - minAcrossTrackIndx + 1))];
                        right_extent_2 = FOVResponse[i + (((maxAlongTrackIndx - minAlongTrackIndx) - (j-1)) * (maxAcrossTrackIndx - minAcrossTrackIndx + 1))];
                        break;
                    }
                }
            }

            //TODO reinstate new code (version 3) java.lang.ClassNotFoundException: javax.media.jai.ParameterBlockJAI

            // 1. Old code where threshold position was not interpolated (i.e. the cell where intensity > threshold/extent)
            double pixelAcrossDistance = acrossTrackAngleArray[(maxAcrossTrackIndx - minAcrossTrackIndx + 1) - bottom] - acrossTrackAngleArray[top];
            double pixelAlongDistance = alongTrackAngleArray[(maxAlongTrackIndx - minAlongTrackIndx + 1) - right] - alongTrackAngleArray[left];

            // 2. Old code that interpolates for threshold between array cells using linear interpolation
/*
            double bottomDistance_1 = acrossTrackAngleArray[(maxAcrossTrackIndx - minAcrossTrackIndx + 1) - bottom];
            double bottomDistance_2 = acrossTrackAngleArray[(maxAcrossTrackIndx - minAcrossTrackIndx + 1) - bottom+1];
            double bottomDistance = linearInterp(bottom_extent_1, bottom_extent_2, bottomDistance_1, bottomDistance_2, extent);

            double topDistance_1 = acrossTrackAngleArray[top];
            double topDistance_2 = acrossTrackAngleArray[top-1];
            double topDistance = linearInterp(top_extent_1, top_extent_2, topDistance_1, topDistance_2, extent);

            double rightDistance_1 = alongTrackAngleArray[(maxAlongTrackIndx - minAlongTrackIndx + 1) - right];
            double rightDistance_2 = alongTrackAngleArray[(maxAlongTrackIndx - minAlongTrackIndx + 1) - right+1];
            double rightDistance = linearInterp(right_extent_1, right_extent_2, rightDistance_1, rightDistance_2, extent);

            double leftDistance_1 = alongTrackAngleArray[left];
            double leftDistance_2 = alongTrackAngleArray[left-1];
            double leftDistance = linearInterp(left_extent_1, left_extent_2, leftDistance_1, leftDistance_2, extent);

            double pixelAcrossDistance = bottomDistance - topDistance;
            double pixelAlongDistance = rightDistance - leftDistance;
*/
            // 3. New code that interpolates for threshold by generating a contour around FOV extent in array
/*
            // Exception here: java.lang.ClassNotFoundException: javax.media.jai.ParameterBlockJAI
            FOVContour contouredExtent = new FOVContour();
            contouredExtent.createContour(maxAcrossTrackIndx, maxAlongTrackIndx, minAcrossTrackIndx, minAlongTrackIndx, FOVResponse, extent);
            contouredExtent.getExtent(acrossTrackAngleArray, alongTrackAngleArray);
            double pixelAcrossDistance = contouredExtent.pixelAcrossDistance;
            double pixelAlongDistance = contouredExtent.pixelAlongDistance;
*/
            if (k == 0) {
                pixelDimensions[0] = pixelAlongDistance;
                pixelDimensions[1] = pixelAcrossDistance;
            } else if (k == 1) {
                pixelDimensions[2] = pixelAlongDistance;
                pixelDimensions[3] = pixelAcrossDistance;
            }
        }
    }

    public static double linearInterp(double x0, double x1, double y0, double y1, double x){
        double y = y0 + ((y1-y0)*((x - x0)/(x1 - x0)));
        return y;
    }

    public static void getConstantPixelProjection(InputParameters parameters, List<List<Double>> pixelProjectionMap) {
        // Assuming spherical earth geometry & constant altitude results in each pixel number having a constant projection dimension
        for (int i = 0; i < 2000; i++) {
            double[] pixelDimensions = new double[4];
            int[] pixelRelativeNumbers = {i, 0};
            getPixelProjection(parameters, pixelDimensions, pixelRelativeNumbers);
            List<Double> projection = new ArrayList<>();
            projection.add(pixelDimensions[0]);
            projection.add(pixelDimensions[1]);
            pixelProjectionMap.add(projection);
        }
    }
}
