package eu.esa.snap.core.renderer.classfraction;

import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.isBlank;


/**
 * Configuration for the class-fraction renderer.
 */
public final class ClassFractionRendererConfig {


    private final String displayName;
    private final ClassBandGroupConfig classBandGroup;


    public ClassFractionRendererConfig(String displayName, ClassBandGroupConfig classBandGroup) {
        if (isBlank(displayName)) {
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
