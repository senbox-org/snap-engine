package org.esa.snap.core.datamodel;


import java.io.File;
import java.io.IOException;

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
    private String colorBarLabels;
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

    public void setColorBarTitle(String colorBarTitle) {
        this.colorBarTitle = colorBarTitle;
    }

    public String getColorBarLabels() {
        return colorBarLabels;
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
}
