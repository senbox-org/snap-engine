/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.dataio.bigtiff;

import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;

public class BigGeoTiffGdalMetadataTest extends TestCase {

    // TODO: these tests are duplicated from standard GeoTiff reader. Try to make GDAL support more generic.
    
    public void testGetBandsFromGdalMetadata_probav_toa() throws Exception {
        // TOA product
        // e.g. PROBAV_S1_TOA_X20Y10_20180308_333M_V101_RADIOMETRY.tif
        final int productType = 11;
        final String probavGdalMetadatatring =
                "<GDALMetadata>\n" +
                        "<Item name=\"DATATYPES\">Byte Int16 UInt16 Float32</Item>\n" +
                        "<Item name=\"BAND\" sample=\"0\">RED</Item>\n" +
                        "<Item name=\"DESCRIPTION\" sample=\"0\">Top Of Atmosphere Reflectance RED - channel</Item>\n" +
                        "<Item name=\"UNITS\" sample=\"0\">-</Item>\n" +
                        "<Item name=\"NODATA\" sample=\"0\">-1.00000</Item>\n" +
                        "<Item name=\"BAND\" sample=\"1\">NIR</Item>\n" +
                        "<Item name=\"DESCRIPTION\" sample=\"1\">Top Of Atmosphere Reflectance NIR - channel</Item>\n" +
                        "<Item name=\"UNITS\" sample=\"1\">-</Item>\n" +
                        "<Item name=\"NODATA\" sample=\"1\">-1.00000</Item>\n" +
                        "<Item name=\"BAND\" sample=\"2\">BLUE</Item>\n" +
                        "<Item name=\"DESCRIPTION\" sample=\"2\">Top Of Atmosphere Reflectance BLUE - channel</Item>\n" +
                        "<Item name=\"UNITS\" sample=\"2\">-</Item>\n" +
                        "<Item name=\"NODATA\" sample=\"2\">-1.00000</Item>\n" +
                        "<Item name=\"BAND\" sample=\"3\">SWIR</Item>\n" +
                        "<Item name=\"DESCRIPTION\" sample=\"3\">Top Of Atmosphere Reflectance SWIR - channel</Item>\n" +
                        "<Item name=\"UNITS\" sample=\"3\">-</Item>\n" +
                        "<Item name=\"NODATA\" sample=\"3\">-1.00000</Item>\n" +
                "</GDALMetadata>";

        Band[] bands = BigGeoTiffUtils.setupBandsFromGdalMetadata(probavGdalMetadatatring, productType, 1, 1);
        assertNotNull(bands);
        assertEquals(4, bands.length);

        assertEquals("RED", bands[0].getName());
        assertEquals("Top Of Atmosphere Reflectance RED - channel", bands[0].getDescription());
        assertEquals("-", bands[0].getUnit());
        assertEquals(-1.0, bands[0].getNoDataValue());

        assertEquals("NIR", bands[1].getName());
        assertEquals("Top Of Atmosphere Reflectance NIR - channel", bands[1].getDescription());
        assertEquals("-", bands[1].getUnit());
        assertEquals(-1.0, bands[1].getNoDataValue());

        assertEquals("BLUE", bands[2].getName());
        assertEquals("Top Of Atmosphere Reflectance BLUE - channel", bands[2].getDescription());
        assertEquals("-", bands[2].getUnit());
        assertEquals(-1.0, bands[2].getNoDataValue());

        assertEquals("SWIR", bands[3].getName());
        assertEquals("Top Of Atmosphere Reflectance SWIR - channel", bands[3].getDescription());
        assertEquals("-", bands[3].getUnit());
        assertEquals(-1.0, bands[3].getNoDataValue());
    }

