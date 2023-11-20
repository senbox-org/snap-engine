package org.esa.snap.core.util;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.util.ArrayList;

public class MetadataUtils {

    private enum DATE_FORMAT {
        MMM_DD_YYYY,
        MONTH_DD_YYYY,
        DD_MMM_YYYY,
        DD_MONTH_YYYY,
        MMM_YYYY,
        MONTH_YYYY,
        YYYY,
        NONE
    }

    private enum TIME_FORMAT {
        HH_MM_SS_ms,
        HH_MM_SS,
        HH_MM,
        HH,
        NONE
    }

    private static final String INFO_PARAM_FILE = "FILE";
    private static final String INFO_PARAM_TEMPORAL_RANGE_PARENTHESIS = "TEMPORAL_RANGE_PARENTHESIS";
    private static final String INFO_PARAM_MISSION_LEVEL_INFO = "MISSION_LEVEL_INFO";
    private static final String INFO_PARAM_MISSION_LEVEL_TEMPORAL_INFO = "MISSION_LEVEL_TEMPORAL_INFO";
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
    private static final String INFO_PARAM_SCENE_DATE_INFO = "SCENE_DATE_INFO";
    private static final String INFO_PARAM_SCENE_DATE_MONTHDDYYYY = "SCENE_DATE_MONTHDDYYYY";
    private static final String INFO_PARAM_SCENE_DATE_MMMDDYYYY = "SCENE_DATE_MMMDDYYYY";
    private static final String INFO_PARAM_SCENE_DATE_DDMONTHYYYY = "SCENE_DATE_DDMONTHYYYY";
    private static final String INFO_PARAM_SCENE_DATE_DDMMMYYYY = "SCENE_DATE_DDMMMYYYY";

    private static final String INFO_PARAM_SCENE_START_TIME = "SCENE_START_TIME";
    private static final String INFO_PARAM_SCENE_START_TIME_MONTHDDYYYY = "SCENE_START_TIME_MONTHDDYYYY";
    private static final String INFO_PARAM_SCENE_START_TIME_DDMONTHYYYY = "SCENE_START_TIME_DDMONTHYYYY";
    private static final String INFO_PARAM_SCENE_START_TIME_MMMDDYYYY = "SCENE_START_TIME_MMMDDYYYY";
    private static final String INFO_PARAM_SCENE_START_TIME_DDMMMYYYY = "SCENE_START_TIME_DDMMMYYYY";

    private static final String INFO_PARAM_SCENE_END_TIME = "SCENE_END_TIME";
    private static final String INFO_PARAM_SCENE_END_TIME_MONTHDDYYYY = "SCENE_END_TIME_MONTHDDYYYY";
    private static final String INFO_PARAM_SCENE_END_TIME_DDMONTHYYYY = "SCENE_END_TIME_DDMONTHYYYY";
    private static final String INFO_PARAM_SCENE_END_TIME_MMMDDYYYY = "SCENE_END_TIME_MMMDDYYYY";
    private static final String INFO_PARAM_SCENE_END_TIME_DDMMMYYYY = "SCENE_END_TIME_DDMMMYYYY";
    
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
            INFO_PARAM_TEMPORAL_RANGE_PARENTHESIS,
            INFO_PARAM_MISSION_LEVEL_INFO,
            INFO_PARAM_MISSION_LEVEL_TEMPORAL_INFO,
            INFO_PARAM_SCENE_DATE_INFO,
            INFO_PARAM_SCENE_DATE_MMMDDYYYY,
            INFO_PARAM_SCENE_DATE_DDMONTHYYYY,
            INFO_PARAM_SCENE_DATE_DDMMMYYYY,
            INFO_PARAM_SCENE_START_TIME,
            INFO_PARAM_SCENE_START_TIME_MONTHDDYYYY,
            INFO_PARAM_SCENE_START_TIME_DDMONTHYYYY,
            INFO_PARAM_SCENE_START_TIME_MMMDDYYYY,
            INFO_PARAM_SCENE_START_TIME_DDMMMYYYY,
            INFO_PARAM_SCENE_END_TIME,
            INFO_PARAM_SCENE_END_TIME_MONTHDDYYYY,
            INFO_PARAM_SCENE_END_TIME_DDMONTHYYYY,
            INFO_PARAM_SCENE_END_TIME_MMMDDYYYY,
            INFO_PARAM_SCENE_END_TIME_DDMMMYYYY,
            
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




