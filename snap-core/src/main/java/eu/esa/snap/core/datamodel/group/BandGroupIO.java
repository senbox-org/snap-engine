package eu.esa.snap.core.datamodel.group;

import org.esa.snap.core.util.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@SuppressWarnings("SuspiciousToArrayCall")
public class BandGroupIO {

    @SuppressWarnings("unchecked")
    public static BandGroup[] read(InputStream stream) throws IOException, ParseException {
        final JSONParser jsonParser = new JSONParser();
        final JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        final JSONArray bandGroups = (JSONArray) jsonObject.get("bandGroups");

        final int numBandGroups = bandGroups.size();
        final BandGroup[] bandGroupings = new BandGroup[numBandGroups];

        for (int i = 0; i < numBandGroups; i++) {
            final JSONObject bandGroupObject = (JSONObject) bandGroups.get(i);
            final JSONArray pathsObject = (JSONArray) bandGroupObject.get("paths");

            final int numPaths = pathsObject.size();
            final String[][] inputPaths = new String[numPaths][];
            for (int k = 0; k < numPaths; k++) {
                final JSONArray pathList = (JSONArray) pathsObject.get(k);
                inputPaths[k] = (String[]) pathList.toArray(new String[0]);
            }

            final BandGroupImpl bandGrouping = new BandGroupImpl(inputPaths);

            final String name = (String) bandGroupObject.get("name");
            if (StringUtils.isNotNullAndNotEmpty(name)) {
                bandGrouping.setName(name);
            }

            bandGroupings[i] = bandGrouping;
        }

        return bandGroupings;
    }

    public static void write(BandGroup[] bandGroupings, OutputStream jsonStream) throws IOException {
        final JSONObject jsonDoc = new JSONObject();
        final JSONArray bandGroups = new JSONArray();

        jsonDoc.put("bandGroups", bandGroups);

        for (final BandGroup grouping : bandGroupings) {
            final String groupingName = grouping.getName();
            final JSONObject jsonGrouping = new JSONObject();
            jsonGrouping.put("name", groupingName);

            final JSONArray pathsArray = new JSONArray();
            final int numPaths = grouping.size();
            for (int i = 0; i < numPaths; i++) {
                final String[] paths = grouping.get(i);
                String[] modifiedPaths = new String[paths.length];
                for (int j = 0; j < paths.length; j++) {
                    if (paths[j].contains("#") || paths[j].contains("*")) {
                        modifiedPaths[j] = paths[j];
                    } else {
                        modifiedPaths[j] = paths[j] + "#" + paths[j];
                    }
                }
                JSONArray path = new JSONArray();
                Collections.addAll(path, modifiedPaths);
                pathsArray.add(path);
            }

            jsonGrouping.put("paths", pathsArray);

            bandGroups.add(jsonGrouping);
        }

        final String jsonString = jsonDoc.toJSONString();
        jsonStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
    }
}
