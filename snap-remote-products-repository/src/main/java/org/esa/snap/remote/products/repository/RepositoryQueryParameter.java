package org.esa.snap.remote.products.repository;

/**
 * The data about a query parameter to be displayed in the application for a remote repository
 *
 * Created by jcoravu on 14/8/2019.
 */
public class RepositoryQueryParameter {

    public static final String START_DATE = "startDate";
    public static final String END_DATE = "endDate";
    public static final String FOOTPRINT = "footprint";

    private final String name;
    private final String label;
    private final Class<?> type;
    private final Object defaultValue;
    private final boolean required;
    private final Object[] valueSet;

    public RepositoryQueryParameter(String name, Class<?> type, String label, Object defaultValue, boolean required, Object[] valueSet) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
        this.required = required;
        this.valueSet = valueSet;
    }

    public String getName() { return name; }

    public String getLabel() { return label; }

    public Class<?> getType() { return type; }

    public boolean isRequired() { return required; }

    public Object getDefaultValue() { return defaultValue; }

    public Object[] getValueSet() { return valueSet; }
}
