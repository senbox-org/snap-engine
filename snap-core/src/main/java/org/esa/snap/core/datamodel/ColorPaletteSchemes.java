package org.esa.snap.core.datamodel;

/**
 * Created by knowles on 11/20/19.
 */



import org.esa.snap.core.util.PropertyMap;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.nio.file.Path;



/**
 * Created by danielknowles on 6/28/14.
 */
public class ColorPaletteSchemes {


    //    public static final String CPD_DEFAULTS_FILENAME = "scheme_defaults.txt";
//    public static final String CPD_COLORBLIND_DEFAULTS_FILENAME = "scheme_defaults_universal.txt";
//    public static final String CPD_COLORBLIND_SELECTOR_FILENAME = "scheme_selector_universal.txt";
//    public static final String CPD_SCHEMES_FILENAME = "scheme_selector.txt";
//    public static final String USER_CPD_DEFAULTS_FILENAME = "scheme_defaults_user.txt";
//    public static final String USER_CPD_SCHEMES_FILENAME = "scheme_selector_user.txt";
    public static final String DEFAULT_CPD_FILENAME = "gray_scale.cpd";


    public static final String NEW_CPD_SELECTOR_FILENAME = "color_palette_scheme_selector.txt";
    public static final String NEW_CPD_DEFAULTS_FILENAME = "color_palette_scheme_defaults.txt";
    public static final String NEW_CPD_SCHEMES_FILENAME = "color_palette_schemes.txt";
    public static final String COLORBAR_TITLE_OVERRIDE_MACRO = "USE_SCHEME_VALUE";


    public static final String PROPERTY_NAME_PALETTES_COLOR_BLIND_ENABLED = "palettes.colorBlind.enabled";
    public static final boolean DEFAULT_PALETTES_COLOR_BLIND_ENABLED = false;

    public boolean isjComboBoxShouldFire() {
        return jComboBoxShouldFire;
    }

    public void setjComboBoxShouldFire(boolean jComboBoxShouldFire) {
        this.jComboBoxShouldFire = jComboBoxShouldFire;
    }

    public static enum Id {
        SELECTOR,
        DEFAULTS
    }


    private final String STANDARD_SCHEME_COMBO_BOX_FIRST_ENTRY_NAME = "Scheme Selector";

    private ArrayList<ColorPaletteInfo> newColorPaletteSchemeInfos = new ArrayList<ColorPaletteInfo>();
//    private ArrayList<ColorPaletteInfo> newColorPaletteDefaultInfos = new ArrayList<ColorPaletteInfo>();
//    private ArrayList<ColorPaletteInfo> newColorPaletteSelectorInfos = new ArrayList<ColorPaletteInfo>();

    private File newColorPaletteSchemesFile = null;
    private File newColorPaletteDefaultsFile = null;
    private File newColorPaletteSelectorFile = null;


    private JComboBox jComboBox = null;
    private boolean jComboBoxShouldFire = true;

    private ArrayList<ColorPaletteInfo> colorPaletteInfos = new ArrayList<ColorPaletteInfo>();
    private ColorPaletteInfo jComboBoxFirstEntryColorPaletteInfo = null;

    private File colorPaletteDir = null;
    private File schemesFile = null;
    //   private File userSchemesFile = null;
    private String jComboBoxFirstEntryName = null;
    PropertyMap configuration = null;
    boolean useColorBlind = false;


    public ColorPaletteSchemes(File colorPaletteDir, Id id, boolean userInterfaceMode, PropertyMap configuration) {
        this.colorPaletteDir = colorPaletteDir;
        this.configuration = configuration;
        this.useColorBlind = getUseColorBlind();


        initColorPaletteSchemeInfos();

        switch (id) {
            case SELECTOR:
                schemesFile = new File(this.colorPaletteDir, NEW_CPD_SELECTOR_FILENAME);

                setjComboBoxFirstEntryName(STANDARD_SCHEME_COMBO_BOX_FIRST_ENTRY_NAME);
                break;
            case DEFAULTS:
                schemesFile = new File(this.colorPaletteDir, NEW_CPD_DEFAULTS_FILENAME);
                break;

        }


        if (colorPaletteDir != null && colorPaletteDir.exists()) {

            if (userInterfaceMode) {
                initComboBox();
            } else {
                // this mode is used for setting the default color scheme for an image when first opened
                // it doesn't need comboBoxes, only the colorPaletteInfos is needed
                initColorPaletteInfos(colorPaletteDir, colorPaletteInfos, schemesFile, false);
            }

            reset();
        }
    }


