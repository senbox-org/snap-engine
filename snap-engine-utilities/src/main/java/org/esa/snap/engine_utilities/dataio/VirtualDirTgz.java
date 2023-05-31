package org.esa.snap.engine_utilities.dataio;

import com.bc.ceres.core.VirtualDir;
import org.esa.snap.engine_utilities.commons.FilePath;
import org.esa.snap.engine_utilities.commons.FilePathInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.esa.snap.core.util.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Implementation of a virtual directory representing the contents of a tar or a tar-gz file.
 */
public class VirtualDirTgz extends VirtualDirEx {

    private static final int TRANSFER_BUFFER_SIZE = 1024 * 1024;

    private final Path archiveFile;
    private File extractDir;

    public VirtualDirTgz(File tgz) {
        if (tgz == null) {
            throw new IllegalArgumentException("Input file shall not be null");
        }
        this.archiveFile = tgz.toPath();
    }

    public VirtualDirTgz(Path tgz) {
        if (tgz == null) {
            throw new NullPointerException("Input file shall not be null");
        }
        this.archiveFile = tgz;
    }

    public static String getFilenameFromPath(String path) {
        int lastSepIndex = path.lastIndexOf("/");
        if (lastSepIndex == -1) {
            lastSepIndex = path.lastIndexOf("\\");
            if (lastSepIndex == -1) {
                return path;
            }
        }

        return path.substring(lastSepIndex + 1);
    }

    public static boolean isTgz(String filename) {
        String lowerCaseFilename = filename.toLowerCase();
        return lowerCaseFilename.endsWith(".tgz") || lowerCaseFilename.endsWith(".tar.gz");
    }

    public static boolean isTbz(String filename) {
        String lcName = filename.toLowerCase();
        return lcName.endsWith(".tar.bz") || lcName.endsWith(".tbz") ||
                lcName.endsWith(".tar.bz2") || lcName.endsWith(".tbz2");
    }

    public static boolean isTar(String filename) {
        return ".tar".equals(FileUtils.getExtension(filename));
    }

    @Override
    public String getBasePath() {
        return this.archiveFile.toFile().getPath();
    }

    @Override
    public File getBaseFile() {
        return this.archiveFile.toFile();
    }

    @Override
    public Path buildPath(String first, String... more) {
        FileSystem fileSystem = this.archiveFile.getFileSystem();
        return fileSystem.getPath(first, more);
    }

    @Override
    public String getFileSystemSeparator() {
        FileSystem fileSystem = this.archiveFile.getFileSystem();
        return fileSystem.getSeparator();
    }

    @Override
    public FilePathInputStream getInputStream(String childRelativePath) throws IOException {
        Path file = getFile(childRelativePath).toPath();
        InputStream inputStream = Files.newInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        return new FilePathInputStream(file, bufferedInputStream, null);
    }

    @Override
    public FilePath getFilePath(String childRelativePath) throws IOException {
        Path file = getFile(childRelativePath).toPath();
        return new FilePath(file, null);
    }

    @Override
    public File getFile(String childRelativePath) throws IOException {
        ensureUnpacked(null);
        File file = new File(this.extractDir, childRelativePath);
        if (!(file.isFile() || file.isDirectory())) {
            throw new FileNotFoundException("The path '"+childRelativePath+"' does not exist in the folder '"+this.extractDir.getAbsolutePath()+"'.");
        }
        return file;
    }

    @Override
    public String[] list(String path) throws IOException {
        final File file = getFile(path);
        return file.list();
    }

    @Override
    public String[] listAllFiles() throws IOException {
        try (TarArchiveInputStream tarStream = buildTarInputStream()) {

            TarArchiveEntry entry;
            List<String> entryNames = new ArrayList<>();
            while ((entry = tarStream.getNextTarEntry()) != null) {
                if (!entry.isDirectory()) {
                    entryNames.add(entry.getName());
                }
            }
            return entryNames.toArray(new String[0]);
        }
    }

    public boolean exists(String s) {
        return Files.exists(this.archiveFile);
    }

    @Override
    public void close() {
        if (this.extractDir != null) {
            FileUtils.deleteTree(this.extractDir);
            this.extractDir = null;
        }
    }

    @Override
    public boolean isCompressed() {
        final String fileName = this.archiveFile.getFileName().toString();
        return isTgz(fileName) || isTbz(fileName);
    }

