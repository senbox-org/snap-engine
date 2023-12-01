package org.esa.stac.internal;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public interface STACUtils {

    public static JSONObject getJSONFromURLStatic(String jsonURL){
        try {
            URL url = new URL(jsonURL);
            URLConnection request = url.openConnection();
            request.connect();
            String content = new String(
                    ((InputStream) request.getContent()).readAllBytes(),
                    StandardCharsets.UTF_8
            );
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(content);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    default JSONObject getJSONFromURL(String jsonURL){
        return getJSONFromURLStatic(jsonURL);
    }

    default JSONArray getAllFeaturesRecursive(JSONObject rootObject, JSONArray returnArray){
        JSONArray features = (JSONArray) rootObject.get("features");
        JSONArray links = (JSONArray) rootObject.get("links");
        returnArray.addAll(features);
        for (Object o : links){
            if (Objects.equals("next", ((JSONObject) o).get("rel"))){
                getAllFeaturesRecursive(
                        getJSONFromURL((String) ((JSONObject) o).get("href")),
                        returnArray);
            }
        }
        return returnArray;
    }

    default JSONArray getAllFeatures(JSONObject rootObject){
        return getAllFeaturesRecursive(rootObject, new JSONArray());
    }

    // To be used with MSFT Planetary data requests.
    default String getPlanetaryToken(){
        JSONObject authResponse = getJSONFromURL("https://planetarycomputer.microsoft.com/api/sas/v1/token/naip");
        return (String) authResponse.get("token");
    }
}
