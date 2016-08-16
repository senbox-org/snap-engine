package org.esa.s3tbx.dataio.probav;

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
    public static final int SM_SWIR_COVERAGE_INDEX = 10;
    public static final int SM_NIR_COVERAGE_BIT_INDEX = 11;
    public static final int SM_RED_COVERAGE_BIT_INDEX = 12;
    public static final int SM_BLUE_COVERAGE_BIT_INDEX = 13;

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
    public static final String SM_SWIR_COVERAGE_FLAG_NAME = "SWIR_COVERAGE";
    public static final String SM_NIR_COVERAGE_FLAG_NAME = "NIR_COVERAGE";
    public static final String SM_RED_COVERAGE_FLAG_NAME = "RED_COVERAGE";
    public static final String SM_BLUE_COVERAGE_FLAG_NAME = "BLUE_COVERAGE";

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
    public static final String SM_SWIR_COVERAGE_FLAG_DESCR = "Pixel with SWIR coverage";
    public static final String SM_NIR_COVERAGE_FLAG_DESCR = "Pixel with NIR coverage";
    public static final String SM_RED_COVERAGE_FLAG_DESCR = "Pixel with RED coverage";
    public static final String SM_BLUE_COVERAGE_FLAG_DESCR = "Pixel with BLUE coverage";

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
            new Color(0, 255, 0),
            new Color(0, 120, 180),
            new Color(180, 0, 255),
            new Color(130, 200, 250),
            new Color(50, 50, 100)
    };

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

    public static final String MPH_NAME = "MPH";
    public static final String QUALITY_NAME = "QUALITY";

    public static final int GEOMETRY_NO_DATA_VALUE = 255;
    public static final float NDVI_NO_DATA_VALUE = Float.NaN;
    public static final int RADIOMETRY_NO_DATA_VALUE = -1;
    public static final int TIME_NO_DATA_VALUE_UINT16 = 0;
    public static final int TIME_NO_DATA_VALUE_UINT8 = 255;

    public static final int[] RADIOMETRY_CHILD_INDEX = {0, 2, 1, 3};
}
