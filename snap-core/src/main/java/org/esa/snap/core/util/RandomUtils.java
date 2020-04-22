package org.esa.snap.core.util;

/**
 * Random Utilities to manage seeds and other.
 *
 * @author Martino Ferrari (CS-MAP)
 * @since SNAP 8
 */
public class RandomUtils {
    public static final String SEED_ENV_VAR = "snap.random.seed";
    /**
     * Returns a seed from the current datetime or 
     * the user defined one.
     */
    public static long seed() {
        String value = System.getenv(SEED_ENV_VAR);
        if (value != null) {
            try {
                long l =  Long.parseLong(value);
                return l;
            } catch (NumberFormatException nfe) {
                System.out.println("Seed env variable is not long: " + nfe.getMessage());
            }
        }
        return System.currentTimeMillis();
    }
}
