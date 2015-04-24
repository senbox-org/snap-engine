package org.esa.beam.dataio;

import java.awt.*;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 13.03.2015
 * Time: 11:14
 *
 * @author olafd
 */
public class ProbaVConstants {

    public static final int SYNTHESIS_PRODUCT_DIMENSION_1km = 1120;
    public static final int SYNTHESIS_PRODUCT_DIMENSION_333m = 3360;

    public static final int L1C_PRODUCT_SWIR_SWATH_WIDTH = 1024;

    public static final int L1C_TIEPOINT_OFFS_X = 0;
    public static final int L1C_TIEPOINT_OFFS_Y = 0;
    public static final int L1C_TIEPOINT_SUBS_X = 8;
//    public static final int L1C_TIEPOINT_SUBS_X = 1;
    public static final int L1C_TIEPOINT_SUBS_Y = 8;
//    public static final int L1C_TIEPOINT_SUBS_Y = 1;

    public static final String SM_BAND_NAME = "SM";
    public static final String SM_FLAG_BAND_NAME = "SM_FLAGS";

    public static final int SM_CLEAR_BIT_INDEX = 0;
    public static final int SM_UNDEFINED_BIT_INDEX = 1;
    public static final int SM_CLOUD_BIT_INDEX = 2;
    public static final int SM_SNOWICE_INDEX = 3;
    public static final int SM_CLOUD_SHADOW_BIT_INDEX = 4;
    public static final int SM_LAND_BIT_INDEX = 5;
    public static final int SM_GOOD_SWIR_INDEX = 6;
    public static final int SM_GOOD_NIR_BIT_INDEX = 7;
    public static final int SM_GOOD_RED_BIT_INDEX = 8;
    public static final int SM_GOOD_BLUE_BIT_INDEX = 9;

    public static final String SM_CLEAR_FLAG_NAME = "CLEAR";
    public static final String SM_UNDEFINED_FLAG_NAME = "UNDEFINED";
    public static final String SM_CLOUD_FLAG_NAME = "CLOUD";
    public static final String SM_SNOWICE_FLAG_NAME = "SNOWICE";
    public static final String SM_CLOUD_SHADOW_FLAG_NAME = "CLOUD_SHADOW";
    public static final String SM_LAND_FLAG_NAME = "LAND";
    public static final String SM_GOOD_SWIR_FLAG_NAME = "GOOD_SWIR";
    public static final String SM_GOOD_NIR_FLAG_NAME = "GOOD_NIR";
    public static final String SM_GOOD_RED_FLAG_NAME = "GOOD_RED";
    public static final String SM_GOOD_BLUE_FLAG_NAME = "GOOD_BLUE";

    public static final String SM_CLEAR_FLAG_DESCR = "Clear pixel";
    public static final String SM_UNDEFINED_FLAG_DESCR = "Pixel classified as undefined";
    public static final String SM_CLOUD_FLAG_DESCR = "Cloudy pixel";
    public static final String SM_SNOWICE_FLAG_DESCR = "Snow or ice pixel";
    public static final String SM_CLOUD_SHADOW_FLAG_DESCR = "Cloud shadow pixel";
    public static final String SM_LAND_FLAG_DESCR = "Land pixel";
    public static final String SM_GOOD_SWIR_FLAG_DESCR = "Pixel with good SWIR data";
    public static final String SM_GOOD_NIR_FLAG_DESCR = "Pixel with good NIR data";
    public static final String SM_GOOD_RED_FLAG_DESCR = "Pixel with good RED data";
    public static final String SM_GOOD_BLUE_FLAG_DESCR = "Pixel with good BLUE data";

    public static final Color[] FLAG_COLORS = {
            new Color(120, 255, 180),
            new Color(255, 255, 0),
            new Color(0, 255, 255),
            new Color(255, 100, 0),
            new Color(255, 255, 180),
            new Color(255, 0, 255),
            new Color(0, 0, 255),
            new Color(180, 180, 255),
            new Color(255, 150, 100),
            new Color(0, 255, 0)
    };

    public static final String Q_BAND_NAME = "Q";
    public static final String Q_FLAG_BAND_NAME = "Q_FLAGS";

    public static final int Q_CORRECT_BIT_INDEX = 0;
    public static final int Q_MISSING_BIT_INDEX = 1;
    public static final int Q_WAS_SATURATED_BIT_INDEX = 2;
    public static final int Q_BECAME_SATURATED_INDEX = 3;
    public static final int Q_BECAME_NEGATIVE_BIT_INDEX = 4;
    public static final int Q_INTERPOLATED_BIT_INDEX = 5;
    public static final int Q_BORDER_COMPRESSED_INDEX = 6;

