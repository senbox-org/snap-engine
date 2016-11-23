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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ABeaton, Telespazio VEGA UK Ltd 30/10/2013
 *
 * Contact: alasdhair(dot)beaton(at)telespazio(dot)com
 *
 */
public class InputParameters {
    /*
     * This class reads in the input parameters and stores them for use in computation
     * This class also reads in the raw FOV data and regrids it using adapted IDL code provided by RAL.
     */

    public int firstForwardPixel;
    public int firstNadirPixel;
    public String FOVMeasurementDataBandName;
    public double[] alongTrackAngle;
    public double[] acrossTrackAngle;
    public double[] ifov1D;
    public double pixelIFOVReportingExtent;
    public boolean cornerReferenceFlag;
    public boolean topographicFlag;
    public double topographyHomogenity;


    public InputParameters() {

        alongTrackAngle = new double[31 * 31];
        acrossTrackAngle = new double[31 * 31];
        ifov1D = new double[31 * 31];
    }

     void parseCharacterisationFile(String L1BCharacterisationFileLocation) {
        // This method reads the first nadir & forward pixel numbers from the L1B characterisation file
        try {
            File binaryFile = new File(L1BCharacterisationFileLocation);
            if (binaryFile.exists() == false) {
                throw new RuntimeException();
            }
            // Read all the data as bytes into an array
            try (FileInputStream input = new FileInputStream(binaryFile)) {
                byte[] data = new byte[1800];
                try {
                    int read = input.read(data, 0, 1800);
                    if (read <= 0) {
                        throw new RuntimeException();
                    }
                } catch (IOException | RuntimeException ex) {
                    System.out.println(ex.getMessage());
                }

                // Location of the pixels in the byte array
                byte[] nadirPixelNumber = {data[1753], data[1754], data[1755], data[1756]};
                byte[] forwardPixelNumber = {data[1757], data[1758], data[1759], data[1760]};

                // Convert the byte arrays to integers
                boolean littleEndianFlag = false;
                if (L1BCharacterisationFileLocation.contains("AT1") || L1BCharacterisationFileLocation.contains("AT2")){
                    littleEndianFlag = true;
                }
                this.firstNadirPixel = byteArrayToInt(nadirPixelNumber, littleEndianFlag);
                this.firstForwardPixel = byteArrayToInt(forwardPixelNumber, littleEndianFlag);
                System.out.println("First Nadir Pixel is: " + this.firstNadirPixel + " and First Forward Pixel is: " + this.firstForwardPixel);
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.exit(1);
        }
    }

    private static int byteArrayToInt(byte[] b, boolean littleEndianFlag) {
        final ByteBuffer bb = ByteBuffer.wrap(b);
        bb.order(ByteOrder.BIG_ENDIAN);
        if (littleEndianFlag){
            bb.order(ByteOrder.LITTLE_ENDIAN);
        }
        return bb.getInt();
    }

    void parseRawIFOV(String FOVMeasurementFileLocation) {
        // This function extracts the raw FOV from AATSR calibration measurements following the methodology established by Dave Smith (RAL)
        /* This code has been translated from IDL code provided by RAL */
        try {
            // Read all the lines from the file and place in an array for convenience
            List<String> lines = new ArrayList<>();
            String line;
            FileReader fileReader = new FileReader(FOVMeasurementFileLocation);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }
            bufferedReader.close();

            // Set the number of points and spacing using the first line
            String[] values = lines.get(0).split(" ");
            int xstart = Integer.parseInt(values[0]);
            int ystart = Integer.parseInt(values[1]);
            int xstop = Integer.parseInt(values[2]);
            int ystop = Integer.parseInt(values[3]);
            int xstep = Integer.parseInt(values[4]);
            int ystep = Integer.parseInt(values[5]);
            int channelNumber = Integer.parseInt(values[6]);

            switch (channelNumber) {
                case 0:
                    this.FOVMeasurementDataBandName = "12um";
                    break;
                case 1:
                    this.FOVMeasurementDataBandName = "11um";
                    break;
                case 2:
                    this.FOVMeasurementDataBandName = "3.7um";
                    break;
                case 3:
                    this.FOVMeasurementDataBandName = "1.6um";
                    break;
                case 4:
                    this.FOVMeasurementDataBandName = "0.87um";
                    break;
                case 5:
                    this.FOVMeasurementDataBandName = "0.66um";
                    break;
                case 6:
                    this.FOVMeasurementDataBandName = "0.56um";
                    break;
                default:
                    System.out.println("Unable to read channel name from data");
                    System.exit(1);
            }
            System.out.println("Raw FOV data ingested is for channel: " + this.FOVMeasurementDataBandName);

            // Calculate number of x and y points in file 
            int countX = Math.abs(xstop - xstart) / xstep;
            int countY = Math.abs(ystop - ystart) / ystep;

            // Allocate the output arrays
            double[] fovArrayX = new double[(countX + 1) * (countY + 1)];
            double[] fovArrayY = new double[(countX + 1) * (countY + 1)];
            double[] ifov = new double[(countX + 1) * (countY + 1)];

            /* Definition of fov arrays */
            for (int i = 0; i < countX + 1; i++) {
                for (int j = 0; j < countY + 1; j++) {
                    fovArrayX[i + (j * (countX + 1))] = -2.0 * (xstart + i * xstep) / 1000;
                    fovArrayY[i + (j * (countX + 1))] = -1.398 * (ystart + j * ystep) / 1000;
                }
            }
            /* Skip the first set of measurements associated with scan number -1 */
            lines = lines.subList(countX + 4, lines.size());

            // Read FOV data
            for (int j = 0; j < countY + 1; j++) {
                List<String> scanLines = lines.subList((j * (countX + 3)) + 2, (j * (countX + 3)) + (countX + 3));
                for (int i = 0; i < countX + 1; i++) {
                    values = scanLines.get(i).split(" ");
                    double tempX = 0.0;
                    double tempY = 0.0;
                    if (values.length == 4) {
                        tempX = Double.parseDouble(values[1]);
                        tempY = Double.parseDouble(values[3]);
                    } else if (values.length == 2) {
                        tempX = Double.parseDouble(values[0]);
                        tempY = Double.parseDouble(values[1]);
                    } else if (values.length == 3) {
                        if (values[0].contentEquals("")) {
                            tempX = Double.parseDouble(values[1]);
                        } else {
                            tempX = Double.parseDouble(values[0]);
                        }
                        tempY = Double.parseDouble(values[2]);
                    }
                    if (channelNumber < 4) {
                        ifov[j + (i * (countX + 1))] = tempY - tempX;
                    } else if (channelNumber == 6) {
                        ifov[j + (i * (countX + 1))] = tempY + tempX;
                    } else {

                        ifov[j + (i * (countX + 1))] = tempY;
                    }
                }
            }
            bufferedReader.close();
            fileReader.close();

            /* Subtract the minimum value of each column from all column values */
            for (int j = 0; j < countY + 1; j++) {
                double minimum = 500.0;
                for (int i = 0; i < countX + 1; i++) {
                    if (ifov[j + (i * (countX + 1))] < minimum) {
                        minimum = ifov[j + (i * (countX + 1))];
                    }
                }
                for (int i = 0; i < countX + 1; i++) {
                    ifov[j + (i * (countX + 1))] = ifov[j + (i * (countX + 1))] - minimum;
                }
            }

            /* Remove underlying background drift in signal
             Note that this methodology is undocumented and used as is */
            if (channelNumber < 3) {
                /* Create incremental double matrix */
                double[] dIndexX = new double[countX + 1];
                double[] dIndexY = new double[countY + 1];
                for (int i = 0; i < countX + 1; i++) {
                    dIndexX[i] = (double) i;
                }
                for (int j = 0; j < countY + 1; j++) {
                    dIndexY[j] = (double) j;
                }


                /* Remove the drift in the X direction */
                for (int i = 0; i < 49; i++) {
                    double m = (ifov[48 + (i * (countX + 1))] - ifov[i * (countX + 1)]) / 48.0;
                    double c = ifov[i * (countX + 1)];
                    for (int j = 0; j < (countY + 1); j++) {
                        ifov[j + (i * (countX + 1))] = ifov[j + (i * (countX + 1))] - ((m * dIndexX[j]) + c);
                    }
                }

                /* Remove the drift in the Y direction */
                for (int i = 0; i < 49; i++) {
                    double m = (ifov[i + (48 * (countX + 1))] - ifov[i]) / 48.0;
                    double c = ifov[i];
                    for (int j = 0; j < (countX + 1); j++) {
                        ifov[i + (j * (countX + 1))] = ifov[i + (j * (countX + 1))] - ((m * dIndexY[j]) + c);
                    }
                }
            }

            /* Normalise the fov data */
            double maximum = -500.0;
            double minimum = 500.0;

            /* Find the minimum and maximum values from the ifov data */
            for (int i = 0; i < countX + 1; i++) {
                for (int j = 0; j < countY + 1; j++) {
                    if (ifov[i + (j * (countX + 1))] > maximum) {
                        maximum = (ifov[i + (j * (countX + 1))]);
                    }
                    if (ifov[i + (j * (countX + 1))] < minimum) {
                        minimum = (ifov[i + (j * (countX + 1))]);
                    }
                }
            }

            /* Now normalise using min/max/range */
            double range = maximum - minimum;
            for (int i = 0; i < countX + 1; i++) {
                for (int j = 0; j < countY + 1; j++) {
                    ifov[i + (j * (countX + 1))] = (ifov[i + (j * (countX + 1))] - minimum) / range;
                }
            }


            /* Establish the sum of the values in each direction */
            double sumxz = 0.0;
            double sumyz = 0.0;
            double sumz = 0.0;

            for (int i = 0; i < 49.0; i++) {
                for (int j = 0; j < 49.0; j++) {
                    sumxz = sumxz + (fovArrayX[i + (j * (countX + 1))] * ifov[i + (j * (countX + 1))]);
                    sumyz = sumyz + (fovArrayY[i + (j * (countX + 1))] * ifov[i + (j * (countX + 1))]);
                    sumz = sumz + ifov[i + (j * (countX + 1))];
                }
            }
            double sumX = sumxz / sumz;
            double sumY = sumyz / sumz;

            /* Modify the x and y arrays using the sum */
            for (int i = 0; i < countX + 1; i++) {
                for (int j = 0; j < countY + 1; j++) {
                    fovArrayX[i + (j * (countX + 1))] = fovArrayX[i + (j * (countX + 1))] - sumX;
                    fovArrayY[i + (j * (countX + 1))] = fovArrayY[i + (j * (countX + 1))] - sumY;
                }
            }
            regridFOV(fovArrayX, fovArrayY, ifov, countX, countY);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.out.println("Could not open raw FOV data, check input filename");
            System.exit(1);
        }
    }

