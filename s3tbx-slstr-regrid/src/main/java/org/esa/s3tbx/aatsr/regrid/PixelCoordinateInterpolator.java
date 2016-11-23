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

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;

/**
 *
 * @author ABeaton, Telespazio VEGA UK Ltd 30/10/2013
 *
 * Contact: alasdhair(dot)beaton(at)telespazio(dot)com
 *
 */
class PixelCoordinateInterpolator {

    static void searchScanPixelADS(int[] scanAndPixelIndices, int s0, ProductNodeGroup<MetadataElement> scanPixelADS, int firstPixelNumber, double[] pixelCoordinatesAndTime) {

        /* This function finds the pixel coordinates using the instrument scan and instrument pixel numbers and the pixel acqusition time
         Note that this methodology is taken from a Technical Note by Andrew Birks of Rutherford Appelton Laboratory.
         "Instrument Pixel Co-ordinates and Measurement Times from AATSR Products",
         available @ (https://earth.esa.int/handbooks/Instrument_Pixel_Coordinates_Measurement_Times_AATSR_Products.html) (Link accessed 01/08/2013)
         This particular function maps to step 2 of the presented methodology.
         */
        int s = scanAndPixelIndices[0];
        int p = scanAndPixelIndices[1];

        /* Find the tie-scan corresponding to/before the input scan number */
        int sg = (int) Math.floor(((double) s - (double) s0) / (double) 32);

        /* The scanPixelADS begins with scan 32, so for scans 1-31 the coordinates cannot be solved via interpolation.
         Future: investigate possibility to extrapolate data backward
         Now: retrieve first ADS record and correct for this
         */
        if (sg < 0) {
            sg = 0;
        }

        /* Retrieve the correct record that is before or contains the scan number
         Note that the scanPixelADS ends (i.e. acquisition time) sometime before the MDS and other ADS,
         when this happens, retrieve the last record of the scanPixelADS
         */
        int numberOfADSRecords = scanPixelADS.getNodeCount();

        if (sg > (numberOfADSRecords - 1)) {
            sg = numberOfADSRecords - 1;
        }

        MetadataElement scanADSRecord = scanPixelADS.get(sg);

        /* If data gaps are present, this may be the incorrect record (does not correspond to/before input scan number)
         Check to see if this is the case, if during testing this frequently occurs, may have to implement search algorithm
         */
        int instrumentScanNumberCheck = (32 * sg) + s0;

        int currentRecordScanNumber = scanADSRecord.getAttributeAt(2).getData().getElemInt();

        if (currentRecordScanNumber != instrumentScanNumberCheck) {
            System.out.println("Data Gaps are present in Scan Pixel ADS");
            System.exit(1);
        }

        /* Calculate the relative pixel index using the absolute pixel index */
        int relativePixelIndex = p - firstPixelNumber;

        /* Calculate the tie pixel and the part thereof */
        double tiePixelPart = relativePixelIndex / 10.0;
        int tiePixel = (int) Math.floor(tiePixelPart);

        /* Calculate the interpolation weight (i.e. the distance between tie-points)
         Note that the weight is corrected for the short interval at the end of the Nadir Pixel Scan, other intervals are 10 pixels
         the final interval is only 4.
         */
        double weight = (tiePixelPart - tiePixel);

        if (relativePixelIndex > 570) {
            weight = (relativePixelIndex - 570) / 4.0;
        }

        /* If interpolating for the forward view, need to interpolate between the later 40 tie points */
        if (firstPixelNumber > 1000) {
            tiePixel += 59;
        }

        /* Calculate the x and y coordinates of the relative pixel in the tie scan ADS */
        MetadataAttribute tiePixXList = scanADSRecord.getAttributeAt(3);
        int[] xCoordinateList = (int[]) tiePixXList.getDataElems();
        MetadataAttribute tiePixYList = scanADSRecord.getAttributeAt(4);
        int[] yCoordinateList = (int[]) tiePixYList.getDataElems();

        double xCoord = ((1 - weight) * xCoordinateList[tiePixel]) + (weight * xCoordinateList[tiePixel + 1]);
        double yCoord = ((1 - weight) * yCoordinateList[tiePixel]) + (weight * yCoordinateList[tiePixel + 1]);

        /* If the instrument scan occurs between ADS records, interpolate between the coordinates we have for the current record and
         the coordinates from the next record using linear interpolation. Follow the same process as above and then interpolate.
         */
        if (s != currentRecordScanNumber && s > s0 && sg < (numberOfADSRecords - 1)) {
            MetadataElement scanADSRecord2 = scanPixelADS.get(sg + 1);

            int nextRecordScanNumber = scanADSRecord2.getAttributeAt(2).getData().getElemInt();
            tiePixXList = scanADSRecord2.getAttributeAt(3);
            xCoordinateList = (int[]) tiePixXList.getDataElems();
            tiePixYList = scanADSRecord2.getAttributeAt(4);
            yCoordinateList = (int[]) tiePixYList.getDataElems();

            double xCoordNext = ((1 - weight) * xCoordinateList[tiePixel]) + (weight * xCoordinateList[tiePixel + 1]);
            double yCoordNext = ((1 - weight) * yCoordinateList[tiePixel]) + (weight * yCoordinateList[tiePixel + 1]);

            /* Now interpolate between coordinates using the scan number*/

            xCoord = xCoord + (((s - currentRecordScanNumber) * (xCoordNext - xCoord)) / (nextRecordScanNumber - currentRecordScanNumber));
            yCoord = yCoord + (((s - currentRecordScanNumber) * (yCoordNext - yCoord)) / (nextRecordScanNumber - currentRecordScanNumber));

        }
        /* Get the pixel sample time */

        double pixelTime = solvePixelTime(scanADSRecord, s, sg, s0, p, currentRecordScanNumber);

        /* Store the results */
        pixelCoordinatesAndTime[0] = xCoord;
        pixelCoordinatesAndTime[1] = yCoord;
        pixelCoordinatesAndTime[2] = pixelTime;
    }

