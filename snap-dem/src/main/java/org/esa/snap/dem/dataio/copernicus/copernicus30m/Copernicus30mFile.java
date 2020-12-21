package org.esa.snap.dem.dataio.copernicus.copernicus30m;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.ElevationFile;
import java.io.*;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;
import org.esa.snap.dem.dataio.copernicus.CopernicusDownloader;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationTile;

public class Copernicus30mFile extends ElevationFile {

    private final Copernicus30mElevationModel demModel;

    public Copernicus30mFile(Copernicus30mElevationModel copernicus30mElevationModel, File localFile, ProductReader reader) {
        super(localFile, reader);
        demModel = copernicus30mElevationModel;
    }

    @Override
    protected ElevationTile createTile(final Product product) throws IOException {
        final CopernicusElevationTile tile ;
        if (product.getSceneRasterWidth() != product.getSceneRasterHeight()){
            System.out.println("Rescaling the raster");
            ResamplingOp resampler = new ResamplingOp();
            resampler.setParameter("targetWidth", 3600);
            resampler.setParameter("targetHeight", 3600);
            resampler.setParameter("upsampling", "Nearest");
            resampler.setParameter("downsampling", "First");
            resampler.setParameter("flagDownsampling", "First");
            resampler.setSourceProduct(product);
            Product resampled = resampler.getTargetProduct();


            System.out.println("Size is now "+ resampled.getSceneRasterWidth() + " by " + resampled.getSceneRasterHeight());


            tile = new CopernicusElevationTile(demModel, resampled);

        }else{
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

            return downloader.downloadTiles(lat, lon,30);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }







}
