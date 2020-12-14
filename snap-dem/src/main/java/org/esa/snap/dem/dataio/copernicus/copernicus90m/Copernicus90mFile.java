package org.esa.snap.dem.dataio.copernicus.copernicus90m;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.ElevationFile;
import java.io.*;
import java.util.Collection;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.dem.dataio.copernicus.CopernicusDownloader;
import org.esa.snap.dem.dataio.copernicus.CopernicusElevationTile;

public class Copernicus90mFile extends ElevationFile {

    private File tarFile;
    private Product copernicusProduct;
    private final Copernicus90mElevationModel demModel;

    public Copernicus90mFile(Copernicus90mElevationModel copernicus90mElevationModel, File localFile, ProductReader reader) throws IOException {
        super(localFile, reader);
        demModel = copernicus90mElevationModel;
        //CopernicusDownloader.validateCredentials();


    }
    @Override
    protected ElevationTile createTile(final Product product) throws IOException {
        final CopernicusElevationTile tile = new CopernicusElevationTile(demModel, product);
        demModel.updateCache(tile);
        return tile;
    }

    @Override
    protected Boolean getRemoteFile() throws IOException {
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
            e.printStackTrace();
            return false;
        }
    }







}
