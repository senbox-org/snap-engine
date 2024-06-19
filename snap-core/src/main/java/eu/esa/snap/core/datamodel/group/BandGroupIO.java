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
    public static BandGrouping[] read(InputStream stream) throws IOException, ParseException {
        final JSONParser jsonParser = new JSONParser();
        final JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
        final JSONArray bandGroups = (JSONArray) jsonObject.get("bandGroups");

        final int numBandGroups = bandGroups.size();
        final BandGrouping[] bandGroupings = new BandGrouping[numBandGroups];

        for (int i = 0; i < numBandGroups; i++) {
            final JSONObject bandGroupObject = (JSONObject) bandGroups.get(i);
            final JSONArray pathsObject = (JSONArray) bandGroupObject.get("paths");

            final int numPaths = pathsObject.size();
            final String[][] inputPaths = new String[numPaths][];
            for (int k = 0; k < numPaths; k++) {
                final JSONArray pathList = (JSONArray) pathsObject.get(k);
                inputPaths[k] = (String[]) pathList.toArray(new String[0]);
            }

            final BandGroupingImpl bandGrouping = new BandGroupingImpl(inputPaths);

            final String name = (String) bandGroupObject.get("name");
            if (StringUtils.isNotNullAndNotEmpty(name)) {
                bandGrouping.setName(name);
            }

            bandGroupings[i] = bandGrouping;
        }

        return bandGroupings;
    }

    public static void write(BandGrouping[] bandGroupings, OutputStream jsonStream) throws IOException {
        final JSONObject jsonDoc = new JSONObject();
        final JSONArray bandGroups = new JSONArray();

        jsonDoc.put("bandGroups", bandGroups);

        for (final BandGrouping grouping : bandGroupings) {
            final String groupingName = grouping.getName();
            final JSONObject jsonGrouping = new JSONObject();
            jsonGrouping.put("name", groupingName);

            final JSONArray pathsArray = new JSONArray();
            final int numPaths = grouping.size();
            for (int i = 0; i < numPaths; i++) {
                final String[] paths = grouping.get(i);
                JSONArray path = new JSONArray();
                Collections.addAll(path, paths);
                pathsArray.add(path);
            }

            jsonGrouping.put("paths", pathsArray);

            bandGroups.add(jsonGrouping);
        }

        final String jsonString = jsonDoc.toJSONString();
        jsonStream.write(jsonString.getBytes(StandardCharsets.UTF_8));
    }
}
