package eu.esa.snap.core.datamodel.group;

public class BandGroupingPath {

    private final String[] groups;
    private final BGEntry[] entries;

    BandGroupingPath(String[] groups) {
        this.groups = groups;
        entries = new BGEntry[groups.length];
        for (int i = 0; i < groups.length; i++) {
            if (groups[i].contains("*") || groups[i].contains("?")) {
                entries[i] = new BGWildCardEntry(groups[i]);
            } else {
                entries[i] = new BGEntryImpl(groups[i]);
            }
        }
    }

    boolean contains(String name) {
        for (BGEntry entry : entries) {
            if (!entry.matches(name)) {
                return false;
            }
        }
        return true;
    }

    String[] getInputPath() {
        return groups;
    }

}
