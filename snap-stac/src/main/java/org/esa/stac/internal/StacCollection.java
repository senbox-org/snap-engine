package org.esa.stac.internal;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Objects;

public class StacCollection implements STACUtils {

    private JSONObject collectionJSON;

    private final String collectionURL;

    private final String collectionItemURL;

    public StacCollection(String collectionURL){
        this.collectionURL = collectionURL;
        collectionJSON = getJSONFromURL(collectionURL);
        collectionItemURL = getCollectionItemURL();
        int x = 0;
    }

    public String getCollectionURL(){
        return collectionURL;
    }
    private String getCollectionItemURL(){
        JSONArray links = (JSONArray) collectionJSON.get("links");
        for (Object o : links){
            JSONObject j = ((JSONObject) o);
            if(j.containsKey("rel") && Objects.equals(j.get("rel"), "items")){
                return (String) j.get("href");
            }
        }
        return null;
    }

    public StacItem[] getAllItems(){
        // Get the initial items
        JSONObject items = getJSONFromURL(collectionItemURL);
        JSONArray features = getAllFeatures(items);



        return null;
    }

    public JSONObject getAssets(){
        return null;
    }

    public JSONObject getJSON(){
        return collectionJSON;
    }
}
