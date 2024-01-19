package org.esa.stac;

// Author Alex McVittie, SkyWatch Space Applications Inc. December 2023


import org.esa.snap.core.jexp.ParseException;
import org.esa.stac.internal.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class StacClient implements STACUtils {

    private DownloadModifier downloadModifier;

    private boolean signDownloads;

    public String title;

    private StacCatalog catalog;

    private final String stacURL;

    public StacClient(final String catalogURL) throws Exception {
        this.stacURL = catalogURL;
        this.catalog = new StacCatalog(catalogURL);
        if(catalogURL.contains("planetarycomputer")){
            this.downloadModifier = EstablishedModifiers.planetaryComputer();
            this.signDownloads = true;
        }else{
            this.signDownloads = false;
        }
    }

    public StacClient(final String catalogURL, DownloadModifier modifierFunction) throws Exception {
        this.stacURL = catalogURL;
        this.catalog = new StacCatalog(catalogURL);
        this.downloadModifier = modifierFunction;
        this.signDownloads = true;
    }



    public StacCatalog getCatalog(){
        return this.catalog;
    }

    // Performs a search and returns an array of STAC Items from the server that match
    // the search
    public StacItem[] search(String [] collections, double [] bbox, String datetime) throws Exception {
        String searchEndpoint = stacURL + "/search?";
        String bboxStr = bbox[0] + "," + bbox[1] + "," + bbox[2] + "," + bbox[3];
        String validCollections = "";
        for (String collectionName : collections){
            if (this.catalog.containsCollection(collectionName)){
                validCollections = validCollections + collectionName + ",";
            }
        }
        if (Objects.equals(validCollections, "")){
            return new StacItem[0];
        }
        validCollections = validCollections.substring(0, validCollections.length() - 1);
        String query = searchEndpoint + "collections=" + validCollections +
                "&bbox=" + bboxStr +
                "&datetime=" + datetime;
        JSONObject queryResults = getJSONFromURL(query);
        JSONArray jsonFeatures =  getAllFeatures(queryResults);
        StacItem [] items = new StacItem[jsonFeatures.size()];
        for (int x = 0; x < jsonFeatures.size(); x++){
            items[x] = new StacItem((JSONObject) jsonFeatures.get(x));
        }
        return items;
    }

    // Search using a defined GeoJSON polygon.
    // TODO implement.
    public void search(String [] collections, JSONObject intersects, String datetime){

    }

    private String signURL(StacItem.StacAsset asset){
        if(signDownloads){
            return downloadModifier.signURL(asset.getURL());
        }else{
             return asset.getURL();
        }
    }

    public InputStream streamAsset(StacItem.StacAsset asset) throws IOException {
        String downloadURL = signURL(asset);
        InputStream inputStream = new URL(downloadURL).openStream();
        return inputStream;
    }

    public void downloadAsset(StacItem.StacAsset asset, File targetFolder){

        String outputFilename = asset.getFileName();

        System.out.println("Downloading asset " + asset.getId());
        String downloadURL = signURL(asset);
        try (BufferedInputStream in = new BufferedInputStream(new URL(downloadURL).openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(new File(targetFolder.getAbsolutePath(), outputFilename))) {
            byte dataBuffer[] = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        } catch (IOException e) {
            // handle exception
        }
    }

    public void downloadItem(StacItem item, File targetFolder){
        File outputFolder = new File(targetFolder, item.getId());
        outputFolder.mkdirs();
        System.out.println("Downloading STAC item " + item.getId() + " to directory " + outputFolder.getAbsolutePath());
        for (String s : item.listAssetIds()){
            StacItem.StacAsset curAsset = item.getAsset(s);
            downloadAsset(curAsset, outputFolder);
        }
    }
}
