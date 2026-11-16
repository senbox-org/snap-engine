package org.esa.snap.engine_utilities.dataio;

import com.bc.ceres.core.VirtualDir;
import com.bc.ceres.core.runtime.internal.IOHelper;
import com.bc.ceres.util.CleanUpState;
import com.bc.ceres.util.CleanerRegistry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.engine_utilities.commons.FilePath;
import org.esa.snap.engine_utilities.commons.FilePathInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Implementation of a virtual directory representing the contents of a tar or a tar-gz file.
 */
public class VirtualDirTgz extends VirtualDirEx {

    private static final int TRANSFER_BUFFER_SIZE = 1024 * 1024;

    private final Path archiveFile;
    private final VirtualDirTgzState state;

    public VirtualDirTgz(Path tgz) {
        if (tgz == null) {
            throw new IllegalArgumentException("Input file shall not be null");
        }
        archiveFile = tgz;
        state = new VirtualDirTgzState();
        CleanerRegistry.getInstance().register(this, state);
    }

    public static boolean isTgz(String filename) {
        final String lowerCaseFilename = filename.toLowerCase();
        return lowerCaseFilename.endsWith(".tgz") || lowerCaseFilename.endsWith(".tar.gz");
    }

    public static boolean isTbz(String filename) {
        final String lcName = filename.toLowerCase();
        return lcName.endsWith(".tar.bz") || lcName.endsWith(".tbz") ||
                lcName.endsWith(".tar.bz2") || lcName.endsWith(".tbz2");
    }

    @Override
    public String getBasePath() {
        return archiveFile.toString();
    }

    @Override
    public Path getBaseFile() {
        return archiveFile;
    }

    @Override
    public Path buildPath(String first, String... more) {
        FileSystem fileSystem = archiveFile.getFileSystem();
        return fileSystem.getPath(first, more);
    }

    @Override
    public String getFileSystemSeparator() {
        FileSystem fileSystem = archiveFile.getFileSystem();
        return fileSystem.getSeparator();
    }

    @Override
    public FilePathInputStream getInputStream(String childRelativePath) throws IOException {
        Path file = getFile(childRelativePath);
        InputStream inputStream = Files.newInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        return new FilePathInputStream(file, bufferedInputStream, null);
    }

    @Override
    public FilePath getFilePath(String childRelativePath) throws IOException {
        Path file = getFile(childRelativePath);
        return new FilePath(file, null);
    }

    @Override
    public Path getFile(String childRelativePath) throws IOException {
        ensureUnpacked(null);
        Path file = state.extractDir.resolve(childRelativePath);
        if (!(Files.isRegularFile(file) || Files.isDirectory(file))) {
            final Path fileFromMapping = state.locationMap.get(childRelativePath);
            if (fileFromMapping == null) {
                throw new FileNotFoundException("The path '" + childRelativePath + "' does not exist in the folder '" + state.extractDir.toAbsolutePath() + "'.");
            }
            return fileFromMapping;
        }
        return file;
    }

    @Override
    public String[] list(String path) throws IOException {
        final Path file = getFile(path);
        return IOHelper.list(file).toArray(new String[0]);
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
        return Files.exists(archiveFile);
    }

    @Override
    public void close() {
        CleanerRegistry.getInstance().cleanup(this);
    }

    @Override
    public boolean isCompressed() {
        final String fileName = archiveFile.getFileName().toString();
        return isTgz(fileName) || isTbz(fileName);
    }

    @Override
    public boolean isArchive() {
        return true;
    }


    @Override
    public Path getTempDir() {
        return state.extractDir;
    }

    @Override
    public Path makeLocalTempFolder() throws IOException {
        if (state.extractDir == null) {
            state.extractDir = VirtualDir.createUniqueTempDir();
        }
        return state.extractDir;
    }

