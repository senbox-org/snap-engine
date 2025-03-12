package org.esa.snap.dem.dataio.copernicus;

import org.esa.snap.core.dataop.downloadable.DownloadStatusManager;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CopernicusDownloader {

    private static final String s3_prefix_30m = "https://copernicus-dem-30m.s3.eu-central-1.amazonaws.com";
    private static final String s3_prefix_90m = "https://copernicus-dem-90m.s3.eu-central-1.amazonaws.com";

    private final File installDir;


    public CopernicusDownloader(File installDir) {
        this.installDir = installDir;
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
        String download_path = "";
        String target_filename = "";

        int latRounded = (int) lat;
        int lonRounded = (int) lon;

        if (resolution == 30) {
            String name = createTileFilename(latRounded, lonRounded, "10");
            download_path = s3_prefix_30m + "/" + name + "/" + name + ".tif";
            target_filename = name + ".tif";
        } else {
            String name = createTileFilename(latRounded, lonRounded, "30");
            download_path = s3_prefix_90m + "/" + name + "/" + name + ".tif";
            target_filename = name + ".tif";
        }
        //System.out.println("Downloading " + download_path + " to fulfill search of area " + lat + ", " + lon + " at specified resolution " + resolution);

        DownloadStatusManager statusManager = DownloadStatusManager.getInstance();
        statusManager.setDownloading(true, target_filename);

        try (BufferedInputStream is = new BufferedInputStream(new URL(download_path).openStream())) {;
            final Path installDirPath = Paths.get(installDir);
            if (Files.notExists(installDirPath)) {
                Files.createDirectories(installDirPath);
            }
            try (FileOutputStream fileOutputStream = new FileOutputStream(installDir + "/" + target_filename)) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            statusManager.setDownloading(false, "");
            throw new FileNotFoundException("Tile does not exist");
        } finally {
            statusManager.setDownloading(false, "");
        }
        return true;
    }

}