    private void initComboBox() {

        jComboBoxFirstEntryColorPaletteInfo = new ColorPaletteInfo(getjComboBoxFirstEntryName(), null, null, null, 0, 0, false, true, true, null, null, null, colorPaletteDir);
        colorPaletteInfos.add(jComboBoxFirstEntryColorPaletteInfo);

        initColorPaletteInfos(colorPaletteDir, colorPaletteInfos, schemesFile, true);


        Object[] colorPaletteInfosArray = colorPaletteInfos.toArray();

        final String[] toolTipsArray = new String[colorPaletteInfos.size()];

        int i = 0;
        for (ColorPaletteInfo colorPaletteInfo : colorPaletteInfos) {
            toolTipsArray[i] = colorPaletteInfo.getDescription();
            i++;
        }

        final Boolean[] enabledArray = new Boolean[colorPaletteInfos.size()];

        i = 0;
        for (ColorPaletteInfo colorPaletteInfo : colorPaletteInfos) {
            enabledArray[i] = colorPaletteInfo.isEnabled();
            i++;
        }

        final MyComboBoxRenderer myComboBoxRenderer = new MyComboBoxRenderer();
        myComboBoxRenderer.setTooltipList(toolTipsArray);
        myComboBoxRenderer.setEnabledList(enabledArray);

        jComboBox = new JComboBox(colorPaletteInfosArray);
        jComboBox.setRenderer(myComboBoxRenderer);
        jComboBox.setEditable(false);
        jComboBox.setMaximumRowCount(20);
        if (schemesFile != null) {
            jComboBox.setToolTipText("To modify see file: " + colorPaletteDir + "/" + schemesFile.getName());
        }


    }


