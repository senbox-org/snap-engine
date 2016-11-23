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
 * @author ABeaton, Telespazio VEGA UK Ltd 30/10/2013
 *         <p>
 *         Contact: alasdhair(dot)beaton(at)telespazio(dot)com
 */
class ScanAndPixelIndicesExtractor {

    static void searchScanAndPixelNumberADS(int iRow, int jPixel, ProductNodeGroup<MetadataElement> viewADS, int[] scanAndPixelIndices) {
        /* This function finds the instrument scan and instrument pixel numbers of pixel i,j using the appropriate view ADS
         Note that this methodology is taken from a Technical Note by Andrew Birks of Rutherford Appelton Laboratory.
         "Instrument Pixel Co-ordinates and Measurement Times from AATSR Products",
         available @ (https://earth.esa.int/handbooks/Instrument_Pixel_Coordinates_Measurement_Times_AATSR_Products.html) (Link accessed 01/08/2013)
         This particular function maps to step 1 of the presented methodology.
         */

        //System.out.println("searchScanAndPixelNumberADS(" + iRow + ", " + jPixel + ")" );
        /* Calculate the granule index ig */
        int ig = (int) Math.floor(iRow / 32.0);

        /* Calculate partial granule index idash */
        int idash = iRow - (32 * ig);

        /* Fetch the viewADS record corresponding to the granule index */
        MetadataElement adsRecord = viewADS.get(ig);

        // Get instrument scan number list from record (note have to convert from short due to number of scans...)
        MetadataAttribute instrumentScanNumbering = adsRecord.getAttribute("instr_scan_num");
        ProductData scanNumbering = instrumentScanNumbering.getData();
        int[] scanNumbers = new int[(int) instrumentScanNumbering.getNumDataElems()];
        for (int k = 0; k < instrumentScanNumbering.getNumDataElems(); k++) {
            scanNumbers[k] = (int) scanNumbering.getElemIntAt(k);
        }
        // Get instrument pixel number list
        MetadataAttribute pixelNumbering = adsRecord.getAttribute("pix_num");
        short[] pixelNumbers = (short[]) pixelNumbering.getDataElems();

        /* Get instrument scan (s) and pixel (p) number for image pixel.
            Modify the scan number if the image pixel comes from a granule inbetween the ADS samples (every 32 granules.
         */
        int s = scanNumbers[jPixel] + idash;
        int p = (int) pixelNumbers[jPixel];

        /* Return the indices */

        scanAndPixelIndices[0] = s;
        scanAndPixelIndices[1] = p;

    }
}
