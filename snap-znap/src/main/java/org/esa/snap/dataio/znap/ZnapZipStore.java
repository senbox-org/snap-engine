package org.esa.snap.dataio.znap;

import com.bc.zarr.ZarrConstants;
import com.bc.zarr.ZarrUtils;
import com.bc.zarr.storage.Store;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ZnapZipStore implements Store {
    private final FileSystem zfs;
    private final Path internalRoot;

    public ZnapZipStore(Path zipFilePath) throws IOException {
        final HashMap<String, String> zipParams = new HashMap<>();
        if (!Files.exists(zipFilePath)) {
            zipParams.put("create", "true");
            zipParams.put("noCompression", "true");
        }
        final URI uri = URI.create("jar:file:" + zipFilePath.toUri().getPath());
        zfs = getFileSystem(zipParams, uri);
        internalRoot = zfs.getRootDirectories().iterator().next();
    }

    @Override
    public InputStream getInputStream(String key) throws IOException {
        final Path path = internalRoot.resolve(key);
        if (Files.isReadable(path)) {
            byte[] bytes = Files.readAllBytes(path);
            return new ByteArrayInputStream(bytes);
        }
        return null;
    }

    @Override
    public OutputStream getOutputStream(String key) {
        return new ByteArrayOutputStream() {
            private boolean closed = false;

            @Override
            public void close() throws IOException {
                try {
                    if (!closed) {
                        final byte[] bytes = this.toByteArray();
                        final Path filePath = internalRoot.resolve(key);
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                        } else {
                            Files.createDirectories(filePath.getParent());
                        }
                        Files.write(filePath, bytes);
                    }
                } finally {
                    closed = true;
                }
            }
        };
    }

    @Override
    public void delete(String key) throws IOException {
        final Path toBeDeleted = internalRoot.resolve(key);
        if (Files.isDirectory(toBeDeleted)) {
            ZarrUtils.deleteDirectoryTreeRecursively(toBeDeleted);
        }
        if (Files.exists(toBeDeleted)) {
            Files.delete(toBeDeleted);
        }
        if (Files.exists(toBeDeleted) || Files.isDirectory(toBeDeleted)) {
            throw new IOException("Unable to initialize " + toBeDeleted.toAbsolutePath());
        }
    }

    @Override
    public TreeSet<String> getArrayKeys() throws IOException {
        return getParentsOf(ZarrConstants.FILENAME_DOT_ZARRAY);
    }

    @Override
    public TreeSet<String> getGroupKeys() throws IOException {
        return getParentsOf(ZarrConstants.FILENAME_DOT_ZGROUP);
    }

    private TreeSet<String> getParentsOf(String suffix) throws IOException {
        return getKeysEndingWith(suffix).stream()
                .map(s -> internalRoot.relativize(internalRoot.resolve(s).getParent()).toString())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public TreeSet<String> getKeysEndingWith(String suffix) throws IOException {
        return Files.walk(internalRoot)
                .filter(path -> path.toString().endsWith(suffix))
                .map(path -> internalRoot.relativize(path).toString())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public Stream<String> getRelativeLeafKeys(String key) throws IOException {
        final Path walkingRoot = internalRoot.resolve(key);
        return Files.walk(walkingRoot)
                .filter(path -> !Files.isDirectory(path))
                .map(path -> walkingRoot.relativize(path).toString())
                .map(ZarrUtils::normalizeStoragePath)
                .filter(s -> s.trim().length() > 0);
    }

    @Override
    public void close() throws IOException {
        zfs.close();
    }

    // Gets or creates a FileSystem
    private FileSystem getFileSystem(HashMap<String, String> zipParams, URI uri) throws IOException {
        try {
            return FileSystems.newFileSystem(uri, zipParams);
        } catch (FileSystemAlreadyExistsException e) {
            return FileSystems.getFileSystem(uri);
        }
    }
}
