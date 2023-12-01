package org.esa.stac.internal;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

public class StacCatalog implements StacComponent, STACUtils {
    private String rootURL;

    private JSONObject catalogJSON;

    private JSONObject collectionJSON;

    private String [] allCollections;

    private HashMap<String, String> collectionsWithURLs;

    private String title;
    public StacCatalog(String catalogURL){
        rootURL = catalogURL;

        catalogJSON = getJSONFromURL(catalogURL);
        collectionJSON = getJSONFromURL(catalogURL + "/collections");

        title = (String) catalogJSON.get("title");

        collectionsWithURLs = new HashMap<>();

        // Store list of collections
        allCollections = new String[((JSONArray) collectionJSON.get("collections")).size()];
        for (int x = 0; x < ((JSONArray) collectionJSON.get("collections")).size(); x++){
            JSONObject curCollection = (JSONObject) ((JSONArray) collectionJSON.get("collections")).get(x);
            allCollections[x] = (String) curCollection.get("id");
            for (Object o: (JSONArray) curCollection.get("links")){
                if (Objects.equals("self", ((JSONObject) o).get("rel"))){
                    collectionsWithURLs.put((String) curCollection.get("id"), (String) ((JSONObject) o).get("href"));
                }
            }
        }
        Arrays.sort(allCollections);
    }


    @Override
    public JSONObject getJSON() {
        return catalogJSON;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getSelfURL() {
        return null;
    }

    @Override
    public String getRootURL() {
        return null;
    }

    public String getTitle() {
        return this.title;
    }

    public String [] listCollections(){
        return this.allCollections;
    }
    public int getNumCollections(){
        return this.allCollections.length;
    }

    public boolean conformsTo(String confirmityCheck) {
        return true;
    }

    public StacCollection getCollection(String collectionName) {
        if (collectionsWithURLs.containsKey(collectionName)){
            return new StacCollection(collectionsWithURLs.get(collectionName));
        }
        return null;
    }

    public boolean containsCollection(String collectionName){
        for (String collection : allCollections){
            if (Objects.equals(collectionName, collection)){
                return true;
            }
        }
        return false;
    }


}
