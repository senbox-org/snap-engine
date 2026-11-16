/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.dataop.dem;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.downloadable.DownloadStatusManager;
import org.esa.snap.core.dataop.downloadable.FtpDownloader;
import org.esa.snap.core.dataop.downloadable.SSLUtil;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import eu.esa.snap.core.util.ProgressMonitorContext;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Holds information about a dem file.
 */
public abstract class ElevationFile {

    protected Path localFile;
    protected Path localZipFile;
    protected final ProductReader productReader;
    private boolean localFileExists = false;
    protected boolean remoteFileExists = true;
    private boolean errorInLocalFile = false;
    protected ElevationTile tile = null;
    private FtpDownloader ftp = null;
    private Map<String, Long> fileSizeMap = null;
    private boolean unrecoverableError = false;

    public ElevationFile(final Path localFile, final ProductReader reader) {
        this.localFile = localFile;
        this.localZipFile = localFile.getParent().resolve(
                FileUtils.getFilenameWithoutExtension(localFile) + ".zip");
        this.productReader = reader;
    }

    public void dispose() {
        try {
            if (ftp != null)
                ftp.disconnect();
            ftp = null;
            tile.dispose();
            tile = null;
        } catch (Exception e) {
            //
        }
    }

    public String getFileName() {
        return localFile.getFileName().toString();
    }

    public final ElevationTile getTile() throws IOException {
        return getTile(ProgressMonitorContext.getCurrentProgressMonitor());
    }

    public final ElevationTile getTile(ProgressMonitor progressMonitor) throws IOException {
        // A tile may have been disposed by BaseElevationModel#updateCache's LRU
        // eviction while this ElevationFile still holds a reference to it. Treat
        // the disposed reference as absent and reload, otherwise getSample() reads
        // through a Band whose backing Product is gone — yielding garbage.
        try (ProgressMonitorContext.Scope ignored = ProgressMonitorContext.use(progressMonitor)) {
            ProgressMonitorContext.checkCanceled(progressMonitor);
            if (tile == null || tile.isDisposed()) {
                tile = null;
                if (!remoteFileExists && !localFileExists)
                    return null;
                getFile(progressMonitor);
            }
            return tile;
        }
    }

    protected ElevationTile createTile(final Product product) throws IOException {
        return null;
    }

    protected boolean findLocalFile() {
        return (Files.exists(localFile) && Files.isRegularFile(localFile)) || (Files.exists(localZipFile) && Files.isRegularFile(localZipFile));
    }

    private synchronized void getFile(ProgressMonitor progressMonitor) throws IOException {
        try {
            if (tile != null) return;
            if (!localFileExists && !errorInLocalFile) {
                localFileExists = findLocalFile();
            }
            ProgressMonitorContext.checkCanceled(progressMonitor);
            if (localFileExists) {
                getLocalFile();
            } else if (remoteFileExists) {
                if (getRemoteFile(progressMonitor)) {
                    getLocalFile();
                }
            }
            if (tile != null) {
                errorInLocalFile = false;
            } else {
                if (!remoteFileExists && localFileExists) {
                    SystemUtils.LOG.severe("Unable to reader product " + localFile.toAbsolutePath());
                }
                localFileExists = false;
                errorInLocalFile = true;
            }
        } catch (IOException e) {
            SystemUtils.LOG.severe(e.getMessage());

            tile = null;
            localFileExists = false;
            errorInLocalFile = true;
            if(e.getCause() instanceof UnsupportedOperationException) {
                unrecoverableError = true;
            }
            if (unrecoverableError) {
                throw e;
            }
        }
    }

    protected void getLocalFile() throws IOException {
        Product product = null;
        if (Files.exists(localFile)) {
            product = productReader.readProductNodes(localFile, null);
        } else if(Files.exists(localZipFile)) {
            product = productReader.readProductNodes(localZipFile, null);
        }

        if (product != null) {
            tile = createTile(product);
        }
    }

    protected abstract Boolean getRemoteFile() throws IOException;

    protected Boolean getRemoteFile(ProgressMonitor progressMonitor) throws IOException {
        return getRemoteFile();
    }

    protected Boolean getRemoteHttpFile(final String baseUrl) throws IOException {
        return getRemoteHttpFile(baseUrl, ProgressMonitorContext.getCurrentProgressMonitor());
    }