    public void testGetBandsFromGdalMetadata_probav_toc() throws Exception {
        // TOC product
        // e.g. PROBAV_S1_TOC_X20Y10_20180308_333M_V101_RADIOMETRY.tif
        final int productType = 11;
        final String probavGdalMetadatatring =
                "<GDALMetadata>\n" +
                        "<Item name=\"\">DRDC COASP SAR Processor Raster</Item>\n" +
                        "<Item name=\"BAND\" sample=\"0\">RED</Item>\n" +
                        "<Item name=\"DESCRIPTION\" sample=\"0\">Top Of Canopy Reflectance RED - channel</Item>\n" +
                        "<Item name=\"UNITS\" sample=\"0\">-</Item>\n" +
                        "<Item name=\"NODATA\" sample=\"0\">-1.00000</Item>\n" +
                        "<Item name=\"BAND\" sample=\"1\">NIR</Item>\n" +
                        "<Item name=\"DESCRIPTION\" sample=\"1\">Top Of Canopy Reflectance NIR - channel</Item>\n" +
                        "<Item name=\"UNITS\" sample=\"1\">-</Item>\n" +
                        "<Item name=\"NODATA\" sample=\"1\">-1.00000</Item>\n" +
                        "<Item name=\"BAND\" sample=\"2\">BLUE</Item>\n" +
                        "<Item name=\"DESCRIPTION\" sample=\"2\">Top Of Canopy Reflectance BLUE - channel</Item>\n" +
                        "<Item name=\"UNITS\" sample=\"2\">-</Item>\n" +
                        "<Item name=\"NODATA\" sample=\"2\">-1.00000</Item>\n" +
                        "<Item name=\"BAND\" sample=\"3\">SWIR</Item>\n" +
                        "<Item name=\"DESCRIPTION\" sample=\"3\">Top Of Canopy Reflectance SWIR - channel</Item>\n" +
                        "<Item name=\"UNITS\" sample=\"3\">-</Item>\n" +
                        "<Item name=\"NODATA\" sample=\"3\">-1.00000</Item>\n" +
                "</GDALMetadata>";

        Band[] bands = BigGeoTiffUtils.setupBandsFromGdalMetadata(probavGdalMetadatatring, productType, 1, 1);
        assertNotNull(bands);
        assertEquals(4, bands.length);

        assertEquals("RED", bands[0].getName());
        assertEquals("Top Of Canopy Reflectance RED - channel", bands[0].getDescription());
        assertEquals("-", bands[0].getUnit());
        assertEquals(-1.0, bands[0].getNoDataValue());

        assertEquals("NIR", bands[1].getName());
        assertEquals("Top Of Canopy Reflectance NIR - channel", bands[1].getDescription());
        assertEquals("-", bands[1].getUnit());
        assertEquals(-1.0, bands[1].getNoDataValue());

        assertEquals("BLUE", bands[2].getName());
        assertEquals("Top Of Canopy Reflectance BLUE - channel", bands[2].getDescription());
        assertEquals("-", bands[2].getUnit());
        assertEquals(-1.0, bands[2].getNoDataValue());

        assertEquals("SWIR", bands[3].getName());
        assertEquals("Top Of Canopy Reflectance SWIR - channel", bands[3].getDescription());
        assertEquals("-", bands[3].getUnit());
        assertEquals(-1.0, bands[3].getNoDataValue());
    }

    public void testGetBandsFromGdalMetadata_probav_ndvi() throws Exception {
        // NDVI product
        // e.g. PROBAV_S10_TOC_X20Y10_20180311_333M_NDVI_V101_NDVI.tif
        final int productType = 20;
        final String probavGdalMetadatatring =
                "<GDALMetadata>\n" +
                        "<Item name=\"BAND\" sample=\"0\">NDVI</Item>\n" +
                        "<Item name=\"DESCRIPTION\" sample=\"0\">Normalized Difference Vegetation Index</Item>\n" +
                        "<Item name=\"UNITS\" sample=\"0\">-</Item>\n" +
                        "<Item name=\"NODATA\" sample=\"0\">255.00000</Item>\n" +
                        "<Item name=\"OFFSET\" sample=\"0\" role=\"offset\">-0.0800000000000000017</Item>\n" +
                        "<Item name=\"SCALE\" sample=\"0\" role=\"scale\">0.00400000000000000008</Item>\n" +
                        "<Item name=\"UNITTYPE\" sample=\"0\">255.00000</Item>\n" +
                "</GDALMetadata>";

        Band[] bands = BigGeoTiffUtils.setupBandsFromGdalMetadata(probavGdalMetadatatring, productType, 1, 1);
        assertNotNull(bands);
        assertEquals(1, bands.length);

        assertEquals("NDVI", bands[0].getName());
        assertEquals("Normalized Difference Vegetation Index", bands[0].getDescription());
        assertEquals("-", bands[0].getUnit());
        assertEquals(255.0, bands[0].getNoDataValue());
        assertEquals(0.00400000000000000008, bands[0].getScalingFactor());
        assertEquals(-0.0800000000000000017, bands[0].getScalingOffset());
    }

    public void testGetBandsFromGdalMetadata_bandname_only() throws Exception {
        // we have a band name in metadata, but no other attributes like description, unit,...
        // e.g. CCI-LC-MERIS-SR-L3-300m-v4.0--FR-2009-01-01-364d.STATUS.tif
        final int productType = 11;
        final String probavGdalMetadatatring =
                "<GDALMetadata>\n" +
                        "<Item name=\"Generate by\">gdal_mosaic</Item>\n" +
                        "<Item name=\"Copyright\">UCL Geomatics, BELGIUM 1999-2012</Item>\n" +
                        "<Item name=\"Authors\">Pierre Defourny et al.</Item>\n" +
                        "<Item name=\"BAND\" sample=\"0\">STATUS</Item>\n" +
                "</GDALMetadata>";

        Band[] bands = BigGeoTiffUtils.setupBandsFromGdalMetadata(probavGdalMetadatatring, productType, 1, 1);
        assertNotNull(bands);
        assertEquals(1, bands.length);

        assertEquals("STATUS", bands[0].getName());
        assertNull(bands[0].getDescription());
        assertNull(bands[0].getUnit());
        assertFalse(bands[0].isNoDataValueUsed());
    }

}
