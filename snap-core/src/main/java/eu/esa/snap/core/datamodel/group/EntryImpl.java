package eu.esa.snap.core.datamodel.group;

public class EntryImpl implements Entry {

    private final String group;

    public EntryImpl(String group) {
        this.group = group;
    }

    @Override
    public boolean matches(String name) {
        if (name == null) {
            return false;
        }
        return name.contains(group);
    }
}
