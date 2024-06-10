package eu.esa.snap.core.datamodel.group;

public class EntryImpl implements Entry {

    private final String group;

    protected EntryImpl(String group) {
        this.group = group;
    }

    @Override
    public boolean matches(String name) {
        return name.contains(group);
    }
}
