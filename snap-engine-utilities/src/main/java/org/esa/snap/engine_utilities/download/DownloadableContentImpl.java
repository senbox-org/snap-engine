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
package org.esa.snap.engine_utilities.download;

import com.bc.ceres.core.runtime.internal.IOHelper;
import org.esa.snap.core.dataop.downloadable.FtpDownloader;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.core.util.PropertyMap;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Retrieves downloadable content
 */
public abstract class DownloadableContentImpl implements DownloadableContent {

    private final Path localFile;
    private final Path localZipFile;
    private boolean localFileExists = false;
    private boolean remoteFileExists = true;
    private boolean errorInLocalFile = false;
    private DownloadableFile contentFile = null;
    private FtpDownloader ftp = null;
    private Map<String, Long> fileSizeMap = null;
    private boolean unrecoverableError = false;

    private final URL remoteURL;

    private int remoteVersion = 0;

    private final static String versionFileName = "contentVersion.txt";

    public DownloadableContentImpl(final Path localFile,
                                   final URL remoteURL, final String archiveExt) {
        this.remoteURL = remoteURL;

        this.localFile = localFile;
        this.localZipFile = FileUtils.exchangeExtension(localFile, archiveExt);
    }

    public void dispose() {
        try {
            if (ftp != null)
                ftp.disconnect();
            ftp = null;
            contentFile.dispose();
            contentFile = null;
        } catch (Exception e) {
            //
        }
    }

    public final DownloadableFile getContentFile() throws IOException {
        if (contentFile == null) {
            if (!remoteFileExists && !localFileExists)
                return null;
            findFile();
        }
        return contentFile;
    }

    public String getFileName() {
        return localFile.getFileName().toString();
    }

    protected abstract DownloadableFile createContentFile(final Path dataFile);

    private boolean findLocalFile() {
        return (Files.exists(localFile) && Files.isRegularFile(localFile)) || (Files.exists(localZipFile) && Files.isRegularFile(localZipFile));
    }

    private boolean getRemoteFile() throws IOException {
        if(remoteURL == null) {
            throw new IOException("DownloadableContent: Remote archive url not set");
        }
        if (remoteURL.getProtocol().contains("http")) {
            try {
                boolean newVersionAvailable = checkForNewRemoteHttpFile(remoteURL, localZipFile);
                if(newVersionAvailable) {
                    getRemoteHttpFile(remoteURL, localZipFile);

                    saveNewVersion(localZipFile);
                    return true;
                }
            } catch (Exception e) {
                SystemUtils.LOG.warning("http error:" + e.getMessage() + " on " + remoteURL + localZipFile.getFileName());
                remoteFileExists = false;
                return false;
            }
        } else {
            return getRemoteFTPFile(remoteURL);
        }
        return false;
    }

    private boolean checkForNewRemoteHttpFile(final URL remoteURL, final Path localZipFile) throws IOException {

        final Path remoteVersionFile = localZipFile.getParent().resolve("remote_"+versionFileName);
        try {
            downloadFile(new URL(remoteURL.toString() + remoteVersionFile.getFileName()), remoteVersionFile);
        } catch (Exception e) {
            // remote version file not found
            // continue
        }

        boolean newVersion = true;
        if(Files.exists(remoteVersionFile)) {
            final PropertyMap remoteVersionMap = new DefaultPropertyMap();
            remoteVersionMap.load(remoteVersionFile);

            remoteVersion = remoteVersionMap.getPropertyInt(localZipFile.getFileName().toString());

            final Path localVersionFile = localZipFile.getParent().resolve(versionFileName);
            if(Files.exists(localVersionFile)) {

                final PropertyMap localVersionMap = new DefaultPropertyMap();
                localVersionMap.load(localVersionFile);

                int localVersion = localVersionMap.getPropertyInt(localZipFile.getFileName().toString());

                if(remoteVersion != 0 && localVersion != 0 && remoteVersion == localVersion) {
                    newVersion = false;
                }
            }
            Files.delete(remoteVersionFile);
        }

        return newVersion;
    }

    private void saveNewVersion(final Path localZipFile) throws IOException {
        if(remoteVersion == 0)
            return;

        final Path localVersionFile = localZipFile.getParent().resolve(versionFileName);
        final PropertyMap localVersionMap = new DefaultPropertyMap();
        if(Files.exists(localVersionFile)) {
            localVersionMap.load(localVersionFile);
        }

        localVersionMap.setPropertyInt(localZipFile.getFileName().toString(), remoteVersion);
        localVersionMap.store(localVersionFile, "");
    }

    private synchronized void findFile() throws IOException {
        try {
            if (contentFile != null) return;

            if (!localFileExists && !errorInLocalFile) {
                localFileExists = findLocalFile();
            }
            if (localFileExists) {
                getLocalFile();
            } else if (remoteFileExists) {
                if (getRemoteFile()) {
                    getLocalFile();
                }
            }
            if (contentFile != null) {
                errorInLocalFile = false;
            } else {
                if (!remoteFileExists && localFileExists) {
                    SystemUtils.LOG.warning("Unable to read product " + localFile.toAbsolutePath());
                }
                localFileExists = false;
                errorInLocalFile = true;
            }
        } catch (Exception e) {
            SystemUtils.LOG.warning(e.getMessage());
            contentFile = null;
            localFileExists = false;
            errorInLocalFile = true;
            if (unrecoverableError) {
                throw new IOException(e);
            }
        }
    }