    private boolean initColorPaletteSchemeInfos() {


        newColorPaletteSchemesFile = new File(this.colorPaletteDir, NEW_CPD_SCHEMES_FILENAME);
        if (!newColorPaletteSchemesFile.exists()) {
            return false;
        }

        ArrayList<String> lines = readFileIntoArrayList(newColorPaletteSchemesFile);


        int i = 0;
        for (String line : lines) {
            line.trim();
            if (!line.startsWith("#")) {
                String[] values = line.split(":");

                if (values != null) {
                    boolean validEntry = true;
                    boolean fieldsInitialized = false;

                    String id = null;
                    Double min = null;
                    Double max = null;
                    boolean logScaled = false;
                    String cpdFileNameStandard = null;
                    String cpdFileNameColorBlind = null;
                    String colorBarTitle = null;
                    String colorBarLabels = null;
                    String description = null;
                    String rootSchemeName = null;

                    File standardCpdFile = null;
                    File colorBlindCpdFile = null;
                    File cpdFile = null;

                    boolean overRide = true;

                    //        ID    MIN   MAX     LOG_SCALE  CPD_FILENAME   CPD_FILENAME(COLORBLIND)  COLORBAR_TITLE    COLORBAR_LABELS     DESCRIPTION


                    if (values.length >= 8) {

                        if (values.length >= 9) {
                            description = values[8].trim();
                        } else {
                            description = "";
                        }

                        if (values.length >= 8) {
                            colorBarLabels = values[7].trim();
                        } else {
                            colorBarLabels = "";
                        }

                        if (values.length >= 7) {
                            colorBarTitle = values[6].trim();
                        } else {
                            colorBarTitle = "";
                        }

                        id = values[0].trim();
                        String minStr = values[1].trim();
                        String maxStr = values[2].trim();
                        String logScaledStr = values[3].trim();
                        cpdFileNameStandard = values[4].trim();
                        cpdFileNameColorBlind = values[5].trim();


                        if (id != null && id.length() > 0 &&
                                minStr != null && minStr.length() > 0 &&
                                maxStr != null && maxStr.length() > 0 &&
                                logScaledStr != null && logScaledStr.length() > 0 &&
                                cpdFileNameStandard != null && cpdFileNameStandard.length() > 0 &&
                                cpdFileNameColorBlind != null && cpdFileNameStandard.length() > 0
                                ) {


                            min = Double.valueOf(minStr);
                            max = Double.valueOf(maxStr);
                            logScaled = false;
                            if (logScaledStr != null && logScaledStr.toLowerCase().equals("true")) {
                                logScaled = true;
                            }

                            fieldsInitialized = true;
                        }
                    }

                    if (fieldsInitialized) {

                        if (!testMinMax(min, max, logScaled)) {
                            validEntry = false;
                        }

                        if (validEntry) {
                            standardCpdFile = new File(colorPaletteDir, cpdFileNameStandard);

                            if (!standardCpdFile.exists()) {
                                validEntry = false;
                                //  standardCpdFile = new File(dirName, DEFAULT_CPD_FILENAME);
                            }
                        }

                        if (validEntry) {
                            colorBlindCpdFile = new File(colorPaletteDir, cpdFileNameColorBlind);

                            if (!colorBlindCpdFile.exists()) {
                                validEntry = false;
                            }
                        }

                        if (validEntry) {
                            if (getUseColorBlind()) {
                                cpdFile = colorBlindCpdFile;
                            } else {
                                cpdFile = standardCpdFile;
                            }
                        }


                        if (validEntry) {
                            ColorPaletteInfo colorPaletteInfo = null;

                            ColorPaletteDef colorPaletteDef;
                            try {
                                colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(cpdFile);
                                colorPaletteInfo = new ColorPaletteInfo(id, rootSchemeName, description, cpdFileNameStandard, min, max, logScaled, overRide, true, cpdFileNameColorBlind, colorBarTitle, colorBarLabels, colorPaletteDir);

                            } catch (IOException e) {
                                //        colorPaletteInfo = new ColorPaletteInfo(name, description);
                            }


                            if (colorPaletteInfo != null) {
                                if (overRide) {
                                    // look for previous name which user may be overriding and delete it in the colorPaletteInfo object
                                    ColorPaletteInfo colorPaletteInfoToDelete = null;
                                    for (ColorPaletteInfo storedColorPaletteInfo : newColorPaletteSchemeInfos) {
                                        if (storedColorPaletteInfo.getName().equals(id)) {
                                            colorPaletteInfoToDelete = storedColorPaletteInfo;
                                            break;
                                        }
                                    }
                                    if (colorPaletteInfoToDelete != null) {
                                        newColorPaletteSchemeInfos.remove(colorPaletteInfoToDelete);
                                    }
                                }
                                newColorPaletteSchemeInfos.add(colorPaletteInfo);
                            }
                        }
                    }
                }
            }
        }

        return true;
    }


