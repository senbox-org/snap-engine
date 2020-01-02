package org.esa.snap.core.datamodel;

import org.esa.snap.core.util.PropertyMap;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;



/**
 * Created by knowles on 11/20/19.
 */


/**
 * Created by danielknowles on 6/28/14.
 */
public class ColorSchemes {

    //    public static final String DEFAULT_CPD_FILENAME = "gray_scale.cpd";
    public static final String DEFAULT_CPD_FILENAME = "oceancolor_standard.cpd";

    public static final String GRAY_SCALE = "GRAY SCALE";
    public static final String STANDARD_COLOR = "STANDARD";
    public static final String UNIVERSAL_COLOR = "UNIVERSAL";
    public static final String OTHER_COLOR = "OTHER";
    public static final String STANDARD_SCHEME = "From Scheme STANDARD";
    public static final String UNIVERSAL_SCHEME = "From Scheme UNIVERSAL";


    public static final String RANGE_FROM_SCHEME = "From Scheme";
    public static final String RANGE_FROM_DATA = "From Data";
    public static final String RANGE_FROM_CPD = "From Cpd";

    public static final String LOG_TRUE = "TRUE";
    public static final String LOG_FALSE = "FALSE";
    public static final String LOG_FROM_CPD = "From Cpd";
    public static final String LOG_FROM_SCHEME = "From Scheme";




    public static final String PROPERTY_DEFAULT_CPD_SECTION_KEY = "color.manipulation.default.cpd.section";
    public static final String PROPERTY_DEFAULT_CPD_SECTION_LABEL = "Default Palettes";
    public static final String PROPERTY_DEFAULT_CPD_SECTION_TOOLTIP = "Default palettes";
    public static final String PROPERTY_DEFAULT_CPD_SECTION_ALIAS = "colorManipulationDefaultCpdSection";

    public static final String PROPERTY_STANDARD_COLOR_CPD_KEY = "color.manipulation.standard.color.cpd";
    public static final String PROPERTY_STANDARD_COLOR_CPD_LABEL = STANDARD_COLOR;
    public static final String PROPERTY_STANDARD_COLOR_CPD_TOOLTIP = "The cpd file to use when STANDARD COLOR is selected";
    public static final String PROPERTY_STANDARD_COLOR_CPD_ALIAS = "colorManipulationStandardColorCpd";
    public static final String PROPERTY_STANDARD_COLOR_CPD_DEFAULT = "oceancolor_standard.cpd";
    public static final Class PROPERTY_STANDARD_COLOR_CPD_TYPE = String.class;

    public static final String PROPERTY_COLOR_BLIND_CPD_KEY = "color.manipulation.color.blind.cpd";
    public static final String PROPERTY_COLOR_BLIND_CPD_LABEL = UNIVERSAL_COLOR;
    public static final String PROPERTY_COLOR_BLIND_CPD_TOOLTIP = "The color blind compliant cpd file to use when UNIVERSAL COLOR is selected";
    public static final String PROPERTY_COLOR_BLIND_CPD_ALIAS = "colorManipulationColorBlindCpd";
    public static final String PROPERTY_COLOR_BLIND_CPD_DEFAULT = "universal_bluered.cpd";
    public static final Class PROPERTY_COLOR_BLIND_CPD_TYPE = String.class;

    public static final String PROPERTY_GRAY_SCALE_CPD_KEY = "color.manipulation.gray.scale.cpd";
    public static final String PROPERTY_GRAY_SCALE_CPD_LABEL = GRAY_SCALE;
    public static final String PROPERTY_GRAY_SCALE_CPD_TOOLTIP = "The cpd file to use when GRAY SCALE is selected";
    public static final String PROPERTY_GRAY_SCALE_CPD_ALIAS = "colorManipulationGrayScaleCpd";
    public static final String PROPERTY_GRAY_SCALE_CPD_DEFAULT = "gray_scale.cpd";
    public static final Class PROPERTY_GRAY_SCALE_CPD_TYPE = String.class;