                case INFO_PARAM_SCENE_DATE_INFO:
                    value = getSceneDate(raster, DATE_FORMAT.MONTH_DD_YYYY, TIME_FORMAT.HH_MM_SS, true);
                    break;

                case INFO_PARAM_SCENE_DATE_MONTHDDYYYY:
                    value = getSceneDate(raster, DATE_FORMAT.MONTH_DD_YYYY, TIME_FORMAT.HH_MM_SS, false);
                    break;

                case INFO_PARAM_SCENE_DATE_MMMDDYYYY:
                    value = getSceneDate(raster, DATE_FORMAT.MMM_DD_YYYY, TIME_FORMAT.HH_MM_SS, false);
                    break;

                case INFO_PARAM_SCENE_DATE_DDMONTHYYYY:
                    value = getSceneDate(raster, DATE_FORMAT.DD_MONTH_YYYY, TIME_FORMAT.HH_MM_SS, false);
                    break;

                case INFO_PARAM_SCENE_DATE_DDMMMYYYY:
                    value = getSceneDate(raster, DATE_FORMAT.DD_MMM_YYYY, TIME_FORMAT.HH_MM_SS, false);
                    break;

                case INFO_PARAM_SCENE_START_TIME:
                    value = getSceneStartDateTime(raster, null, null);
                    break;

                case INFO_PARAM_SCENE_START_TIME_MONTHDDYYYY:
                    value = getSceneStartDateTime(raster, DATE_FORMAT.MONTH_DD_YYYY, TIME_FORMAT.HH_MM_SS);
                    break;

                case INFO_PARAM_SCENE_START_TIME_DDMONTHYYYY:
                    value = getSceneStartDateTime(raster, DATE_FORMAT.DD_MONTH_YYYY, TIME_FORMAT.HH_MM_SS);
                    break;

                case INFO_PARAM_SCENE_START_TIME_MMMDDYYYY:
                    value = getSceneStartDateTime(raster, DATE_FORMAT.MMM_DD_YYYY, TIME_FORMAT.HH_MM_SS);
                    break;

                case INFO_PARAM_SCENE_START_TIME_DDMMMYYYY:
                    value = getSceneStartDateTime(raster, DATE_FORMAT.DD_MMM_YYYY, TIME_FORMAT.HH_MM_SS);
                    break;



                case INFO_PARAM_SCENE_END_TIME:
                    value = getSceneEndDateTime(raster, null, null);
                    break;

                case INFO_PARAM_SCENE_END_TIME_MONTHDDYYYY:
                    value = getSceneEndDateTime(raster, DATE_FORMAT.MONTH_DD_YYYY, TIME_FORMAT.HH_MM_SS);
                    break;

                case INFO_PARAM_SCENE_END_TIME_DDMONTHYYYY:
                    value = getSceneEndDateTime(raster, DATE_FORMAT.DD_MONTH_YYYY, TIME_FORMAT.HH_MM_SS);
                    break;

                case INFO_PARAM_SCENE_END_TIME_MMMDDYYYY:
                    value = getSceneEndDateTime(raster, DATE_FORMAT.MMM_DD_YYYY, TIME_FORMAT.HH_MM_SS);
                    break;

                case INFO_PARAM_SCENE_END_TIME_DDMMMYYYY:
                    value = getSceneEndDateTime(raster, DATE_FORMAT.DD_MMM_YYYY, TIME_FORMAT.HH_MM_SS);
                    break;



                case INFO_PARAM_TEMPORAL_RANGE_PARENTHESIS:
                    try {
                    value = ProductUtils.getMetaData(raster.getProduct(), "temporal_range");
                    if (value != null && value.length() > 0) {
                        value = "(" + value + ")";
                    }
                    } catch (Exception e) {
                    }
                    break;


                case INFO_PARAM_MISSION_LEVEL_INFO:
                    value = getMissionLevelInfo(raster, false);
                    break;

