package eu.esa.snap.core.renderer.classfraction;

import java.util.Objects;


/**
 * Configuration for the class-fraction renderer.
 */
public final class ClassFractionRendererConfig {


    private final String displayName;
    private final ClassBandGroupConfig classBandGroup;


    public ClassFractionRendererConfig(String displayName, ClassBandGroupConfig classBandGroup) {
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }

        this.displayName = displayName;
        this.classBandGroup = Objects.requireNonNull(classBandGroup, "classBandGroup");
    }


    public String getDisplayName() {
        return displayName;
    }

    public ClassBandGroupConfig getClassBandGroup() {
        return classBandGroup;
    }
}
