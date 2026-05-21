package eu.esa.snap.core.datamodel.group;

class StartsWithEntry implements Entry {

    private final String group;

    protected StartsWithEntry(String group) {
        this.group = group;
    }

    @Override
    public boolean matches(String name) {
        return name.startsWith(group);
    }
}
