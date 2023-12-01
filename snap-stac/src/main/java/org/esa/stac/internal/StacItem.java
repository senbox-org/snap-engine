package org.esa.stac.internal;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class StacItem implements STACUtils {

    private final String DATAROLE = "data";
    private final String PREVIEWROLE = "thumbnail";
    private final String OVERVIEWROLE = "overview";
    private final String METADATAROLE = "metadata";
    private final String TILESROLE = "tiles";


    private String id;
    private JSONArray linksJSON;
    private JSONObject assetsJSON;
    private double [] bbox;
    private HashMap<String, StacAsset> assetsById = new HashMap<>();
    private List<StacAsset> dataAssets = new ArrayList<>();
    private List<StacAsset> metadataAssets = new ArrayList<>();


    public StacItem(JSONObject stacItemJSON){
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
        return assetArray;
    }
    public String getId(){
        return this.id;
    }
    public class StacAsset{
        private String href;
        private String title;
        private String id;
        private JSONObject assetJSON;
        private EOBandData bandData;
        private List<String> roles;
        private String fileName;
        StacAsset(JSONObject asset, String id){
            this.id = id;
            this.title = (String) asset.get("title");
            this.href = (String) asset.get("href");
            this.assetJSON = asset;
            if (asset.containsKey("eo:bands")){
                bandData = new EOBandData(
                        (JSONObject) ((JSONArray) asset.get("eo:bands")).get(0));
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
        public JSONObject getJSON(){
            return this.assetJSON;
        }
        public String getRole(){
            return roles.get(0);
        }

    }
    public class EOBandData {
        double centerWavelength;
        double fullWidthHalfMax;
        String name;
        String description;
        String commonName;
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
