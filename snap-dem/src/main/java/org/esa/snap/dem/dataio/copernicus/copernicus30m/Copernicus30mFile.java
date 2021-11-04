package org.esa.snap.dem.dataio.copernicus.copernicus30m;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.ElevationFile;
import java.io.*;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.dem.dataio.copernicus.CopernicusDownloader;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationModel;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationTile;

public class Copernicus30mFile extends ElevationFile {

    private final CopernicusElevationModel demModel;

    public Copernicus30mFile(CopernicusElevationModel copernicusElevationModel, File localFile, ProductReader reader) {
        super(localFile, reader);
        demModel = copernicusElevationModel;
    }

    @Override
    protected ElevationTile createTile(final Product product) throws IOException {
        final CopernicusElevationTile tile = new CopernicusElevationTile(demModel, product);
        tile.setHeight(product.getSceneRasterHeight());
        tile.setWidth(product.getSceneRasterWidth());
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
            //e.printStackTrace();
            remoteFileExists = false;
            return false;
        }
    }
}