    public void ensureUnpacked(Path unpackFolder) throws IOException {
        if (state.extractDir == null) {
            state.extractDir = (unpackFolder == null) ? VirtualDir.createUniqueTempDir() : unpackFolder;

            state.locationMap = new HashMap<>();
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
                        Path directory = state.extractDir.resolve(entryName);
                        ensureDirectory(directory, state.extractDir);

                        continue;
                    }

                    final String fileNameFromPath = FileUtils.getFilenameFromPath(entryName);
                    final int pathIndex = entryName.indexOf(fileNameFromPath);
                    String tarPath = null;
                    if (pathIndex > 0) {
                        tarPath = entryName.substring(0, pathIndex - 1);
                    }

                    Path targetDir;
                    if (tarPath == null) {
                        targetDir = state.extractDir;
                    } else {
                        targetDir = state.extractDir.resolve(tarPath);
                    }

                    final Path ensuredDirectory = ensureDirectory(targetDir, state.extractDir);
                    final Path targetFile = ensuredDirectory.resolve(fileNameFromPath);
                    if (!entryIsLink && Files.isRegularFile(targetFile)) {
                        continue;
                    }

                    if (!entryIsLink && !IOHelper.createFile(targetFile)) {
                        throw new IOException("Unable to create file: " + targetFile.toAbsolutePath());
                    }

                    state.locationMap.put(entryName, targetFile);

                    try (OutputStream fileOutputStream = Files.newOutputStream(targetFile);
                         BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {

                        int count;
                        while ((count = tarStream.read(data)) != -1) {
                            bufferedOutputStream.write(data, 0, count);
                            //if the entry is a link, must be saved, since the name of the next entry depends on this
                            if (entryIsLink) {
                                longLink = (longLink == null ? "" : longLink) + new String(data, 0, count, StandardCharsets.UTF_8);
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
            stream = new BufferedInputStream(Files.newInputStream(archiveFile));
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

    private Path ensureDirectory(Path targetDir, Path extractDir) throws IOException {
        if (!Files.isDirectory(targetDir)) {
            if (!IOHelper.createDirectories(targetDir)) {
                // if this fails, we try to correct eventually present invalid characters in the path or
                // shorten the path if it is too long for windows.
                final Path ensuredTargetDir = ensureCorrectPathSegments(targetDir, extractDir);
                if (!Files.isDirectory(ensuredTargetDir)) {
                    if (!IOHelper.createDirectories(ensuredTargetDir)) {
                        throw new IOException("unable to create directory: " + ensuredTargetDir.toAbsolutePath());
                    }
                }
                return ensuredTargetDir;
            }
        }

        return targetDir;
    }

    static Path ensureCorrectPathSegments(Path targetDir, Path extractDir) {
        final String extractDirName = FileUtils.getFilenameWithoutExtension(extractDir);
        final StringTokenizer stringTokenizer = new StringTokenizer(targetDir.toAbsolutePath().toString(), IOHelper.SEPARATOR);

        final ArrayList<String> segmentList = new ArrayList<>();
        int pathLength = 0;
        while (stringTokenizer.hasMoreTokens()) {
            final String token = stringTokenizer.nextToken();
            segmentList.add(token);

            pathLength += token.length() + 1;
        }

        // overall target path is too long for windows, replace with a unique, shorter path
        if (pathLength > 255) {
            final long nanoTime = System.nanoTime();
            Path path = Paths.get(extractDir.toAbsolutePath().toString());
            path = path.resolve(Long.toString(nanoTime));
            return path;
        }

        final String[] segments = segmentList.toArray(new String[0]);
        final int index = ArrayUtils.indexOf(segments, extractDirName) + 1;
        Path path = Paths.get(extractDir.toAbsolutePath().toString());
        for (int i = index; i < segments.length; i++) {
            segments[i] = StringUtils.createValidName(segments[i], new char[]{'.', '-', '/'}, '_');
            path = path.resolve(segments[i]);
        }

        return path;
    }

    @Override
    public String[] listAll(Pattern... patterns) {
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
                        longLink = (longLink == null ? "" : longLink) + new String(data, 0, count, StandardCharsets.UTF_8);
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


    private static final class VirtualDirTgzState implements CleanUpState {
        private Path extractDir;
        private HashMap<String, Path> locationMap;

        @Override
        public synchronized void run() {
            if (extractDir != null) {
                FileUtils.deleteTree(extractDir);
                extractDir = null;
            }
            if (locationMap != null) {
                locationMap.clear();
                locationMap = null;
            }
        }
    }
}
