package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.util.SystemUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;

public class CopernicusDownloader {

    private static final String S3_PREFIX_30M = "https://copernicus-dem-30m.s3.eu-central-1.amazonaws.com";
    private static final String S3_PREFIX_90M = "https://copernicus-dem-90m.s3.eu-central-1.amazonaws.com";

    private final File installDir;

    public CopernicusDownloader(File downloadDir) {
        this.installDir = downloadDir;
    }

    public static String createTileFilename(int minLat, int minLon, String arcseconds) {
        final StringBuilder name = new StringBuilder("Copernicus_DSM_COG_" + arcseconds + "_");
        name.append(minLat < 0 ? "S" : "N");
        String latString = String.valueOf(Math.abs(minLat));
        while (latString.length() < 2) {
            latString = '0' + latString;
        }
        name.append(latString);

        name.append("_00_");

        name.append(minLon < 0 ? "W" : "E");
        String lonString = String.valueOf(Math.abs(minLon));
        while (lonString.length() < 3) {
            lonString = '0' + lonString;
        }
        name.append(lonString);
        name.append("_00_DEM");

        return name.toString();
    }

    public boolean downloadTiles(double lat, double lon, int resolution) throws Exception {
        String installDir = this.installDir.getAbsolutePath();
        String download_path;
        String target_filename;

        int latRounded = (int) lat;
        int lonRounded = (int) lon;

        if (resolution == 30) {
            String name = createTileFilename(latRounded, lonRounded, "10");
            download_path = S3_PREFIX_30M + "/" + name + "/" + name + ".tif";
            target_filename = name + ".tif";
        } else {
            String name = createTileFilename(latRounded, lonRounded, "30");
            download_path = S3_PREFIX_90M + "/" + name + "/" + name + ".tif";
            target_filename = name + ".tif";
        }
        SystemUtils.LOG.log(Level.FINE, String.format("Requested %s by point %s, %s at specified resolution %d", download_path, lat, lon, resolution));

        try{
            InputStream is = createInputStream(download_path);
            FileOutputStream fileOutputStream = new FileOutputStream(installDir + "/" + target_filename);
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            SystemUtils.LOG.log(Level.FINE, "Downloaded file");
        } catch (Exception e) {
            throw new FileNotFoundException("Tile does not exist");
        }
        return true;
    }

    InputStream createInputStream(String download_path) throws IOException {
        return new BufferedInputStream(new URL(download_path).openStream());
    }

}
