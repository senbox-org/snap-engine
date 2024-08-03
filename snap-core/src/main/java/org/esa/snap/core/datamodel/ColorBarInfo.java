package org.esa.snap.core.datamodel;

import java.text.DecimalFormat;

/**
 * @author Daniel Knowles
 */

public class ColorBarInfo {

    private double value;
    private double locationWeight;
    private String formattedValue;
    private int decimalPlaces;
    private boolean decimalPlacesForce;


    public ColorBarInfo(double value, double locationWeight, int decimalPlaces, boolean decimalPlacesForce) {
        setValue(value);
        setLocationWeight(locationWeight);
        setDecimalPlaces(decimalPlaces);
        setDecimalPlacesForce(decimalPlacesForce);
        setFormattedValue();
    }

    public ColorBarInfo(double value, double locationWeight, String formattedValue) {
        setValue(value);
        setLocationWeight(locationWeight);
        setFormattedValue(formattedValue);
    }

    public double getValue() {
        return value;
    }

    private void setValue(double value) {
        this.value = value;
    }

    public double getLocationWeight() {
        return locationWeight;
    }

    public void setLocationWeight(double locationWeight) {
        this.locationWeight = locationWeight;
    }

    public String getFormattedValue() {
        return formattedValue;
    }


    public void setFormattedValue(String formattedValue) {
        this.formattedValue = formattedValue;
    }


    private void setFormattedValue() {
        StringBuilder decimalFormatStringBuilder = new StringBuilder("0");


        if (getDecimalPlaces() > 0) {

            double actualDecimalPlaces = getDecimalPlaces();
            double minDisplayableValue = 1;
            for (int j = 0; j < getDecimalPlaces(); j++) {
                if (j > 0) {
                    minDisplayableValue = minDisplayableValue / 10.0;
                }
            }

            // handle small numbers increasing number of decimal places if needed
            if (getValue() > 0) {
                while ((getValue()) < minDisplayableValue) {
                    double testValue = getValue();
                    minDisplayableValue = minDisplayableValue / 10.0;
                    actualDecimalPlaces++;
                }
            }

            //set max decimal places
            for (int j = 0; j < actualDecimalPlaces; j++) {
                if (j == 0) {
                    decimalFormatStringBuilder.append(".");
                }
                decimalFormatStringBuilder.append("0");

            }

            String formattedDecimalValue = new DecimalFormat(decimalFormatStringBuilder.toString()).format(getValue()).toString();


            if (!isDecimalPlacesForce()) {
                // trim off trailing zeros
                while (formattedDecimalValue.length() > 0 && formattedDecimalValue.endsWith("0")) {
                    formattedDecimalValue = formattedDecimalValue.substring(0, formattedDecimalValue.length() - 1);
                }

                // trim of period in the case of an integer
                if (formattedDecimalValue.length() > 0 && formattedDecimalValue.endsWith(".")) {
                    formattedDecimalValue = formattedDecimalValue.substring(0, formattedDecimalValue.length() - 1);
                }
            }


            this.formattedValue = formattedDecimalValue;
        } else {
            String formattedDecimalValue = new DecimalFormat(decimalFormatStringBuilder.toString()).format(getValue()).toString();
            this.formattedValue = formattedDecimalValue;
        }
    }


    public int getDecimalPlaces() {
        return decimalPlaces;
    }

    private void setDecimalPlaces(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public boolean isDecimalPlacesForce() {
        return decimalPlacesForce;
    }

    public void setDecimalPlacesForce(boolean decimalPlacesForce) {
        this.decimalPlacesForce = decimalPlacesForce;
    }
}