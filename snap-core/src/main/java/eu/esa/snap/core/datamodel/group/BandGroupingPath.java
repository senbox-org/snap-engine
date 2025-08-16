package eu.esa.snap.core.datamodel.group;

import org.esa.snap.core.util.StringUtils;

public class BandGroupingPath {

    private final String[] groups;
    private final Entry[] entries;

    BandGroupingPath(String[] groups) {
        this.groups = groups;
        entries = new Entry[groups.length];

        // Default for SNAP and SeaDAS differs here, SeaDAS uses 'true', SNAP uses 'false'
        // Ideally this would be defined in preferences
        boolean useStartsWithInsteadOfContains = true;

        for (int i = 0; i < groups.length; i++) {
            final String groupPattern = groups[i];
             if (groupPattern.contains("*") || groupPattern.contains("?")) {
                 // Wildcard Matching
                entries[i] = new WildCardEntry(groupPattern);
            } else if (groupPattern.contains("#") || groupPattern.contains(",")) {
                 // BandNames Matching
                 if (groupPattern.contains("#")) {
                     // Rename the group
                     final String[] split = StringUtils.split(groupPattern, new char[]{'#'}, true);
                     final String groupName = split[0];
                     this.groups[i] = groupName;
                     // todo this is problematic, although it does work, the Band 'Properties' editor reads it as only the name and not the full expression
                     entries[i] = new BandNamesEntry(groupName, split[1]);
                 } else {
                     // Maintain the original group expression as the group name
                     entries[i] = new BandNamesEntry(groupPattern, groupPattern);
                 }
             } else if (groupPattern.startsWith("^") && groupPattern.endsWith("$")) {
                 // Exact Matching
                String groupTrimmed = groupPattern.substring(1, groupPattern.length() -1);
                entries[i] = new ExactEntry(groupTrimmed);
             } else if (groupPattern.startsWith("^")) {
                 // StartsWith Matching
                 String groupTrimmed = groupPattern.substring(1);
                 entries[i] = new StartsWithEntry(groupTrimmed);
             } else if (groupPattern.endsWith("$")) {
                 // EndsWith Matching
                 String groupTrimmed = groupPattern.substring(0, groupPattern.length() - 1);
                 entries[i] = new EndsWithEntry(groupTrimmed);
            } else {
                 // Default Matching
                if (useStartsWithInsteadOfContains) {
                    // StartsWith Matching
                    entries[i] = new StartsWithEntry(groupPattern);
                } else {
                    // Contains Matching
                    entries[i] = new EntryImpl(groupPattern);
                }
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
