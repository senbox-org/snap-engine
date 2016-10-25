/*
 *
 *  * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s3tbx.olci.radiometry;

/**
 * @author muhammad.bc.
 */
public enum Sensor {
    MERIS() {
        @Override
        public int getNumBands() {
            return 15;
        }

        @Override
        public String getSZA() {
            return "sun_zenith";
        }

        @Override
        public String getOZA() {
            return "view_zenith";
        }

        @Override
        public String getSAA() {
            return "sun_azimuth";
        }

        @Override
        public String getOAA() {
            return "view_azimuth";
        }

        @Override
        public String getTotalOzone() {
            return "ozone";
        }

        @Override
        public String getLatitude() {
            return "latitude";
        }

        @Override
        public String getLongitude() {
            return "longitude";
        }

        @Override
        public String getAltitude() {
            return "dem_alt";
        }

        @Override
        public String getSeaLevelPressure() {
            return "atm_press";
        }

        public int[] getBounds() {
            return new int[]{13, 14};
        }

        public String getNamePattern() {
            return "radiance_%d";
        }

        public String getBandInfoFileName() {
            return "band_info_meris.txt";
        }
    },
    OLCI() {
        @Override
        public int getNumBands() {
            return 21;
        }

        @Override
        public String getSZA() {
            return "SZA";
        }

        @Override
        public String getOZA() {
            return "OZA";
        }

        @Override
        public String getSAA() {
            return "SAA";
        }

        @Override
        public String getOAA() {
            return "OAA";
        }

        @Override
        public String getTotalOzone() {
            return "total_ozone";
        }

        @Override
        public String getLatitude() {
            return "latitude";
        }

        @Override
        public String getLongitude() {
            return "longitude";
        }

        @Override
        public String getAltitude() {
            return "altitude";
        }

        @Override
        public String getSeaLevelPressure() {
            return "sea_level_pressure";
        }

        public int[] getBounds() {
            return new int[]{17, 18};
        }

        public String getNamePattern() {
            return "Oa%02d_radiance";
        }

        public String getBandInfoFileName() {
            return "band_info_olci.txt";
        }
    };


    public int[] getBounds() {
        return new int[]{13, 14};
    }

    public String getNamePattern() {
        return "radiance_%d";
    }

    public abstract String getBandInfoFileName();

    public abstract int getNumBands();

    public abstract String getSZA();

    public abstract String getOZA();

    public abstract String getSAA();

    public abstract String getOAA();

    public abstract String getTotalOzone();

    public abstract String getLatitude();

    public abstract String getLongitude();

    public abstract String getAltitude();


    public abstract String getSeaLevelPressure();


}
