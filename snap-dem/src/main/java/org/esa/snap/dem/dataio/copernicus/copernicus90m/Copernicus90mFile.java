package org.esa.snap.dem.dataio.copernicus.copernicus90m;

import com.bc.ceres.multilevel.MultiLevelImage;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.dem.ElevationFile;

import java.awt.*;
import java.awt.image.Raster;
import java.io.*;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;
import org.esa.snap.core.gpf.internal.TileImpl;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.dem.dataio.copernicus.CopernicusDownloader;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationModel;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationTile;

public class Copernicus90mFile extends ElevationFile {

    private final CopernicusElevationModel demModel;

    public Copernicus90mFile(CopernicusElevationModel copernicusElevationModel, File localFile, ProductReader reader) {
        super(localFile, reader);
        demModel = copernicusElevationModel;
    }

    @Override
    protected ElevationTile createTile(final Product product) throws IOException {

        if (product.getSceneRasterWidth() != product.getSceneRasterHeight()){

            final int sourceWidth = product.getSceneRasterWidth();
            final int sourceHeight = product.getSceneRasterHeight();
            final int targetWidth = 1200;
            final int targetHeight = 1200;
            final double upSamplingFactor = (double)targetWidth / (double)sourceWidth;

            final Band sourceBand = product.getBandAt(0);
            final Rectangle sourceRectangle = new Rectangle(0, 0, sourceWidth, sourceHeight);
            final MultiLevelImage image = sourceBand.getSourceImage();
            final Raster awtRaster = image.getData(sourceRectangle);
            final Tile sourceTile = new TileImpl(sourceBand, awtRaster);
            final ProductData sourceData = sourceTile.getDataBuffer();

            final Product targetProduct = new Product(product.getName(), product.getProductType(), targetWidth, targetHeight);
            final Band targetBand = new Band(sourceBand.getName(), ProductData.TYPE_FLOAT32, targetWidth, targetHeight);
            final ProductData data = targetBand.createCompatibleRasterData(targetWidth, targetHeight);
            targetBand.setRasterData(data);
            targetProduct.addBand(targetBand);
            ProductUtils.copyProductNodes(product, targetProduct);

            for (int ty = 0; ty < targetHeight; ++ty) {
                double[] upSampledLine = new double[targetWidth];
                for (int tx = 0; tx < targetWidth; ++tx) {
                    final double sx = tx / upSamplingFactor;
                    final int sx0 = (int)sx;
                    final int sx1 = Math.min(sx0 + 1, sourceWidth - 1);
                    final int idx0 = sourceTile.getDataBufferIndex(sx0, ty);
                    final int idx1 = sourceTile.getDataBufferIndex(sx1, ty);
                    final double sv0 = sourceData.getElemDoubleAt(idx0);
                    final double sv1 = sourceData.getElemDoubleAt(idx1);
                    final double mu = sx - sx0;
                    upSampledLine[tx] = (1 - mu) * sv0 + mu * sv1;
                }
                targetBand.setPixels(0, ty, targetWidth, 1, upSampledLine);
            }

            tile = new CopernicusElevationTile(demModel, targetProduct);
            product.dispose();
        } else {
            tile = new CopernicusElevationTile(demModel, product);
        }
        demModel.updateCache(tile);
        return tile;
    }

    @Override
    protected Boolean getRemoteFile() {
        try {
            String [] fileNameSplit = localFile.getName().split("_");
            System.out.println(localFile.getName());
            String north = fileNameSplit[4];
            String east =  fileNameSplit[6];
            int lat = Integer.parseInt(north.substring(1));
            int lon = Integer.parseInt(east.substring(1));
            if(east.startsWith("W")){
                lon *= -1;
            }
            if(north.startsWith("S")){
                lat *= -1;
            }

            CopernicusDownloader downloader = new CopernicusDownloader(localFile.getParentFile());

            return downloader.downloadTiles(lat, lon, 90);
        } catch (Exception e) {
            //e.printStackTrace();
            remoteFileExists = false;
            return false;
        }
    }
}
