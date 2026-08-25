package org.esa.snap.core.datamodel;

import org.esa.snap.core.layer.ColorBarLayerType;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static org.esa.snap.core.datamodel.ColorManipulationDefaults.*;

/**
 * Manages all the color schemes
 *
 * @author Daniel Knowles (NASA)
 * @author Bing Yang (NASA)
 * @date Jan 2020
 */

public class ColorBarSchemeManager {
//
//    private ArrayList<ColorSchemeInfo> colorSchemeStoredInfos = new ArrayList<ColorSchemeInfo>();
//    private ArrayList<ColorSchemeInfo> colorSchemeOrganizedInfos = new ArrayList<ColorSchemeInfo>();
//
//    private ArrayList<ColorSchemeLookupInfo> colorSchemeLookupInfos = new ArrayList<ColorSchemeLookupInfo>();
//
//    private File colorSchemesFile = null;
//    private File colorSchemesUserFile = null;
//    private File colorSchemeLookupFile = null;
//    private File colorSchemeLookupUserFile = null;
//
//    private ColorSchemeInfo noneColorSchemeInfo = null;
//    private ColorSchemeInfo dividerPrimarySchemes = null;
//    private ColorSchemeInfo dividerAdditionalSchemes = null;
//
//
//    private File colorPaletteAuxDir = null;
//    private File colorSchemesAuxDir = null;
//
//    private boolean initialized = false;
//
//    private ColorSchemeInfo currentSelection = null;
//
//    static ColorBarSchemeManager manager = new ColorBarSchemeManager();
//
//    public static ColorBarSchemeManager getDefault() {
//        return manager;
//    }
//
//
//    public ColorBarSchemeManager() {
//        init();
//    }
//
//
//    public void init() {
//
//        if (!initialized) {
//
//            Path getColorSchemesAuxDir = SystemUtils.getAuxDataPath().resolve(ColorManipulationDefaults.DIR_NAME_COLOR_SCHEMES);
//            if (getColorSchemesAuxDir != null) {
//                this.colorSchemesAuxDir = getColorSchemesAuxDir.toFile();
//                if (!colorSchemesAuxDir.exists()) {
//                    return;
//                }
//            } else {
//                return;
//            }
//
//            Path getColorPalettesAuxDir = SystemUtils.getAuxDataPath().resolve(ColorManipulationDefaults.DIR_NAME_COLOR_PALETTES);
//            if (getColorPalettesAuxDir != null) {
//                this.colorPaletteAuxDir = getColorPalettesAuxDir.toFile();
//                if (!colorPaletteAuxDir.exists()) {
//                    return;
//                }
//            } else {
//                return;
//            }
//
//            colorSchemesFile = new File(this.colorSchemesAuxDir, ColorManipulationDefaults.COLOR_SCHEMES_FILENAME);
//            colorSchemesUserFile = new File(this.colorSchemesAuxDir, ColorManipulationDefaults.COLOR_SCHEMES_USER_FILENAME);
//            colorSchemeLookupFile = new File(this.colorSchemesAuxDir, ColorManipulationDefaults.COLOR_SCHEME_LOOKUP_FILENAME);
//            colorSchemeLookupUserFile = new File(this.colorSchemesAuxDir, ColorManipulationDefaults.COLOR_SCHEME_LOOKUP_USER_FILENAME);
//
//            if (colorSchemesFile.exists() && colorSchemeLookupFile.exists()) {
//                createPrimaryAndAdditionalColorSchemeInfos();
//
//                dividerPrimarySchemes = new ColorSchemeInfo("-- Primary Schemes --", true, true, "-- Primary Schemes --", null, null, 0, 0, false, false, null, null, null, colorPaletteAuxDir);
//                dividerAdditionalSchemes = new ColorSchemeInfo("-- Additional Schemes --", true, true, "-- Additional Schemes --", null, null, 0, 0, false, false, null, null, null, colorPaletteAuxDir);
//
//                initColorSchemeLookup();
//
//            }
//
//            initialized = true;
//        }
//    }
//
//
//    private Document getFileDocument(File file) {
//        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
//        Document dom = null;
//
//        try {
//            DocumentBuilder db = dbf.newDocumentBuilder();
//            dom = db.parse(new FileInputStream(file));
//        } catch (ParserConfigurationException pce) {
//            pce.printStackTrace();
//        } catch (SAXException se) {
//            se.printStackTrace();
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//        }
//
//        return dom;
//    }
//
//
//    private boolean createPrimaryAndAdditionalColorSchemeInfos() {
//
//        // todo When this fails maybe warn user
//        if (colorSchemesUserFile.exists()) {
//            Document domCustom = getFileDocument(colorSchemesUserFile);
//            if (domCustom == null) { return false; }
//            Element rootElementCustom = domCustom.getDocumentElement();
//            if (rootElementCustom == null) { return false; }
//
//            NodeList schemeNodeListCustom = rootElementCustom.getElementsByTagName("Scheme");
//
//            if (schemeNodeListCustom != null && schemeNodeListCustom.getLength() > 0) {
//                for (int i = 0; i < schemeNodeListCustom.getLength(); i++) {
//                    Element schemeElement = (Element) schemeNodeListCustom.item(i);
//                    addPrimaryAndAdditionalColorSchemeInfos(schemeElement);
//                }
//            }
//        }
//
//        Document dom = getFileDocument(colorSchemesFile);
//        if (dom == null) { return false; }
//
//        Element rootElement = dom.getDocumentElement();
//        if (rootElement == null) { return false; }
//
//        NodeList schemeNodeList = rootElement.getElementsByTagName("Scheme");
//
//        if (schemeNodeList != null && schemeNodeList.getLength() > 0) {
//            for (int i = 0; i < schemeNodeList.getLength(); i++) {
//                Element schemeElement = (Element) schemeNodeList.item(i);
//                addPrimaryAndAdditionalColorSchemeInfos(schemeElement);
//            }
//        }
//
//        return true;
//    }
//
//
//    private void addPrimaryAndAdditionalColorSchemeInfos(Element schemeElement) {
//
//        if (schemeElement != null) {
//            String id = schemeElement.getAttribute("name");
//            String description = getTextValue(schemeElement, "DESCRIPTION");
//            String displayName = getTextValue(schemeElement, "VERBOSE_NAME");
//            String colorBarLabels = getTextValue(schemeElement, "COLORBAR_LABELS");
//            String colorBarTitle = getTextValue(schemeElement, "COLORBAR_TITLE");
//            String colorBarTitleAlt = getTextValue(schemeElement, "COLORBAR_TITLE_ALT");
//            String colorBarUnits = getTextValue(schemeElement, "COLORBAR_UNITS");
//            String colorBarUnitsAlt = getTextValue(schemeElement, "COLORBAR_UNITS_ALT");
//            String colorBarLengthStr = getTextValue(schemeElement, "COLORBAR_LENGTH");
//            String colorBarLabelScalingStr = getTextValue(schemeElement, "COLORBAR_LABEL_SCALING");
//            String minStr = getTextValue(schemeElement, "MIN");
//            String maxStr = getTextValue(schemeElement, "MAX");
//            String logScaledStr = getTextValue(schemeElement, "LOG_SCALE");
//            String standardCpdFilename = getTextValue(schemeElement, "STANDARD_FILENAME");
//            String universalCpdFilename = getTextValue(schemeElement, "UNIVERSAL_FILENAME");
//            String dividerString = getTextValue(schemeElement, "DIVIDER");
//            String primarySchemeString = getTextValue(schemeElement, "PRIMARY");
//
//            boolean validEntry = true;
//            boolean fieldsInitialized = false;
//
//            Double min;
//            Double max;
//            boolean logScaled;
//            File standardCpdFile;
//            File colorBlindCpdFile;
//            boolean enabled;
//            boolean divider;
//            boolean primaryScheme;
//
//
//            if (minStr != null && minStr.length() > 0) {
//                min = Double.valueOf(minStr);
//            } else {
//                min = ColorManipulationDefaults.DOUBLE_NULL;
//            }
//
//            if (maxStr != null && maxStr.length() > 0) {
//                max = Double.valueOf(maxStr);
//            } else {
//                max = ColorManipulationDefaults.DOUBLE_NULL;
//            }
//
//            logScaled = false;
//            if (logScaledStr != null && logScaledStr.length() > 0 && logScaledStr.toLowerCase().equals("true")) {
//                logScaled = true;
//            }
//
//            divider = false;
//            if (dividerString != null && dividerString.length() > 0 && dividerString.toLowerCase().equals("true")) {
//                divider = true;
//            }
//
//            primaryScheme = false;
//            if (primarySchemeString != null && primarySchemeString.length() > 0 && primarySchemeString.toLowerCase().equals("true")) {
//                primaryScheme = true;
//            }
//
//            if (id != null && id.length() > 0) {
//                fieldsInitialized = true;
//            }
//
//            if (fieldsInitialized) {
//
//                if (!testMinMax(min, max, logScaled)) {
//                    validEntry = false;
//                }
//
//                if (validEntry) {
//                    if (standardCpdFilename != null) {
//                        standardCpdFile = new File(colorPaletteAuxDir, standardCpdFilename);
//                        if (standardCpdFile == null || !standardCpdFile.exists()) {
//                            validEntry = false;
//                        }
//                    } else {
//                        validEntry = false;
//                    }
//                }
//
//                if (validEntry) {
//                    if (universalCpdFilename != null) {
//                        colorBlindCpdFile = new File(colorPaletteAuxDir, universalCpdFilename);
//                        if (colorBlindCpdFile == null || !colorBlindCpdFile.exists()) {
//                            validEntry = false;
//                        }
//                    } else {
//                        validEntry = false;
//                    }
//                }
//
//
//                ColorSchemeInfo colorSchemeInfo = null;
//
//                enabled = validEntry;
//
//                colorSchemeInfo = new ColorSchemeInfo(id, primaryScheme, divider, displayName, description, standardCpdFilename, min, max, logScaled, enabled, universalCpdFilename, colorBarTitle, colorBarTitleAlt, colorBarLabels,
//                        colorBarUnits, colorBarUnitsAlt,  colorBarLabelScalingStr, colorBarLengthStr, colorPaletteAuxDir);
//
//                if (!colorSchemeInfo.isEnabled()) {
//                    description = checkScheme(colorSchemeInfo);
//                    colorSchemeInfo.setDescription(description);
//                }
//
//
//                if (colorSchemeInfo != null) {
//                    // determine if this is a duplicate entry
//                    if (!colorSchemeInfo.isDuplicateEntry()) {
//                        for (ColorSchemeInfo storedColorSchemeInfo : colorSchemeStoredInfos) {
//                            if (id.equals(storedColorSchemeInfo.getName())) {
//                                colorSchemeInfo.setDuplicateEntry(true);
//                                break;
//                            }
//                        }
//                    }
//
//                    if (colorSchemeInfo.isDuplicateEntry()) {
//                        colorSchemeInfo.setDescription("WARNING!: duplicate scheme entry");
//                    }
//
//                    colorSchemeStoredInfos.add(colorSchemeInfo);
//                }
//            }
//        }
//
//
//    }
//
//
//    private void initColorSchemeLookup() {
//
//        if (colorSchemeLookupUserFile.exists()) {
//            Document domCustom = getFileDocument(colorSchemeLookupUserFile);
//            Element rootElementCustom = domCustom.getDocumentElement();
//            NodeList keyNodeListCustom = rootElementCustom.getElementsByTagName("KEY");
//
//            if (keyNodeListCustom != null && keyNodeListCustom.getLength() > 0) {
//                for (int i = 0; i < keyNodeListCustom.getLength(); i++) {
//                    Element schemeElement = (Element) keyNodeListCustom.item(i);
//                    addColorSchemeLookup(schemeElement);
//                }
//            }
//        }
//
//        Document dom = getFileDocument(colorSchemeLookupFile);
//        Element rootElement = dom.getDocumentElement();
//        NodeList keyNodeList = rootElement.getElementsByTagName("KEY");
//
//        if (keyNodeList != null && keyNodeList.getLength() > 0) {
//
//            for (int i = 0; i < keyNodeList.getLength(); i++) {
//                Element schemeElement = (Element) keyNodeList.item(i);
//                addColorSchemeLookup(schemeElement);
//            }
//        }
//
//    }
//
//
//    private void addColorSchemeLookup(Element schemeElement) {
//
//        boolean checksOut = true;
//        if (schemeElement != null) {
//            String regex = schemeElement.getAttribute("REGEX");
//            String schemeId = getTextValue(schemeElement, "SCHEME_ID");
//            String description = getTextValue(schemeElement, "DESCRIPTION");
//
//
//            if (regex == null || regex.length() == 0) {
//                checksOut = false;
//            }
//
//            if (schemeId == null || schemeId.length() == 0) {
//                schemeId = regex;
//            }
//
//            if (schemeId == null || schemeId.length() == 0) {
//                checksOut = false;
//            }
//
//            ColorSchemeInfo colorSchemeInfo = getColorSchemeInfoBySchemeId(schemeId);
//            if (colorSchemeInfo == null) {
//                checksOut = false;
//            }
//
//            if (checksOut) {
//                ColorSchemeLookupInfo colorSchemeLookupInfo = new ColorSchemeLookupInfo(regex, schemeId, description, colorSchemeInfo);
//
//                if (colorSchemeLookupInfo != null) {
//                    colorSchemeLookupInfos.add(colorSchemeLookupInfo);
//                }
//            }
//        }
//
//    }
//
//
//    public ColorSchemeInfo getColorSchemeInfoBySchemeId(String schemeId) {
//        if (schemeId != null && schemeId.length() > 0) {
//            for (ColorSchemeInfo colorSchemeInfo : colorSchemeStoredInfos) {
//                if (colorSchemeInfo != null && colorSchemeInfo.getName() != null) {
//                    if (schemeId.toLowerCase().equals(colorSchemeInfo.getName().toLowerCase())) {
//                        return colorSchemeInfo;
//                    }
//                }
//            }
//        }
//
//        return null;
//    }
//
//
//    private boolean testMinMax(double min, double max, boolean isLogScaled) {
//        boolean checksOut = true;
//
//        if (min != ColorManipulationDefaults.DOUBLE_NULL && max != ColorManipulationDefaults.DOUBLE_NULL) {
//            if (min >= max) {
//                checksOut = false;
//            }
//        }
//
//
//        if (min != ColorManipulationDefaults.DOUBLE_NULL && max != ColorManipulationDefaults.DOUBLE_NULL) {
//            if (isLogScaled && min == 0) {
//                checksOut = false;
//            }
//        }
//
//        return checksOut;
//    }
//
//    public static String getTextValue(Element ele, String tagName) {
//        String textVal = null;
//        NodeList nl = ele.getElementsByTagName(tagName);
//        if (nl != null && nl.getLength() > 0) {
//            Element el = (Element) nl.item(0);
//
//            if (el.hasChildNodes()) {
//                textVal = el.getFirstChild().getNodeValue();
//            }
//
//        }
//
//        return textVal;
//    }
//
//    public ArrayList<ColorSchemeLookupInfo> getColorSchemeLookupInfos() {
//        return colorSchemeLookupInfos;
//    }
//
//
//    public ColorSchemeInfo getCurrentSelection() {
//        return currentSelection;
//    }
//
//    public String checkScheme(ColorSchemeInfo colorSchemeInfo) {
//        String message = "";
//
//
//        if (colorSchemeInfo != null) {
//            String standardFileMessage = "";
//            String universalFileMessage = "";
//
//            String message_head = "WARNING!: Configuration issue for scheme = '" + colorSchemeInfo.getName() + "':<br>";
//
//            String standardFilename = colorSchemeInfo.getCpdFilename(false);
//
//            if (standardFilename != null && standardFilename.length() > 0) {
//                File standardFile = new File(colorPaletteAuxDir, standardFilename);
//
//                if (standardFile == null || !standardFile.exists()) {
//                    standardFileMessage = "Scheme standard file '" + standardFilename + "' does not exist<br>";
//                }
//            } else {
//                standardFileMessage = "Scheme does not contain a standard file<br>";
//            }
//
//            String universalFilename = colorSchemeInfo.getCpdFilename(true);
//
//            if (universalFilename != null && universalFilename.length() > 0) {
//                File universalFile = new File(colorPaletteAuxDir, universalFilename);
//
//                if (universalFile == null || !universalFile.exists()) {
//                    universalFileMessage = "Scheme universal file '" + universalFilename + "' does not exist<br>";
//                }
//            } else {
//                universalFileMessage = "Scheme does not contain a universal file<br>";
//            }
//
//            String minMaxIssue = "";
//            if (!testMinMax(colorSchemeInfo.getMinValue(), colorSchemeInfo.getMaxValue(), colorSchemeInfo.isLogScaled())) {
//                minMaxIssue = "Issue with min, max values: min= '"
//                        + colorSchemeInfo.getMinValue()
//                        + "', max= '"
//                        + colorSchemeInfo.getMaxValue()
//                        + "' <br>";
//            }
//
//            message = "<html>" + message_head + standardFileMessage + universalFileMessage + minMaxIssue + "</html>";
//
//        } else {
//            message = "Configuration Error";
//        }
//
//
//        return message;
//    }

}
