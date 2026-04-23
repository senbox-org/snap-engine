package org.esa.snap.core.datamodel;

import org.esa.snap.core.datamodel.ColorSchemeInfo;
import org.esa.snap.core.util.ProductUtils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Contains all info for a color scheme lookup
 *
 * @author Daniel Knowles (NASA)
 * @date Jan 2020
 */

public class ColorSchemeLookupInfo {

    private String regex;
    private String[] regexArray;
    private String scheme_id;
    private ColorSchemeInfo colorSchemeInfo;
    private String description;
    private String mission;
    private String productType;
    private String filename;


    public ColorSchemeLookupInfo(String regex, String scheme_id, String description, String mission, String productType, String filename, ColorSchemeInfo colorSchemeInfo) {

        if (regex != null && scheme_id != null) {
            setRegex(regex);
            setScheme_id(scheme_id);
            setDescription(description);
            setMission(mission);
            setProductType(productType);
            setFilename(filename);
            setColorSchemeInfo(colorSchemeInfo);
            setRegexArray(regex);
        }
    }


    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String[] getRegexArray() {
        return regexArray;
    }

    public void setRegexArray(String regex) {

        if (regex != null && regex.length() > 0) {
            String wildcard = ",";
            String regexArray[] = regex.split(wildcard);

            for (int i=0; i < regexArray.length-1; i++) {
                regexArray[i] = regexArray[i].trim();
            }

            this.regexArray = regexArray;
        } else {
            this.regexArray =  null;
        }
    }

    public String getScheme_id() {
        return scheme_id;
    }

    public void setScheme_id(String scheme_id) {
        this.scheme_id = scheme_id;
    }

    public ColorSchemeInfo getColorSchemeInfo() {
        return colorSchemeInfo;
    }

    public void setColorSchemeInfo(ColorSchemeInfo colorSchemeInfo) {
        this.colorSchemeInfo = colorSchemeInfo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getMission() {
        return mission;
    }

    public void setMission(String mission) {
        this.mission = mission;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }


    public boolean isMatch(String bandName, String mission, String regex) {

        if (bandName == null || bandName.length() == 0) { return false; }
        if (regex == null || regex.length() == 0) { return false; }
        bandName = bandName.trim();
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(bandName);
        boolean match = matcher.find();

//        if (!match) {
//            bandName = "^" + bandName;
//            matcher = pattern.matcher(bandName);
//            match = matcher.find();
//        }

        if (!match) {
            final String WILDCARD = new String("*");

            regex = regex.trim();

            if (bandName.equals(regex)) {
                match = true;
            } else if (regex.contains(WILDCARD)) {
                if (!regex.startsWith(WILDCARD) && regex.endsWith(WILDCARD)) {
                    String basename = regex.substring(0, regex.length() - 1);
                    if (bandName.startsWith(basename)) {
                        match = true;
                    }
                } else if (regex.startsWith(WILDCARD) && !regex.endsWith(WILDCARD)) {
                    String basename = regex.substring(1, regex.length());
                    if (bandName.endsWith(basename)) {
                        match = true;
                    }
                } else if (regex.startsWith(WILDCARD) && regex.endsWith(WILDCARD)) {
                    String basename = regex.substring(1, regex.length() - 1);
                    if (bandName.contains(basename)) {
                        match = true;
                    }
                } else {
                    String basename = regex;
                    String wildcard = "\\*";
                    String basenameSplit[] = basename.split(wildcard);
                    if (basenameSplit.length == 2 && basenameSplit[0].length() > 0 && basenameSplit[1].length() > 0) {
                        if (bandName.startsWith(basenameSplit[0]) && bandName.endsWith(basenameSplit[1])) {
                            match = true;
                        }
                    }
                }
            }
            if (match) {
                if (!bandName.startsWith(regex.substring(0, 1))) {
                    match = false;
                }
            }
        }

        return match;
    }





    public boolean isMatch(String bandName, Product product) {
        boolean match = false;

        String mission = ProductUtils.getMetaData(product, ProductUtils.METADATA_POSSIBLE_SENSOR_KEYS);
        if (mission == null || mission.length() == 0) {
            mission = product.getProductType();
        }

        if (getMission() != null) {
            if (mission == null || !mission.contains(getMission()) ) {
                return false;
            }
        }

        if (getProductType() != null) {
            if (product.getProductType() == null || !product.getProductType().contains(getProductType()) ) {
                return false;
            }
        }

        if (getFilename() != null) {
            if (product.getName() == null || !product.getName().contains(getFilename()) ) {
                return false;
            }
        }


//        if (mission != null && getMission() != null) {
//            if (!mission.contains(getMission())) {
//                return false;
//            }
//        }

        if (bandName == null || bandName.length() == 0) { return false; }

        for (String regex: getRegexArray()) {
            match = isMatch(bandName, mission, regex);
            if (match) {
                break;
            }
        }

        return match;
    }


}


