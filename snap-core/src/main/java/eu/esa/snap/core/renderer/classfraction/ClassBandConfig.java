package eu.esa.snap.core.renderer.classfraction;


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
        this.productName = requireText(productName, "productName");
        this.bandName = requireText(bandName, "bandName");
        this.displayName = requireText(displayName, "displayName");
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

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