    public static final String Q_CORRECT_FLAG_NAME = "CORRECT";
    public static final String Q_MISSING_FLAG_NAME = "MISSING";
    public static final String Q_WAS_SATURATED_FLAG_NAME = "WAS_SATURATED";
    public static final String Q_BECAME_SATURATED_FLAG_NAME = "BECAME_SATURATED";
    public static final String Q_BECAME_NEGATIVE_FLAG_NAME = "BECAME_NEGATIVE";
    public static final String Q_INTERPOLATED_FLAG_NAME = "INTERPOLATED";
    public static final String Q_BORDER_COMPRESSED_FLAG_NAME = "BORDER_COMPRESSED";

    public static final String Q_CORRECT_FLAG_DESCR = "Pixel has no bad quality indicators";
    public static final String Q_MISSING_FLAG_DESCR = "Pixel value missing due to a bad detector";
    public static final String Q_WAS_SATURATED_FLAG_DESCR = "Pixel DN value is equal to 4095 in 12-bits coding";
    public static final String Q_BECAME_SATURATED_FLAG_DESCR = "Pixel value became 4095 during the TOA calculation";
    public static final String Q_BECAME_NEGATIVE_FLAG_DESCR = "Pixel value became negative during the TOA calculation";
    public static final String Q_INTERPOLATED_FLAG_DESCR = "Pixel value was interpolated using the neighbour pixels";
    public static final String Q_BORDER_COMPRESSED_FLAG_DESCR = "Pixel value is uncertaion due to onboard compression artefacts";

    public static final String PROBAV_DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    // root metadata:

    // we have these root metadata:
//        Group size = 1
//        Number of attributes = 13
//        DESCRIPTION = PROBA-V Level3 Top Of Atmosphere Daily Synthesis at 333M resolution
//        INSTRUMENT = VEGETATION
//        MAP_PROJECTION_FAMILY = GEOGRAPHIC
//        MAP_PROJECTION_NAME = GEOGRAPHIC_LAT_LON_WGS84
//        MAP_PROJECTION_REFERENCE = EPSG:4326
//        MAP_PROJECTION_UNITS = DEGREES
//        MAP_PROJECTION_WKT = GEOGCS["WGS 84",DATUM["WGS_1984",SPHEROID["WGS 84",6378137,298.257223563,AUTHORITY["EPSG","7030"]],AUTHORITY["EPSG","6326"]],PRIMEM["Greenwich",0,AUTHORITY["EPSG","8901"]],UNIT["degree",0.01745329251994328,AUTHORITY["EPSG","9122"]],AUTHORITY["EPSG","4326"]]
//        PLATFORM = PROBA-1
//        PROCESSING_DATE = 2014-07-07
//        PROCESSING_TIME = 15:44:26
//        PRODUCT_REFERENCE = Synthesis_PROBAV_20131025_S1_TOA_333M_V003
//        PRODUCT_VERSION = 3
//        SYNTHESIS_PERIOD = 1

    public static final String MPH_NAME = "MPH";
    public static final String BAND_INFORMATION_NAME = "Band_Information";
    // Meta data root attributes
    public static final String ATTR_NAME_DESCRIPTION = "Product_Description";
    public static final String ATTR_NAME_INSTRUMENT = "Instrument";
    public static final String ATTR_NAME_MAP_PROJECTION_FAMILY = "Map_Projection_Family";
    public static final String ATTR_NAME_MAP_PROJECTION_NAME = "Map_Projection_Name";
    public static final String ATTR_NAME_MAP_PROJECTION_REFERENCE = "Map_Projection_Reference";
    public static final String ATTR_NAME_MAP_PROJECTION_UNITS = "Map_Projection_Units";
    public static final String ATTR_NAME_MAP_PROJECTION_WKT = "Map_Projection_Wkt";
    public static final String ATTR_NAME_PLATFORM = "Platform";
    public static final String ATTR_NAME_PROCESSING_DATE = "Processing_Date";
    public static final String ATTR_NAME_PROCESSING_TIME = "Processing_Time";
    public static final String ATTR_NAME_PRODUCT_REFERENCE = "Product_Reference";
    public static final String ATTR_NAME_PRODUCT_VERSION = "Product_Version";
    public static final String ATTR_NAME_SYNTHESIS_PERIOD = "Synthesis_Period";

    public static final float SAA_NO_DATA_VALUE = Float.NaN;
    public static final float VAA_NO_DATA_VALUE = 382.49808f;
    public static final float VZA_NO_DATA_VALUE = 127.5f;
    public static final float SZA_NO_DATA_VALUE = 127.5f;
    public static final float NDVI_NO_DATA_VALUE = Float.NaN;
    public static final float REFL_NO_DATA_VALUE = -5.0E-4f;
    public static final float TIME_NO_DATA_VALUE = Float.NaN;
}
