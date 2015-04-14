package org.esa.s3tbx.dataio.s3.util;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Tonio Fincke
 */
public class ColorProvider {

    private int colourCounter;
    private int numberOfIntermediateSamplePoints;
    private int thirdPowerOfCurrentDivisions;
    private int numberOfTotalSamplePoints;
    private Map<String, Color> predefinedColors;

    public ColorProvider() {
        colourCounter = 0;
        numberOfIntermediateSamplePoints = 0;
        numberOfTotalSamplePoints = 2;
        evaluateMaxNumberOfSamplePointsForDivision();
        setupReservedColors();
    }

    private void setupReservedColors() {
        predefinedColors = new HashMap<>();
        predefinedColors.put("coastline", new Color(255, 0, 0));
        predefinedColors.put("water", new Color(0, 63, 255));
        predefinedColors.put("land", new Color(0, 127, 63));
        predefinedColors.put("lake", new Color(0, 127, 255));
        predefinedColors.put("ocean", new Color(0, 0, 191));
        predefinedColors.put("snow", new Color(255, 255, 255));
        predefinedColors.put("ice", new Color(191, 255, 255));
        predefinedColors.put("cloud", new Color(191, 191, 191));
    }

    private void evaluateMaxNumberOfSamplePointsForDivision() {
        thirdPowerOfCurrentDivisions = (int) Math.pow(numberOfTotalSamplePoints, 3);
    }

    private Color getPredefinedColor(String maskName) {
        final String maskNameToLowerCase = maskName.toLowerCase();
        final Set<Map.Entry<String, Color>> entries = predefinedColors.entrySet();
        for (Map.Entry<String, Color> entry : entries) {
            if (maskNameToLowerCase.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public Color getMaskColor(String maskName) {
        Color color = getPredefinedColor(maskName);
        while (color == null) {
            int redStep = 0;
            int greenStep = 0;
            int blueStep = 0;
            double stepSize = 255.0;
            while (redStep % 2 == 0 && greenStep % 2 == 0 && blueStep % 2 == 0) {
                updateDivision();
                stepSize = 255.0 / (numberOfIntermediateSamplePoints + 1);
                redStep = (colourCounter / (numberOfTotalSamplePoints * numberOfTotalSamplePoints))
                        % numberOfTotalSamplePoints;
                greenStep = (colourCounter / numberOfTotalSamplePoints) % numberOfTotalSamplePoints;
                blueStep = colourCounter % numberOfTotalSamplePoints;
                colourCounter++;
            }
            final Color candidateColor =
                    new Color((int) (redStep * stepSize), (int) (greenStep * stepSize), (int) (blueStep * stepSize));
            if (!isPredefinedColor(candidateColor)) {
                color = candidateColor;
            }
        }
        return color;
    }

    private boolean isPredefinedColor(Color color) {
        for (Color predefinedColor : predefinedColors.values()) {
            if (predefinedColor.equals(color)) {
                return true;
            }
        }
        return false;
    }

    private void updateDivision() {
        if (colourCounter > thirdPowerOfCurrentDivisions) {
            numberOfIntermediateSamplePoints = (numberOfIntermediateSamplePoints * 2) + 1;
            numberOfTotalSamplePoints = numberOfIntermediateSamplePoints + 2;
            evaluateMaxNumberOfSamplePointsForDivision();
            colourCounter = 0;
        }
    }

}
