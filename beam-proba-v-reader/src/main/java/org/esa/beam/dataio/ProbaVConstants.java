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
    public static final String MPH_NAME = "MPH";
    public static final String SPH_NAME = "SPH";
    public static final String BAND_INFORMATION_NAME = "Band_Information";
    // Meta data attributes
    public static final String ATTR_NAME_SENSOR_TYPE = "Sensor_Type";
    public static final String ATTR_NAME_DATA_RIGHTS = "Data_Rights";
    public static final String ATTR_NAME_TARGET_NAME = "Target_Name";
    public static final String ATTR_NAME_IMAGE_DATE = "Image_Date";
    public static final String ATTR_NAME_IMAGE_NUMBER = "Image_Number";
    public static final String ATTR_NAME_IMAGE_TAG = "Image_Tag";
    public static final String ATTR_NAME_TARGET_LON = "Target_Longitude";
    public static final String ATTR_NAME_TARGET_LAT = "Target_Latitude";
    public static final String ATTR_NAME_TARGET_ALT = "Target_Altitude";
    public static final String ATTR_NAME_FLY_BY_ZENITH_ANGLE = "Fly-by_Zenith_Angle";
    public static final String ATTR_NAME_MINIMUM_ZENITH_ANGLE = "Minimum_Zenith_Angle";
    public static final String ATTR_NAME_SOLAR_ZENITH_ANGLE = "Solar_Zenith_Angle";
    public static final String ATTR_NAME_SOLAR_AZIMUTH_ANGLE = "Solar_Azimuth_Angle";
    public static final String ATTR_NAME_FLY_BY_TIME = "Fly-by_Time";
    public static final String ATTR_NAME_IMAGE_CENTRE_TIME = "Calculated_Image_Centre_Time";
    public static final String ATTR_NAME_OBSERVATION_ZENITH_ANGLE = "Observation_Zenith_Angle";
    public static final String ATTR_NAME_OBSERVATION_AZIMUTH_ANGLE = "Observation_Azimuth_Angle";
    public static final String ATTR_NAME_CHRIS_MODE = "CHRIS_Mode";
    public static final String ATTR_NAME_NUMBER_OF_SAMPLES = "Number_of_Samples";
    public static final String ATTR_NAME_NUMBER_OF_GROUND_LINES = "Number_of_Ground_Lines";
    public static final String ATTR_NAME_NUMBER_OF_BANDS = "Number_of_Bands";
    public static final String ATTR_NAME_PLATFORM_ALTITUDE = "Platform_Altitude";
    public static final String ATTR_NAME_RESPONSE_FILE_CREATION_TIME = "Response_File_Creation_Time";
    public static final String ATTR_NAME_DARK_FILE_CREATION_TIME = "Dark_File_Creation_Time";
    public static final String ATTR_NAME_CALIBRATION_DATA_UNITS = "Calibration_Data_Units";
    public static final String ATTR_NAME_CHRIS_TEMPERATURE = "CHRIS_Temperature";
    public static final String ATTR_NAME_KEY_TO_MASK = "Key_to_Mask";
    public static final String ATTR_NAME_IMAGE_FLIPPED_ALONG_TRACK = "Image_Flipped_Along-Track";

    public static final String ATTR_NAME_NOISE_REDUCTION = "Noise Reduction";

}
