package org.esa.s3tbx.idepix.algorithms.landsat8;

/**
 * Interface for Landsat 8 pixel properties.
 *
 * @author olafd
 */
public interface Landsat8PixelProperties {
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

    /**
     * returns a boolean indicating if a pixel is 'bright'
     *
     * @return isBright
     */
    boolean isBright();

    /**
     * returns a boolean indicating if a pixel is 'white'
     *
     * @return isWhite
     */
    boolean isWhite();


}
