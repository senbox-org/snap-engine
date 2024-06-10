package eu.esa.snap.core.datamodel.group;

public class BGEntryImpl implements BGEntry {

    private final String group;

    protected BGEntryImpl(String group) {
        this.group = group;
    }


    @Override
    public boolean matches(String name) {
        return name.contains(group);
    }
}
