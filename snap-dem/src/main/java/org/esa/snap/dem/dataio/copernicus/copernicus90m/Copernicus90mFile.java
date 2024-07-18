package org.esa.snap.dem.dataio.copernicus.copernicus90m;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.ElevationFile;

import java.awt.*;
import java.io.*;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.core.gpf.common.SubsetOp;
import org.esa.snap.core.gpf.common.resample.ResamplingOp;
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

            ResamplingOp resampler = new ResamplingOp();
            resampler.setParameter("targetWidth", 1201);
            resampler.setParameter("targetHeight", 1201);
            resampler.setParameter("upsampling", "Bilinear");
            resampler.setParameter("downsampling", "First");
            resampler.setParameter("flagDownsampling", "First");
            resampler.setSourceProduct(product);
            Product resampled = resampler.getTargetProduct();
            product.getName();
            resampled.getBandAt(0).readRasterDataFully();

            SubsetOp subsetOp = new SubsetOp();
            subsetOp.setSourceProduct(resampled);
            subsetOp.setRegion(new Rectangle(1, 1, 1200, 1200));
            Product subsetProd = subsetOp.getTargetProduct();

            tile = new CopernicusElevationTile(demModel, subsetProd);

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
