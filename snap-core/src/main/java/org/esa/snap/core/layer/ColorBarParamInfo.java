package org.esa.snap.core.layer;


import org.esa.snap.core.datamodel.ImageLegend;

import java.awt.*;

/**
 * @author Daniel Knowles
 */

// todo Likely this file can de deleted as it was old code from SeaDAS 7.  It is possible that this file will be useful if implementing a color bar scheme

public class ColorBarParamInfo {

//    public static final String HORIZONTAL_STR = "Horizontal";
//    public static final String VERTICAL_STR = "Vertical";
//    public static final String LOCATION_INSIDE_STR = "Inside Image";
//    public static final String LOCATION_OUTSIDE_STR = "Outside Image";
//
//    public static final String LOCATION_TOP_LEFT = "Top Left";
//    public static final String LOCATION_TOP_CENTER = "Top Center";
//    public static final String LOCATION_TOP_RIGHT = "Top Right";
//    public static final String LOCATION_BOTTOM_LEFT = "Bottom Left";
//    public static final String LOCATION_BOTTOM_CENTER = "Bottom Center";
//    public static final String LOCATION_BOTTOM_RIGHT = "Bottom Right";
//
//    public static final String LOCATION_LEFT_UPPER = "Left Upper";
//    public static final String LOCATION_LEFT_CENTER = "Left Center";
//    public static final String LOCATION_LEFT_LOWER = "Left Lower";
//    public static final String LOCATION_RIGHT_UPPER = "Right Upper";
//    public static final String LOCATION_RIGHT_CENTER = "Right Center";
//    public static final String LOCATION_RIGHT_LOWER = "Right Lower";
//
//
//
//    public static String[] getHorizontalLocationArray() {
//        return  new String[]{
//                LOCATION_TOP_LEFT,
//                LOCATION_TOP_CENTER,
//                LOCATION_TOP_RIGHT,
//                LOCATION_BOTTOM_LEFT,
//                LOCATION_BOTTOM_CENTER,
//                LOCATION_BOTTOM_RIGHT
//        };    }
//
//    public static String[] getVerticalLocationArray() {
//        return  new String[]{
//                LOCATION_LEFT_UPPER,
//                LOCATION_LEFT_CENTER,
//                LOCATION_LEFT_LOWER,
//                LOCATION_RIGHT_UPPER,
//                LOCATION_RIGHT_CENTER,
//                LOCATION_RIGHT_LOWER
//        };    }
//
//
//
//
//
//    public static final int DEFAULT_LABELS_FONT_SIZE = 45;
//    public static final float DEFAULT_BACKGROUND_TRANSPARENCY = 0.5f;
//    public static final Boolean DEFAULT_SHOW_TITLE_ENABLED = Boolean.TRUE;
//    public static final String DEFAULT_TITLE = "";
//    public static final String DEFAULT_TITLE_UNITS = "";
//    public static final int DEFAULT_TITLE_FONT_SIZE = 50;
//    public static final int DEFAULT_TITLE_UNITS_FONT_SIZE = 35;
//    public static final double DEFAULT_SCALING_FACTOR = 1.0;
//    public static final int  DEFAULT_COLOR_BAR_LENGTH = ImageLegend.DEFAULT_COLOR_BAR_LENGTH;
//    public static final int DEFAULT_COLOR_BAR_THICKNESS = 60;
//    public static final double DEFAULT_LAYER_SCALING = 50;
//    public static final double DEFAULT_LAYER_OFFSET = 0;
//    public static final double DEFAULT_LAYER_SHIFT = 0;
//    public static final Boolean DEFAULT_CENTER_ON_LAYER = Boolean.TRUE;
//    public static final String DEFAULT_MANUAL_POINTS = "";
//    public static final String DEFAULT_DISTRIBUTION_TYPE = ImageLegend.DISTRIB_MANUAL_STR;
//    public static final int DEFAULT_NUM_TICK_MARKS = 5;
//    public static final int DEFAULT_DECIMAL_PLACES = 2;
//    private static final boolean DEFAULT_DECIMAL_PLACES_FORCE = false;
//    public static final Color DEFAULT_FOREGROUND_COLOR = Color.yellow;
//    public static final Color DEFAULT_BACKGROUND_COLOR = new Color(51,51,0);
//
//    public static final String DEFAULT_ORIENTATION = HORIZONTAL_STR;
//    public static final String DEFAULT_HORIZONTAL_LOCATION = LOCATION_BOTTOM_RIGHT;
//    public static final String DEFAULT_VERTICAL_LOCATION = LOCATION_RIGHT_LOWER;
//    public static final String DEFAULT_INSIDE_OUTSIDE_LOCATION = LOCATION_INSIDE_STR;
//
//
//
//
//
//    private int labelsFontSize = DEFAULT_LABELS_FONT_SIZE;
//    private float backgroundTransparency = DEFAULT_BACKGROUND_TRANSPARENCY;
//    private Boolean showTitle = DEFAULT_SHOW_TITLE_ENABLED;
//    private String title = DEFAULT_TITLE;
//    private String titleUnits = DEFAULT_TITLE_UNITS;
//    private int titleFontSize = DEFAULT_TITLE_FONT_SIZE;
//    private int titleUnitsFontSize = DEFAULT_TITLE_UNITS_FONT_SIZE;
//    private double scalingFactor = DEFAULT_SCALING_FACTOR;
//    private int colorBarLength = DEFAULT_COLOR_BAR_LENGTH;
//    private int colorBarThickness = DEFAULT_COLOR_BAR_THICKNESS;
//    private double layerScaling = DEFAULT_LAYER_SCALING;
//    private double layerOffset = DEFAULT_LAYER_OFFSET;
//    private double layerShift = DEFAULT_LAYER_SHIFT;
//    private Boolean centerOnLayer = DEFAULT_CENTER_ON_LAYER;
//    private String manualPoints = DEFAULT_MANUAL_POINTS;
//    private String orientation = DEFAULT_ORIENTATION;
//    private String distributionType = DEFAULT_DISTRIBUTION_TYPE;
//    private int numTickMarks = DEFAULT_NUM_TICK_MARKS;
//    private int decimalPlaces = DEFAULT_DECIMAL_PLACES;
//    private boolean decimalPlacesForce = DEFAULT_DECIMAL_PLACES_FORCE;
//    private Color foregroundColor = DEFAULT_FOREGROUND_COLOR;
//    private Color backgroundColor = DEFAULT_BACKGROUND_COLOR;
//
//    private String insideOutsideLocation = DEFAULT_INSIDE_OUTSIDE_LOCATION;
//    private String  horizontalLocation = DEFAULT_HORIZONTAL_LOCATION;
//    private String verticalLocation = DEFAULT_VERTICAL_LOCATION;
//
//
//
//
//    private boolean paramsInitialized = false;
//
//
//
//    public ColorBarParamInfo() {
//
//    }
//
//
//
//    public int getLabelsFontSize() {
//        return labelsFontSize;
//    }
//
//    public void setLabelsFontSize(int labelsFontSize) {
//        this.labelsFontSize = labelsFontSize;
//    }
//
//    public boolean isParamsInitialized() {
//        return paramsInitialized;
//    }
//
//    public void setParamsInitialized(boolean titleModified) {
//        this.paramsInitialized = titleModified;
//    }
//
//
//    public Boolean getShowTitle() {
//        return showTitle;
//    }
//
//    public void setShowTitle(Boolean showTitle) {
//        this.showTitle = showTitle;
//    }
//
//    public String getTitle() {
//        return title;
//    }
//
//    public void setTitle(String title) {
//        this.title = title;
//    }
//
//    public String getTitleUnits() {
//        return titleUnits;
//    }
//
//    public void setTitleUnits(String titleUnits) {
//        this.titleUnits = titleUnits;
//    }
//
//    public int getTitleFontSize() {
//        return titleFontSize;
//    }
//
//    public void setTitleFontSize(int titleFontSize) {
//        this.titleFontSize = titleFontSize;
//    }
//
//    public int getTitleUnitsFontSize() {
//        return titleUnitsFontSize;
//    }
//
//    public void setTitleUnitsFontSize(int titleUnitsFontSize) {
//        this.titleUnitsFontSize = titleUnitsFontSize;
//    }
//
//    public double getScalingFactor() {
//        return scalingFactor;
//    }
//
//    public void setScalingFactor(double scalingFactor) {
//        this.scalingFactor = scalingFactor;
//    }
//
//    public int getColorBarLength() {
//        return colorBarLength;
//    }
//
//    public void setColorBarLength(int colorBarLength) {
//        this.colorBarLength = colorBarLength;
//    }
//
//    public int getColorBarThickness() {
//        return colorBarThickness;
//    }
//
//    public void setColorBarThickness(int colorBarThickness) {
//        this.colorBarThickness = colorBarThickness;
//    }
//
//    public double getLayerScaling() {
//        return layerScaling;
//    }
//
//    public void setLayerScaling(double layerScaling) {
//        this.layerScaling = layerScaling;
//    }
//
//    public Boolean getCenterOnLayer() {
//        return centerOnLayer;
//    }
//
//    public void setCenterOnLayer(Boolean centerOnLayer) {
//        this.centerOnLayer = centerOnLayer;
//    }
//
//    public String getManualPoints() {
//        return manualPoints;
//    }
//
//    public void setManualPoints(String manualPoints) {
//        this.manualPoints = manualPoints;
//    }
//
//    public String getOrientation() {
//        return orientation;
//    }
//
//    public void setOrientation(String orientation) {
//        this.orientation = orientation;
//    }
//
//    public String getDistributionType() {
//        return distributionType;
//    }
//
//    public void setDistributionType(String distributionType) {
//        this.distributionType = distributionType;
//    }
//
//    public int getNumTickMarks() {
//        return numTickMarks;
//    }
//
//    public void setNumTickMarks(int numTickMarks) {
//        this.numTickMarks = numTickMarks;
//    }
//
//    public int getDecimalPlaces() {
//        return decimalPlaces;
//    }
//
//    public void setDecimalPlaces(int decimalPlaces) {
//        this.decimalPlaces = decimalPlaces;
//    }
//
//    public Color getForegroundColor() {
//        return foregroundColor;
//    }
//
//    public void setForegroundColor(Color foregroundColor) {
//        this.foregroundColor = foregroundColor;
//    }
//
//    public Color getBackgroundColor() {
//        return backgroundColor;
//    }
//
//    public void setBackgroundColor(Color backgroundColor) {
//        this.backgroundColor = backgroundColor;
//    }
//
//    public boolean isDecimalPlacesForce() {
//        return decimalPlacesForce;
//    }
//
//    public void setDecimalPlacesForce(boolean decimalPlacesForce) {
//        this.decimalPlacesForce = decimalPlacesForce;
//    }
//
//    public double getLayerOffset() {
//        return layerOffset;
//    }
//
//    public void setLayerOffset(double layerOffset) {
//        this.layerOffset = layerOffset;
//    }
//
//
//    public double getLayerShift() {
//        return layerShift;
//    }
//
//    public void setLayerShift(double layerShift) {
//        this.layerShift = layerShift;
//    }
//
//
//    public String getHorizontalLocation() {
//        return horizontalLocation;
//    }
//
//    public void setHorizontalLocation(String horizontalLocation) {
//        this.horizontalLocation = horizontalLocation;
//    }
//
//    public String getVerticalLocation() {
//        return verticalLocation;
//    }
//
//    public void setVerticalLocation(String verticalLocation) {
//        this.verticalLocation = verticalLocation;
//    }
//
//    public String getInsideOutsideLocation() {
//        return insideOutsideLocation;
//    }
//
//    public void setInsideOutsideLocation(String insideOutsideLocation) {
//        this.insideOutsideLocation = insideOutsideLocation;
//    }
//
//    public boolean isInsideLocation() {
//        if (LOCATION_INSIDE_STR.equals(getInsideOutsideLocation())) {
//            return true;
//        } else {
//            return false;
//        }
//    }
//
//    public float getBackgroundTransparency() {
//        return backgroundTransparency;
//    }
//
//    public void setBackgroundTransparency(float backgroundTransparency) {
//        this.backgroundTransparency = backgroundTransparency;
//    }
}
