package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.RasterDataNode;

import java.util.ArrayList;
import java.util.Locale;

public class MetadataUtils {

    private enum DATE_FORMAT {
        CAMEL_LONG,
        CAMEL_SHORT
    }

    private enum TIME_FORMAT {
        HHMM,
        HHMMSS,
        HHMMSSms
    }

    private static final String INFO_PARAM_FILE = "FILE";
    private static final String INFO_PARAM_PROCESSING_VERSION = "PROCESSING_VERSION";
    private static final String INFO_PARAM_SENSOR = "SENSOR";
    private static final String INFO_PARAM_PLATFORM = "PLATFORM";
    private static final String INFO_PARAM_PROJECTION = "PROJECTION";
    private static final String INFO_PARAM_RESOLUTION = "RESOLUTION";
    private static final String INFO_PARAM_DAY_NIGHT = "DAY_NIGHT";
    private static final String INFO_PARAM_ORBIT = "ORBIT";
    private static final String INFO_PARAM_START_ORBIT = "START_ORBIT";
    private static final String INFO_PARAM_END_ORBIT = "END_ORBIT";
    private static final String INFO_PARAM_BAND = "BAND";
    private static final String INFO_PARAM_UNIT = "UNITS";
    private static final String INFO_PARAM_BAND_DESCRIPTION = "BAND_DESCRIPTION";
    private static final String INFO_PARAM_FILE_LOCATION = "FILE_LOCATION";
    private static final String INFO_PARAM_PRODUCT_TYPE = "PRODUCT_TYPE";
    private static final String INFO_PARAM_SCENE_DATE = "SCENE_DATE";
    private static final String INFO_PARAM_SCENE_DATE_SHORT = "SCENE_DATE_SHORT";
    private static final String INFO_PARAM_SCENE_DATE_LONG = "SCENE_DATE_LONG";
    private static final String INFO_PARAM_SCENE_DATETIME = "SCENE_DATETIME";
    private static final String INFO_PARAM_SCENE_DATETIME_SHORT = "SCENE_DATETIME_SHORT_HHMM";
    private static final String INFO_PARAM_SCENE_DATETIME_SHORT_SEC = "SCENE_DATETIME_SHORT_HHMMSS";
//    private static final String INFO_PARAM_SCENE_DATETIME_LONG = "SCENE_DATETIME_LONG_HHMM";
//    private static final String INFO_PARAM_SCENE_DATETIME_LONG_SEC = "SCENE_DATETIME_LONG_HHMMSS";
    private static final String INFO_PARAM_SCENE_START_DATETIME_SHORT = "SCENE_START_DATETIME_SHORT_HHMM";
    private static final String INFO_PARAM_SCENE_START_DATETIME_SHORT_SEC = "SCENE_START_DATETIME_SHORT_HHMMSS";
//    private static final String INFO_PARAM_SCENE_START_DATETIME_LONG = "SCENE_START_DATETIME_LONG_HHMM";
//    private static final String INFO_PARAM_SCENE_START_DATETIME_LONG_SEC = "SCENE_START_DATETIME_LONG_HHMMSS";
    private static final String INFO_PARAM_SCENE_START_TIME = "SCENE_START_TIME";
    private static final String INFO_PARAM_SCENE_END_TIME = "SCENE_END_TIME";
    private static final String INFO_PARAM_SCENE_HEIGHT = "SCENE_HEIGHT";
    private static final String INFO_PARAM_SCENE_WIDTH = "SCENE_WIDTH";
    private static final String INFO_PARAM_SCENE_SIZE = "SCENE_SIZE";


    public static final String INFO_PARAM_WAVE = "WAVELENGTH";
    private static final String INFO_PARAM_ANGLE = "ANGLE";
    private static final String INFO_PARAM_FLAG_CODING = "FLAG_CODING";
    private static final String INFO_PARAM_VALID_PIXEL_EXPRESSION = "VALID_PIXEL_EXPRESSION";
    private static final String INFO_PARAM_NO_DATA_VALUE = "NO_DATA_VALUE";
    private static final String INFO_PARAM_IS_NO_DATA_VALUE_SET = "IS_NO_DATA_VALUE_SET";
    private static final String INFO_PARAM_IS_NO_DATA_VALUE_USED = "IS_NO_DATA_VALUE_USED";
    private static final String INFO_PARAM_IS_SCALING_APPLIED = "IS_SCALING_APPLIED";
    private static final String INFO_PARAM_SCALING_FACTOR = "SCALING_FACTOR";
    private static final String INFO_PARAM_SCALING_OFFSET = "SCALING_OFFSET";
    private static final String INFO_PARAM_IS_LOG_SCALED = "IS_LOG_SCALED";
    private static final String INFO_PARAM_IS_PALETTE_LOG_SCALED = "IS_PALETTE_LOG_SCALED";
    private static final String INFO_PARAM_NODE_DISPLAY_NAMES = "NODE_DISPLAY_NAMES";
    private static final String INFO_PARAM_NODE_NAMES = "NODE_NAMES";