    private void initColorPaletteInfos(File dirName, ArrayList<ColorPaletteInfo> colorPaletteInfos, File file, boolean schemeSelectorMode) {

        ArrayList<String> lines = readFileIntoArrayList(file);


        int i = 0;
        for (String line : lines) {
            line.trim();
            if (!line.startsWith("#")) {
                String[] values = line.split(":");

                if (values != null) {
                    boolean validEntry = false;
                    boolean fieldsInitialized = false;

                    String name = null;
                    Double minVal = null;
                    Double maxVal = null;
                    boolean logScaled = false;
                    String cpdFileNameStandard = null;
                    boolean overRide = true;
                    String description = null;
                    String rootSchemeName = null;
                    String cpdFileNameColorBlind = null;
                    String colorBarTitle = null;
                    String colorBarLabels = null;

                    String desiredScheme = null;

                    File standardCpdFile = null;
                    File colorBlindCpdFile = null;
                    File cpdFile = null;


                    //    #PATTERN              SCHEME_ID        :COLORBAR_TITLE(OVERRIDE)    :COLORBAR_LABELS(OVERRIDE)      :DESCRIPTION(OVERRIDE)


                    if (values.length >= 1) {
                        name = values[0].trim();


                        if (values.length >= 5) {
                            description = values[4].trim();
                            if (description.length() == 0) {
                                description = null;
                            }
                        }

                        if (values.length >= 4) {
                            colorBarLabels = values[3].trim();
                            if (colorBarLabels.length() == 0) {
                                colorBarLabels = null;
                            }
                        }

                        if (values.length >= 3) {
                            colorBarTitle = values[2].trim();
                            if (colorBarTitle.length() == 0) {
                                colorBarTitle = null;
                            }
                        }


                        if (values.length >= 2) {
                            desiredScheme = values[1].trim();
                            if (desiredScheme.length() == 0) {
                                desiredScheme = name;
                            }
                        } else {
                            desiredScheme = name;
                        }


                        if (name != null && name.length() > 0 && desiredScheme != null && desiredScheme.length() > 0) {

                            for (ColorPaletteInfo storedColorPaletteInfo : newColorPaletteSchemeInfos) {
                                if (storedColorPaletteInfo.getName().equals(desiredScheme)) {
                                    if (!fieldsInitialized ||
                                            (fieldsInitialized && overRide)) {

                                        cpdFileNameStandard = storedColorPaletteInfo.getCpdFilenameStandard();
                                        minVal = storedColorPaletteInfo.getMinValue();
                                        maxVal = storedColorPaletteInfo.getMaxValue();
                                        logScaled = storedColorPaletteInfo.isLogScaled();
                                        rootSchemeName = desiredScheme;
                                        cpdFileNameColorBlind = storedColorPaletteInfo.getCpdFilenameColorBlind();

                                        if (colorBarTitle != null && COLORBAR_TITLE_OVERRIDE_MACRO.equals(colorBarTitle)) {
                                            colorBarTitle = storedColorPaletteInfo.getColorBarTitle();
                                        }


                                        if (colorBarLabels != null && COLORBAR_TITLE_OVERRIDE_MACRO.equals(colorBarLabels)) {
                                            colorBarLabels = storedColorPaletteInfo.getColorBarLabels();
                                        }

                                        if (description != null && COLORBAR_TITLE_OVERRIDE_MACRO.equals(description)) {
                                            description = storedColorPaletteInfo.getDescription();
                                        }

                                        fieldsInitialized = true;
                                    }
                                }
                            }
                        }
                    }


                    if (fieldsInitialized) {

                        colorBlindCpdFile = new File(colorPaletteDir, cpdFileNameColorBlind);
                        standardCpdFile = new File(colorPaletteDir, cpdFileNameStandard);
                        if (getUseColorBlind()) {
                            cpdFile = colorBlindCpdFile;
                        } else {
                            cpdFile = standardCpdFile;
                        }

                        ColorPaletteInfo colorPaletteInfo = null;
                        ColorPaletteDef colorPaletteDef;


                        try {
                            colorPaletteDef = ColorPaletteDef.loadColorPaletteDef(cpdFile);
                            colorPaletteInfo = new ColorPaletteInfo(name, rootSchemeName, description, cpdFileNameStandard, minVal, maxVal, logScaled, overRide, true, cpdFileNameColorBlind, colorBarTitle, colorBarLabels, colorPaletteDir);

                        } catch (IOException e) {
                            //        colorPaletteInfo = new ColorPaletteInfo(name, description);
                        }


                        if (colorPaletteInfo != null) {
                            if (schemeSelectorMode && overRide) {
                                // look for previous name which user may be overriding and delete it in the colorPaletteInfo object
                                ColorPaletteInfo colorPaletteInfoToDelete = null;
                                for (ColorPaletteInfo storedColorPaletteInfo : colorPaletteInfos) {
                                    if (storedColorPaletteInfo.getName().equals(name)) {
                                        colorPaletteInfoToDelete = storedColorPaletteInfo;
                                        break;
                                    }
                                }
                                if (colorPaletteInfoToDelete != null) {
                                    colorPaletteInfos.remove(colorPaletteInfoToDelete);
                                }
                            }
                            colorPaletteInfos.add(colorPaletteInfo);
                        }
                    }


                }
            }
        }
    }