    private void regridFOV(double[] fovArrayX, double[] fovArrayY, double[] ifov, int countX, int countY) {
        /* This function takes the normalised and drift-removed FOV data and re-grids to a new spacing then translates to 1D arrays*/
        /* This code has been translated from IDL code provided by RAL */
        /* Allocate variables for the new axis */
        double[] axisX = new double[31];
        double[] axisY = new double[31];

        /* Create axis values */
        for (int i = 0; i < 31; i++) {
            axisX[i] = (double) ((i * 20) - 300);
            axisY[i] = (double) ((i * 20) - 300);
        }

        double[] regridIfov = new double[31 * 31];

        System.out.println("Resampling IFOV using Bilinear Interpolation");

        //Reallocated arrays for "easier" interpolation
        double[][] reallocatedIFOV = new double[countX + 1][countY + 1];// IFOV matrix is x,y packed
        for (int i = 0; i < countX + 1; i++) {
            for (int j = 0; j < countY + 1; j++) {
                reallocatedIFOV[i][j] = ifov[j + (i * (countX + 1))];
            }
        }

        double[] reallocatedFovArrayX = new double[countX + 1];
        double[] reallocatedFovArrayY = new double[countY + 1];

        System.arraycopy(fovArrayX, 0, reallocatedFovArrayX, 0, countX + 1);

        for (int j = 0; j < countY + 1; j++) {
            reallocatedFovArrayY[j] = fovArrayY[j * (countY + 1)];
        }

        /* Bilinear interpolation scheme */
        for (int i = 0; i < 31; i++) {
            for (int j = 0; j < 31; j++) {
                double X = axisX[i];
                double Y = axisY[j];
                int solvedX = 0;
                int solvedY = 0;
                for (int m = 0; m < countX + 1; m++) {
                    if (reallocatedFovArrayX[m] >= X) {
                        solvedX = m;
                    } else {
                        break;
                    }
                }

                for (int n = 0; n < countY + 1; n++) {
                    if (reallocatedFovArrayY[n] >= Y) {
                        solvedY = n;
                    } else {
                        break;
                    }
                }
                double minX = reallocatedFovArrayX[reallocatedFovArrayX.length - 1];
                double x1;
                double x2;
                if (X < minX) {
                    // Need to extrapolate in X
                    x1 = reallocatedFovArrayX[reallocatedFovArrayX.length - 2];
                    x2 = reallocatedFovArrayX[reallocatedFovArrayX.length - 1];
                    solvedX--;
                } else {
                    x1 = reallocatedFovArrayX[solvedX];
                    x2 = reallocatedFovArrayX[solvedX + 1];
                }
                double y1;
                double y2;
                double minY = reallocatedFovArrayY[reallocatedFovArrayY.length - 1];
                if (Y < minY) {
                    // Need to extrapolate in Y
                    y1 = reallocatedFovArrayY[reallocatedFovArrayY.length - 2];
                    y2 = reallocatedFovArrayY[reallocatedFovArrayY.length - 1];
                    solvedY--;
                } else {
                    y1 = reallocatedFovArrayY[solvedY];
                    y2 = reallocatedFovArrayY[solvedY + 1];
                }
                double f11 = reallocatedIFOV[solvedX][solvedY];
                double f21 = reallocatedIFOV[solvedX + 1][solvedY];
                double f12 = reallocatedIFOV[solvedX][solvedY + 1];
                double f22 = reallocatedIFOV[solvedX + 1][solvedY + 1];
                regridIfov[j + (i * 31)] = bilinearInterp(x1, x2, y1, y2, f11, f12, f21, f22, X, Y);
            }
        }
        /* copy the ifov and dimensions to 1D arrays */
        for (int i = 0; i < 31; i++) {
            for (int j = 0; j < 31; j++) {
                this.alongTrackAngle[i + (j * 31)] = (double) ((i * 20) - 300);
                this.acrossTrackAngle[i + (j * 31)] = (double) ((j * 20) - 300);
                this.ifov1D[i + (j * 31)] = regridIfov[i + (j * 31)];
            }
        }
    }

    private static double bilinearInterp(double x1, double x2, double y1, double y2, double f11, double f12, double f21, double f22, double x, double y) {
        // Linear interpolation in x
        double fxy1 = (((x2 - x) / (x2 - x1)) * f11) + (((x - x1) / (x2 - x1)) * f21);
        double fxy2 = (((x2 - x) / (x2 - x1)) * f12) + (((x - x1) / (x2 - x1)) * f22);

        // Linear interpolation in y
        double fxy = (((y2 - y) / (y2 - y1)) * fxy1) + (((y - y1) / (y2 - y1)) * fxy2);
        return fxy;
    }
}
