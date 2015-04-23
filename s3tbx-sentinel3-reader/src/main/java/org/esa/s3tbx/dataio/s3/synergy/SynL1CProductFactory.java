package org.esa.s3tbx.dataio.s3.synergy;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s3tbx.dataio.s3.AbstractProductFactory;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.IndexCoding;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.util.ProductUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Tonio Fincke
 */
public class SynL1CProductFactory extends AbstractProductFactory {

    public SynL1CProductFactory(Sentinel3ProductReader productReader) {
        super(productReader);
    }

    @Override
    protected List<String> getFileNames(Manifest manifest) {
        final List<String> manifestFileNames = manifest.getFileNames("M");
        final List<String> fileNames = new ArrayList<>();
        for (String manifestFileName : manifestFileNames) {
            if (manifestFileName.contains("/")) {
                manifestFileName = manifestFileName.substring(manifestFileName.lastIndexOf("/") + 1);
            }
            if (!manifestFileName.equals("MISREGIST_OLC_Oref_O17.nc") && !manifestFileName.contains("QUALITY_INFO")) {
                fileNames.add(manifestFileName);
            }
        }
        return fileNames;
    }

    @Override
    protected int getSceneRasterWidth(Product masterProduct) {
        return masterProduct.getSceneRasterWidth() * 5;
    }

    @Override
    protected void addDataNodes(Product masterProduct, Product targetProduct) throws IOException {
        for (final Product sourceProduct : getOpenProductList()) {
            final Map<String, String> mapping = new HashMap<String, String>();
            final Map<String, List<String>> partition = Partitioner.partition(sourceProduct.getBandNames(), "_CAM");

            for (final Map.Entry<String, List<String>> entry : partition.entrySet()) {
                final String targetBandName = sourceProduct.getName() + "_" + entry.getKey();
                final List<String> sourceBandNames = entry.getValue();
                final String sourceBandName = sourceBandNames.get(0);
                final Band targetBand = ProductUtils.copyBand(sourceBandName, sourceProduct, targetBandName,
                                                              targetProduct, false);
                //todo change this later
                targetBand.setNoDataValueUsed(false);
                targetBand.setValidPixelExpression("");
                final MultiLevelImage[] sourceImages = new MultiLevelImage[sourceBandNames.size()];
                for (int i = 0; i < sourceImages.length; i++) {
                    sourceImages[i] = sourceProduct.getBand(sourceBandNames.get(i)).getSourceImage();
                }
                targetBand.setSourceImage(CameraImageMosaic.create(sourceImages));
                final Band sourceBand = sourceProduct.getBand(sourceBandName);
                configureTargetNode(sourceBand, targetBand);
                mapping.put(sourceBand.getName(), targetBand.getName());
            }
            copyMasks(targetProduct, sourceProduct, mapping);
        }
        addCameraIndexBand(targetProduct, masterProduct.getSceneRasterWidth());
    }

    private void addCameraIndexBand(Product targetProduct, int cameraImageWidth) {
        final int sceneRasterWidth = targetProduct.getSceneRasterWidth();
        final int sceneRasterHeight = targetProduct.getSceneRasterHeight();
        StringBuilder expression = new StringBuilder();
        int width = 0;
        for (int i = 0; i < 4; i++) {
            width += cameraImageWidth;
            expression.append("X < ").append(width).append(" ? ");
            expression.append(i);
            expression.append(" : ");
            if (i == 3) {
                expression.append(i + 1);
            }
        }
        Band cameraIndexBand = new VirtualBand("Camera_Index", ProductData.TYPE_INT8,
                                               sceneRasterWidth, sceneRasterHeight, expression.toString());
        targetProduct.addBand(cameraIndexBand);
        IndexCoding indexCoding = new IndexCoding("Camera_Index");
        for (int i = 0; i < 5; i++) {
            final String description = "Images from camera " + i;
            indexCoding.addIndex("Camera_Index_" + (i + 1), i, description);
        }

        cameraIndexBand.setSampleCoding(indexCoding);
        targetProduct.getIndexCodingGroup().add(indexCoding);
    }

    @Override
    protected Band addBand(Band sourceBand, Product targetProduct) {
        final String sourceBandName = sourceBand.getName();
        sourceBand.setName(sourceBand.getProduct().getName() + "_" + sourceBandName);
        return super.addBand(sourceBand, targetProduct);
    }

    @Override
    protected Product readProduct(String fileName) throws IOException {
//        if (!fileName.startsWith("MISREGIST")) {
//            return super.readProduct(fileName);
//        }
        final File file = new File(getInputFileParentDirectory(), fileName);
        final S3NetcdfReader synNetcdfReader = SynNetcdfReaderFactory.createSynNetcdfReader(file);
        return synNetcdfReader.readProduct();
    }

}
