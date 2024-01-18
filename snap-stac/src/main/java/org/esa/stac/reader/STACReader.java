package org.esa.stac.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.*;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.dataio.geotiff.GeoTiffProductReader;
import org.esa.stac.internal.StacItem;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

public class STACReader extends GeoTiffProductReader {

    // Data structure to map band name to remote URL
    HashMap<String, URL> bandDataMap = new HashMap<>();
    private ProductReaderPlugIn readerPlugIn;

    private StacItem thisItem;

    public STACReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        this.readerPlugIn = readerPlugIn;

    }
    public STACReader() {
        super(new STACReaderPlugIn());
        this.readerPlugIn = new STACReaderPlugIn();
    }

    @Override
    public ProductReaderPlugIn getReaderPlugIn() {
        return this.readerPlugIn;
    }


    @Override
    public Product readProductNodes(Object input, ProductSubsetDef subsetDef) throws IOException, IllegalFileFormatException {
        StacItem item = null;

        try{
            item = new StacItem(input);
        }catch(ParseException e){
            throw new IOException("Product is an invalid STAC item.");
        }

        Product stacProduct = new Product(item.getId(), "Optical");
        STACMetadataFactory metadataFactory = new STACMetadataFactory(item);
        stacProduct.getMetadataRoot().addElement(metadataFactory.generate());

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
                Band b  = new Band(name, ProductData.TYPE_INT16, asset.getWidth(), asset.getHeight());
                stacProduct.addBand(b);
                bandDataMap.put(name, new URL(asset.getURL()));
            }
        }
        return stacProduct;
    }

    @Override
    protected Product readProductNodesImpl() throws IOException{




        return null;
    }


    @Override
    public void readBandRasterData(Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {

    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
