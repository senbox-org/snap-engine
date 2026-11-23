package eu.esa.snap.core.renderer.classfraction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


/**
 * Ordered group. Order is presentation/persistence order, never class meaning.
 */
public final class ClassBandGroupConfig {


    private final List<ClassBandConfig> bands;
    private final double displayScale;
    private final ValidityPolicy validityPolicy;


    public ClassBandGroupConfig(List<ClassBandConfig> bands, double displayScale, ValidityPolicy validityPolicy) {
        Objects.requireNonNull(bands, "bands");
        if (bands.size() < 2) {
            throw new IllegalArgumentException("At least two class bands are required");
        }

        Set<String> identities = new HashSet<>();
        for (ClassBandConfig band : bands) {
            if (band == null) {
                throw new IllegalArgumentException("bands must not contain null");
            }
            if (!identities.add(band.sourceIdentity())) {
                throw new IllegalArgumentException("Duplicate class source: " + band.getBandName());
            }
        }

        if (!Double.isFinite(displayScale) || displayScale <= 0.0) {
            throw new IllegalArgumentException("displayScale must be finite and greater than zero");
        }

        this.bands = Collections.unmodifiableList(new ArrayList<>(bands));
        this.displayScale = displayScale;
        this.validityPolicy = Objects.requireNonNull(validityPolicy, "validityPolicy");
    }


    public List<ClassBandConfig> getBands() {
        return bands;
    }

    public double getDisplayScale() {
        return displayScale;
    }

    public ValidityPolicy getValidityPolicy() {
        return validityPolicy;
    }


    /** The validity rule shared by every input in this class group. */
    public enum ValidityPolicy {
        STRICT_SOURCE_VALIDITY, FINITE_AND_DECLARED_NODATA
    }
}
