package org.esa.snap.core.datamodel;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Contains all info for a color scheme
 * @author Daniel Knowles (NASA)
 * @date Nov 2019
 *
 */

public class ColorSchemeInfo {
    private String name;
    private String displayName;
    private String description;
    private String cpdFilenameStandard;
    private String cpdFilenameColorBlind;
    private String colorBarTitle;
    private String colorBarTitleAlt;
    private String colorBarLabels;
    private String colorBarUnits;
    private String colorBarUnitsAlt;
    private String colorBarLengthStr;
    private String colorBarLabelScalingStr;
    private double minValue;
    private double maxValue;
    private boolean isLogScaled;
    private boolean enabled;
    private boolean divider;
    private boolean primary;
    private File colorPaletteDir;
    private boolean duplicateEntry = false;

    private boolean useDisplayName = true;

    public ColorSchemeInfo(String name, boolean primary, boolean divider, String displayName, String description, String cpdFilenameStandard, double minValue, double maxValue,
                           boolean isLogScaled, boolean enabled, String cpdFilenameColorBlind, String colorBarTitle, String colorBarLabels, File colorPaletteDir) {
        this(name, primary, divider, displayName, description, cpdFilenameStandard, minValue, maxValue,
                isLogScaled, enabled, cpdFilenameColorBlind, colorBarTitle, null, colorBarLabels, null, null, null, null, colorPaletteDir);

    }

    public ColorSchemeInfo(String name, boolean primary, boolean divider, String displayName, String description, String cpdFilenameStandard, double minValue, double maxValue,
                           boolean isLogScaled, boolean enabled, String cpdFilenameColorBlind, String colorBarTitle, String colorBarTitleAlt, String colorBarLabels,
                           String colorBarUnits, String colorBarUnitsAlt, String colorBarLabelScalingStr, String colorBarLengthStr, File colorPaletteDir) {
        this.setName(name);

        this.primary = primary;
        this.divider = divider;
        this.displayName = displayName;
        this.setDescription(description);
        this.setCpdFilenameStandard(cpdFilenameStandard);
        this.setMinValue(minValue);
        this.setMaxValue(maxValue);
        this.setLogScaled(isLogScaled);
        this.setEnabled(enabled);
        this.cpdFilenameColorBlind = cpdFilenameColorBlind;
        this.colorBarLabels = colorBarLabels;
        this.colorBarTitle = colorBarTitle;
        this.colorBarTitleAlt = colorBarTitleAlt;
        this.colorBarUnits = colorBarUnits;
        this.colorBarUnitsAlt = colorBarUnitsAlt;
        this.colorBarLabelScalingStr = colorBarLabelScalingStr;
        this.colorBarLengthStr = colorBarLengthStr;
        this.setColorPaletteDir(colorPaletteDir);
    }


    public boolean isDivider() {
        return divider;
    }

    public void setUseDisplayName(boolean useDisplayName) {
        this.useDisplayName = useDisplayName;
    }

    public boolean isUseDisplayName() {
        return this.useDisplayName;
    }


    public ColorPaletteDef getColorPaletteDef(boolean useColorBlindPalette) {
        File cpdFile = new File(colorPaletteDir, getCpdFilename(useColorBlindPalette));
        try {
            return ColorPaletteDef.loadColorPaletteDef(cpdFile);

        } catch (IOException e) {
            return null;
        }
    }


    public String getCpdFilename(boolean isUseColorBlind) {
        if (isUseColorBlind) {
            return getCpdFilenameColorBlind();
        } else {
            return getCpdFilenameStandard();
        }
    }

    public String getCpdFilenameStandard() {
        return cpdFilenameStandard;
    }

    private void setCpdFilenameStandard(String cpdFilename) {
        this.cpdFilenameStandard = cpdFilename;
    }

    public double getMinValue() {
        return minValue;
    }

    private void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    private void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public boolean isLogScaled() {
        return isLogScaled;
    }

    private void setLogScaled(boolean isLogScaled) {
        this.isLogScaled = isLogScaled;
    }

    public String toString() {
        return toString(isUseDisplayName());
    }

    public String toString(boolean verbose) {
        if (verbose && displayName != null && displayName.length() > 0) {
            return displayName;
        } else {
            return getName();
        }
    }

    public String getDisplayName() {
        return displayName;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public boolean isEnabled() {
        return enabled;
    }

    private void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }



    public String getCpdFilenameColorBlind() {
        return cpdFilenameColorBlind;
    }

    public void setCpdFilenameColorBlind(String cpdFilenameColorBlind) {
        this.cpdFilenameColorBlind = cpdFilenameColorBlind;
    }

    public String getColorBarTitle() {
        return colorBarTitle;
    }

    public String getColorBarTitleAlt() {
        return colorBarTitleAlt;
    }

    public void setColorBarTitle(String colorBarTitle) {
        this.colorBarTitle = colorBarTitle;
    }

    public String getColorBarLabels() {
        return colorBarLabels;
    }

    public String getColorBarLabelScalingStr() {
        return colorBarLabelScalingStr;
    }

    public String getColorBarLengthStr() {
        return colorBarLengthStr;
    }

    public String getColorBarUnits() {
        return colorBarUnits;
    }

