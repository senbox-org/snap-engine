package eu.esa.snap.core.datamodel.group;

import org.esa.snap.core.util.StringUtils;

public class BandNamesEntry implements Entry {

    final String[] bandNames;
    final String groupName;

    public BandNamesEntry(String groupName, String bandNames) {
        this.groupName = groupName;
        this.bandNames = StringUtils.split(bandNames, new char[]{','}, true);
    }

    @Override
    public boolean matches(String name) {
        for (final String bandName : bandNames) {
            if (bandName.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
