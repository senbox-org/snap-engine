package eu.esa.snap.core.datamodel.group;

class EndsWithEntry implements Entry {

    private final String group;

    protected EndsWithEntry(String group) {
        this.group = group;
    }

    @Override
    public boolean matches(String name) {
        return name.endsWith(group);
    }
}
