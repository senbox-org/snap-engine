package org.esa.stac.reader;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dataio.geotiff.GeoTiffImageReader;
import org.esa.snap.dataio.geotiff.GeoTiffProductReader;
import org.esa.snap.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.stac.StacClient;
import org.esa.stac.internal.StacItem;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Main class that handles converting a STAC Item to a SNAP Product class object.

public class StacItemToProduct {

    private final StacItem item;

    private final StacClient client;

    private int maxWidth = 0;
    private int maxHeight = 0;

    private List<Band> bandList;

    // Class to provide reader functionality, to convert a STAC Item to a SNAP Product object.
    public StacItemToProduct(StacItem item) throws MalformedURLException {
        this.item = item;
        this.client = item.getClient();
        bandList = createBandList();
    }
    public StacItemToProduct(StacItem item, StacClient client){
        this.item = item;
        this.client = client;
        bandList = createBandList();
        for (Band b : bandList){
            if (b.getRasterWidth() > maxWidth){
                maxWidth = b.getRasterWidth();
                maxHeight = b.getRasterHeight();
            }
        }
    }


    public InputStream streamBand(String bandName) throws IOException {
        return client.streamAsset(getAsset(bandName));
    }
    private Product createTifProduct(InputStream is) throws Exception {
        GeoTiffImageReader imageReader = new GeoTiffImageReader(is, null);
        GeoTiffProductReader productReader = new GeoTiffProductReader(new GeoTiffProductReaderPlugIn());
        Product singleBandProduct = productReader.readProduct(imageReader, "bandProduct");

        return singleBandProduct;
    }


    public Product createProduct() throws Exception{
        return createProduct(false, true);
    }

    public Product createProduct(boolean resampleBands, boolean streamData) throws Exception{

        Product product  = new Product(item.getId(), "Optical");

        // Create metadata object
        STACMetadataFactory factory = new STACMetadataFactory(item);
        MetadataElement originalMetadata = factory.generate();

        product.getMetadataRoot().addElement(originalMetadata);
        for (Band b : bandList){
            if(streamData){
                System.out.println("Streaming band " + b.getName());
                InputStream bandInputStream = streamBand(b.getName());
                Product singleBandProduct = createTifProduct(bandInputStream);
                ProductUtils.copyBand(singleBandProduct.getBands()[0].getName(), singleBandProduct, b.getName(), product, true);
                if (b.getRasterWidth() == maxWidth){
                    product.setSceneGeoCoding(singleBandProduct.getSceneGeoCoding());
                }
            }else{
                // Grab geocoding from single band
                if(product.getSceneGeoCoding() == null){
                    InputStream bandInputStream = streamBand(b.getName());
                    Product singleBandProduct = createTifProduct(bandInputStream);
                    product.setSceneGeoCoding(singleBandProduct.getSceneGeoCoding());
                }
                product.addBand(b);
            }

        }
        if(resampleBands && streamData){
            ResamplingOp resamplingOp = new ResamplingOp();
            resamplingOp.setParameter("targetWidth", maxWidth);
            resamplingOp.setParameter("targetHeight", maxHeight);
            resamplingOp.setParameter("upsamplingMethod", "Nearest");
            resamplingOp.setParameter("downsamplingMethod", "Mean");
            resamplingOp.setParameter("flagDownsamplingMethod", "First");
            resamplingOp.setSourceProduct(product);
            return resamplingOp.getTargetProduct();
        }else{
            return product;
        }



    }

    protected String getDataURL(String bandName){
        for (String assetID : item.listAssetIds()) {
            StacItem.StacAsset asset = item.getAsset(assetID);
            if (asset.getRole().equals("data") && ! assetID.equals("visual")){
                String name;
                if (asset.bandData == null){
                    name = asset.getTitle();
                }else{
                    name = asset.bandData.description;
                }
                if(name.equals(bandName)){
                    return asset.getURL();
                }
            }
        }
        return null;
    }
    protected StacItem.StacAsset getAsset(String bandName){
        for (String assetID : item.listAssetIds()) {
            StacItem.StacAsset asset = item.getAsset(assetID);
            if (asset.getRole().equals("data") && ! assetID.equals("visual")){
                String name;
                if (asset.bandData == null){
                    name = asset.getTitle();
                }else{
                    name = asset.bandData.description;
                }
                name = name  + " (" + assetID + ")";
                if(name.equals(bandName)){
                    return asset;
                }
            }
        }
        return null;
    }

    private List<Band> createBandList(){
        List<Band> bandList = new ArrayList<>();

        for (String assetID : item.listAssetIds()){
            StacItem.StacAsset asset = item.getAsset(assetID);

            // We only want to process data assets and avoid preview assets.
            if (asset.getRole().equals("data") && ! assetID.equals("visual")){
                String name;
                if (asset.bandData == null){
                    name = asset.getTitle();
                }else{
                    name = asset.bandData.description;
                }
                name = name + " (" + assetID + ")";
                Band b  = new Band(name, ProductData.TYPE_INT16, asset.getWidth(), asset.getHeight());
                bandList.add(b);
            }
        }
        return bandList;

    }

    protected  List<Band> getBandList(){
        return this.bandList;
    }


}
