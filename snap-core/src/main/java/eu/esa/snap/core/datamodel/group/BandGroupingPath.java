package eu.esa.snap.core.datamodel.group;

import org.esa.snap.core.util.StringUtils;

public class BandGroupingPath {

    private final String[] groups;
    private final String[] groups_orig;
    private final Entry[] entries;

    BandGroupingPath(String[] groups) {
        this.groups = groups;
        this.groups_orig = groups;
        entries = new Entry[groups.length];
        for (int i = 0; i < groups.length; i++) {
            final String groupPattern = groups[i];
            if (groupPattern.contains(",")) {
                String groupPatternTrimmed = groupPattern;
                if (groupPattern.startsWith("#")) {
                    groupPatternTrimmed = groupPatternTrimmed.substring(1);
                }
                entries[i] = new BandNamesEntry(groupPattern, groupPatternTrimmed);
            } else if (groupPattern.startsWith("^")) {
                String groupTrimmed = groupPattern.substring(1);
                entries[i] = new StartsWithEntry(groupTrimmed);
//                String groupName = groupPattern;
//                if (groupName.endsWith("_")) {
//                    groupName = groupName.substring(1, groupName.length() - 1);
//                } else {
//                    groupName = groupName.substring(1);
//                }
//                this.groups[i] = groupName;
            } else if (groupPattern.contains("*") || groupPattern.contains("?")) {
                entries[i] = new WildCardEntry(groupPattern);
            } else if (groupPattern.contains("#")) {
                final String[] split = StringUtils.split(groupPattern, new char[]{'#'}, true);
                final String groupName = split[0];
                this.groups[i] = groupName;
                entries[i] = new BandNamesEntry(groupName, split[1]);
            } else {
//                entries[i] = new EntryImpl(groupPattern);
                entries[i] = new StartsWithEntry(groupPattern);
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

    String[] getInputPathOrig() {
        return groups_orig;
    }
}