    public static final String PROPERTY_OTHER_CPD_KEY = "color.manipulation.other.cpd";
    public static final String PROPERTY_OTHER_CPD_LABEL = OTHER_COLOR;
    public static final String PROPERTY_OTHER_CPD_TOOLTIP = "The cpd file to use when OTHER is selected";
    public static final String PROPERTY_OTHER_CPD_ALIAS = "colorManipulationOtherCpd";
    public static final String PROPERTY_OTHER_CPD_DEFAULT = "gradient_red_white_blue.cpd";
    public static final Class PROPERTY_OTHER_CPD_TYPE = String.class;



    public static final String PROPERTY_GENERAL_BEHAVIOR_SECTION_KEY = "color.manipulation.general.behavior.section";
    public static final String PROPERTY_GENERAL_BEHAVIOR_SECTION_LABEL = "General (No Scheme) Options";
    public static final String PROPERTY_GENERAL_BEHAVIOR_SECTION_TOOLTIP = "General behavior";
    public static final String PROPERTY_GENERAL_BEHAVIOR_SECTION_ALIAS = "colorManipulationGeneralBehaviorSection";


    public static final String PROPERTY_SCHEME_BEHAVIOR_SECTION_KEY = "color.manipulation.scheme.behavior.section";
    public static final String PROPERTY_SCHEME_BEHAVIOR_SECTION_LABEL = "Scheme Options";
    public static final String PROPERTY_SCHEME_BEHAVIOR_SECTION_TOOLTIP = "Scheme behavior";
    public static final String PROPERTY_SCHEME_BEHAVIOR_SECTION_ALIAS = "colorManipulationSchemeBehaviorSection";

    public static final String PROPERTY_GENERAL_CPD_KEY = "color.manipulation.general.cpd";
    public static final String PROPERTY_GENERAL_CPD_LABEL = "Palette";
    public static final String PROPERTY_GENERAL_CPD_TOOLTIP = "The cpd file to use when no color scheme";
    public static final String PROPERTY_GENERAL_CPD_ALIAS = "colorManipulationGeneralCpd";
    public static final String PROPERTY_GENERAL_CPD_OPTION1 = GRAY_SCALE;
    public static final String PROPERTY_GENERAL_CPD_OPTION2 = STANDARD_COLOR;
    public static final String PROPERTY_GENERAL_CPD_OPTION3 = UNIVERSAL_COLOR;
    public static final String PROPERTY_GENERAL_CPD_OPTION4 = OTHER_COLOR;
    public static final String PROPERTY_GENERAL_CPD_DEFAULT = GRAY_SCALE;
    public static final Class PROPERTY_GENERAL_CPD_TYPE = String.class;

    public static final String PROPERTY_GENERAL_RANGE_KEY = "color.manipulation.general.range";
    public static final String PROPERTY_GENERAL_RANGE_LABEL = "Range";
    public static final String PROPERTY_GENERAL_RANGE_TOOLTIP = "range options for the no scheme case";
    public static final String PROPERTY_GENERAL_RANGE_ALIAS = "colorManipulationGeneralRange";
    public static final String PROPERTY_GENERAL_RANGE_OPTION1 = RANGE_FROM_DATA;
    public static final String PROPERTY_GENERAL_RANGE_OPTION2 = RANGE_FROM_CPD;
    public static final String PROPERTY_GENERAL_RANGE_DEFAULT = RANGE_FROM_DATA;
    public static final Class PROPERTY_GENERAL_RANGE_TYPE = String.class;


    public static final String PROPERTY_GENERAL_LOG_KEY = "color.manipulation.general.log";
    public static final String PROPERTY_GENERAL_LOG_LABEL = "Log Scaling";
    public static final String PROPERTY_GENERAL_LOG_TOOLTIP = "log options for the no scheme case";
    public static final String PROPERTY_GENERAL_LOG_ALIAS = "colorManipulationGeneralLog";
    public static final String PROPERTY_GENERAL_LOG_OPTION1 = LOG_TRUE;
    public static final String PROPERTY_GENERAL_LOG_OPTION2 = LOG_FALSE;
    public static final String PROPERTY_GENERAL_LOG_OPTION3 = LOG_FROM_CPD;
    public static final String PROPERTY_GENERAL_LOG_DEFAULT = LOG_FALSE;
    public static final Class PROPERTY_GENERAL_LOG_TYPE = String.class;









