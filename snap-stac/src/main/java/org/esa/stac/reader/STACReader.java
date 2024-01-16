package org.esa.stac.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.jexp.ParseException;
import org.esa.stac.internal.StacItem;

import java.io.IOException;

public class STACReader implements ProductReader {

    private StacItem thisItem;

    public STACReader(ProductReaderPlugIn readerPlugIn) {

    }



    @Override
    public ProductReaderPlugIn getReaderPlugIn() {
        return null;
    }

    @Override
    public Object getInput() {
        return null;
    }

    @Override
    public ProductSubsetDef getSubsetDef() {
        return null;
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
            if (asset.getRole().equals("data")){
                Band b  = new Band(asset.bandData.commonName, ProductData.TYPE_INT16, asset.getWidth(), asset.getHeight());
                stacProduct.addBand(b);
            }

        }
        return stacProduct;
    }

    Product readProductNodesImpl(){
        return null;
    }


    @Override
    public void readBandRasterData(Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
