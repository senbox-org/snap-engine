package eu.esa.snap.core.datamodel.group;

class ExactEntry implements Entry {

    private final String group;

    protected ExactEntry(String group) {
        this.group = group;
    }

    @Override
    public boolean matches(String name) {
        return name.equals(group);
    }
}