    @Override
    public boolean isArchive() {
        return true;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    @Override
    public File getTempDir() {
        return this.extractDir;
    }

    @Override
    public Path makeLocalTempFolder() throws IOException {
        if (this.extractDir == null) {
            this.extractDir = VirtualDir.createUniqueTempDir();
        }
        return this.extractDir.toPath();
    }

    public void ensureUnpacked(File unpackFolder) throws IOException {
        if (this.extractDir == null) {
            this.extractDir = (unpackFolder == null) ? VirtualDir.createUniqueTempDir() : unpackFolder;

            try (TarArchiveInputStream tarStream = buildTarInputStream()) {
                byte[] data = new byte[TRANSFER_BUFFER_SIZE];
                TarArchiveEntry entry;
                String longLink = null;
                while ((entry = tarStream.getNextTarEntry()) != null) {
                    String entryName = entry.getName();
                    boolean entryIsLink = entry.isLink() || entry.isSymbolicLink();
                    if (longLink != null && longLink.startsWith(entryName)) {
                        entryName = longLink;
                        longLink = null;
                    }
                    if (entry.isDirectory()) {
                        File directory = new File(this.extractDir, entryName);
                        ensureDirectory(directory);
                        continue;
                    }

                    final String fileNameFromPath = getFilenameFromPath(entryName);
                    final int pathIndex = entryName.indexOf(fileNameFromPath);
                    String tarPath = null;
                    if (pathIndex > 0) {
                        tarPath = entryName.substring(0, pathIndex - 1);
                    }

                    File targetDir;
                    if (tarPath == null) {
                        targetDir = this.extractDir;
                    } else {
                        targetDir = new File(this.extractDir, tarPath);
                    }

                    ensureDirectory(targetDir);
                    File targetFile = new File(targetDir, fileNameFromPath);
                    if (!entryIsLink && targetFile.isFile()) {
                        continue;
                    }

                    if (!entryIsLink && !targetFile.createNewFile()) {
                        throw new IOException("Unable to create file: " + targetFile.getAbsolutePath());
                    }

                    try (FileOutputStream fileOutputStream = new FileOutputStream(targetFile);
                         BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {

                        int count;
                        while ((count = tarStream.read(data)) != -1) {
                            bufferedOutputStream.write(data, 0, count);
                            //if the entry is a link, must be saved, since the name of the next entry depends on this
                            if (entryIsLink) {
                                longLink = (longLink == null ? "" : longLink) + new String(data, 0, count);
                            } else {
                                longLink = null;
                            }
                        }
                        // the last character is \u0000, so it must be removed
                        if (longLink != null) {
                            longLink = longLink.substring(0, longLink.length() - 1);
                        }
                        bufferedOutputStream.flush();
                    }
                }
            }
        }
    }

    private TarArchiveInputStream buildTarInputStream() throws IOException {
        final CompressorStreamFactory compressorStreamFactory = new CompressorStreamFactory();
        CompressorInputStream compressorInputStream = null;
        InputStream stream = null;
        try {
            stream = new BufferedInputStream(Files.newInputStream(this.archiveFile));
            compressorInputStream = compressorStreamFactory.createCompressorInputStream(stream);
            return new TarArchiveInputStream(compressorInputStream);
        } catch (IOException | CompressorException | IllegalArgumentException ex) {
            if (stream == null) {
                throw new IOException("Cannot open file");
            }
            // maybe it's just a simple tar
            return new TarArchiveInputStream(stream);
        }
    }

    private void ensureDirectory(File targetDir) throws IOException {
        if (!targetDir.isDirectory()) {
            if (!targetDir.mkdirs()) {
                throw new IOException("unable to create directory: " + targetDir.getAbsolutePath());
            }
        }
    }

    @Override
    public String[] listAll(Pattern...patterns) {
        List<String> fileNames;
        try (TarArchiveInputStream tis = buildTarInputStream()) {

            fileNames = new ArrayList<>();
            byte[] data = new byte[TRANSFER_BUFFER_SIZE];
            TarArchiveEntry entry;
            String longLink = null;
            while ((entry = tis.getNextTarEntry()) != null) {
                String entryName = entry.getName();
                boolean entryIsLink = entry.isLink() || entry.isSymbolicLink();
                if (longLink != null && longLink.startsWith(entryName)) {
                    entryName = longLink;
                    longLink = null;
                }
                // if the entry is a link, must be saved, since the name of the next entry depends on this
                if (entryIsLink) {
                    int count;
                    while ((count = tis.read(data)) != -1) {
                        longLink = (longLink == null ? "" : longLink) + new String(data, 0, count);
                    }
                } else {
                    longLink = null;
                    fileNames.add(entryName);
                }
                // the last character is \u0000, so it must be removed
                if (longLink != null) {
                    longLink = longLink.substring(0, longLink.length() - 1);
                }
            }
        } catch (IOException e) {
            // cannot open/read tar, list will be empty
            fileNames = new ArrayList<>();
        }
        return fileNames.toArray(new String[0]);
    }
}