    public static String[] INFO_PARAMS = {
            INFO_PARAM_FILE,
//            INFO_PARAM_PROCESSING_VERSION,
//            INFO_PARAM_SENSOR,
//            INFO_PARAM_PLATFORM,
//            INFO_PARAM_PROJECTION,
//            INFO_PARAM_RESOLUTION,
//            INFO_PARAM_DAY_NIGHT,
//            INFO_PARAM_ORBIT,
//            INFO_PARAM_START_ORBIT,
//            INFO_PARAM_END_ORBIT,
            INFO_PARAM_BAND,
            INFO_PARAM_UNIT,
            INFO_PARAM_BAND_DESCRIPTION,
            INFO_PARAM_FILE_LOCATION,
            INFO_PARAM_PRODUCT_TYPE,
            INFO_PARAM_SCENE_DATE,
            INFO_PARAM_SCENE_DATE_SHORT,
            INFO_PARAM_SCENE_DATE_LONG,
            INFO_PARAM_SCENE_DATETIME,
            INFO_PARAM_SCENE_DATETIME_SHORT,
            INFO_PARAM_SCENE_DATETIME_SHORT_SEC,
//            INFO_PARAM_SCENE_DATETIME_LONG,
//            INFO_PARAM_SCENE_DATETIME_LONG_SEC,
            INFO_PARAM_SCENE_START_DATETIME_SHORT,
            INFO_PARAM_SCENE_START_DATETIME_SHORT_SEC,
//            INFO_PARAM_SCENE_START_DATETIME_LONG,
//            INFO_PARAM_SCENE_START_DATETIME_LONG_SEC,
            INFO_PARAM_SCENE_START_TIME,
            INFO_PARAM_SCENE_END_TIME,
            INFO_PARAM_SCENE_HEIGHT,
            INFO_PARAM_SCENE_WIDTH,
            INFO_PARAM_SCENE_SIZE,

            INFO_PARAM_WAVE,
            INFO_PARAM_ANGLE,
            INFO_PARAM_FLAG_CODING,
            INFO_PARAM_VALID_PIXEL_EXPRESSION,
            INFO_PARAM_NO_DATA_VALUE,
            INFO_PARAM_IS_NO_DATA_VALUE_SET,
            INFO_PARAM_IS_NO_DATA_VALUE_USED,
            INFO_PARAM_IS_SCALING_APPLIED,
            INFO_PARAM_SCALING_FACTOR,
            INFO_PARAM_SCALING_OFFSET,
            INFO_PARAM_IS_LOG_SCALED,
            INFO_PARAM_IS_PALETTE_LOG_SCALED,
            INFO_PARAM_NODE_DISPLAY_NAMES,
            INFO_PARAM_NODE_NAMES
    };


