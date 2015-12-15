package org.esa.s3tbx.idepix.core.util;

/**
 * Class representing the angular position of the Sun.
 * // todo: this is taken from beam-chris-reader. move to a more general place!
 *
 * @author olafd
 */
public class SunPosition {
    private double lat;
    private double lon;

    public SunPosition(final double lat, final double lon) {
        if (this.lat < -90.0) {
            throw new IllegalArgumentException("lat < -90.0");
        }
        if (this.lat > 90.0) {
            throw new IllegalArgumentException("lat > 90.0");
        }
        if (this.lon < -180.0) {
            throw new IllegalArgumentException("lon < -180.0");
        }
        if (this.lon > 180.0) {
            throw new IllegalArgumentException("lon > 180.0");
        }

        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
