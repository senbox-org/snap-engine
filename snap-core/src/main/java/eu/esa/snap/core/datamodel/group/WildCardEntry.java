package eu.esa.snap.core.datamodel.group;

import org.esa.snap.core.util.io.WildcardMatcher;

class WildCardEntry implements Entry {

    private final WildcardMatcher wildcardMatcher;

    protected WildCardEntry(String group) {
        wildcardMatcher = new WildcardMatcher(group);
    }

    @Override
    public boolean matches(String name) {
        return wildcardMatcher.matches(name);
    }
}
