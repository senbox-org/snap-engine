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

import com.vividsolutions.jts.geom.Coordinate;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.List;

//import jaitools.media.jai.contour.ContourDescriptor;

/**
 *
 * @author abeaton
 */
public class FOVContour {

    Coordinate[] coordinates;
    double pixelAcrossDistance;
    double pixelAlongDistance;

    public FOVContour() {

    }

    public void createContour(double maxAcrossTrackIndex, double maxAlongTrackIndex, double minAcrossTrackIndex, double minAlongTrackIndex, double[] FOVResponse, double extent) {
        // First create a tiled image from FOV array using JAI library.
        int width = (int) (maxAcrossTrackIndex - minAcrossTrackIndex + 1);
        int height = (int) (maxAlongTrackIndex - minAlongTrackIndex + 1);
        
        // Use a sample model to write double values to the image
        SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_DOUBLE, width, height, 1);
        TiledImage tiledImage = new TiledImage(0, 0, width, height, 0, 0, sampleModel, null);
        WritableRaster wr = tiledImage.getWritableTile(0, 0);

        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                wr.setSample(w, h, 0, FOVResponse[w + (h * (width))]);
            }
        }

        // Now generate a contour around the tiled image using the user specified "extent" of the intensity
        ParameterBlockJAI pb = new ParameterBlockJAI("Contour");
        pb.setSource("source0", tiledImage);
        List<Double> levels = Arrays.asList(new Double[]{extent});
        pb.setParameter("levels", levels);
        pb.setParameter("smooth", Boolean.TRUE);
        RenderedOp dest = JAI.create("Contour", pb);

     /*   PDB hiding until we import JAI
        // Get the coordinates of the contour (in new image reference frame)
        Collection<LineString> contours = (Collection<LineString>) dest.getProperty(ContourDescriptor.CONTOUR_PROPERTY_NAME);
        LineString contour = contours.iterator().next();
        this.coordinates = contour.getCoordinates();
*/
    }

    public void getExtent(double[] acrossTrackAngleArray, double[] alongTrackAngleArray) {
        // Get max and min of coordinates from FOV array
        double maxX = coordinates[0].x;
        double maxY = coordinates[0].y;
        double minX = maxX;
        double minY = maxY;

        for (Coordinate coordinate : coordinates) {
            double x = coordinate.x;
            double y = coordinate.y;
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
        }
        
        // From the angle/distance array, linearly interpolate to get the extent distance between cells.
        //width = x = across track, height = y = along
        double acrossDistance1 = Calculator.linearInterp(Math.floor(maxX), Math.ceil(maxX), acrossTrackAngleArray[(int) Math.floor(maxX)], acrossTrackAngleArray[(int) Math.ceil(maxX)], maxX);
        double acrossDistance2 = Calculator.linearInterp(Math.floor(minX), Math.ceil(minX), acrossTrackAngleArray[(int) Math.floor(minX)], acrossTrackAngleArray[(int) Math.ceil(minX)], minX);
        this.pixelAcrossDistance = acrossDistance1 - acrossDistance2;

        double alongDistance1 = Calculator.linearInterp(Math.floor(maxY), Math.ceil(maxY), alongTrackAngleArray[(int) Math.floor(maxY)], alongTrackAngleArray[(int) Math.ceil(maxY)], maxY);
        double alongDistance2 = Calculator.linearInterp(Math.floor(minY), Math.ceil(minY), alongTrackAngleArray[(int) Math.floor(minY)], alongTrackAngleArray[(int) Math.ceil(minY)], minY);
        this.pixelAlongDistance = alongDistance1 - alongDistance2;
    }
}