    public static final String PROPERTY_USE_COLOR_BLIND_CPD_KEY = "color.manipulation.use.color.blind.cpd";
    public static final String PROPERTY_USE_COLOR_BLIND_CPD_LABEL = "Use Color Blind Palettes";
    public static final String PROPERTY_USE_COLOR_BLIND_CPD_TOOLTIP = "Use the color blind compliant palettes";
    public static final String PROPERTY_USE_COLOR_BLIND_CPD_ALIAS = "colorManipulationUseColorBlindCpd";
    public static final boolean PROPERTY_USE_COLOR_BLIND_CPD_DEFAULT = false;
    public static final Class PROPERTY_USE_COLOR_BLIND_CPD_TYPE = Boolean.class;


    public static final String PROPERTY_AUTO_APPLY_SCHEMES_KEY = "color.manipulation.auto.apply.schemes";
    public static final String PROPERTY_AUTO_APPLY_SCHEMES_LABEL = "Automatically Apply Color Schemes";
    public static final String PROPERTY_AUTO_APPLY_SCHEMES_TOOLTIP = "Automatically apply color schemes when opening a band based on its name";
    public static final String PROPERTY_AUTO_APPLY_SCHEMES_ALIAS = "colorManipulationAutoApplySchemes";
    public static final boolean PROPERTY_AUTO_APPLY_SCHEMES_DEFAULT = true;
    public static final Class PROPERTY_AUTO_APPLY_SCHEMES_TYPE = Boolean.class;


    public static final String PROPERTY_USE_SCHEME_PALETTE_STX_KEY = "color.manipulation.use.scheme.palette.stx";
    public static final String PROPERTY_USE_SCHEME_PALETTE_STX_LABEL = "Use Scheme Palette and Band Data Range";
    public static final String PROPERTY_USE_SCHEME_PALETTE_STX_TOOLTIP = "Only apply the color palette of a scheme and set min and max based on band data";
    public static final String PROPERTY_USE_SCHEME_PALETTE_STX_ALIAS = "colorManipulationUseSchemePaletteStx";
    public static final boolean PROPERTY_USE_SCHEME_PALETTE_STX_DEFAULT = false;
    public static final Class PROPERTY_USE_SCHEME_PALETTE_STX_TYPE = Boolean.class;



    public static final String PROPERTY_SCHEME_CPD_KEY = "color.manipulation.scheme.cpd";
    public static final String PROPERTY_SCHEME_CPD_LABEL = "Palette";
    public static final String PROPERTY_SCHEME_CPD_TOOLTIP = "The cpd file to use when for the scheme";
    public static final String PROPERTY_SCHEME_CPD_ALIAS = "colorManipulationSchemeCpd";
    public static final String PROPERTY_SCHEME_CPD_OPTION1 = STANDARD_SCHEME;
    public static final String PROPERTY_SCHEME_CPD_OPTION2 = UNIVERSAL_SCHEME;
    public static final String PROPERTY_SCHEME_CPD_OPTION3 = GRAY_SCALE;
    public static final String PROPERTY_SCHEME_CPD_OPTION4 = STANDARD_COLOR;
    public static final String PROPERTY_SCHEME_CPD_OPTION5 = UNIVERSAL_COLOR;
    public static final String PROPERTY_SCHEME_CPD_OPTION6 = OTHER_COLOR;
    public static final String PROPERTY_SCHEME_CPD_DEFAULT = STANDARD_SCHEME;
    public static final Class PROPERTY_SCHEME_CPD_TYPE = String.class;



    public static final String PROPERTY_SCHEME_RANGE_KEY = "color.manipulation.scheme.range";
    public static final String PROPERTY_SCHEME_RANGE_LABEL = "Range";
    public static final String PROPERTY_SCHEME_RANGE_TOOLTIP = "range options for the scheme";
    public static final String PROPERTY_SCHEME_RANGE_ALIAS = "colorManipulationSchemeRange";
    public static final String PROPERTY_SCHEME_RANGE_OPTION1 = RANGE_FROM_SCHEME;
    public static final String PROPERTY_SCHEME_RANGE_OPTION2 = RANGE_FROM_DATA;
    public static final String PROPERTY_SCHEME_RANGE_OPTION3 = RANGE_FROM_CPD;
    public static final String PROPERTY_SCHEME_RANGE_DEFAULT = RANGE_FROM_SCHEME;
    public static final Class PROPERTY_SCHEME_RANGE_TYPE = String.class;



