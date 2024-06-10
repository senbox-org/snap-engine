package eu.esa.snap.core.datamodel.group;

import org.esa.snap.core.util.io.WildcardMatcher;

public class BGWildCardEntry implements BGEntry {

    private final WildcardMatcher wildcardMatcher;

    protected BGWildCardEntry(String group) {
        wildcardMatcher = new WildcardMatcher(group);
    }

    @Override
    public boolean matches(String name) {
        return wildcardMatcher.matches(name);
    }
}