    protected Boolean getRemoteHttpFile(final String baseUrl, ProgressMonitor progressMonitor) throws IOException {
        final String remotePath = baseUrl + localZipFile.getFileName();
        SystemUtils.LOG.info("http retrieving " + remotePath);
        try {
            downloadFile(new URL(remotePath), localZipFile, progressMonitor);

            return true;
        } catch (FileNotFoundException e) {
            // no need to alarm the user. Tiles may not be found because they are in the ocean or outside valid areas
            SystemUtils.LOG.warning("http error:" + e.getMessage() + " on " + remotePath);
            remoteFileExists = false;
        } catch (CancellationException e) {
            throw e;
        } catch (Exception e) {
            throw e;
        }
        return false;
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
    private static Path downloadFile(final URL fileUrl, final Path localZipFile) throws IOException {
        return downloadFile(fileUrl, localZipFile, ProgressMonitorContext.getCurrentProgressMonitor());
    }

    private static Path downloadFile(final URL fileUrl, final Path localZipFile, ProgressMonitor progressMonitor) throws IOException {
        SSLUtil sslUtil = new SSLUtil();
        sslUtil.disableSSLCertificateCheck();

        final Path outputFile = localZipFile.getParent().resolve(Path.of(fileUrl.getFile()).getFileName());
        final URLConnection urlConnection = fileUrl.openConnection();
        urlConnection.setConnectTimeout(30000);
        urlConnection.setReadTimeout(60000);
        final int contentLength = urlConnection.getContentLength();

        DownloadStatusManager statusManager = DownloadStatusManager.getInstance();
        statusManager.setDownloading(true, localZipFile.getFileName().toString());

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.DATA_TRANSFER);
        status.beginTask("Downloading " + localZipFile.getFileName() + "... ",
                         contentLength > 0 ? contentLength : ProgressMonitor.UNKNOWN);

        boolean deleteOutputFile = false;
        try (ProgressMonitorContext.Scope ignored = ProgressMonitorContext.use(progressMonitor)) {
            ProgressMonitorContext.checkCanceled(progressMonitor);
            try (final InputStream is = new BufferedInputStream(urlConnection.getInputStream())) {
                try (final OutputStream fileOS = Files.newOutputStream(outputFile)) {
                    try (final OutputStream os = new BufferedOutputStream(fileOS)) {

                        final int size = 32768;
                        final byte[] buf = new byte[size];
                        int n;
                        ProgressMonitorContext.checkCanceled(progressMonitor);
                        while ((n = is.read(buf, 0, size)) > -1) {
                            ProgressMonitorContext.checkCanceled(progressMonitor);
                            os.write(buf, 0, n);
                            if (contentLength > 0) {
                                status.worked(n);
                            }
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            deleteOutputFile = true;
            throw e;
        } finally {
            if (deleteOutputFile) {
                IOHelper.delete(outputFile);
            }
            status.done();
            sslUtil.enableSSLCertificateCheck();
            statusManager.setDownloading(false, "");
        }
        return outputFile;
    }

    protected Boolean getRemoteFTPFile(final String remoteFTP, final String remotePath) throws IOException {
        return getRemoteFTPFile(remoteFTP, remotePath, ProgressMonitorContext.getCurrentProgressMonitor());
    }

    protected Boolean getRemoteFTPFile(final String remoteFTP, final String remotePath, ProgressMonitor progressMonitor) throws IOException {
        try {
            if (ftp == null) {
                ftp = new FtpDownloader(remoteFTP);
                fileSizeMap = FtpDownloader.readRemoteFileList(ftp, remoteFTP, remotePath);
            }

            final String remoteFileName = localZipFile.getFileName().toString();
            final Long fileSize = fileSizeMap.get(remoteFileName);

            final FtpDownloader.FTPError result = ftp.retrieveFile(remotePath + remoteFileName, localZipFile, fileSize, progressMonitor);
            if (result == FtpDownloader.FTPError.OK) {
                return true;
            } else {
                if (result == FtpDownloader.FTPError.FILE_NOT_FOUND) {
                    remoteFileExists = false;
                } else {
                    dispose();
                }
                IOHelper.delete(localZipFile);
            }
            return false;
        } catch (SocketException e) {
            unrecoverableError = true;
            throw e;
        } catch (CancellationException e) {
            IOHelper.delete(localFile);
            throw e;
        } catch (Exception e) {
            SystemUtils.LOG.info(e.getMessage());

            if (ftp == null) {
                unrecoverableError = false;      // allow to continue
                remoteFileExists = false;
                throw new IOException("Failed to connect to FTP " + remoteFTP +
                        '\n' + e.getMessage());
            }
            dispose();
        }
        return false;
    }

    protected InputStream getZipInputStream(final Path dataFile) throws IOException {
        final String ext = FileUtils.getExtension(dataFile.getFileName());
        if (ext != null && ext.equalsIgnoreCase(".zip")) {
            final String baseName = localFile.getFileName().toString();

            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(dataFile.toFile());
                ZipEntry zipEntry = zipFile.getEntry(baseName);
                if (zipEntry == null) {
                    zipEntry = zipFile.getEntry(baseName.toLowerCase());
                    if (zipEntry == null) {
                        final String folderName = FileUtils.getFilenameWithoutExtension(dataFile.getFileName());
                        zipEntry = zipFile.getEntry(folderName + '/' + localFile.getFileName());
                        if (zipEntry == null) {
                            localFileExists = false;
                            throw new IOException("Entry '" + baseName + "' not found in zip file.");
                        }
                    }
                }

                return zipFile.getInputStream(zipEntry);
            } finally {
                if (zipFile != null)
                    zipFile.close();
            }
        }
        return null;
    }
}
