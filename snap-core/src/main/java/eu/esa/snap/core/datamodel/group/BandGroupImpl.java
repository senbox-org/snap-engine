package eu.esa.snap.core.datamodel.group;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.ObjectUtils;
import org.esa.snap.core.util.StringUtils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BandGroupImpl extends AbstractList<String[]> implements BandGroup {

    private static final String GROUP_SEPARATOR = "/";
    private static final String PATH_SEPARATOR = ":";

    private final BandGroupingPath[] autoGroupingPaths;
    private final Index[] indexes;

    private String name;

    public BandGroupImpl(String groupName, String[] bandNames) {
        name = groupName;
        autoGroupingPaths = new BandGroupingPath[1];
        autoGroupingPaths[0] = new BandGroupingPath(bandNames);
        indexes = new Index[1];
        indexes[0] = new Index(autoGroupingPaths[0], 0);
    }

    @Override
    public String[] getMatchingBandNames(Product product) {
        final ArrayList<String> bandNamesList = new ArrayList<>();
        final Band[] bands = product.getBands();
        for (final Band band : bands) {
            final String bandName = band.getName();
            for (final Index idx : indexes) {
                if (idx.path.matchesGrouping(bandName)) {
                    bandNamesList.add(bandName);
                }
            }
        }
        return bandNamesList.toArray(new String[0]);
    }

    protected BandGroupImpl(String[][] inputPaths) {
        autoGroupingPaths = new BandGroupingPath[inputPaths.length];
        indexes = new Index[inputPaths.length];
        for (int i = 0; i < inputPaths.length; i++) {
            final BandGroupingPath autoGroupingPath = new BandGroupingPath(inputPaths[i]);
            autoGroupingPaths[i] = autoGroupingPath;
            indexes[i] = new Index(autoGroupingPath, i);
        }
        Arrays.sort(indexes, (o1, o2) -> {
            final String[] o1InputPath = o1.path.getInputPath();
            final String[] o2InputPath = o2.path.getInputPath();
            int index = 0;

            while (index < o1InputPath.length && index < o2InputPath.length) {
                final String currentO1InputPathString = o1InputPath[index];
                final String currentO2InputPathString = o2InputPath[index];
                if (currentO1InputPathString.length() != currentO2InputPathString.length()) {
                    return currentO2InputPathString.length() - currentO1InputPathString.length();
                }
                index++;
            }
            if (o1InputPath.length != o2InputPath.length) {
                return o2InputPath.length - o1InputPath.length;
            }
            return o2InputPath[0].compareTo(o1InputPath[0]);
        });

        name = "";
    }

    public static BandGroup parse(String text) {
        if (StringUtils.isNullOrEmpty(text)) {
            return null;
        }

        final List<String[]> pathLists = new ArrayList<>();
        final String[] pathTexts = StringUtils.toStringArray(text, PATH_SEPARATOR);
        for (String pathText : pathTexts) {
            final String[] subPaths = StringUtils.toStringArray(pathText, GROUP_SEPARATOR);
            final ArrayList<String> subPathsList = new ArrayList<>();
            for (String subPath : subPaths) {
                if (StringUtils.isNotNullAndNotEmpty(subPath)) {
                    subPathsList.add(subPath);
                }
            }
            if (!subPathsList.isEmpty()) {
                pathLists.add(subPathsList.toArray(new String[subPathsList.size()]));
            }
        }
        if (pathLists.isEmpty()) {
            return null;
        }
        return new BandGroupImpl(pathLists.toArray(new String[pathLists.size()][]));
    }

    @Override
    public int indexOf(String name) {
        for (Index index : indexes) {
            final int i = index.index;
            if (index.path.contains(name)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String[] get(int index) {
        return autoGroupingPaths[index].getInputPath();
    }

    @Override
    public int size() {
        return autoGroupingPaths.length;
    }

    public String format() {
        if (autoGroupingPaths.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < autoGroupingPaths.length; i++) {
                if (i > 0) {
                    sb.append(PATH_SEPARATOR);
                }
                String[] path = autoGroupingPaths[i].getInputPath();
                for (int j = 0; j < path.length; j++) {
                    if (j > 0) {
                        sb.append(GROUP_SEPARATOR);
                    }
                    sb.append(path[j]);
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        return format();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof BandGroup) {
            BandGroup other = (BandGroup) o;
            if (other.size() != size()) {
                return false;
            }
            for (int i = 0; i < autoGroupingPaths.length; i++) {
                String[] path = autoGroupingPaths[i].getInputPath();
                if (!ObjectUtils.equalObjects(path, other.get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int code = 0;
        for (BandGroupingPath autoGroupingPath : autoGroupingPaths) {
            String[] path = autoGroupingPath.getInputPath();
            code += Arrays.hashCode(path);
        }
        return code;
    }


    static class Index {

        final int index;
        final BandGroupingPath path;

        Index(BandGroupingPath path, int index) {
            this.path = path;
            this.index = index;
        }
    }
}