    public static final String PROPERTY_SCHEME_LOG_KEY = "color.manipulation.scheme.log";
    public static final String PROPERTY_SCHEME_LOG_LABEL = "Log Scaling";
    public static final String PROPERTY_SCHEME_LOG_TOOLTIP = "log options for the scheme case";
    public static final String PROPERTY_SCHEME_LOG_ALIAS = "colorManipulationSchemeLog";
    public static final String PROPERTY_SCHEME_LOG_OPTION1 = LOG_FROM_SCHEME;
    public static final String PROPERTY_SCHEME_LOG_OPTION2 = LOG_FROM_CPD;
    public static final String PROPERTY_SCHEME_LOG_OPTION3 = LOG_TRUE;
    public static final String PROPERTY_SCHEME_LOG_OPTION4 = LOG_FALSE;
    public static final String PROPERTY_SCHEME_LOG_DEFAULT = LOG_FROM_SCHEME;
    public static final Class PROPERTY_SCHEME_LOG_TYPE = String.class;







    public static final String PROPERTY_RESTORE_SECTION_KEY = "color.manipulation.restoreDefaults.section";
    public static final String PROPERTY_RESTORE_SECTION_LABEL = "---";
    public static final String PROPERTY_RESTORE_SECTION_TOOLTIP = "---";
    public static final String PROPERTY_RESTORE_SECTION_ALIAS = "colorManipulationDefaultCpdSection";

    // Property Setting: Restore Defaults
    public static final String PROPERTY_RESTORE_DEFAULTS_NAME = "color.manipulation.restoreDefaults";
    public static final String PROPERTY_RESTORE_TO_DEFAULTS_LABEL = "RESTORE DEFAULTS (Color Preferences)";
    public static final String PROPERTY_RESTORE_TO_DEFAULTS_TOOLTIP = "Restore all color preferences to the default";
    public static final boolean PROPERTY_RESTORE_TO_DEFAULTS_DEFAULT = false;




    public static final String NEW_CPD_SELECTOR_FILENAME = "color_palette_scheme_selector.txt";
    public static final String NEW_CPD_DEFAULTS_FILENAME = "color_palette_scheme_defaults.txt";
    public static final String NEW_CPD_SCHEMES_FILENAME = "color_palette_schemes.txt";
    public static final String COLORBAR_TITLE_OVERRIDE_MACRO = "USE_SCHEME_VALUE";

    public static final String PROPERTY_NAME_PALETTES_COLOR_BLIND_ENABLED = "palettes.colorBlind.enabled";
    public static final boolean DEFAULT_PALETTES_COLOR_BLIND_ENABLED = false;

    public static final double DOUBLE_NULL = -Double.MAX_VALUE;
    public static final String NULL_ENTRY = "null";


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


    private final String STANDARD_SCHEME_COMBO_BOX_FIRST_ENTRY_NAME = "-- none --";

    private ArrayList<ColorPaletteInfo> colorSchemeInfos = new ArrayList<ColorPaletteInfo>();
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
    private File schemeDefaultsFile = null;
    //   private File userSchemesFile = null;
    private String jComboBoxFirstEntryName = null;
    PropertyMap configuration = null;
    boolean useColorBlind = false;


    public ColorSchemes(File colorPaletteDir, Id id, boolean userInterfaceMode, PropertyMap configuration) {
        this.colorPaletteDir = colorPaletteDir;
        this.configuration = configuration;
        this.useColorBlind = getUseColorBlind();


        initColorPaletteSchemeInfos();

        schemeDefaultsFile = new File(this.colorPaletteDir, NEW_CPD_DEFAULTS_FILENAME);


        if (colorPaletteDir != null && colorPaletteDir.exists()) {

//            if (userInterfaceMode) {
//                initComboBox();
//            } else {
//                // this mode is used for setting the default color scheme for an image when first opened
//                // it doesn't need comboBoxes, only the colorPaletteInfos is needed
//                initColorPaletteInfos(colorPaletteDir, colorPaletteInfos, schemesFile, false);
//            }

            schemeDefaultsFile = new File(this.colorPaletteDir, NEW_CPD_DEFAULTS_FILENAME);

            setjComboBoxFirstEntryName(STANDARD_SCHEME_COMBO_BOX_FIRST_ENTRY_NAME);

            initComboBox();
            initSchemeDefaults(colorPaletteInfos, schemeDefaultsFile);

            reset();
        }
    }


