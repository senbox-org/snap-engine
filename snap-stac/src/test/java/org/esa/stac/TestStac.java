package org.esa.stac;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.jexp.ParseException;
import org.esa.stac.internal.EstablishedModifiers;
import org.esa.stac.internal.StacCatalog;
import org.esa.stac.internal.StacItem;
import org.esa.stac.reader.STACMetadataFactory;
import org.esa.stac.reader.STACReader;
import org.esa.stac.reader.STACReaderPlugIn;
import org.esa.stac.reader.StacItemToProduct;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

public class TestStac {
    @Test
    public void testStac() throws Exception {
        StacClient client = new StacClient(
                "https://planetarycomputer.microsoft.com/api/stac/v1",
                EstablishedModifiers.planetaryComputer());

        StacCatalog catalog = client.getCatalog();
        System.out.println(catalog.getTitle());

        StacItem[] results = client.search(
                new String[]{"landsat-c2-l2"},
                new double[]{-124.2751,45.5469,-123.9613,45.7458},
                "2020-01-01/2022-11-05" );
        System.out.println(results.length);
        System.out.println(results[0].getURL());
        //System.out.println(results[0].getAsset("B08").getURL());

        //client.downloadItem(results[35], new File("/tmp"));
        STACMetadataFactory factory = new STACMetadataFactory(results[35]);
        MetadataElement element = factory.generate();
        STACReader reader = new STACReader();

        System.out.println(3);
    }

    @Test
    public void testReader() throws IOException {
        STACReaderPlugIn readerPlugIn = new STACReaderPlugIn();
        String remoteProduct = "https://planetarycomputer.microsoft.com/api/stac/v1/collections/sentinel-2-l2a/items/S2A_MSIL2A_20220828T190931_R056_T10TCR_20220830T153754";
        DecodeQualification q = readerPlugIn.getDecodeQualification(remoteProduct);
        System.out.println(q);
        ProductReader reader = readerPlugIn.createReaderInstance();
        Product p = reader.readProductNodes(remoteProduct, null);
        System.out.println();
    }

    @Test
    public void testCreateProduct() throws Exception {
        String remoteProduct = "https://planetarycomputer.microsoft.com/api/stac/v1/collections/landsat-c2-l2/items/LC09_L2SP_047028_20221031_02_T2";
        StacClient client = new StacClient("https://planetarycomputer.microsoft.com/api/stac/v1", EstablishedModifiers.planetaryComputer());
        StacItemToProduct converter = new StacItemToProduct(new StacItem(remoteProduct), client);
        Product p = converter.createProduct(false, true);
        ProductIO.writeProduct(p, "c:/tmp/test3.tif", "GeoTIFF");
        System.out.println(3);
    }


}
