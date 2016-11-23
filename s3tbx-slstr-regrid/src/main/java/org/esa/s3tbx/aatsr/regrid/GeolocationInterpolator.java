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
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;

import java.util.List;

/**
 *
 * @author ABeaton, Telespazio VEGA UK Ltd 30/10/2013
 *
 * Contact: alasdhair(dot)beaton(at)telespazio(dot)com
 *
 * 
 */
class GeolocationInterpolator {

    static void searchGeolocationADS(double xCoordinate, double yCoordinate, ProductNodeGroup<MetadataElement> geolocationADS, List<Double> ADSScanYList, double[] pixelLatsLongs, boolean topographicFlag, boolean nadirFlag, double topographyHomogenity) {
        /* This function finds the pixel geolocation using the provided instrument coordinates and the geolocationADS.
         Note that this methodology is extracted from the AATSR Frequently Asked Questions (FAQ) document
         "Appendix A Interpolations of pixel geolocation in AATSR full resolution products"
         available @ (https://earth.esa.int/instruments/aatsr/faq/) (Link accessed 02/08/2013)
         */

        /* Convert x coordinate into km */
        xCoordinate /= 1000.0;

        /* Find the index of the tie-point to the left of this coordinate */
        int jg = (int) Math.floor((xCoordinate + 275.0) / 25.0);

        /* Get the X interpolation weighting */
        double wx = ((xCoordinate + 275.0) / 25.0) - jg;

        /* Find the index of the tie point to the top */
        int ig = 0;
        int finalCount = ADSScanYList.size();
        for (int count = 0; count < finalCount; count++) {
            if (yCoordinate >= ADSScanYList.get(count)) {
                ig = count;
            } else {
                break;
            }
        }

        /* For ATSR-1/2 Geolocation ADS does not contain all yCoordinates for image pixels
         return fill values for geolocation (-999999.0) Typically occurs for image final row*/
        if (ig + 1 < ADSScanYList.size()) {

            /* Get the Y interpolation weighting */
            double wy = (yCoordinate - ADSScanYList.get(ig)) / (ADSScanYList.get(ig + 1) - ADSScanYList.get(ig));

            /* Get the geolocation records for ig and ig+1 */
            MetadataElement geoRecordIg = geolocationADS.get(ig);
            MetadataElement geoRecordIgPlus1 = geolocationADS.get(ig + 1);

            /* Calculate the latitude of the pixel */
            ProductData geoRecordIgLatitude = geoRecordIg.getAttribute("tie_pt_lat").getData();
            int igGeoLatitude1 = geoRecordIgLatitude.getElemIntAt(jg);
            int igGeoLatitude2 = geoRecordIgLatitude.getElemIntAt(jg + 1);
            ProductData geoRecordIgPlus1Latitude = geoRecordIgPlus1.getAttribute("tie_pt_lat").getData();
            int igGeoPlus1Latitude1 = geoRecordIgPlus1Latitude.getElemIntAt(jg);
            int igGeoPlus1Latitude2 = geoRecordIgPlus1Latitude.getElemIntAt(jg + 1);

            double phi1 = igGeoLatitude1 + wx * (igGeoLatitude2 - igGeoLatitude1);
            double phi2 = igGeoPlus1Latitude1 + wx * (igGeoPlus1Latitude2 - igGeoPlus1Latitude1);

            double latitude = phi1 + (wy * (phi2 - phi1));

            /* Calculate the longitude of the pixel, check and account for presence  of the 180 degree meridian
             This check finds the minimum and maximum longitudes and checks to see if the difference between
             is greater than 180 degrees, if so, 360 is added to negative longitudes prior to interpolation.
             After interpolation, the longitude is translated back into -180<long<180 by subtracting 360 if the value exceeds 180
             */
            ProductData geoRecordIgLongitude = geoRecordIg.getAttribute("tie_pt_long").getData();
            int igGeoLongitude1 = geoRecordIgLongitude.getElemIntAt(jg);
            int igGeoLongitude2 = geoRecordIgLongitude.getElemIntAt(jg + 1);
            ProductData geoRecordIgPlus1Longitude = geoRecordIgPlus1.getAttribute("tie_pt_long").getData();
            int igGeoPlus1Longitude1 = geoRecordIgPlus1Longitude.getElemIntAt(jg);
            int igGeoPlus1Longitude2 = geoRecordIgPlus1Longitude.getElemIntAt(jg + 1);

            double minLongitude = getMinValue(igGeoLongitude1, igGeoLongitude2, igGeoPlus1Longitude1, igGeoPlus1Longitude2);
            double maxLongitude = getMaxValue(igGeoLongitude1, igGeoLongitude2, igGeoPlus1Longitude1, igGeoPlus1Longitude2);

            if ((maxLongitude - minLongitude) > (180.0 * 1.0e6)) {
                if (igGeoLongitude1 < 0) {
                    igGeoLongitude1 += 360.0 * 1.0e6;
                }
                if (igGeoLongitude2 < 0) {
                    igGeoLongitude2 += 360.0 * 1.0e6;
                }
                if (igGeoPlus1Longitude1 < 0) {
                    igGeoPlus1Longitude1 += 360.0 * 1.0e6;
                }
                if (igGeoPlus1Longitude2 < 0) {
                    igGeoPlus1Longitude2 += 360.0 * 1.0e6;
                }
            }

            double lambda1 = igGeoLongitude1 + wx * (igGeoLongitude2 - igGeoLongitude1);
            double lambda2 = igGeoPlus1Longitude1 + wx * (igGeoPlus1Longitude2 - igGeoPlus1Longitude1);

            double longitude = lambda1 + wy * (lambda2 - lambda1);
            if (longitude > (180.0 * 1.0e6)) {
                longitude -= 360.0 * 1.0e6;
            }

            /* Store the results */
            pixelLatsLongs[0] = latitude / 1.0e6;
            pixelLatsLongs[1] = longitude / 1.0e6;

            /* Apply Topographic Corrections */
            if (topographicFlag) {
                /* When the pixel is close to a tie-point (depending on user defined
                 * topography homogenity parameter) extract the topographic
                 * corrections from the current Geolocation ADS record, then add the correction 
                 * to the computed latitudes/longitudes.
                 */
                double count = xCoordinate /25.0;
                double remainder = count % 1;
                double integer = count - remainder;
                if (xCoordinate > integer*25.0 - topographyHomogenity && xCoordinate < integer*25.0 + topographyHomogenity) {
                    if (yCoordinate > ADSScanYList.get(ig) - (topographyHomogenity*1000) && yCoordinate < ADSScanYList.get(ig) + (topographyHomogenity*1000)) {
                        if (nadirFlag) {
                            ProductData geoRecordNadLatCorr = geoRecordIg.getAttribute("lat_corr_nadv").getData();
                            ProductData geoRecordNadLongCorr = geoRecordIg.getAttribute("long_corr_nadv").getData();
                            int nadLatCorr = geoRecordNadLatCorr.getElemIntAt(jg);
                            int nadLongCorr = geoRecordNadLongCorr.getElemIntAt(jg);
                            if (nadLatCorr != -999999 && nadLongCorr != -999999) {
                                pixelLatsLongs[0] = (latitude + nadLatCorr) / 1.0e6;
                                pixelLatsLongs[1] = (longitude + nadLongCorr) / 1.0e6;
                            }
                        } else {
                            ProductData geoRecordFwdLatCorr = geoRecordIg.getAttribute("lat_corr_forv").getData();
                            ProductData geoRecordFwdLongCorr = geoRecordIg.getAttribute("long_corr_forv").getData();
                            int fwdLatCorr = geoRecordFwdLatCorr.getElemIntAt(jg);
                            int fwdLongCorr = geoRecordFwdLongCorr.getElemIntAt(jg);
                            if (fwdLatCorr != -999999 && fwdLongCorr != -999999) {
                                pixelLatsLongs[0] = (latitude + fwdLatCorr) / 1.0e6;
                                pixelLatsLongs[1] = (longitude + fwdLongCorr) / 1.0e6;
                            }
                        }
                    }
                }
            }
        } else {
            pixelLatsLongs[0] = -999999.0;
            pixelLatsLongs[1] = -999999.0;
        }
    }

    static int getMaxValue(int a, int b, int c, int d) {
        int biggest = a;
        if (biggest < b) {
            biggest = b;
        }
        if (biggest < c) {
            biggest = c;
        }
        if (biggest < d) {
            biggest = d;
        }
        return biggest;
    }

    static int getMinValue(int a, int b, int c, int d) {
        int smallest = a;
        if (smallest > b) {
            smallest = b;
        }
        if (smallest > c) {
            smallest = c;
        }
        if (smallest > d) {
            smallest = d;
        }
        return smallest;
    }
}
