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

    public static final String SM_BAND_NAME = "SM";
    public static final String SM_FLAG_BAND_NAME = "SM_FLAGS";

    public static final int CLEAR_BIT_INDEX = 0;
    public static final int UNDEFINED_BIT_INDEX = 1;
    public static final int CLOUD_BIT_INDEX = 2;
    public static final int SNOWICE_INDEX = 3;
    public static final int CLOUD_SHADOW_BIT_INDEX = 4;
    public static final int LAND_BIT_INDEX = 5;
    public static final int GOOD_SWIR_INDEX = 6;
    public static final int GOOD_NIR_BIT_INDEX = 7;
    public static final int GOOD_RED_BIT_INDEX = 8;
    public static final int GOOD_BLUE_BIT_INDEX = 9;

    public static final String CLEAR_FLAG_NAME = "CLEAR";
    public static final String UNDEFINED_FLAG_NAME = "UNDEFINED";
    public static final String CLOUD_FLAG_NAME = "CLOUD";
    public static final String SNOWICE_FLAG_NAME = "SNOWICE";
    public static final String CLOUD_SHADOW_FLAG_NAME = "CLOUD_SHADOW";
    public static final String LAND_FLAG_NAME = "LAND";
    public static final String GOOD_SWIR_FLAG_NAME = "GOOD_SWIR";
    public static final String GOOD_NIR_FLAG_NAME = "GOOD_NIR";
    public static final String GOOD_RED_FLAG_NAME = "GOOD_RED";
    public static final String GOOD_BLUE_FLAG_NAME = "GOOD_BLUE";

    public static final String CLEAR_FLAG_DESCR = "Clear pixel";
    public static final String UNDEFINED_FLAG_DESCR = "Pixel classified as undefined";
    public static final String CLOUD_FLAG_DESCR = "Cloudy pixel";
    public static final String SNOWICE_FLAG_DESCR = "Snow or ice pixel";
    public static final String CLOUD_SHADOW_FLAG_DESCR = "Cloud shadow pixel";
    public static final String LAND_FLAG_DESCR = "Land pixel";
    public static final String GOOD_SWIR_FLAG_DESCR = "Pixel with good SWIR data";
    public static final String GOOD_NIR_FLAG_DESCR = "Pixel with good NIR data";
    public static final String GOOD_RED_FLAG_DESCR = "Pixel with good RED data";
    public static final String GOOD_BLUE_FLAG_DESCR = "Pixel with good BLUE data";

    public static final Color[] SM_COLORS = {
            new Color(120,255,180),
            new Color(255,255,0),
            new Color(0,255,255),
            new Color(255,100,0),
            new Color(255,255,180),
            new Color(255,0,255),
            new Color(0,0,255),
            new Color(180,180,255),
            new Color(255,150,100),
            new Color(0,255,0)
        };

}