    private static double solvePixelTime(MetadataElement scanADSRecord, int s, int sg, int s0, int p, int currentRecordScanNumber) {
        /* This function reads the scan record instrument scan time and then calculates the pixel sample time.
         Note the return of this function is unit: MJD2000
         */
        /* Get scan time of record as a double */
        ProductData dsrTime = scanADSRecord.getAttributeAt(0).getData();
        
        double scanTime = dsrTime.getElemIntAt(0) + (dsrTime.getElemDoubleAt(1) / 86400.0) + ((dsrTime.getElemDoubleAt(2) / 1.0e6) / 86400.0);
        
        /* If the scan number does not occur at this granule, correct the time */
        if (s != currentRecordScanNumber && s > s0) {
            double scanTimeAlongTrackMod = ((0.15 * (s - (32 * sg) - s0)) / 86400.0);
            scanTime = scanTime + scanTimeAlongTrackMod;
        }
        /* Calculate exact pixel sample time */
        double scanTimeModScan = (((p - 1) * (0.15 / 2000.0)) / 86400.0);
        double pixelSampleTime = scanTime + scanTimeModScan;

        
        return pixelSampleTime;
    }

    static void convertCentreLocationToReference(double[] pixelCoordinatesAndTime, int i, ProductNodeGroup<MetadataElement> geolocationADS) {
        /* This function converts the pixel coordinates referenced to the centre of the pixel to pixel coordinates referenced to the bottom left corner of the pixel 
         * Note that this step is missing from the Technical Note "Instrument Pixel Co-ordinates and Measurement Times from AATSR Products"
         * 
         * Also note that the pixel spacing along track is not constant ~= 1011 metres. The BEAM Java API does not provide functions
         * for returning the exact pixel along track coordinate. As an approximation, the y coordinates of the ADS are extracted,
         * then divided by the ADS spacing (32 rows) to produce an estimate average pixel along track coordinate.
         */

        
        pixelCoordinatesAndTime[0] -= 500.0;

        /* Find the tie-scan corresponding to/before the input row number */
        int sg = (int) Math.floor(((double) i - (double) 0) / (double) 32);

        int lastADSCoordinate = geolocationADS.get(sg).getAttribute("img_scan_y").getData().getElemInt();
        int nextADSCoordinate;
        if ((sg + 1) < geolocationADS.getNodeCount()) {
            nextADSCoordinate = geolocationADS.get(sg + 1).getAttribute("img_scan_y").getData().getElemInt();
        } else {
            nextADSCoordinate = lastADSCoordinate += (32 * 1011); // An approximation for the end of the product
                                                                  // Note not needed if the product is "trimmed"
        }

        double averagePixelSpacing = (nextADSCoordinate - lastADSCoordinate) / 32.0;
        
        double currentPixelCoordinate = lastADSCoordinate + ((i - (sg * 32)) * averagePixelSpacing);
        double nextPixelCoordinate = lastADSCoordinate + (((i+1) - (sg * 32)) * averagePixelSpacing);
        
        double stepSizeY = (nextPixelCoordinate - currentPixelCoordinate)/2.0;
        
        pixelCoordinatesAndTime[1] -= stepSizeY;
    }
}