                case INFO_PARAM_MISSION_LEVEL_TEMPORAL_INFO:
                    value = getMissionLevelInfo(raster, true);
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


            }


        }


        return value;
    }

    public static String getMissionLevelInfo(RasterDataNode raster, boolean includeTemporalRange) {
        String value = "";

        try {

            String sensor = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_SENSOR_KEYS);
            String platform = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_PLATFORM_KEYS);
            String productType = raster.getProduct().getProductType();

            if (sensor != null && sensor.length() > 0 && platform != null && platform.length() > 0) {
                value = platform + "-" + sensor;
            } else if (sensor != null && sensor.length() > 0) {
                value = sensor;
            } else if (platform != null && platform.length() > 0) {
                value = platform;
            }

            if (productType != null && productType.length() > 0) {
                value = value + ": " + productType;

                if (includeTemporalRange) {
                    String temporalRange = ProductUtils.getMetaData(raster.getProduct(), "temporal_range");
                    if (temporalRange != null && temporalRange.length() > 0) {
                        value = value + " (" + temporalRange + ")";
                    }
                }
            }
        } catch (Exception e) {
        }
        return value;
    }



    public static String getSceneDate(RasterDataNode raster, DATE_FORMAT dateFormatDefault , TIME_FORMAT timeFormatDefault, boolean addTemporalRange) {
        String value = null;
        String temporalRange = ProductUtils.getMetaData(raster.getProduct(), "temporal_range");

        if (temporalRange != null && temporalRange.length() > 0) {
            if ("MONTH".equals(temporalRange.toUpperCase())) {
                value = getSceneStartDateTime(raster, DATE_FORMAT.MONTH_YYYY, TIME_FORMAT.NONE);
            } else if ("DAY".equals(temporalRange.toUpperCase())) {
                value = getSceneStartDateTime(raster, DATE_FORMAT.DD_MONTH_YYYY, TIME_FORMAT.NONE);
            } else if ("YEAR".equals(temporalRange.toUpperCase())) {
                value = getSceneStartDateTime(raster, DATE_FORMAT.YYYY, TIME_FORMAT.NONE);
            }
        }

        if (value == null) {
            String startDatetime = getSceneStartDateTime(raster, dateFormatDefault, timeFormatDefault);
            String endDatetime = getSceneEndDateTime(raster, dateFormatDefault, timeFormatDefault);

            if (startDatetime != null && startDatetime.length() > 0 && endDatetime != null && endDatetime.length() > 0) {
                value = startDatetime + " - " + endDatetime;
            } else if (startDatetime != null && startDatetime.length() > 0) {
                value = startDatetime;
            } else if (endDatetime != null && endDatetime.length() > 0) {
                value = endDatetime;
            }
        }


        if (addTemporalRange && temporalRange != null && temporalRange.length() > 0) {
            if ("MONTH".equals(temporalRange.toUpperCase())) {
                value = getSceneStartDateTime(raster, DATE_FORMAT.MONTH_YYYY, TIME_FORMAT.NONE) + " (Month Composite)";
            } else if ("DAY".equals(temporalRange.toUpperCase())) {
                value = getSceneStartDateTime(raster, DATE_FORMAT.DD_MONTH_YYYY, TIME_FORMAT.NONE) + " (Day Composite)";
            } else if ("YEAR".equals(temporalRange.toUpperCase())) {
                value = getSceneStartDateTime(raster, DATE_FORMAT.YYYY, TIME_FORMAT.NONE) + " (Year Composite)";
            } else {
                value = value + " (" + temporalRange + " Composite)";
            }
        }

        return value;
    }


    public static String getSceneStartDateTime(RasterDataNode raster, DATE_FORMAT dateFormat, TIME_FORMAT timeFormat) {

        String sceneStartTimeMeta = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_START_TIME_KEYS);
        if (sceneStartTimeMeta != null && sceneStartTimeMeta.length() > 0) {
            if ((dateFormat == null && timeFormat == null) || (dateFormat == DATE_FORMAT.NONE && timeFormat == TIME_FORMAT.NONE)) {
                return sceneStartTimeMeta;
            }
            return getSceneDateTime(sceneStartTimeMeta, dateFormat, timeFormat);
        }
        return "";
    }


    public static String getSceneEndDateTime(RasterDataNode raster, DATE_FORMAT dateFormat, TIME_FORMAT timeFormat) {

        String sceneEndTimeMeta = ProductUtils.getMetaData(raster.getProduct(), ProductUtils.METADATA_POSSIBLE_END_TIME_KEYS);
        if (sceneEndTimeMeta != null && sceneEndTimeMeta.length() > 0) {
            if ((dateFormat == null && timeFormat == null) || (dateFormat == DATE_FORMAT.NONE && timeFormat == TIME_FORMAT.NONE)) {
                return sceneEndTimeMeta;
            }
            return getSceneDateTime(sceneEndTimeMeta, dateFormat, timeFormat);
        }
        return "";
    }


    public static String getSceneDateTime(String dateTimeMeta, DATE_FORMAT dateFormat, TIME_FORMAT timeFormat) {

        String value = "";

        if (dateTimeMeta != null && dateTimeMeta.length() > 0) {
            ProductData.UTC dateTime = ProductUtils.parseUtcDate(dateTimeMeta);
            if (dateTime != null) {
                if (dateFormat != DATE_FORMAT.NONE) {
                    String datePart = getSceneDatePart(dateTime.toString(), dateFormat);
                    if (datePart != null) {
                        value = datePart;
                    }
                }

                if (timeFormat != TIME_FORMAT.NONE) {
                    String timePart = getSceneTimePart(dateTime.toString(), timeFormat);
                    if (timePart != null) {
                        if (value.length() > 0) {
                            value = value + " " + timePart;
                        } else {
                            value = timePart;
                        }
                    }
                }
            }
        }

        if (value.length() > 0) {
            return value;
        } else {
            return dateTimeMeta;
        }
    }


    public static String getSceneTimePart(String datetime, TIME_FORMAT timeFormat) {

        if (datetime == null) {
            return datetime;
        }

        String value = "";

        String[] datetimeArray = datetime.split("\\s+", 2);

        if (datetimeArray != null && datetimeArray.length > 1) {

            String fullTime = datetimeArray[1].trim();

            if (timeFormat == null) {
                return fullTime;
            }

            String hourMinuteSecond = "";
            String milliSecond = "";

            String[] timeMilliSplit = fullTime.split("\\.", 2);

            if (timeMilliSplit != null && timeMilliSplit.length == 2) {
                hourMinuteSecond = timeMilliSplit[0];
                milliSecond = timeMilliSplit[1];
            }

            if (hourMinuteSecond != null) {
                String[] timePartsArray = hourMinuteSecond.split(":", 3);

                if (timePartsArray != null && timePartsArray.length == 3) {
                    String hour = timePartsArray[0];
                    String minute = timePartsArray[1];
                    String second = timePartsArray[2];


                    if (timeFormat == TIME_FORMAT.HH_MM_SS_ms) {
                        value = hour + ":" + minute + ":" + second + "." + milliSecond;
                    } else if (timeFormat == TIME_FORMAT.HH_MM_SS) {
                        value = hour + ":" + minute + ":" + second;
                    } else if (timeFormat == TIME_FORMAT.HH_MM) {
                        value = hour + ":" + minute;
                    } else if (timeFormat == TIME_FORMAT.HH) {
                        value = hour;
                    } else {
                        value = fullTime;
                    }
                }
            }
        }

        return value;
    }


    public static String getSceneDatePart(String datetime, DATE_FORMAT dateFormat) {

        if (datetime == null) {
            return datetime;
        }

        String value = "";

        String[] datetimeArray = datetime.split("\\s+", 2);

        if (datetimeArray != null && datetimeArray.length > 0) {

            String fullDate = datetimeArray[0].trim();

            if (dateFormat == null) {
                return fullDate;
            }

            String[] datePartsArray = fullDate.split("-", 3);

            if (datePartsArray != null && datePartsArray.length == 3) {
                String day = datePartsArray[0];
                String month = datePartsArray[1];
                String year = datePartsArray[2];

                if (dateFormat == DATE_FORMAT.MONTH_DD_YYYY || dateFormat == DATE_FORMAT.MONTH_YYYY || dateFormat == DATE_FORMAT.DD_MONTH_YYYY) {
                    month = formatMonthCamelLong(month);
                } else if (dateFormat == DATE_FORMAT.MMM_DD_YYYY || dateFormat == DATE_FORMAT.MMM_YYYY || dateFormat == DATE_FORMAT.DD_MMM_YYYY) {
                    month = formatMonthCamelShort(month);
                }

                if (dateFormat == DATE_FORMAT.MMM_DD_YYYY || dateFormat == DATE_FORMAT.MONTH_DD_YYYY) {
                    value = month + " " + day + ", " + year;
                } else if (dateFormat == DATE_FORMAT.DD_MONTH_YYYY || dateFormat == DATE_FORMAT.DD_MMM_YYYY) {
                    value = day + " " + month + ", " + year;
                } else if (dateFormat == DATE_FORMAT.MMM_YYYY || dateFormat == DATE_FORMAT.MONTH_YYYY) {
                    value = month + " " + year;
                } else if (dateFormat == DATE_FORMAT.YYYY) {
                    value = year;
                } else {
                    value = datetimeArray[0];
                }
            }
        }

        return value;
    }


    public static String formatMonthCamelLong(String month) {

        if (month == null) {
            return month;
        }

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

        if (month == null) {
            return month;
        }

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
