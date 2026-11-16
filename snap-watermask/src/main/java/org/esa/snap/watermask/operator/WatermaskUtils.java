package org.esa.snap.watermask.operator;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.runtime.Config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

public class WatermaskUtils {

    private WatermaskUtils() {
    }

    /**
     * Computes the side length of the images to be generated for the given resolution.
     *
     * @param resolution The resolution.
     * @return The side length of the images to be generated.
     */
    public static int computeSideLength(int resolution) {
        final int pixelXCount = 40024000 / resolution;
        final int pixelXCountPerTile = pixelXCount / 360;
        // these two lines needed to create a multiple of 8
        final int temp = pixelXCountPerTile / 8;
        return temp * 8;
    }

    /**
     * Creates the name of the img file for the given latitude and longitude.
     *
     * @param lat latitude in degree
     * @param lon longitude in degree
     * @return the name of the img file
     */
    public static String createImgFileName(float lat, float lon) {
        final boolean geoPosIsWest = lon < 0;
        final boolean geoPosIsSouth = lat < 0;
        StringBuilder result = new StringBuilder();
        final String eastOrWest = geoPosIsWest ? "w" : "e";
        result.append(eastOrWest);
        int positiveLon = (int) Math.abs(Math.floor(lon));
        if (positiveLon >= 10 && positiveLon < 100) {
            result.append("0");
        } else if (positiveLon < 10) {
            result.append("00");
        }
        result.append(positiveLon);

        final String northOrSouth = geoPosIsSouth ? "s" : "n";
        result.append(northOrSouth);

        final int positiveLat = (int) Math.abs(Math.floor(lat));
        if (positiveLat < 10) {
            result.append("0");
        }
        result.append(positiveLat);
        result.append(".img");

        return result.toString();
    }

    // currently not used, as we do http only
//    public static boolean installRemoteFTPFiles(FtpDownloader ftpDownloader, final String remotePath) throws IOException {
//        try {
//            Map<String, Long> fileSizeMap = ftpDownloader.readRemoteFileListNoCache(remotePath);
//
//            for (String remoteFileName: fileSizeMap.keySet()) {
//                final Path localZipFile = WatermaskConstants.LOCAL_AUXDATA_PATH.toString().resolve(remoteFileName);
//                final Long fileSize = fileSizeMap.get(remoteFileName);
//
//                if (!Files.exists(localZipFile) || Files.size(localZipFile) != fileSize) {
//                    SystemUtils.LOG.fine("Downloading auxdata file '" + localZipFile.getFileName() + "' ...'");
//                    final FtpDownloader.FTPError result = ftpDownloader.retrieveFile(remotePath + remoteFileName, localZipFile, fileSize);
//                    if (result == FtpDownloader.FTPError.OK) {
//                        SystemUtils.LOG.fine("Downloaded file '" + localZipFile.getFileName() + "' to local auxdata path '" +
//                                                     WatermaskConstants.LOCAL_AUXDATA_PATH + "'.");
//                    } else {
//                        disposeFtp(ftpDownloader);
//                        Files.delete(localZipFile);
//                        final String message = "Mandatory auxdata file '" + localZipFile.getFileName() +
//                                "' could not be downloaded.";
//                        throw new OperatorException(message);
//                    }
//                } else {
//                    SystemUtils.LOG.fine("Auxdata file '" + localZipFile.getFileName() + "' found in local auxdata path '" +
//                                                 WatermaskConstants.LOCAL_AUXDATA_PATH + "'.");
//                }
//            }
//            return true;
//        } catch (SocketException e) {
//            throw e;
//        } catch (Exception e) {
//            SystemUtils.LOG.fine(e.getMessage());
//            disposeFtp(ftpDownloader);
//        }
//        return false;
//    }
//
//    private static void disposeFtp(FtpDownloader ftp) {
//        try {
//            if (ftp != null) {
//                ftp.disconnect();
//                ftp = null;
//            }
//        } catch (Exception ignore) {
//        }
//    }

    public static boolean  installRemoteHttpFiles(final String baseUrl) throws IOException {

        for (String remoteFileName: WatermaskConstants.AUXDATA_FILENAMES) {
            final Path localZipFile = WatermaskConstants.LOCAL_AUXDATA_PATH.resolve(remoteFileName);
            final String remotePath = baseUrl + localZipFile.getFileName();
            SystemUtils.LOG.fine("Checking for '" + localZipFile + "' ...");
            try {
                // avoid query for length if download is disabled
                Preferences preferences = Config.instance().preferences();
                if (preferences.getBoolean(GPF.SNAP_GPF_ALLOW_AUXDATA_DOWNLOAD, true)) {
                    final URL fileUrl = new URL(remotePath);
                    final URLConnection urlConnection = fileUrl.openConnection();
                    if (!Files.exists(localZipFile) || (Files.size(localZipFile) != urlConnection.getContentLength() &&
                                            urlConnection.getContentLength() >= 0)) {
                        SystemUtils.LOG.fine(localZipFile + " exists " + Files.exists(localZipFile) + " local length " + (Files.exists(localZipFile) ? Files.size(localZipFile) : 0) + " remote length " + urlConnection.getContentLength());
                        SystemUtils.LOG.fine("http retrieving " + remotePath);
                        downloadHttpFile(fileUrl, urlConnection, localZipFile);
                    } else {
                        SystemUtils.LOG.fine("Found '" + localZipFile.getFileName() + "'.");
                    }
                } else if (Files.exists(localZipFile)) {
                    SystemUtils.LOG.fine("Found '" + localZipFile.getFileName() + "'.");
                } else {
                    throw new IllegalAccessException("snap.gpf.allowAuxdataDownload is set to false");
                }
            } catch (Exception e) {
                final String message = "Mandatory auxdata file '" + localZipFile.getFileName() +
                        "' could not be downloaded: " + e.getMessage();
                throw new OperatorException(message);
            }
        }
        return true;
    }

    /**
     * Downloads a file from the specified URL to the specified local target directory.
     * The method uses a Swing progress monitor to visualize the download process.
     *
     * @param fileUrl      the URL of the file to be downloaded
     * @param localZipFile the target file
     * @return Path the downloaded file
     * @throws IOException if an I/O error occurs
     */
    private static Path downloadHttpFile(URL fileUrl, URLConnection urlConnection, final Path localZipFile) throws IOException {
        final Path outputFile = localZipFile.getParent().resolve(Path.of(fileUrl.getFile()).getFileName());


        final Path parentFolder = localZipFile.getParent();
        if (!Files.exists(parentFolder)) {
            IOHelper.createDirectories(parentFolder);
        }

        final int contentLength = urlConnection.getContentLength();
        final InputStream is = new BufferedInputStream(urlConnection.getInputStream(), contentLength);
        final OutputStream os;
        try {
            os = new BufferedOutputStream(Files.newOutputStream(outputFile));
        } catch (IOException e) {
            is.close();
            throw e;
        }

        try {
            final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.DATA_TRANSFER);
            status.beginTask("Downloading " + localZipFile.getFileName() + "... ", contentLength);

            final int size = 32768;
            final byte[] buf = new byte[size];
            int n;
            while ((n = is.read(buf, 0, size)) > -1) {
                os.write(buf, 0, n);
                status.worked(n);
            }
            status.done();

            while (true) {
                final int b = is.read();
                if (b == -1) {
                    break;
                }
                os.write(b);
            }
        } catch (IOException e) {
            Files.delete(outputFile);
            throw e;
        } finally {
            try {
                os.close();
            } finally {
                is.close();
            }
        }
        return outputFile;
    }


}
