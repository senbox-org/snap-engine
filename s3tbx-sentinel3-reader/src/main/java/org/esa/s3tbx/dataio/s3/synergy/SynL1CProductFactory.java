package org.esa.s3tbx.dataio.s3.synergy;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.s3tbx.dataio.s3.AbstractProductFactory;
import org.esa.s3tbx.dataio.s3.Manifest;
import org.esa.s3tbx.dataio.s3.Sentinel3ProductReader;
import org.esa.s3tbx.dataio.s3.util.S3NetcdfReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.BasicPixelGeoCoding;
import org.esa.snap.core.datamodel.GeoCodingFactory;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.datamodel.SceneRasterTransform;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.ProductUtils;

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
        fileNames.add("GEOLOCATION_REF.nc");
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
                final Band sourceBand = sourceProduct.getBand(sourceBandName);
                MultiLevelImage sourceImage;
                if (sourceBandNames.size() > 1) {
                    final MultiLevelImage[] sourceImages = new MultiLevelImage[sourceBandNames.size()];
                    for (int i = 0; i < sourceImages.length; i++) {
                        sourceImages[i] = sourceProduct.getBand(sourceBandNames.get(i)).getSourceImage();
                    }
                    sourceImage = CameraImageMosaic.create(sourceImages);
                } else {
                    sourceImage = sourceBand.getSourceImage();
                }
                Band targetBand = new Band(targetBandName, sourceBand.getDataType(),
                                           sourceImage.getWidth(), sourceImage.getHeight());
                ProductUtils.copyRasterDataNodeProperties(sourceBand, targetBand);
                targetProduct.addBand(targetBand);
                //todo change this later
                targetBand.setNoDataValueUsed(false);
                targetBand.setValidPixelExpression("");
                targetBand.setSourceImage(sourceImage);
                configureTargetNode(sourceBand, targetBand);
                mapping.put(sourceBand.getName(), targetBand.getName());
            }
            copyMasks(targetProduct, sourceProduct, mapping);
        }
        addCameraIndexBand(targetProduct, masterProduct.getSceneRasterWidth());
    }

    @Override
    protected void setSceneRasterTransforms(Product product) {
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            final String bandName = band.getName();
            if (bandName.startsWith("OLC_RADIANCE") && !bandName.contains("17")) {
                int end = 16;
                if (bandName.substring(15, 16).equals("_")) {
                    end = 15;
                }
                final String identifier = bandName.substring(14, end);
                final Band rowMisregistrationBand = product.getBand("MISREGIST_OLC_Oref_O" + identifier + "_delta_row");
                final Band columnMisregistrationBand = product.getBand("MISREGIST_OLC_Oref_O" + identifier + "_delta_col");
                final SceneRasterTransform sceneRasterTransform =
                        new SynL1COlciSceneRasterTransform(columnMisregistrationBand, rowMisregistrationBand);
                band.setSceneRasterTransform(sceneRasterTransform);
            } else if (bandName.startsWith("SLST_NAD_RADIANCE")) {
                final String identifier = bandName.substring(19, 20);
                final Band columnCorrespondenceBand = product.getBand("MISREGIST_SLST_NAD_Oref_S" + identifier + "_col_corresp");
                final Band rowCorrespondenceBand = product.getBand("MISREGIST_SLST_NAD_Oref_S" + identifier + "_row_corresp");
                final SceneRasterTransform sceneRasterTransform =
                        new SynL1CSlstrSceneRasterTransform(columnCorrespondenceBand, rowCorrespondenceBand);
                band.setSceneRasterTransform(sceneRasterTransform);
            } else if (bandName.startsWith("SLST_NAD_BT_S")) {
                final String identifier = bandName.substring(13, 14);
                final Band columnCorrespondenceBand = product.getBand("MISREGIST_SLST_NAD_Oref_S" + identifier + "_col_corresp");
                final Band rowCorrespondenceBand = product.getBand("MISREGIST_SLST_NAD_Oref_S" + identifier + "_row_corresp");
                final SceneRasterTransform sceneRasterTransform =
                        new SynL1CSlstrSceneRasterTransform(columnCorrespondenceBand, rowCorrespondenceBand);
                band.setSceneRasterTransform(sceneRasterTransform);
            } else if (bandName.startsWith("SLST_NAD_BT_F")) {
                final String identifier = bandName.substring(13, 14);
                final Band columnCorrespondenceBand = product.getBand("MISREGIST_SLST_NAD_Oref_F" + identifier + "_col_corresp");
                final Band rowCorrespondenceBand = product.getBand("MISREGIST_SLST_NAD_Oref_F" + identifier + "_row_corresp");
                final SceneRasterTransform sceneRasterTransform =
                        new SynL1CSlstrSceneRasterTransform(columnCorrespondenceBand, rowCorrespondenceBand);
                band.setSceneRasterTransform(sceneRasterTransform);
            }
        }
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
        final File file = new File(getInputFileParentDirectory(), fileName);
        if (!file.exists()) {
            return null;
        }
        final S3NetcdfReader synNetcdfReader = SynNetcdfReaderFactory.createSynNetcdfReader(file);
        return synNetcdfReader.readProduct();
    }

    @Override
    protected void setGeoCoding(Product targetProduct) throws IOException {
        final ProductNodeGroup<Band> bandGroup = targetProduct.getBandGroup();
        if (bandGroup.contains("GEOLOCATION_REF_latitude") && bandGroup.contains("GEOLOCATION_REF_longitude")) {
            final BasicPixelGeoCoding pixelGeoCoding =
                    GeoCodingFactory.createPixelGeoCoding(bandGroup.get("GEOLOCATION_REF_latitude"),
                                                          bandGroup.get("GEOLOCATION_REF_longitude"), "", 5);
            targetProduct.setSceneGeoCoding(pixelGeoCoding);
        }
    }

}
