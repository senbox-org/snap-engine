package org.esa.s3tbx.idepix.core.pixel;

/**
 * Interface for pixel properties.
 * To be used for instrument-specific implementations.
 *
 * @author Olaf Danne
 *
 */
interface PixelProperties {

    /**
     * returns a boolean indicating if a pixel is 'brightwhite'
     *
     * @return isBrightWhite
     */
    boolean isBrightWhite();

    /**
     * returns a boolean indicating if a pixel is cloudy
     *
     * @return isCloud
     */
    boolean isCloud();

    /**
     * returns a boolean indicating if a pixel is a clear land pixel
     *
     * @return isClearLand
     */
    boolean isClearLand();

    /**
     * returns a boolean indicating if a pixel is a clear water pixel
     *
     * @return isClearWater
     */
    boolean isClearWater();

    /**
     * returns a boolean indicating if a pixel a clear snow pixel
     *
     * @return isClearSnow
     */
    boolean isClearSnow();

    /**
     * returns a boolean indicating if a pixel is a sea ice pixel
     *
     * @return isSeaIce
     */
    boolean isSeaIce();

    /**
     * returns a boolean indicating if a pixel is over land
     *
     * @return isLand
     */
    boolean isLand();

    /**
     * returns a boolean indicating if a pixel is over water
     *
     * @return isWater
     */
    boolean isWater();

    /**
     * returns a boolean indicating if a pixel is over water (taken from level 1 information)
     *
     * @return isL1Water
     */
    boolean isL1Water();

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

    /**
     * returns a boolean indicating if a pixel likely contains vegetation
     *
     * @return isVegRisk
     */
    boolean isVegRisk();

    /**
     * returns a boolean indicating if a pixel has sun glint risk
     *
     * @return isGlintRisk
     */
    boolean isGlintRisk();

    /**
     * returns a boolean indicating if a pixel reflectance surface is at high altitude
     *
     * @return isHigh
     */
    boolean isHigh();

    /**
     * returns a boolean indicating if a pixel is invalid
     *
     * @return isInvalid
     */
    boolean isInvalid();

}
