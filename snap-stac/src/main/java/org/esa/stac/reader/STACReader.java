package org.esa.stac.reader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.*;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.dataio.geotiff.GeoTiffImageReader;
import org.esa.snap.dataio.geotiff.GeoTiffProductReader;
import org.esa.stac.internal.StacItem;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

public class STACReader extends GeoTiffProductReader {

    // Data structure to map band name to remote URL
    HashMap<String, URL> bandDataMap = new HashMap<>();
    private ProductReaderPlugIn readerPlugIn;
    private GeoTiffImageReader geoTiffImageReader;

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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        StacItemToProduct converter = new StacItemToProduct(item);

        Product stacProduct = new Product(item.getId(), "Optical");
        STACMetadataFactory metadataFactory = new STACMetadataFactory(item);
        stacProduct.getMetadataRoot().addElement(metadataFactory.generate());

        for (Band b : converter.getBandList()){
            stacProduct.addBand(b);
            String url = converter.getDataURL(b.getName());

        }
        return stacProduct;
    }

    @Override
    protected Product readProductNodesImpl() throws IOException{

        if (this.geoTiffImageReader != null) {
            throw new IllegalStateException("There is already an image reader.");
        }
        boolean success = false;
        try {
            Object productInput = super.getInput(); // invoke the 'getInput' method from the parent class
            ProductSubsetDef subsetDef = super.getSubsetDef(); // invoke the 'getSubsetDef' method from the parent class

            Path productPath = null;
            if (productInput instanceof String) {
                productPath = new File((String) productInput).toPath();
                this.geoTiffImageReader = GeoTiffImageReader.buildGeoTiffImageReader(productPath);
            } else if (productInput instanceof File) {
                productPath = ((File) productInput).toPath();
                this.geoTiffImageReader = GeoTiffImageReader.buildGeoTiffImageReader(productPath);
            } else if (productInput instanceof Path) {
                productPath = (Path) productInput;
                this.geoTiffImageReader = GeoTiffImageReader.buildGeoTiffImageReader(productPath);
            } else if (productInput instanceof InputStream) {
                this.geoTiffImageReader = new GeoTiffImageReader((InputStream) productInput, null);
            } else {
                throw new IllegalArgumentException("Unknown input '" + productInput + "'.");
            }

            String defaultProductName = null;
            if (productPath != null) {
                defaultProductName = FileUtils.getFilenameWithoutExtension(productPath.getFileName().toString());
            }

            Product product = readProduct(this.geoTiffImageReader, defaultProductName, subsetDef);
            if (productPath != null) {
                product.setFileLocation(productPath.toFile());
            }

            success = true;

            return product;
        } catch (RuntimeException | IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException(exception);
        } finally {
            if (!success) {
                //closeResources();
            }
        }






       // return null;
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