    private void getLocalFile() throws IOException {
        Path dataFile = localFile;
        if (!Files.exists(dataFile))
            dataFile = getFileFromZip(localZipFile);
        if (dataFile != null) {
            contentFile = createContentFile(dataFile);
        }
    }

    public static Path getRemoteHttpFile(final URL remoteURL, final Path localZipFile) {
        final String remotePath = remoteURL.toString() + localZipFile.getFileName();
        SystemUtils.LOG.info("http retrieving " + remotePath);

        final AtomicReference<Path> returnValue = new AtomicReference<>();
        Runnable operation = () -> {
            try {
                returnValue.set(downloadFile(new URL(remotePath), localZipFile));
            } catch (IOException e) {
                SystemUtils.LOG.log(Level.SEVERE, "Failed to download remote file.", e);
            }
        };
        operation.run();

        return returnValue.get();
    }

    /**
     * Downloads a file from the specified URL to the specified local target directory.
     * The method uses a progress monitor to visualize the download process.
     *
     * @param fileUrl      the URL of the file to be downloaded
     * @param localZipFile the target file
     * @return Path the downloaded file
     * @throws java.io.IOException if an I/O error occurs
     */
    private static Path downloadFile(final URL fileUrl, final Path localZipFile) throws IOException {
        final Path outputFile = localZipFile.getParent().resolve(Path.of(fileUrl.getFile()).getFileName());
        final URLConnection urlConnection = fileUrl.openConnection();
        final int contentLength = urlConnection.getContentLength();
        final InputStream is = new BufferedInputStream(urlConnection.getInputStream(), contentLength);
        final OutputStream os;
        try {
            if (!Files.exists(outputFile.getParent())) {
                if(!IOHelper.createDirectories(outputFile.getParent())) {
                    SystemUtils.LOG.severe("Unable to create folders in "+outputFile.getParent());
                }
            }
            os = new BufferedOutputStream(Files.newOutputStream(outputFile));
        } catch (IOException e) {
            is.close();
            throw e;
        }

        final StatusProgressMonitor pm = new StatusProgressMonitor(StatusProgressMonitor.TYPE.DATA_TRANSFER);
        pm.beginTask("Downloading " + localZipFile.getFileName() + "... ", contentLength);

        try {
            final int size = 32768;
            final byte[] buf = new byte[size];
            int n;
            while ((n = is.read(buf, 0, size)) > -1) {
                os.write(buf, 0, n);
                pm.worked(n);
            }

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
            pm.done();
        }
        return outputFile;
    }

    private boolean getRemoteFTPFile(final URL remoteURL) throws IOException {
        try {
            if (ftp == null) {
                ftp = new FtpDownloader(remoteURL.getHost());
                fileSizeMap = FtpDownloader.readRemoteFileList(ftp, remoteURL.getHost(), remoteURL.getPath());
            }

            final String remoteFileName = localZipFile.getFileName().toString();
            final Long fileSize = fileSizeMap.get(remoteFileName);

            final FtpDownloader.FTPError result = ftp.retrieveFile(remoteURL.getPath() + remoteFileName, localZipFile, fileSize);
            if (result == FtpDownloader.FTPError.OK) {
                return true;
            } else {
                if (result == FtpDownloader.FTPError.FILE_NOT_FOUND) {
                    remoteFileExists = false;
                } else {
                    dispose();
                }
                Files.delete(localZipFile);
            }
            return false;
        } catch (SocketException e) {
            unrecoverableError = true;
            throw e;
        } catch (Exception e) {
            SystemUtils.LOG.warning(e.getMessage());
            if (ftp == null) {
                unrecoverableError = false;      // allow to continue
                remoteFileExists = false;
                throw new IOException("Failed to connect to FTP " + remoteURL.getHost() + '\n' + e.getMessage());
            }
            dispose();
        }
        return false;
    }

    private Path getFileFromZip(final Path dataFile) throws IOException {
        final String ext = FileUtils.getExtension(dataFile.getFileName());
        if (".zip".equalsIgnoreCase(ext)) {
            final String baseName = localFile.getFileName().toString();
            final Path newFile = SystemUtils.getCacheDir().resolve(baseName);
            if (Files.exists(newFile))
                return newFile;

            try (ZipFile zipFile = new ZipFile(dataFile.toFile());
                BufferedOutputStream fileOutputStream = new BufferedOutputStream(Files.newOutputStream(newFile))){

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

                final int size = 8192;
                final byte[] buf = new byte[size];
                try (InputStream zipInputStream = zipFile.getInputStream(zipEntry)) {

                    int n;
                    while ((n = zipInputStream.read(buf, 0, size)) > -1)
                        fileOutputStream.write(buf, 0, n);
                }
                return newFile;
            } catch (Exception e) {
                SystemUtils.LOG.warning(e.getMessage());
                Files.delete(dataFile);
                return null;
            }
        }
        return dataFile;
    }
}
