package org.esa.snap.dem.dataio.copernicus90m;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.ElevationFile;

import java.io.*;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Stream;
import java.util.zip.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.dem.dataio.srtm1_hgt.SRTM1HgtElevationModel;
import org.esa.snap.dem.dataio.srtm1_hgt.SRTM1HgtElevationTile;

public class CopernicusFile extends ElevationFile {

    private File tarFile;
    private Product copernicusProduct;
    private final CopernicusElevationModel demModel;

    public CopernicusFile(CopernicusElevationModel copernicusElevationModel, File localFile, ProductReader reader) throws IOException {
        super(localFile, reader);
        demModel = copernicusElevationModel;
        CopernicusDownloader.validateCredentials();


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
            String north = fileNameSplit[3];
            String east =  fileNameSplit[5];
            int lat = Integer.parseInt(north.substring(1));
            int lon = Integer.parseInt(east.substring(1));
            if(east.startsWith("W")){
                lon *= -1;
            }
            if(north.startsWith("S")){
                lat *= -1;
            }

            CopernicusDownloader downloader = new CopernicusDownloader(localFile.getParentFile());

            return downloader.downloadTiles(lon, lat);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    protected void readData() throws IOException {
        CopernicusGeoTIFFReaderPlugIn readerPlugin = new CopernicusGeoTIFFReaderPlugIn();
        CopernicusGeoTIFFReader reader = (CopernicusGeoTIFFReader) readerPlugin.createReaderInstance();

        copernicusProduct = reader.readProductNodes(localFile, null);
    }

    protected static String getNameFromTarFile(final File tarFile) throws IOException {
        String returnString = "";
        TarArchiveInputStream tarInputStream  = new TarArchiveInputStream(new FileInputStream(tarFile));
        ArchiveEntry e = tarInputStream.getNextEntry();
        while( e != null){
            if (e.getName().endsWith(".tif") && e.getName().contains("DEM/")){
                returnString = e.getName().split("/")[e.getName().split("/").length - 1];
                break;
            }
            e = tarInputStream.getNextEntry();
        }
        tarInputStream.close();
        returnString = returnString.replace(".tif", ".tar");
        return returnString;
    }

    //Renames a directory of copernicus tar files to the proper name for indexing
    public static void renameDirectory(String directory) throws IOException {
        Collection<File> files = FileUtils.listFiles(new File(directory), null, true);
        for(File file2 : files){
            if(file2.getName().endsWith(".tar")){
                String newname = getNameFromTarFile(file2);
                String oldname = file2.getName();
                file2.renameTo(new File(file2.getAbsolutePath().replace(oldname, newname)));
            }
        }
    }







}