    private boolean testMinMax(double min, double max, boolean isLogScaled) {
        boolean checksOut = true;

        if (min == max) {
            checksOut = false;
        }

        if (isLogScaled && min == 0) {
            checksOut = false;
        }

        return checksOut;
    }


    public void reset() {
        if (jComboBox != null) {
            jComboBox.setSelectedItem(jComboBoxFirstEntryColorPaletteInfo);
        }
    }

    public ColorPaletteInfo setSchemeName(String schemeName) {

        if (schemeName != null) {
            for (ColorPaletteInfo colorPaletteInfo : colorPaletteInfos) {
                if (schemeName.trim().equals(colorPaletteInfo.getName().trim())) {
                    jComboBox.setSelectedItem(colorPaletteInfo);
                    return colorPaletteInfo;
                }
            }
        }

        return null;
    }


    public ArrayList<String> readFileIntoArrayList(File file) {
        String lineData;
        ArrayList<String> fileContents = new ArrayList<String>();
        BufferedReader moFile = null;
        try {
            moFile = new BufferedReader(new FileReader(file));
            while ((lineData = moFile.readLine()) != null) {

                fileContents.add(lineData);
            }
        } catch (IOException e) {
            ;
        } finally {
            try {
                moFile.close();
            } catch (Exception e) {
                //Ignore
            }
        }
        return fileContents;
    }


    public JComboBox getjComboBox() {
        return jComboBox;
    }

    public ArrayList<ColorPaletteInfo> getColorPaletteInfos() {
        return colorPaletteInfos;
    }


    class MyComboBoxRenderer extends BasicComboBoxRenderer {

        private String[] tooltips;
        private Boolean[] enabledList;

        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {

            if (index >= 0 && index < enabledList.length) {
                setEnabled(enabledList[index]);
                setFocusable(enabledList[index]);
            }


            if (isSelected) {
                setBackground(Color.blue);

                if (index >= 0 && index < tooltips.length) {
                    list.setToolTipText(tooltips[index]);
                }

                if (index >= 0 && index < enabledList.length) {

                    if (enabledList[index]) {
                        setForeground(Color.white);
                    } else {
                        setForeground(Color.gray);
                    }
                }

            } else {
                setBackground(Color.white);

                if (index >= 0 && index < enabledList.length) {
                    if (enabledList[index] == true) {
                        setForeground(Color.black);
                    } else {
                        setForeground(Color.gray);
                    }

                }
            }


            if (index == 0) {
                setForeground(Color.black);
            }

            setFont(list.getFont());
            setText((value == null) ? "" : value.toString());


            return this;
        }

        public void setTooltipList(String[] tooltipList) {
            this.tooltips = tooltipList;
        }


        public void setEnabledList(Boolean[] enabledList) {
            this.enabledList = enabledList;
        }
    }


    public String getjComboBoxFirstEntryName() {
        return jComboBoxFirstEntryName;
    }

    private void setjComboBoxFirstEntryName(String jComboBoxFirstEntryName) {
        this.jComboBoxFirstEntryName = jComboBoxFirstEntryName;
    }


    public static boolean getUseColorBlind(PropertyMap configuration) {

        if (configuration != null) {
            return configuration.getPropertyBool(PROPERTY_NAME_PALETTES_COLOR_BLIND_ENABLED, DEFAULT_PALETTES_COLOR_BLIND_ENABLED);
        } else {
            return DEFAULT_PALETTES_COLOR_BLIND_ENABLED;
        }

    }

    public boolean getUseColorBlind() {

        if (configuration != null) {
            return configuration.getPropertyBool(PROPERTY_NAME_PALETTES_COLOR_BLIND_ENABLED, DEFAULT_PALETTES_COLOR_BLIND_ENABLED);
        } else {
            return DEFAULT_PALETTES_COLOR_BLIND_ENABLED;
        }


    }

}