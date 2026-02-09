package org.esa.snap.speclib.model;

import java.util.Objects;
import java.util.Optional;


public class AttributeDef {


    private final String key;
    private final AttributeType type;
    private final boolean required;
    private final AttributeValue defaultValue;
    private final String description;
    private final String uiHint;


    public AttributeDef(String key, AttributeType type, boolean required,
                        AttributeValue defaultValue, String description, String uiHint) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.required = required;
        this.defaultValue = defaultValue;
        this.description = description;
        this.uiHint = uiHint;

        if (defaultValue != null && defaultValue.getType() != type) {
            throw new IllegalArgumentException("defaultValue type does not match AttributeDef type");
        }
    }


    public static AttributeDef optional(String key, AttributeType type) {
        return new AttributeDef(key, type, false, null, null, null);
    }

    public static AttributeDef required(String key, AttributeType type) {
        return new AttributeDef(key, type, true, null, null, null);
    }

    public String getKey() {
        return key;
    }

    public AttributeType getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public Optional<AttributeValue> getDefaultValue() {
        return Optional.ofNullable(defaultValue);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<String> getUiHint() {
        return Optional.ofNullable(uiHint);
    }
}
