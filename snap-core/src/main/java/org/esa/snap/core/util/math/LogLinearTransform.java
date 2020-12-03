package org.esa.snap.core.util.math;

/**
 * Math utils for transforming between log and linear.
 *
 * @author Daniel Knowles (NASA)
 * @version $Revision$ $Date$
 */


public class LogLinearTransform {


    private static final double FORCED_CHANGE_FACTOR = 0.0001;

    public static double getLinearValueUsingLinearWeight(double linearWeight, double min, double max) {

        // Prevent extrapolation which could occur due to machine roundoffs in the calculations
        if (linearWeight == 0) {
            return min;
        }
        if (linearWeight == 1) {
            return max;
        }

        double deltaNormalized = (max - min);
        double linearValue = min + linearWeight * (deltaNormalized);

        // Prevent UNEXPECTED interpolation/extrapolation which could occur due to machine roundoffs in the calculations
        if (linearWeight > 0 && linearValue < min) {
            return min;
        }
        if (linearWeight < 1 && linearValue > max) {
            return max;
        }
        if (linearWeight < 0 && linearValue >= min) {
            return min - (max - min) * FORCED_CHANGE_FACTOR;
        }
        if (linearWeight > 1 && linearValue <= max) {
            return max + (max - min) * FORCED_CHANGE_FACTOR;
        }

        return linearValue;
    }

    public static double getLogarithmicValueUsingLinearWeight(double weight, double min, double max) {

        double linearValue = getLinearValueUsingLinearWeight(weight, min, max);

        // Prevent extrapolation which could occur due to machine roundoffs in the calculations
        if (linearValue == min) {
            return min;
        }
        if (linearValue == max) {
            return max;
        }

        double b = Math.log(max / min) / (max - min);
        double a = min / (Math.exp(b * min));
        double logValue = a * Math.exp(b * linearValue);

        // Prevent UNEXPECTED interpolation/extrapolation which could occur due to machine roundoffs in the calculations
        if (linearValue > min && logValue < min) {
            return min;
        }
        if (linearValue < max && logValue > max) {
            return max;
        }
        if (linearValue < min && logValue >= min) {
            return min - (max - min) * FORCED_CHANGE_FACTOR;
        }
        if (linearValue > max && logValue <= max) {
            return max + (max - min) * FORCED_CHANGE_FACTOR;
        }

        return logValue;
    }

    public static double getLogarithmicValue(double linearValue, double min, double max) {

        // Prevent extrapolation which could occur due to machine roundoffs in the calculations
        if (linearValue == min) {
            return min;
        }
        if (linearValue == max) {
            return max;
        }

        double b = Math.log(max / min) / (max - min);
        double a = min / (Math.exp(b * min));
        double logValue = a * Math.exp(b * linearValue);

        // Prevent UNEXPECTED interpolation/extrapolation which could occur due to machine roundoffs in the calculations
        if (linearValue > min && logValue < min) {
            return min;
        }
        if (linearValue < max && logValue > max) {
            return max;
        }
        if (linearValue < min && logValue >= min) {
            return min - (max - min) * FORCED_CHANGE_FACTOR;
        }
        if (linearValue > max && logValue <= max) {
            return max + (max - min) * FORCED_CHANGE_FACTOR;
        }

        return logValue;
    }

    public static double getLinearValue(double linearWeight, double min, double max) {

        // Prevent extrapolation which could occur due to machine roundoffs in the calculations
        if (linearWeight == 0) {
            return min;
        }
        if (linearWeight == 1) {
            return max;
        }

        double deltaNormalized = (max - min);
        double linearValue = min + linearWeight * (deltaNormalized);

        // Prevent UNEXPECTED interpolation/extrapolation which could occur due to machine roundoffs in the calculations
        if (linearWeight > 0 && linearValue < min) {
            return min;
        }
        if (linearWeight < 1 && linearValue > max) {
            return max;
        }
        if (linearWeight < 0 && linearValue >= min) {
            return min - (max - min) * FORCED_CHANGE_FACTOR;
        }
        if (linearWeight > 1 && linearValue <= max) {
            return max + (max - min) * FORCED_CHANGE_FACTOR;
        }

        return linearValue;
    }


    public static double getLinearWeightFromLogValue(double logValue, double min, double max) {

        // Prevent extrapolation which could occur due to machine roundoffs in the calculations
        if (logValue == min) {
            return 0;
        }
        if (logValue == max) {
            return 1;
        }

        double b = Math.log(max / min) / (max - min);
        double a = min / (Math.exp(b * min));

//        double linearWeight = Math.log(logValue / a) / b;
//        linearWeight = (linearWeight - min) / (max - min);
        double linearWeight = ((Math.log(logValue / a) / b) - min) / (max - min);

        // Prevent UNEXPECTED interpolation/extrapolation which could occur due to machine roundoffs in the calculations
        if (logValue > min && linearWeight < 0) {
            return 0;
        }
        if (logValue < max && linearWeight > 1) {
            return 1;
        }
        if (logValue < min && linearWeight >= 0) {
            return 0 - FORCED_CHANGE_FACTOR;
        }
        if (logValue > max && linearWeight <= 1) {
            return 1 + FORCED_CHANGE_FACTOR;
        }

        return linearWeight;
    }
}
