package org.esa.snap.dem.dataio.copernicus;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataop.downloadable.DownloadStatusManager;
import eu.esa.snap.core.util.ProgressMonitorContext;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CancellationException;

public class CopernicusDownloader {

    private static final String DEFAULT_S3_PREFIX_30M = "https://copernicus-dem-30m.s3.eu-central-1.amazonaws.com";
    private static final String DEFAULT_S3_PREFIX_90M = "https://copernicus-dem-90m.s3.eu-central-1.amazonaws.com";

    private final File installDir;
    private final String s3Prefix30m;
    private final String s3Prefix90m;


    public CopernicusDownloader(File installDir) {
        this(installDir, DEFAULT_S3_PREFIX_30M, DEFAULT_S3_PREFIX_90M);
    }

    CopernicusDownloader(File installDir, String s3Prefix30m, String s3Prefix90m) {
        this.installDir = installDir;
        this.s3Prefix30m = s3Prefix30m;
        this.s3Prefix90m = s3Prefix90m;
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
        return downloadTiles(lat, lon, resolution, ProgressMonitorContext.getCurrentProgressMonitor());
    }

    public boolean downloadTiles(double lat, double lon, int resolution, ProgressMonitor progressMonitor) throws Exception {
        String installDir = this.installDir.getAbsolutePath();
        String download_path = "";
        String target_filename = "";

        int latRounded = (int) lat;
        int lonRounded = (int) lon;

        if (resolution == 30) {
            String name = createTileFilename(latRounded, lonRounded, "10");
            download_path = s3Prefix30m + "/" + name + "/" + name + ".tif";
            target_filename = name + ".tif";
        } else {
            String name = createTileFilename(latRounded, lonRounded, "30");
            download_path = s3Prefix90m + "/" + name + "/" + name + ".tif";
            target_filename = name + ".tif";
        }
        //System.out.println("Downloading " + download_path + " to fulfill search of area " + lat + ", " + lon + " at specified resolution " + resolution);

        DownloadStatusManager statusManager = DownloadStatusManager.getInstance();
        statusManager.setDownloading(true, target_filename);

        Path targetPath = Paths.get(installDir, target_filename);
        try (ProgressMonitorContext.Scope ignored = ProgressMonitorContext.use(progressMonitor)) {
            ProgressMonitorContext.checkCanceled(progressMonitor);
            final Path installDirPath = Paths.get(installDir);
            if (Files.notExists(installDirPath)) {
                Files.createDirectories(installDirPath);
            }

            final URLConnection urlConnection = new URL(download_path).openConnection();
            urlConnection.setConnectTimeout(30000);
            urlConnection.setReadTimeout(60000);
            try (BufferedInputStream is = new BufferedInputStream(urlConnection.getInputStream());
                 FileOutputStream fileOutputStream = new FileOutputStream(targetPath.toFile())) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                ProgressMonitorContext.checkCanceled(progressMonitor);
                while ((bytesRead = is.read(dataBuffer, 0, 1024)) != -1) {
                    ProgressMonitorContext.checkCanceled(progressMonitor);
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            }
        } catch (CancellationException e) {
            Files.deleteIfExists(targetPath);
            throw e;
        } catch (Exception e) {
            Files.deleteIfExists(targetPath);
            throw new FileNotFoundException("Tile does not exist");
        } finally {
            statusManager.setDownloading(false, "");
        }
        return true;
    }

}
