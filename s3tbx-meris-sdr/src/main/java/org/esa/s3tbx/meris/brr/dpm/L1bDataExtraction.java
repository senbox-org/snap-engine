/*
 * $Id: L1bDataExtraction.java,v 1.1 2007/03/27 12:52:22 marcoz Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.s3tbx.meris.brr.dpm;

import org.esa.s3tbx.meris.l2auxdata.Constants;
import org.esa.s3tbx.meris.l2auxdata.L2AuxData;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.BitSetter;

/**
 * The MERIS Level 2 module for L1b data extraction.
 */
public class L1bDataExtraction implements Constants {

    private final L2AuxData auxData;

    /**
     * Constructs the module
     */
    public L1bDataExtraction(L2AuxData auxData) {
        this.auxData = auxData;
    }

    /**
     * Initializes the given pixel with data readRecord from the L1B input product. Combines the geometry/meteo.
     * preprocessing (step 2.1.0) and pixel extraction (step 2.1.4) of the DPM. All level 2 geophysical fields are set
     * to <code>{@link #BAD_VALUE}</code>.
     * <p/>
     * <b>DPM ref.:</b> Step 2.1.0, 2.1.4, 2.1.11 <br> <b>MEGS ref.</b>: <code>extdatl1.c</code>, function
     * <code>l1_extract_pixbloc</code> <br>
     *
     * @param pixel         the current pixel
     * @param x             the current pixel's X coordinate
     * @param y             the current pixel's Y coordinate
     * @param tpdata        interpolated data buffer from tie points of L1B input product
     * @param toars         top-of-atmosphere radiances buffer for 15 bands from L1B input product
     * @param detectorIndex detector index buffer from L1B input product
     * @param l1bFlags      L1B flags buffer from L1B input product
     */
    public void l1_extract_pixbloc(final DpmPixel pixel,
                                   final int x,
                                   final int y,
                                   final Tile[] tpdata,
                                   final Tile[] toars,
                                   final Tile detectorIndex,
                                   final Tile l1bFlags) {

        /////////////////////////////////////////////////////////////////////
        // Initialize still unknown pixel values

        pixel.ozone_ecmwf = BAD_VALUE;
        pixel.ANNOT_F = 0;
        for (int band = 0; band < L1_BAND_NUM; band++) {
            pixel.rho_ag[band] = BAD_VALUE;
            pixel.rho_toa[band] = BAD_VALUE;
            pixel.rho_top[band] = BAD_VALUE;
        }

        /////////////////////////////////////////////////////////////////////
        // Set well known pixel values

        pixel.x = x;
        pixel.y = y;

        pixel.SATURATED_F = 0;
        for (int band = 0; band < 15; band++) {
            pixel.TOAR[band] = toars[band].getSampleDouble(x, y);
            if (pixel.TOAR[band] > auxData.Saturation_L[band]) {
                pixel.SATURATED_F = BitSetter.setFlag(pixel.SATURATED_F, band);
            }
        }
        pixel.detector = detectorIndex.getSampleInt(x, y);

        pixel.l1flags = l1bFlags.getSampleInt(x, y);
        pixel.l2flags = 0L;
        if (BitSetter.isFlagSet(pixel.l1flags, L1_F_COSMETIC)) {
            pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_COSMETIC);
        }
        if (BitSetter.isFlagSet(pixel.l1flags, L1_F_DUPLICATED)) {
            pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_DUPLICATED);
        }
        if (BitSetter.isFlagSet(pixel.l1flags, L1_F_LAND)) {
            pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_LAND);
        }
        if (BitSetter.isFlagSet(pixel.l1flags, L1_F_BRIGHT)) {
            pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_BRIGHT);
        }
        if (BitSetter.isFlagSet(pixel.l1flags, L1_F_COAST)) {
            pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_COASTLINE);
        }
        if (BitSetter.isFlagSet(pixel.l1flags, L1_F_INVALID)) {
            pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_INVALID);
        }
        if (BitSetter.isFlagSet(pixel.l1flags, L1_F_SUSPECT)) {
            pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_SUSPECT);
        }
        if (pixel.detector < 0 || pixel.detector >= auxData.detector_count) {
            pixel.detector = -1; // OK
            // Make pixel invalid
            if (!BitSetter.isFlagSet(pixel.l1flags, L1_F_INVALID)) {
                pixel.l1flags = BitSetter.setFlag(pixel.l1flags, F_INVALID);
                pixel.l2flags = BitSetter.setFlag(pixel.l2flags, F_INVALID);
            }
        }

        // DPM #2.1.0-3
        pixel.sun_zenith = tpdata[SUN_ZENITH_TPG_INDEX].getSampleDouble(x, y);
        // DPM #2.1.0-4
        pixel.view_zenith = tpdata[VIEW_ZENITH_TPG_INDEX].getSampleDouble(x, y);
        // DPM #2.1.0-5
        double view_azimuth = tpdata[VIEW_AZIMUTH_TPG_INDEX].getSampleDouble(x, y);
        // DPM #2.1.0-6
        pixel.sun_azimuth = tpdata[SUN_AZIMUTH_TPG_INDEX].getSampleDouble(x, y);
        // mz 2007-11-22 at the moment lat and lon are not used for any computation  
        // DPM #2.1.0-7
        // pixel.lat = tpdata[LATITUDE_TPG_INDEX].getSampleDouble(x, y);
        // DPM #2.1.0-8
        // pixel.lon = tpdata[LONGITUDE_TPG_INDEX].getSampleDouble(x, y);
        // DPM #2.1.0-9
        pixel.altitude = tpdata[DEM_ALT_TPG_INDEX].getSampleDouble(x, y);
        // DPM #2.1.0-10
        pixel.press_ecmwf = tpdata[ATM_PRESS_TPG_INDEX].getSampleDouble(x, y);
        // DPM #2.1.0-11
        pixel.windu = tpdata[ZONAL_WIND_TPG_INDEX].getSampleDouble(x, y);
        // DPM #2.1.0-12
        pixel.windv = tpdata[MERID_WIND_TPG_INDEX].getSampleDouble(x, y);
        // DPM #2.1.0-13
        pixel.ozone_ecmwf = tpdata[OZONE_TPG_INDEX].getSampleDouble(x, y);
        // DPM #2.1.0-14
        pixel.delta_azimuth = DEG * Math.acos(Math.cos(RAD * (view_azimuth - pixel.sun_azimuth)));
        // DPM #2.1.0-15
        if (BitSetter.isFlagSet(pixel.l2flags, F_LAND)) {
            // ECMWF pressure is only corrected for positive altitudes and only for land pixels */
            double f = Math.exp(-Math.max(0.0, pixel.altitude) / auxData.press_scale_height);
            pixel.press_ecmwf *= f;
        }
        //////////////////////////////////////////////////////////
        // Helpers

        pixel.muv = Math.cos(RAD * pixel.view_zenith);
        pixel.mus = Math.cos(RAD * pixel.sun_zenith);

        // DPM #2.1.12-1, Air Mass Computation
        pixel.airMass = 1.0 / pixel.mus + 1.0 / pixel.muv;
    }
}
