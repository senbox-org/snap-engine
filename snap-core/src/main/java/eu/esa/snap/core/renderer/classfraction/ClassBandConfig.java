package eu.esa.snap.core.renderer.classfraction;

import org.apache.commons.lang3.StringUtils;


/**
 * Display metadata and persistent source identity for one class band.
 */
public final class ClassBandConfig {


    private final String productName;
    private final String bandName;
    private final String displayName;
    private final int argb;
    private final boolean visible;


    public ClassBandConfig(String productName, String bandName, String displayName, int argb, boolean visible) {
        if (StringUtils.isBlank(productName)) {
            throw new IllegalArgumentException("productName must not be blank");
        }
        if (StringUtils.isBlank(bandName)) {
            throw new IllegalArgumentException("bandName must not be blank");
        }
        if (StringUtils.isBlank(displayName)) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        this.productName = productName;
        this.bandName = bandName;
        this.displayName = displayName;
        this.argb = argb;
        this.visible = visible;
    }


    public String getProductName() {
        return productName;
    }

    public String getBandName() {
        return bandName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getArgb() {
        return argb;
    }

    public boolean isVisible() {
        return visible;
    }

    public String sourceIdentity() {
        return productName + "\u0000" + bandName;
    }

}
