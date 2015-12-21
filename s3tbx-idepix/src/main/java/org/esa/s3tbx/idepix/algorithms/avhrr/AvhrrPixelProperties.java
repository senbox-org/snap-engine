package org.esa.s3tbx.idepix.algorithms.avhrr;

/**
 * Interface for AVHRR AC pixel properties.
 *
 * @author olafd
 */
public interface AvhrrPixelProperties {
    /**
     * returns a boolean indicating if a pixel is regarded as invalid
     *
     * @return isCloud
     */
    boolean isInvalid();

    /**
     * returns a boolean indicating if a pixel is cloudy (surely or ambiguous)
     *
     * @return isCloud
     */
    boolean isCloud();

    /**
     * returns a boolean indicating if a pixel is cloudy but ambiguous
     *
     * @return isCloudAmbiguous
     */
    boolean isCloudAmbiguous();

    /**
     * returns a boolean indicating if a pixel is surely cloudy
     *
     * @return isCloudSure
     */
    boolean isCloudSure();

    /**
     * returns a boolean indicating if a pixel is a cloud buffer pixel
     *
     * @return isCloudBuffer
     */
    boolean isCloudBuffer();

    /**
     * returns a boolean indicating if a pixel is a cloud shadow pixel
     *
     * @return isCloudShadow
     */
    boolean isCloudShadow();

    /**
     * returns a boolean indicating if a pixel is snow or ice
     *
     * @return isSnowIce
     */
    boolean isSnowIce();

    /**
     * returns a boolean indicating if a pixel has risk for glint
     *
     * @return isGlintRisk
     */
    boolean isGlintRisk();

    /**
     * returns a boolean indicating if a pixel is a coastline pixel
     *
     * @return isCoastline
     */
    boolean isCoastline();

    /**
     * returns a boolean indicating if a pixel is over land
     *
     * @return isLand
     */
    boolean isLand();

}