    private void initComboBox() {

        // todo Put this entry back
//        jComboBoxFirstEntryColorPaletteInfo = new ColorPaletteInfo(getjComboBoxFirstEntryName(), null, null, null, 0, 0, false, true, true, null, null, null, colorPaletteDir);
//        colorPaletteInfos.add(jComboBoxFirstEntryColorPaletteInfo);

//        initColorPaletteInfos(colorPaletteDir, colorPaletteInfos, schemesFile, true);


        Object[] colorPaletteInfosArray = colorSchemeInfos.toArray();

        final String[] toolTipsArray = new String[colorSchemeInfos.size()];

        int i = 0;
        for (ColorPaletteInfo colorPaletteInfo : colorSchemeInfos) {
            toolTipsArray[i] = colorPaletteInfo.getDescription();
            i++;
        }

        final Boolean[] enabledArray = new Boolean[colorSchemeInfos.size()];

        i = 0;
        for (ColorPaletteInfo colorPaletteInfo : colorSchemeInfos) {
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
        if (schemeDefaultsFile != null) {
            jComboBox.setToolTipText("To modify see file: " + colorPaletteDir + "/" + schemeDefaultsFile.getName());
        }


    }


    private boolean initColorPaletteSchemeInfos() {


//        jComboBoxFirstEntryColorPaletteInfo = new ColorPaletteInfo(getjComboBoxFirstEntryName(), null, null, null, 0, 0, false, true, true, null, null, null, colorPaletteDir);
//        newColorPaletteSchemeInfos.add(jComboBoxFirstEntryColorPaletteInfo);

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

                    boolean overRide = false;

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


                        if (cpdFileNameStandard == null ||
                                cpdFileNameStandard.length() == 0 ||
                                NULL_ENTRY.toLowerCase().equals(cpdFileNameStandard.toLowerCase())) {
                            cpdFileNameStandard = DEFAULT_CPD_FILENAME;
                        }


                        if(minStr.length() > 0 && !NULL_ENTRY.toLowerCase().equals(minStr.toLowerCase())){
                            min = Double.valueOf(minStr);
                        } else {
                            min = DOUBLE_NULL;
                        }

                        if(maxStr.length() > 0 && !NULL_ENTRY.toLowerCase().equals(maxStr.toLowerCase())){
                            max = Double.valueOf(maxStr);
                        } else {
                            max = DOUBLE_NULL;
                        }

                        logScaled = false;
                        if (logScaledStr != null && logScaledStr.length() > 0 && logScaledStr.toLowerCase().equals("true")) {
                            logScaled = true;
                        }


                        if (id != null && id.length() > 0) {
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
                                    for (ColorPaletteInfo storedColorPaletteInfo : colorSchemeInfos) {
                                        if (storedColorPaletteInfo.getName().equals(id)) {
                                            colorPaletteInfoToDelete = storedColorPaletteInfo;
                                            break;
                                        }
                                    }
                                    if (colorPaletteInfoToDelete != null) {
                                        colorSchemeInfos.remove(colorPaletteInfoToDelete);
                                    }
                                }
                                colorSchemeInfos.add(colorPaletteInfo);
                            }
                        }
                    }
                }
            }
        }

        return true;
    }


    private void initSchemeDefaults(ArrayList<ColorPaletteInfo> colorPaletteInfos, File file) {

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
                    boolean overRide = false;
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

                            for (ColorPaletteInfo storedColorPaletteInfo : colorSchemeInfos) {
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
                            colorPaletteInfos.add(colorPaletteInfo);
                        }
                    }


                }
            }
        }
    }


    private boolean testMinMax(double min, double max, boolean isLogScaled) {
        boolean checksOut = true;

        if (min != DOUBLE_NULL && max != DOUBLE_NULL) {
            if (min == max) {
                checksOut = false;
            }
        }


        if (min != DOUBLE_NULL && max != DOUBLE_NULL) {
            if (isLogScaled && min == 0) {
                checksOut = false;
            }
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
