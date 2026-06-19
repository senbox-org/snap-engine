package org.esa.snap.dem.dataio.copernicus.copernicus30m;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.multilevel.MultiLevelImage;
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
import org.esa.snap.core.gpf.internal.TileImpl;
import eu.esa.snap.core.util.ProgressMonitorContext;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.dem.dataio.copernicus.CopernicusDownloader;
import org.esa.snap.dem.dataio.copernicus.CopernicusDirectElevationTile;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationModel;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationTile;
import org.esa.snap.dem.dataio.copernicus.GeoTiffCopernicusTileSource;
import org.esa.snap.dem.dataio.copernicus.CopernicusNoDataElevationTile;
import org.esa.snap.runtime.Config;

import java.util.concurrent.CancellationException;

public class Copernicus30mFile extends ElevationFile {

    private final CopernicusElevationModel demModel;

    public Copernicus30mFile(CopernicusElevationModel copernicusElevationModel, File localFile, ProductReader reader) {
        super(localFile, reader);
        demModel = copernicusElevationModel;
    }

    @Override
    protected void getLocalFile() throws IOException {
        if (Config.instance().preferences().getBoolean("snap.dem.copernicus.directReader", true) && localFile.exists()) {
            try {
                tile = createDirectTile(localFile);
                demModel.updateCache(tile);
                return;
            } catch (IOException e) {
                SystemUtils.LOG.fine("Falling back to ProductReader for " + localFile.getName() + ": " + e.getMessage());
            }
        }
        super.getLocalFile();
    }

    private ElevationTile createDirectTile(final File file) throws IOException {
        final int[] tileIndices = getTileIndices(file);
        return new CopernicusDirectElevationTile(demModel, GeoTiffCopernicusTileSource.createSources(file),
                                                 3600, 3600, tileIndices[0], tileIndices[1]);
    }

    private static int[] getTileIndices(final File file) {
        final String[] fileNameSplit = file.getName().split("_");
        final int minLat = parseSignedTileCoordinate(fileNameSplit[4]);
        final int minLon = parseSignedTileCoordinate(fileNameSplit[6]);
        return new int[]{minLon + 180, 89 - minLat};
    }

    private static int parseSignedTileCoordinate(final String token) {
        final int value = Integer.parseInt(token.substring(1));
        return token.startsWith("S") || token.startsWith("W") ? -value : value;
    }

    @Override
    protected ElevationTile createTile(final Product product) throws IOException {

        if (product.getSceneRasterWidth() != product.getSceneRasterHeight()){

            final int sourceWidth = product.getSceneRasterWidth();
            final int sourceHeight = product.getSceneRasterHeight();
            final int targetWidth = 3600;
            final int targetHeight = 3600;
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
        return getRemoteFile(ProgressMonitorContext.getCurrentProgressMonitor());
    }

    @Override
    protected Boolean getRemoteFile(ProgressMonitor progressMonitor) {
        try {
            String [] fileNameSplit = localFile.getName().split("_");
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

            boolean downloaded = downloadTiles(lat, lon, 30, progressMonitor);
            if (!downloaded) {
                cacheNoDataTile();
            }
            return downloaded;
        } catch (CancellationException e) {
            throw e;
        } catch (Exception e) {
            //e.printStackTrace();
            cacheNoDataTile();
            return false;
        }
    }

    protected boolean downloadTiles(int lat, int lon, int resolution, ProgressMonitor progressMonitor) throws Exception {
        CopernicusDownloader downloader = new CopernicusDownloader(localFile.getParentFile());
        return downloader.downloadTiles(lat, lon, resolution, progressMonitor);
    }

    protected ElevationTile createNoDataTile() {
        return new CopernicusNoDataElevationTile(demModel.getDescriptor().getNoDataValue());
    }

    private void cacheNoDataTile() {
        remoteFileExists = false;
        tile = createNoDataTile();
    }
}
