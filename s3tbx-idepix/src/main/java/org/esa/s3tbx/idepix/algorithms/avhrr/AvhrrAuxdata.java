package org.esa.s3tbx.idepix.algorithms.avhrr;

import org.esa.snap.core.gpf.OperatorException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 01.12.2014
 * Time: 18:00
 *
 * @author olafd
 */
public class AvhrrAuxdata {

    public static final int VZA_TABLE_LENGTH = 2048;
    public static final String VZA_FILE_NAME = "view_zenith.txt";

    public static final int RAD2BT_TABLE_LENGTH = 3;
    public static final String RAD2BT_FILE_NAME_PREFIX = "rad2bt_noaa";

    private static AvhrrAuxdata instance;

    public static AvhrrAuxdata getInstance() {
        if (instance == null) {
            instance = new AvhrrAuxdata();
        }

        return instance;
    }


    public Line2ViewZenithTable createLine2ViewZenithTable() throws IOException {
        final InputStream inputStream = getClass().getResourceAsStream(VZA_FILE_NAME);
        Line2ViewZenithTable vzaTable = new Line2ViewZenithTable();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringTokenizer st;
        try {
            int i = 0;
            String line;
            while ((line = bufferedReader.readLine()) != null && i < VZA_TABLE_LENGTH) {
                line = line.trim();
                st = new StringTokenizer(line, "\t", false);

                if (st.hasMoreTokens()) {
                    // x (whatever that is)
                    vzaTable.setxIndex(i, Integer.parseInt(st.nextToken()));
                }
                if (st.hasMoreTokens()) {
                    // y
                    vzaTable.setVza(i, Double.parseDouble(st.nextToken()));
                }
                i++;
            }
        } catch (IOException | NumberFormatException e) {
            throw new OperatorException("Failed to load Line2ViewZenithTable: \n" + e.getMessage(), e);
        } finally {
            inputStream.close();
        }
        return vzaTable;
    }

    public Rad2BTTable createRad2BTTable(String noaaId) throws IOException {

        final String filename = RAD2BT_FILE_NAME_PREFIX + noaaId + ".txt";
        final InputStream inputStream = getClass().getResourceAsStream(filename);
        Rad2BTTable rad2BTTable = new Rad2BTTable();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringTokenizer st;
        try {
            int i = 0;
            String line;
            while ((line = bufferedReader.readLine()) != null && i < RAD2BT_TABLE_LENGTH) {
                line = line.trim();
                st = new StringTokenizer(line, "\t", false);

                if (st.hasMoreTokens()) {
                    // channel index (3, 4, 5), skip
                    st.nextToken();
                }
                if (st.hasMoreTokens()) {
                    // A
                    rad2BTTable.setA(i, Double.parseDouble(st.nextToken()));
                }
                if (st.hasMoreTokens()) {
                    // B
                    rad2BTTable.setB(i, Double.parseDouble(st.nextToken()));
                }
                if (st.hasMoreTokens()) {
                    // D
                    rad2BTTable.setD(i, Double.parseDouble(st.nextToken()));
                }
                if (st.hasMoreTokens()) {
                    // nu_low
                    rad2BTTable.setNuLow(i, Double.parseDouble(st.nextToken()));
                }
                if (st.hasMoreTokens()) {
                    // nu_mid
                    rad2BTTable.setNuMid(i, Double.parseDouble(st.nextToken()));
                }
                if (st.hasMoreTokens()) {
                    // nu_high_land
                    rad2BTTable.setNuHighland(i, Double.parseDouble(st.nextToken()));
                }
                if (st.hasMoreTokens()) {
                    // nu_high_water
                    rad2BTTable.setNuHighWater(i, Double.parseDouble(st.nextToken()));
                }
                i++;
            }
        } catch (IOException | NumberFormatException e) {
            throw new OperatorException("Failed to load Rad2BTTable: \n" + e.getMessage(), e);
        } finally {
            inputStream.close();
        }
        return rad2BTTable;
    }


    /**
     * Class providing a temperature-radiance conversion data table
     */
    public class Line2ViewZenithTable {
        private int[] xIndex = new int[VZA_TABLE_LENGTH];
        private double[] vza = new double[VZA_TABLE_LENGTH];

        public void setxIndex(int index, int xIndex) {
            this.xIndex[index] = xIndex;
        }

        public double getVza(int index) {
            return vza[index];
        }

        public void setVza(int index, double vza) {
            this.vza[index] = vza;
        }

    }

    /**
     *  Class providing a radiance-to-BT coefficients table
     */
    public class Rad2BTTable {
        private final int OFFSET = 3;

        private double[] A = new double[RAD2BT_TABLE_LENGTH];
        private double[] B = new double[RAD2BT_TABLE_LENGTH];
        private double[] D = new double[RAD2BT_TABLE_LENGTH];
        private double[] nuLow = new double[RAD2BT_TABLE_LENGTH];
        private double[] nuMid = new double[RAD2BT_TABLE_LENGTH];
        private double[] nuHighland = new double[RAD2BT_TABLE_LENGTH];
        private double[] nuHighWater = new double[RAD2BT_TABLE_LENGTH];

        public double getA(int index) {
            return A[index - OFFSET];
        }

        public void setA(int index, double a) {
            this.A[index] = a;
        }

        public double getB(int index) {
            return B[index - OFFSET];
        }

        public void setB(int index, double b) {
            this.B[index] = b;
        }

        public double getD(int index) {
            return D[index - OFFSET];
        }

        public void setD(int index, double d) {
            this.D[index] = d;
        }

        public double getNuLow(int index) {
            return nuLow[index - OFFSET];
        }

        public void setNuLow(int index, double nuLow) {
            this.nuLow[index] = nuLow;
        }

        public double getNuMid(int index) {
            return nuMid[index - OFFSET];
        }

        public void setNuMid(int index, double nuMid) {
            this.nuMid[index] = nuMid;
        }

        public double getNuHighLand(int index) {
            return nuHighland[index - OFFSET];
        }

        public void setNuHighland(int index, double nuHighland) {
            this.nuHighland[index] = nuHighland;
        }

        public double getNuHighWater(int index) {
            return nuHighWater[index - OFFSET];
        }

        public void setNuHighWater(int index, double nuHighWater) {
            this.nuHighWater[index] = nuHighWater;
        }
    }
}