    public String getColorBarUnitsAlt() {
        return colorBarUnitsAlt;
    }

    public void setColorBarLabels(String colorBarLabels) {
        this.colorBarLabels = colorBarLabels;
    }


    public File getColorPaletteDir() {
        return colorPaletteDir;
    }

    public void setColorPaletteDir(File colorPaletteDir) {
        this.colorPaletteDir = colorPaletteDir;
    }

    public boolean isDuplicateEntry() {
        return duplicateEntry;
    }

    public void setDuplicateEntry(boolean duplicateEntry) {
        this.duplicateEntry = duplicateEntry;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }


    public static String getColorBarTitle(String colorBarTitle, String bandname, String description, float wavelength, String units, boolean allowWavelengthZero) {
        
        String wavelengthString = "";
        if (wavelength > 0.0) {
            if (Math.ceil(wavelength) == Math.round(wavelength)) {
                wavelengthString = String.valueOf(Math.round(wavelength));
            } else {
                wavelengthString = String.valueOf(wavelength);
            }
        }

        if (units == null) {
            units = "";
        }
//        if (units != null && units.length() > 0) {
//            units = "dimensionless";
//        }

        // Allow "%d" to become [WAVELENGTH] but only in the description
        if (description != null && description.contains("%d")) {
            while (colorBarTitle.contains("%d")) {
                colorBarTitle = colorBarTitle.replace("%d", "[WAVELENGTH]");
            }
        }

        if (colorBarTitle != null && colorBarTitle.trim().length() > 0) {
            while(colorBarTitle.contains("[DESCRIPTION]")) {
                colorBarTitle = colorBarTitle.replace("[DESCRIPTION]", description);
            }

            while(colorBarTitle.contains("<DESCRIPTION>")) {
                colorBarTitle = colorBarTitle.replace("<DESCRIPTION>", description);
            }
            while(colorBarTitle.contains("[DESC]")) {
                colorBarTitle = colorBarTitle.replace("[DESC]", description);
            }

            while(colorBarTitle.contains("<DESC>")) {
                colorBarTitle = colorBarTitle.replace("<DESC>", description);
            }

            while(colorBarTitle.contains("[BANDNAME]")) {
                colorBarTitle = colorBarTitle.replace("[BANDNAME]", bandname);
            }

            while(colorBarTitle.contains("<BANDNAME>")) {
                colorBarTitle = colorBarTitle.replace("<BANDNAME>", bandname);
            }

            while(colorBarTitle.contains("[BAND]")) {
                colorBarTitle = colorBarTitle.replace("[BAND]", bandname);
            }

            while(colorBarTitle.contains("<BAND>")) {
                colorBarTitle = colorBarTitle.replace("<BAND>", bandname);
            }

            if (units != null) {
                while (colorBarTitle.contains("[UNITS]")) {
                    colorBarTitle = colorBarTitle.replace("[UNITS]", units);
                }
                while (colorBarTitle.contains("<UNITS>")) {
                    colorBarTitle = colorBarTitle.replace("<UNITS>", units);
                }
                while (colorBarTitle.contains("[UNIT]")) {
                    colorBarTitle = colorBarTitle.replace("[UNIT]", units);
                }
                while (colorBarTitle.contains("<UNIT>")) {
                    colorBarTitle = colorBarTitle.replace("<UNIT>", units);
                }
            }

            if (colorBarTitle.contains("[WAVELENGTH]") || colorBarTitle.contains("<WAVELENGTH>") ||
                    colorBarTitle.contains("[WAVE]") || colorBarTitle.contains("<WAVE>")) {
                if (wavelength > 0.0) {
                    while (colorBarTitle.contains("[WAVELENGTH]")) {
                        colorBarTitle = colorBarTitle.replace("[WAVELENGTH]", wavelengthString);
                    }
                    while (colorBarTitle.contains("<WAVELENGTH>")) {
                        colorBarTitle = colorBarTitle.replace("<WAVELENGTH>", wavelengthString);
                    }
                    while (colorBarTitle.contains("[WAVE]")) {
                        colorBarTitle = colorBarTitle.replace("[WAVE]", wavelengthString);
                    }
                    while (colorBarTitle.contains("<WAVE>")) {
                        colorBarTitle = colorBarTitle.replace("<WAVE>", wavelengthString);
                    }
                } else {
                    if (!allowWavelengthZero) {
                        colorBarTitle = "";
                    }
                }
            }
        }

        return  colorBarTitle;
    }

    public static ColorSchemeInfo getColorPaletteInfoByBandNameLookup(String bandName) {

        ColorSchemeManager colorSchemeManager = ColorSchemeManager.getDefault();
        if (colorSchemeManager != null) {

            if (bandName != null) {
                bandName.trim();
            }

            ArrayList<ColorSchemeLookupInfo> colorSchemeLookupInfos = colorSchemeManager.getColorSchemeLookupInfos();
            for (ColorSchemeLookupInfo colorSchemeLookupInfo : colorSchemeLookupInfos) {
                if (colorSchemeLookupInfo.isMatch(bandName)) {
                    return colorSchemeManager.getColorSchemeInfoBySchemeId(colorSchemeLookupInfo.getScheme_id());
                }
            }
        }

        return null;
    }
}
