package org.esa.snap.remote.products.repository;

/**
 * Created by jcoravu on 14/8/2019.
 */
public class QueryFilter {

    private final String name;
    private final String label;
    private final Class<?> type;
    private final Object defaultValue;
    private final boolean required;
    private final Object[] valueSet;
    private final ItemRenderer<Object> valueRenderer;

    public QueryFilter(String name, Class<?> type, String label, Object defaultValue, boolean required, Object[] valueSet) {
        this(name, type, label, defaultValue, required, null, valueSet);
    }

    public QueryFilter(String name, Class<?> type, String label, Object defaultValue, boolean required, ItemRenderer<Object> valueRenderer, Object[] valueSet) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
        this.required = required;
        this.valueSet = valueSet;
        this.valueRenderer = valueRenderer;
    }

    public ItemRenderer<Object> getValueRenderer() {
        return valueRenderer;
    }

    public String getName() { return name; }

    public String getLabel() { return label; }

    public Class<?> getType() { return type; }

    public boolean isRequired() { return required; }

    public Object getDefaultValue() { return defaultValue; }

    public Object[] getValueSet() { return valueSet; }
}
