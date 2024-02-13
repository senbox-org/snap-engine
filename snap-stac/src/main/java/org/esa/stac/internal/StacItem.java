package org.esa.stac.internal;

// Author Alex McVittie, SkyWatch Space Applications Inc. December 2023
// The StacItem class allows you to interact with specific Items retrieved
// from a StacCatalog.


import org.esa.snap.core.jexp.ParseException;
import org.esa.stac.StacClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class StacItem implements STACUtils {

    private final String DATAROLE = "data";
    private final String PREVIEWROLE = "thumbnail";
    private final String OVERVIEWROLE = "overview";
    private final String METADATAROLE = "metadata";
    private final String TILESROLE = "tiles";


    private String id;
    private JSONArray linksJSON;
    private JSONObject assetsJSON;
    private String itemURL;
    private double [] bbox;
    private HashMap<String, StacAsset> assetsById = new HashMap<>();
    private List<StacAsset> dataAssets = new ArrayList<>();
    private List<StacAsset> metadataAssets = new ArrayList<>();
    private JSONObject stacItemJSON;
    private StacClient validStacClient;

    public StacItem(Object productInputFile) throws Exception {
        this.stacItemJSON = parseInputFile(productInputFile);
        initStacItem(stacItemJSON);
    }

    public StacItem(Object productInputFile, StacClient client) throws Exception{
        this.validStacClient = client;
        this.stacItemJSON = parseInputFile(productInputFile);
        initStacItem(stacItemJSON);
    }

    private void initStacItem(JSONObject stacItemJSON) throws Exception {
        if (Objects.isNull(stacItemJSON)){
            throw new ParseException("Null JSON object passed in");
        }
        if (! stacItemJSON.containsKey("id")
                || ! stacItemJSON.containsKey("links")
                || ! stacItemJSON.containsKey("assets")){
            throw new ParseException("Invalid STAC JSON");
        }
        this.stacItemJSON = stacItemJSON;
        this.id = (String) stacItemJSON.get("id");
        this.linksJSON = (JSONArray) stacItemJSON.get("links");
        this.assetsJSON = (JSONObject) stacItemJSON.get("assets");
        for(Object o : assetsJSON.keySet()){
            StacAsset curAsset = new StacAsset((JSONObject) assetsJSON.get(o), (String) o);
            assetsById.put((String) o, curAsset);
            if(Objects.equals(DATAROLE, curAsset.getRole())){
                dataAssets.add(curAsset);
            }else if (Objects.equals(METADATAROLE, curAsset.getRole())){
                metadataAssets.add(curAsset);
            }
        }
        for(Object o : linksJSON){
            String rel = (String)((JSONObject) o).get("rel");
            if (Objects.equals(rel, "self")){
                itemURL = (String)((JSONObject) o).get("href");
            }
        }
    }
    public String getURL(){
        return this.itemURL;
    }

    private JSONObject parseInputFile(Object productInputFile) throws Exception{
        JSONObject stacItemJSON = null;

        if (productInputFile instanceof String){
            try{
                if (((String) productInputFile).startsWith("http")){
                    stacItemJSON = getJSONFromURL((String) productInputFile);
                }else{
                    stacItemJSON = (JSONObject) new JSONParser().parse(new FileReader((String) productInputFile));
                }
            }catch (Exception e){
                throw new ParseException("Unable to parse JSON from given path");
            }
        }else if (productInputFile instanceof File){
            try{
                stacItemJSON = (JSONObject) new JSONParser().parse(new FileReader((File) productInputFile));
            }catch (Exception e){
                throw new ParseException("Unable to parse JSON from given local file.");
            }
        }else if (productInputFile instanceof JSONObject){
            stacItemJSON = (JSONObject) productInputFile;
        }
        return stacItemJSON;
    }

    public StacClient getClient(){
        if (this.validStacClient != null){
            return this.validStacClient;
        }
        for (Object o : linksJSON){
            JSONObject curLinkObject = (JSONObject) o;
            if (curLinkObject.get("rel").equals("root")){
                String mainURL = (String) curLinkObject.get("href");
                try {
                    validStacClient = new StacClient(mainURL);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return this.validStacClient;
    }
    public StacAsset getAsset(String assetID){
        if (assetsById.containsKey(assetID)){
            return assetsById.get(assetID);
        }
        return null;
    }
    public String [] listAssetIds(){
        String [] assetArray = new String[assetsJSON.size()];
        assetArray = (String[]) assetsJSON.keySet().toArray(assetArray);
        Arrays.sort(assetArray);
        return assetArray;
    }
    public JSONObject getItemJSON(){
        return stacItemJSON;
    }
    public String getId(){
        return this.id;
    }
    public class StacAsset{
        private String href;
        private String title;
        private String id;
        private JSONObject assetJSON;
        public EOBandData bandData;
        private List<String> roles;
        private String fileName;

        private int width = 0;
        private int height = 0;

        StacAsset(JSONObject asset, String id){
            this.id = id;
            this.title = (String) asset.get("title");
            this.href = (String) asset.get("href");
            this.assetJSON = asset;
            if (asset.containsKey("eo:bands")){
                bandData = new EOBandData(
                        (JSONObject) ((JSONArray) asset.get("eo:bands")).get(0));
            }
            if (asset.containsKey("proj:shape")){
                this.width = (int) (long) ((JSONArray) asset.get("proj:shape")).get(0);
                this.height = (int) (long) ((JSONArray) asset.get("proj:shape")).get(1);

            }
            roles = (List<String>) asset.get("roles");
            fileName = this.href.split("/")[this.href.split("/").length - 1];
        }
        public String getTitle(){
            return this.title;
        }
        public String getFileName(){
            return fileName;
        }
        public String getId(){
            return this.id;
        }
        public String getURL(){
            return this.href;
        }
        public int getWidth(){
            return this.width;
        }
        public int getHeight(){
            return this.height;
        }
        public JSONObject getJSON(){
            return this.assetJSON;
        }
        public String getRole(){
            return roles.get(0);
        }

    }
    public class EOBandData {
        public double centerWavelength;
        public double fullWidthHalfMax;
        public String name;
        public String description;
        public String commonName;
        public EOBandData(JSONObject eoBandData){
            commonName = "";

            this.centerWavelength = (double) eoBandData.get("center_wavelength");
            this.fullWidthHalfMax = (double) eoBandData.get("full_width_half_max");
            this.name = (String) eoBandData.get("name");
            this.description = (String) eoBandData.get("description");
            this.commonName = (String) eoBandData.get("common_name");


        }
    }
}
