package org.esa.s3tbx.dataio.s3.aatsr;

import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.slstr.SlstrLevel1ProductFactory;
import org.esa.s3tbx.dataio.s3.util.MetTxReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Sabine Embacher
 */
public class AatsrLevel1ProductFactory extends SlstrLevel1ProductFactory {

    private Product masterProduct;
    private Manifest manifest;

    // todo ideas+ implement valid Expression
//    private final static String validExpression = "!quality_flags.invalid";

    public AatsrLevel1ProductFactory(AatsrLevel1ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        this.manifest = manifest;
        final List<String> fileNames = manifest.getFileNames(new String[0]);
        fileNames.sort(String::compareTo);
        return fileNames;
    }

    @Override
    protected Product readProduct(String fileName, Manifest manifest) throws IOException {
        final File file = new File(getInputFileParentDirectory(), fileName);
        if (!file.exists()) {
            return null;
        }
        final S3NetcdfReader reader;
        if (fileName.equals("met_tx.nc")) {
            reader = new MetTxReader();
        } else {
            reader = new AatsrS3NetcdfReader();
            addSeparatingDimensions(reader.getSuffixesForSeparatingDimensions());
        }
        return reader.readProductNodes(file, null);
    }

    @Override
    protected Product findMasterProduct() {
        if (masterProduct != null) {
            return masterProduct;
        }
        masterProduct = findMasterProduct(getOpenProductList());
        return masterProduct;
    }

    @Override
    protected RasterDataNode copyTiePointGrid(Band sourceBand, Product targetProduct, double sourceStartOffset, double sourceTrackOffset, short[] sourceResolutions) {
        if (sourceBand.getRasterHeight() > 1) {
            return super.copyTiePointGrid(sourceBand, targetProduct, sourceStartOffset, sourceTrackOffset, sourceResolutions);  //Todo change body of created method. Use File | Settings | File Templates to change
        }
        // todo ideas+
        return null;
    }

    @Override
    protected void setUncertaintyBands(Product product) {
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final String bandName = band.getName();
            final int last_ = bandName.lastIndexOf("_");
            final String uncertaintyBandName = bandName.substring(0, last_) + "_uncert" + bandName.substring(last_);
            if (product.containsBand(uncertaintyBandName)) {
                final Band uncertaintyBand = product.getBand(uncertaintyBandName);
                band.addAncillaryVariable(uncertaintyBand, "uncertainty");
                addUncertaintyImageInfo(uncertaintyBand);
            }
        }
    }

    @Override
    protected String getProductSpecificMetadataElementName() {
        return "atsrProductInformation";
    }

    @Override
    protected void setAutoGrouping(Product[] sourceProducts, Product targetProduct) {
        String bandGrouping = getAutoGroupingString(sourceProducts);
        targetProduct.setAutoGrouping("S*_radiance*_in:S*_BT*_in:" +
                                      "S*_radiance*_io:S*_BT*_io:" +
                                      "S*exception_in:S*exception_io:" +
                                      "x_*:y_*:" +
                                      "elevation:latitude:longitude:" +
                                      "specific_humidity:temperature_profile:" +
                                      bandGrouping);
    }

    static Product findMasterProduct(List<Product> openProductList) {
        int maxWidth = 0;
        int maxHeight = 0;
        Product masterProduct = null;
        for (Product product : openProductList) {
            final int width = product.getSceneRasterWidth();
            final int height = product.getSceneRasterHeight();
            if (width > maxWidth || height > maxHeight) {
                masterProduct = product;
                maxWidth = Math.max(maxWidth, width);
                maxHeight = Math.max(maxHeight, height);
            }
        }
        return masterProduct;
    }
}
