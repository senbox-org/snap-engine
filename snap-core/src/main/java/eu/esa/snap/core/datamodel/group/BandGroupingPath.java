package eu.esa.snap.core.datamodel.group;

public class BandGroupingPath {

    private final String[] groups;
    private final Entry[] entries;

    BandGroupingPath(String[] groups) {
        this.groups = groups;
        entries = new Entry[groups.length];
        for (int i = 0; i < groups.length; i++) {
            if (groups[i].contains("*") || groups[i].contains("?")) {
                entries[i] = new WildCardEntry(groups[i]);
            } else {
                entries[i] = new EntryImpl(groups[i]);
            }
        }
    }

    // @todo 1 discuss with Sabine, I think the method does not do what is expected from name tb 2024-06-11
    boolean contains(String name) {
        for (Entry entry : entries) {
            if (!entry.matches(name)) {
                return false;
            }
        }
        return true;
    }

    boolean matchesGrouping(String name) {
        for (Entry entry : entries) {
            if (entry.matches(name)) {
                return true;
            }
        }
        return false;
    }

    String[] getInputPath() {
        return groups;
    }

}
