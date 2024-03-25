package org.esa.stac.reader;

import org.esa.snap.core.dataio.*;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.dataio.geotiff.GeoTiffProductReader;
import org.esa.stac.internal.StacItem;

import java.io.IOException;

public class STACReader extends GeoTiffProductReader {

    private ProductReaderPlugIn readerPlugIn;

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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        StacItemToProduct converter = new StacItemToProduct(item);

        try {
            // Create product but do not stream band data. Just read in metadata and create empty bands.
            return converter.createProduct(false, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Product readProductNodesImpl() throws IOException{

        StacItem item = null;

        try{
            item = new StacItem(input);
        }catch(ParseException e){
            throw new IOException("Product is an invalid STAC item.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        StacItemToProduct converter = new StacItemToProduct(item);

        try {
            return converter.createProduct(false, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