    public static String getDerivedMeta(String infoKey, RasterDataNode raster, String percentD_ReplacementKey) {
        String value = "";


        if (infoKey != null && infoKey.length() > 0) {
            infoKey = infoKey.toUpperCase();

            switch (infoKey) {


                case INFO_PARAM_FILE:
                    try {
                        value = raster.getProduct().getName();
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_FILE_LOCATION:
                    try {
                        value = raster.getProduct().getFileLocation().toString();
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_PRODUCT_TYPE:
                    try {
                        value = raster.getProduct().getProductType();
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_SCENE_DATE:
                    value = getSceneDate(raster, null);
                    break;

                case INFO_PARAM_SCENE_DATE_SHORT:
                    value = getSceneDate(raster, DATE_FORMAT.CAMEL_SHORT);
                    break;

                case INFO_PARAM_SCENE_DATE_LONG:
                    value = getSceneDate(raster, DATE_FORMAT.CAMEL_LONG);
                    break;

                case INFO_PARAM_SCENE_DATETIME:
                    value = getSceneDateTime(raster, null, null, false);
                    break;

                case INFO_PARAM_SCENE_DATETIME_SHORT:
                    value = getSceneDateTime(raster, DATE_FORMAT.CAMEL_SHORT, TIME_FORMAT.HHMM, false);
                    break;

//                case INFO_PARAM_SCENE_DATETIME_LONG:
//                    value = getSceneDateTime(raster, DATE_FORMAT.CAMEL_LONG, TIME_FORMAT.HHMM, false);
//                    break;

                case INFO_PARAM_SCENE_DATETIME_SHORT_SEC:
                    value = getSceneDateTime(raster, DATE_FORMAT.CAMEL_SHORT, TIME_FORMAT.HHMMSS, false);
                    break;

//                case INFO_PARAM_SCENE_DATETIME_LONG_SEC:
//                    value = getSceneDateTime(raster, DATE_FORMAT.CAMEL_LONG, TIME_FORMAT.HHMMSS, false);
//                    break;

                case INFO_PARAM_SCENE_START_DATETIME_SHORT:
                    value = getSceneDateTime(raster, DATE_FORMAT.CAMEL_SHORT, TIME_FORMAT.HHMM, true);
                    break;

//                case INFO_PARAM_SCENE_START_DATETIME_LONG:
//                    value = getSceneDateTime(raster, DATE_FORMAT.CAMEL_LONG, TIME_FORMAT.HHMM, true);
//                    break;

                case INFO_PARAM_SCENE_START_DATETIME_SHORT_SEC:
                    value = getSceneDateTime(raster, DATE_FORMAT.CAMEL_SHORT, TIME_FORMAT.HHMMSS, true);
                    break;

//                case INFO_PARAM_SCENE_START_DATETIME_LONG_SEC:
//                    value = getSceneDateTime(raster, DATE_FORMAT.CAMEL_LONG, TIME_FORMAT.HHMMSS, true);
//                    break;


                case INFO_PARAM_SCENE_START_TIME:
                    try {
                        value = raster.getProduct().getStartTime().toString();
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_SCENE_END_TIME:
                    try {
                        value = raster.getProduct().getEndTime().toString();
                    } catch (Exception e) {
                    }
                    break;

//                case INFO_PARAM_PROCESSING_VERSION:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PROCESSING_VERSION_KEYS);
//                    break;
//
//                case INFO_PARAM_SENSOR:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_SENSOR_KEYS);
//                    break;
//
//                case INFO_PARAM_PLATFORM:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PLATFORM_KEYS);
//                    break;
//
//                case INFO_PARAM_PROJECTION:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PROJECTION_KEYS);
//                    break;
//
//                case INFO_PARAM_RESOLUTION:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_RESOLUTION_KEYS);
//                    break;
//
//                case INFO_PARAM_DAY_NIGHT:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_DAY_NIGHT_KEYS);
//                    break;
//
//                case INFO_PARAM_ORBIT:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_ORBIT_KEYS);
//                    break;
//
//                case INFO_PARAM_START_ORBIT:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_START_ORBIT_KEYS);
//                    break;
//
//                case INFO_PARAM_END_ORBIT:
//                    value = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_END_ORBIT_KEYS);
//                    break;

                case INFO_PARAM_BAND:
                    value = raster.getName();
                    break;


                case INFO_PARAM_UNIT:
                    value = raster.getUnit();
                    break;

                case INFO_PARAM_BAND_DESCRIPTION:
                    value = raster.getDescription();
                    if (raster.getDescription() != null && raster.getDescription().length() > 0 && percentD_ReplacementKey != null && percentD_ReplacementKey.length() > 0) {
                        value = value.replace("%d", getDerivedMeta(percentD_ReplacementKey, raster, ""));
                    }

                    break;

                case INFO_PARAM_SCENE_HEIGHT:
                    value = Integer.toString(raster.getRasterHeight());
                    break;

                case INFO_PARAM_SCENE_WIDTH:
                    value = Integer.toString(raster.getRasterWidth());
                    break;

                case INFO_PARAM_SCENE_SIZE:
                    value = "(w x h) " + raster.getRasterWidth() + " pixels x " + raster.getRasterHeight() + " pixels";
                    break;

                case INFO_PARAM_WAVE:
                    //  value = String.valueOf(raster.getProduct().getBand(raster.getName()).getSpectralWavelength());
                    float wavelength = raster.getProduct().getBand(raster.getName()).getSpectralWavelength();
                    if (wavelength > 0.0) {
                        if (Math.ceil(wavelength) == Math.round(wavelength)) {
                            value = String.valueOf(Math.round(wavelength));
                        } else {
                            value = String.valueOf(wavelength);
                        }
                    }
                    break;

                case INFO_PARAM_ANGLE:
                    value = String.valueOf(raster.getProduct().getBand(raster.getName()).getAngularValue());
                    break;

                case INFO_PARAM_FLAG_CODING:
                    try {
                        value = String.valueOf(raster.getProduct().getBand(raster.getName()).getFlagCoding());
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_VALID_PIXEL_EXPRESSION:
                    value = raster.getValidPixelExpression();
                    break;

                case INFO_PARAM_NO_DATA_VALUE:
                    value = String.valueOf(raster.getNoDataValue());
                    break;

                case INFO_PARAM_IS_NO_DATA_VALUE_SET:
                    value = String.valueOf(raster.isNoDataValueSet());
                    break;

                case INFO_PARAM_IS_NO_DATA_VALUE_USED:
                    value = String.valueOf(raster.isNoDataValueUsed());
                    break;

                case INFO_PARAM_IS_SCALING_APPLIED:
                    value = String.valueOf(raster.isScalingApplied());
                    break;

                case INFO_PARAM_SCALING_FACTOR:
                    value = String.valueOf(raster.getScalingFactor());
                    break;

                case INFO_PARAM_SCALING_OFFSET:
                    value = String.valueOf(raster.getScalingOffset());
                    break;

                case INFO_PARAM_IS_LOG_SCALED:
                    value = String.valueOf(raster.isLog10Scaled());
                    break;

                case INFO_PARAM_IS_PALETTE_LOG_SCALED:
                    try {
                        value = String.valueOf(raster.getImageInfo().getColorPaletteDef().isLogScaled());
                    } catch (Exception e) {
                    }
                    break;

                case INFO_PARAM_NODE_DISPLAY_NAMES:
                    try {
                        value = "";
                        String[] nodeDisplayNames = raster.getOverlayMaskGroup().getNodeDisplayNames();
                        for (String nodeDisplayName : nodeDisplayNames) {
                            if (value.length() == 0) {
                                value = nodeDisplayName;
                            } else {
                                value = value + ", " + nodeDisplayName;
                            }
                        }
                    } catch (Exception e) {
                    }

                    break;

                case INFO_PARAM_NODE_NAMES:
                    try {
                        value = "";
                        String[] getNodeNames = raster.getOverlayMaskGroup().getNodeNames();
                        for (String getNodeName : getNodeNames) {
                            if (value.length() == 0) {
                                value = getNodeName;
                            } else {
                                value = value + ", " + getNodeName;
                            }
                        }
                    } catch (Exception e) {
                    }
                    break;


//                case "MY_INFO":
//                    value = getMyInfo();
//                    break;
//
//                case "MY_INFO1":
//                    value = getMyInfo1();
//                    break;
//
//                case "MY_INFO2":
//                    value = getMyInfo2();
//                    break;
//
//                case "MY_INFO3":
//                    value = getMyInfo3();
//                    break;
//
//                case "MY_INFO4":
//                    value = getMyInfo4();
//                    break;
            }


        }


        return value;
    }


    public static String getSceneDate(RasterDataNode raster, DATE_FORMAT dateFormat) {

        String value = null;

        try {
            String start_date = getSceneStartDate(raster, dateFormat);
            String end_date = getSceneEndDate(raster, dateFormat);

            if (start_date != null && start_date.length() > 0 && end_date != null && end_date.length() > 0) {

                if (start_date.trim().equals(end_date.trim())) {
                    value = start_date;
                } else {
                    String start_date_unformatted = getSceneStartDate(raster, null);
                    String end_date_unformatted = getSceneEndDate(raster, null);

                    String start_day = null;
                    String start_month = null;
                    String start_year = null;

                    if (start_date_unformatted != null) {
                        String[] start_date_orig_arr = start_date_unformatted.split("-", 3);
                        if (start_date_orig_arr != null && start_date_orig_arr.length == 3) {
                            start_day = start_date_orig_arr[0];
                            start_month = start_date_orig_arr[1];
                            start_year = start_date_orig_arr[2];
                        }
                    }

                    String end_day = null;
                    String end_month = null;
                    String end_year = null;

                    if (end_date_unformatted != null) {
                        String[] end_date_orig_arr = end_date_unformatted.split("-", 3);
                        if (end_date_orig_arr != null && end_date_orig_arr.length == 3) {
                            end_day = end_date_orig_arr[0];
                            end_month = end_date_orig_arr[1];
                            end_year = end_date_orig_arr[2];
                        }
                    }


                    if (start_day != null && start_month != null && start_year != null && end_day != null && end_month != null && end_year != null) {
                        if (start_day.equals("01") && end_day.equals("01") && !start_month.equals(end_month)) {
                            if (start_year.equals(end_year)) {
                                if (start_month.equals("JAN") && end_month.equals("FEB")) {
                                    value = "January " + start_year;
                                } else if (start_month.equals("FEB") && end_month.equals("MAR")) {
                                    value = "February " + start_year;
                                } else if (start_month.equals("MAR") && end_month.equals("APR")) {
                                    value = "March " + start_year;
                                } else if (start_month.equals("APR") && end_month.equals("MAY")) {
                                    value = "April " + start_year;
                                } else if (start_month.equals("MAY") && end_month.equals("JUN")) {
                                    value = "May " + start_year;
                                } else if (start_month.equals("JUN") && end_month.equals("JUL")) {
                                    value = "June " + start_year;
                                } else if (start_month.equals("JUL") && end_month.equals("AUG")) {
                                    value = "July " + start_year;
                                } else if (start_month.equals("AUG") && end_month.equals("SEP")) {
                                    value = "August " + start_year;
                                } else if (start_month.equals("SEP") && end_month.equals("OCT")) {
                                    value = "September " + start_year;
                                } else if (start_month.equals("OCT") && end_month.equals("NOV")) {
                                    value = "October " + start_year;
                                } else if (start_month.equals("NOV") && end_month.equals("DEC")) {
                                    value = "November " + start_year;
                                }
                            } else {
                                if (Integer.valueOf(start_year) == (Integer.valueOf(end_year)) - 1) {
                                    if (start_month.equals("DEC") && end_month.equals("JAN")) {
                                        value = "December " + start_year;
                                    }
                                }
                            }
                        } else if (start_day.equals("01") && end_day.equals("01") && start_month.equals("JAN") && end_month.equals("JAN")) {
                            if (Integer.valueOf(start_year) == (Integer.valueOf(end_year)) - 1) {
                                value = start_year;
                            }
                        } else {
                            value = "(" + start_date + " - " + end_date + ")";
                        }
                    } else {
                        value = "(" + start_date + " - " + end_date + ")";
                    }
                }
            } else {
                value = start_date;
            }

        } catch (Exception e) {
        }

        return value;
    }


    public static String getSceneDateTime(RasterDataNode raster, DATE_FORMAT dateFormat, TIME_FORMAT timeFormat, boolean startTimeOnly) {

        String value = null;
        try {
            String start_date = getSceneStartDate(raster, dateFormat);
            String start_time_only = getSceneStartTimeOnly(raster, timeFormat);

            String start_datetime = start_date;
            if (start_time_only != null && start_time_only.length() > 0) {
                start_datetime = start_date + " " + start_time_only;
            }


            String end_date = getSceneEndDate(raster, dateFormat);
            String end_time_only = getSceneEndTimeOnly(raster, timeFormat);

            String end_datetime = start_date;
            if (end_time_only != null && end_time_only.length() > 0) {
                end_datetime = end_date + " " + end_time_only;
            }


            if (!startTimeOnly && start_datetime != null && start_datetime.length() > 0 && end_datetime != null && end_datetime.length() > 0) {
                if (start_datetime.trim().equals(end_datetime.trim())) {
                    value = start_datetime;
                } else {
                    value = "(" + start_datetime + " - " + end_datetime + ")";
                }
            } else {
                value = start_datetime;
            }

        } catch (Exception e) {
        }

        return value;
    }

        public static String getSceneStartDate(RasterDataNode raster, DATE_FORMAT dateFormat) {
        String start_datetime = raster.getProduct().getStartTime().toString();
        return getSceneDateInnerMethod(start_datetime, dateFormat);
    }

    public static String getSceneEndDate(RasterDataNode raster, DATE_FORMAT dateFormat) {
        String start_datetime = raster.getProduct().getEndTime().toString();
        return getSceneDateInnerMethod(start_datetime, dateFormat);
    }


    public static String getSceneDateInnerMethod(String datetime, DATE_FORMAT dateFormat) {

        String[] datetime_arr = datetime.split("\\s+", 2);

        if (datetime_arr.length > 0) {
            if (dateFormat == null) {
                return datetime_arr[0].trim();
            }

            if (dateFormat != null && (dateFormat == DATE_FORMAT.CAMEL_LONG || dateFormat == DATE_FORMAT.CAMEL_SHORT)) {
                String[] datetime2_arr = datetime_arr[0].split("-", 3);

                if (datetime2_arr.length == 3) {
                    String day = datetime2_arr[0];
                    String month = datetime2_arr[1];
                    String year = datetime2_arr[2];

                    if (dateFormat == DATE_FORMAT.CAMEL_LONG) {
                        month = formatMonthCamelLong(month);
                    } else if (dateFormat == DATE_FORMAT.CAMEL_SHORT) {
                        month = formatMonthCamelShort(month);
                    }

                    return month + " " + day + ", " + year;
                }
            }

            return datetime_arr[0];
        } else {
            return "";
        }
    }


    public static String formatMonthCamelLong(String month) {

        month = month.toUpperCase();

        switch (month) {
            case "JAN":
                return "January";
            case "FEB":
                return "February";
            case "MAR":
                return "March";
            case "APR":
                return "April";
            case "MAY":
                return "May";
            case "JUN":
                return "June";
            case "JUL":
                return "July";
            case "AUG":
                return "August";
            case "SEP":
                return "September";
            case "OCT":
                return "October";
            case "NOV":
                return "November";
            case "DEC":
                return "December";
        }

        return month;
    }

    public static String formatMonthCamelShort(String month) {

        month = month.toUpperCase();

        switch (month) {
            case "JAN":
                return "Jan";
            case "FEB":
                return "Feb";
            case "MAR":
                return "Mar";
            case "APR":
                return "Apr";
            case "MAY":
                return "May";
            case "JUN":
                return "Jun";
            case "JUL":
                return "Jul";
            case "AUG":
                return "Aug";
            case "SEP":
                return "Sep";
            case "OCT":
                return "Oct";
            case "NOV":
                return "Nov";
            case "DEC":
                return "Dec";
        }

        return month;
    }


    public static String getSceneStartTimeOnly(RasterDataNode raster, TIME_FORMAT timeFormat) {
        String start_datetime = raster.getProduct().getStartTime().toString();
        return getSceneTimeOnly(start_datetime, timeFormat);
    }

    public static String getSceneEndTimeOnly(RasterDataNode raster, TIME_FORMAT timeFormat) {
        String end_datetime = raster.getProduct().getEndTime().toString();
        return getSceneTimeOnly(end_datetime, timeFormat);
    }


    public static String getSceneTimeOnly(String datetime, TIME_FORMAT timeFormat) {
        String[] end_datetime_arr = datetime.split("\\s+", 2);

        if (end_datetime_arr.length > 1) {
            String time_only = end_datetime_arr[1];

            if (timeFormat == null) {
                return time_only;
            }

            if (timeFormat == TIME_FORMAT.HHMMSS || timeFormat == TIME_FORMAT.HHMM) {
                String[] time_milli_split = time_only.split("\\.");

                if (time_milli_split.length > 0) {
                    String[] time_parts = time_milli_split[0].split(":");

                    String hour = "";
                    String minute = "";
                    String second = "";
                    if (time_parts.length >= 2) {
                        hour = time_parts[0];
                        minute = time_parts[1];
                    }

                    if (time_parts.length >= 3) {
                        second = time_parts[2];
                    }

                    if (timeFormat == TIME_FORMAT.HHMM) {
                        return hour + ":" + minute;
                    } else {
                        return time_milli_split[0];
                    }
                } else {
                    return "";
                }
            } else {
                return time_only;
            }
        } else {
            return "";
        }
    }


    public static String getReplacedStringAllVariables(String inputText, RasterDataNode raster, String delimiter, String percentD_ReplacementKey) {

        String currentLine = inputText;

        try {
            currentLine = MetadataUtils.getReplacedStringSingleVariableWrapper(currentLine, false, "PROPERTY=", raster, delimiter, "");
            currentLine = MetadataUtils.getReplacedStringSingleVariableWrapper(currentLine, false, "GLOBAL_ATTRIBUTE=", raster, delimiter, "");
            currentLine = MetadataUtils.getReplacedStringSingleVariableWrapper(currentLine, false, "BAND_ATTRIBUTE=", raster, delimiter, "");

            if (currentLine != null) {
                for (String infoKey : INFO_PARAMS) {
                    String enclosedInfoKey;
//                    String enclosedInfoKey = "{" + infoKey + "}";
//                    currentLine = currentLine.replace(enclosedInfoKey, getDerivedMeta(infoKey.toUpperCase(), raster, percentD_ReplacementKey));

                    enclosedInfoKey = "<" + infoKey + ">";
                    currentLine = currentLine.replace(enclosedInfoKey, getDerivedMeta(infoKey.toUpperCase(), raster, percentD_ReplacementKey));


//                    enclosedInfoKey = "[" + infoKey + "]";
//                    currentLine = currentLine.replace(enclosedInfoKey, getDerivedMeta(infoKey.toUpperCase(), raster, percentD_ReplacementKey));


                    enclosedInfoKey = "&lt;" + infoKey + "&gt;";
                    currentLine = currentLine.replace(enclosedInfoKey, getDerivedMeta(infoKey.toUpperCase(), raster, percentD_ReplacementKey));
                }
            }

        } catch (Exception e) {
            return currentLine;
        }

        return currentLine;
    }

    private static String getReplacedStringSingleVariableWrapper(String inputString, boolean showKeys, String replaceKey, RasterDataNode raster, String delimiter, String percentD_ReplacementKey) {
        String replacedText = inputString;

        try {
            String replaceKeyStart = "<";
            String replaceKeyEnd = ">";
            replacedText = MetadataUtils.getReplacedStringSingleVariable(replacedText, false, replaceKeyStart, replaceKey, replaceKeyEnd, raster, delimiter, "");

//            replaceKeyStart = "{";
//            replaceKeyEnd = "}";
//            replacedText = MetadataUtils.getReplacedStringSingleVariable(replacedText, false, replaceKeyStart, replaceKey, replaceKeyEnd, raster, delimiter, "");


//            replaceKeyStart = "[";
//            replaceKeyEnd = "]";
//            replacedText = MetadataUtils.getReplacedStringSingleVariable(replacedText, false, replaceKeyStart, replaceKey, replaceKeyEnd, raster, delimiter, "");


            replaceKeyStart = "&lt;";
            replaceKeyEnd = "&gt;";
            replacedText = MetadataUtils.getReplacedStringSingleVariable(replacedText, false, replaceKeyStart, replaceKey, replaceKeyEnd, raster, delimiter, "");
        } catch (Exception e) {
        }

        return replacedText;
    }

    public static String getReplacedStringSingleVariable(String inputString, boolean showKeys, String replaceKeyStart, String replaceKey, String replaceKeyEnd, RasterDataNode raster, String delimiter, String percentD_ReplacementKey) {
        if (inputString != null && inputString.length() > 0) {
////            inputString = inputString.replace("[FILE]", raster.getProduct().getName());
////            inputString = inputString.replace("[File]", raster.getProduct().getName());
//            inputString = replaceStringVariablesCase(inputString, "[FILE]", raster.getProduct().getName());
//
//            inputString = inputString.replace("[BAND]", raster.getName());
//            inputString = inputString.replace("[BAND_DESCRIPTION]", raster.getDescription());
//
//            inputString = inputString.replace("[PROCESSING_VERSION]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PROCESSING_VERSION_KEYS));
//            inputString = inputString.replace("[SENSOR]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_SENSOR_KEYS));
//            inputString = inputString.replace("[PLATFORM]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PLATFORM_KEYS));
//            inputString = inputString.replace("[PROJECTION]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PROJECTION_KEYS));
//            inputString = inputString.replace("[RESOLUTION]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_RESOLUTION_KEYS));
//
//            inputString = inputString.replace("[DAY_NIGHT]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_DAY_NIGHT_KEYS));
//            inputString = inputString.replace("[ORBIT]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_ORBIT_KEYS));
//            inputString = inputString.replace("[START_ORBIT]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_START_ORBIT_KEYS));
//            inputString = inputString.replace("[END_ORBIT]", ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_END_ORBIT_KEYS));
//
//
//            inputString = inputString.replace("[ID]", ProductUtils.getMetaData(raster.getProduct(), "id"));
//            inputString = inputString.replace("[L2_FLAG_NAMES]", ProductUtils.getMetaData(raster.getProduct(), "l2_flag_names"));
//
//            inputString = inputString.replace("[TITLE]", raster.getProduct().toString());
//            inputString = inputString.replace("[FILE_LOCATION]", raster.getProduct().getFileLocation().toString());
//            inputString = inputString.replace("[PRODUCT_TYPE]", raster.getProduct().getProductType());
//            inputString = inputString.replace("[SCENE_START_TIME]", raster.getProduct().getStartTime().toString());
//            inputString = inputString.replace("[SCENE_END_TIME]", raster.getProduct().getEndTime().toString());
//            inputString = inputString.replace("[SCENE_HEIGHT]", Integer.toString(raster.getRasterHeight()));
//            inputString = inputString.replace("[SCENE_WIDTH]", Integer.toString(raster.getRasterWidth()));
//            String sceneSize = "(w x h) " + raster.getRasterWidth() + " pixels x " + raster.getRasterHeight() + " pixels";
//            inputString = inputString.replace("[SCENE_SIZE]", sceneSize);
//
//            raster.getImageInfo().getColorPaletteDef().isLogScaled();
//            raster.getValidPixelExpression();
//            raster.getUnit();
//            raster.getOverlayMaskGroup().getNodeDisplayNames();
//            raster.getOverlayMaskGroup().getNodeNames();
//            raster.getProduct().getBand(raster.getName()).getSpectralWavelength();
//            raster.getProduct().getBand(raster.getName()).getAngularValue();
//            raster.getProduct().getBand(raster.getName()).getFlagCoding();
//            raster.getNoDataValue();
//            raster.isNoDataValueSet();
//            raster.isNoDataValueUsed();
//            raster.isScalingApplied();
//            raster.getScalingFactor();
//            raster.getScalingOffset();
//            raster.getProduct().getMetadataRoot().getElement("Band_Attributes").getElement(raster.getName()).getAttribute("reference").getData().getElemString();
//            raster.getProduct().getMetadataRoot().getElement("Band_Attributes").getElement(raster.getName()).getAttribute("valid_min").getData().getElemString();
//            raster.getProduct().getMetadataRoot().getElement("Band_Attributes").getElement(raster.getName()).getAttribute("valid_max").getData().getElemString();


            String metaId = null;
            String beforeMetaData = "";
            String afterMetaData = "";
            String metaStart = "";


            String replaceGoal = replaceKeyStart + replaceKey;

            switch (replaceKey) {
                case "PROPERTY=":
                    inputString = inputString.replace(replaceKeyStart + "Property=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "property=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "INFO=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "Info=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "info=", replaceGoal);
                    break;

                case "GLOBAL_ATTRIBUTE=":
//                    inputString = inputString.replace(replaceKeyStart+"META=", replaceGoal);
//                    inputString = inputString.replace(replaceKeyStart+"Meta=", replaceGoal);
//                    inputString = inputString.replace(replaceKeyStart+"meta=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "Global_Attribute=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "global_attribute=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "GLOBAL_ATTR=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "Global_Attr=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "global_attr=", replaceGoal);
                    break;

                case "BAND_ATTRIBUTE=":
//                    inputString = inputString.replace(replaceKeyStart+"BAND_META=", replaceGoal);
//                    inputString = inputString.replace(replaceKeyStart+"Band_Meta=", replaceGoal);
//                    inputString = inputString.replace(replaceKeyStart+"band_meta=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "Band_Attribute=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "band_attribute=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "BAND_ATTR=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "Band_Attr=", replaceGoal);
                    inputString = inputString.replace(replaceKeyStart + "band_attr=", replaceGoal);
                    break;
            }


            int whileCnt = 0;
            boolean hasMetaData = (inputString.contains(replaceGoal) && inputString.contains(replaceKeyEnd)) ? true : false;

            String safeReplaceGoal = replaceGoal;
            if ("{".equals(replaceKeyStart)) {
                safeReplaceGoal = safeReplaceGoal.replace("{", "\\{");
            }
            if ("[".equals(replaceKeyStart)) {
                safeReplaceGoal = safeReplaceGoal.replace("[", "\\[");
            }

            String safeReplaceEnd = replaceKeyEnd;
            if ("}".equals(safeReplaceEnd)) {
                safeReplaceEnd = safeReplaceEnd.replace("}", "\\}");
            }
            if ("[".equals(safeReplaceEnd)) {
                safeReplaceEnd = safeReplaceEnd.replace("]", "\\]");
            }


            while (hasMetaData && whileCnt < 10) {

                String[] arr1 = inputString.split(safeReplaceGoal, 2);

                if (arr1 != null) {
                    if (arr1.length == 1) {
                        beforeMetaData = arr1[0];
                        metaStart = "";
                    } else if (arr1.length == 2) {
                        beforeMetaData = arr1[0];
                        metaStart = arr1[1];
                    }
                } else {
                    beforeMetaData = "";
                    metaStart = "";
                }

                if (metaStart != null && metaStart.length() > 0) {

                    String[] arr2 = metaStart.split(safeReplaceEnd, 2);

                    if (arr2 != null && arr2.length == 2) {
                        metaId = arr2[0];
                        afterMetaData = arr2[1];
                    }
                }

                if (metaId != null && metaId.length() > 0) {
                    String value = "";


                    switch (replaceKey) {
                        case "PROPERTY=":
                            value = getDerivedMeta(metaId.toUpperCase(), raster, percentD_ReplacementKey);
                            break;

                        case "GLOBAL_ATTRIBUTE=":
                            value = getFileMetaWithPossibleVariantKeys(metaId, raster);
                            break;

                        case "BAND_ATTRIBUTE=":
                            value = ProductUtils.getBandMetaData(raster.getProduct(), metaId, raster.getName());
                            break;

                        default:
                            break;

                    }

                    if (showKeys) {
                        inputString = beforeMetaData + metaId + delimiter + value + afterMetaData;
                    } else {
                        inputString = beforeMetaData + value + afterMetaData;
                    }
                }

                hasMetaData = (inputString.contains(replaceGoal) && inputString.contains(replaceKeyEnd)) ? true : false;

                whileCnt++;
            }

        }

        return inputString;
    }


    private static String getFileMetaWithPossibleVariantKeys(String metaId, RasterDataNode raster) {
        String value = ProductUtils.getMetaData(raster.getProduct(), metaId);
        if (value == null || value.length() == 0) {
            for (String keyInSet : getAllPossibleRelatedKeys(metaId)) {
                value = ProductUtils.getMetaData(raster.getProduct(), keyInSet);
                if (value != null && value.length() > 0) {
                    metaId = keyInSet;
                    break;
                }
            }
        }

        return value;
    }

    public static String[] getAllPossibleRelatedKeys(String key) {
        ArrayList<String[]> keySets = new ArrayList<String[]>();

        keySets.add(ProductUtils.METADATA_POSSIBLE_PROJECTION_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_SENSOR_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_PLATFORM_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_PROCESSING_VERSION_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_DAY_NIGHT_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_ORBIT_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_START_ORBIT_KEYS);
        keySets.add(ProductUtils.METADATA_POSSIBLE_END_ORBIT_KEYS);

        for (String[] keySet : keySets) {
            for (String keyInSet : keySet) {
                if (keyInSet.equals(key)) {
                    return keySet;
                }
            }
        }

        return new String[]{key};
    }


}
