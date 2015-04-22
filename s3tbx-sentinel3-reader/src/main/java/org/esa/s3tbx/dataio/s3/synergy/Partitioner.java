package org.esa.s3tbx.dataio.s3.synergy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Ralf Quast
 */
public class Partitioner {

    public static Map<String, List<String>> partition(final String[] names, final String enumerator) {
        final Map<String, List<String>> map = new HashMap<String, List<String>>();

        for (final String name : names) {
            final String[] parts = name.split(enumerator);
            final String key = parts[0];
            if (!map.containsKey(key)) {
                map.put(key, new ArrayList<String>());
            }
            map.get(key).add(name);
        }
        for (final List<String> nameList : map.values()) {
            Collections.sort(nameList);
        }

        return map;
    }

}
