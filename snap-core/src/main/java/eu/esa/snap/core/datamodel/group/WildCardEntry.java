package eu.esa.snap.core.datamodel.group;

import org.esa.snap.core.util.io.WildcardMatcher;

public class WildCardEntry implements Entry {

    private final WildcardMatcher wildcardMatcher;

    public WildCardEntry(String group) {
        wildcardMatcher = new WildcardMatcher(group);
    }

    @Override
    public boolean matches(String name) {
        return wildcardMatcher.matches(name);
    }
}
