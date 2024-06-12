package eu.esa.snap.core.datamodel.group;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class BandGroupIO {

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
            for (int k = 0; k < numPaths;k++) {
                final JSONArray pathList = (JSONArray) pathsObject.get(k);
                inputPaths[k] = (String[]) pathList.toArray(new String[0]);
            }

            final BandGroupingImpl bandGrouping = new BandGroupingImpl(inputPaths);

            final String name = (String) bandGroupObject.get("name");
            bandGrouping.setName(name);

            bandGroupings[i] = bandGrouping;
        }

        return bandGroupings;
    }
}
