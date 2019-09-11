package org.esa.snap.vfs.remote;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import org.esa.snap.core.util.io.FileUtils;

/**
 * AbstractRemoteWalker for VFSWalker
 *
 * @author Jean Coravu
 * @author Adrian Drăghici
 */
public abstract class AbstractRemoteWalker implements VFSWalker {

    protected final IRemoteConnectionBuilder remoteConnectionBuilder;

    protected AbstractRemoteWalker(IRemoteConnectionBuilder remoteConnectionBuilder) {
        this.remoteConnectionBuilder = remoteConnectionBuilder;
    }

    /**
     * Gets the VFS file basic attributes.
     *
     * @return The HTTP file basic attributes
     * @throws IOException If an I/O error occurs
     */
    @Override
    public BasicFileAttributes readBasicFileAttributes(VFSPath path) throws IOException {
        // check if the address represents a directory
        String address = path.buildURL().toString();
        String fileSystemSeparator = path.getFileSystem().getSeparator();
        String fileSystemRoot = path.getFileSystem().getRoot().getPath();
        URL directoryURL = new URL(address + (address.endsWith(fileSystemSeparator) ? "" : fileSystemSeparator));
        HttpURLConnection connection = this.remoteConnectionBuilder.buildConnection(fileSystemRoot, directoryURL, "GET", null);
        try {
            int responseCode = connection.getResponseCode();
            if (HttpUtils.isValidResponseCode(responseCode)) {
                // the address represents a directory
                return VFSFileAttributes.newDir(path.toString());
            }
        } finally {
            connection.disconnect();
        }

        // the path may be a valid directory but not accesible, but its content be right!
        // e.g. Sentinel S1 bucket at AWS (https://sentinel-s1-l1c.s3.amazonaws.com)
        final String extension = FileUtils.getExtension(path.getPath());
        if (extension == null || extension.length() == 0) {
            try {
                List<BasicFileAttributes> attribList = this.walk(path);
                if (attribList.size() > 0) {
                    return VFSFileAttributes.newDir(path.toString());
                }
            }
            catch (Exception ignored) {
                //leave default
            }
        }

        // the address does not represent a directory
        return readFileAttributes(address, path.toString(), fileSystemRoot);
    }

    private BasicFileAttributes readFileAttributes(String urlAddress, String filePath, String fileSystemRoot) throws IOException {
        RegularFileMetadata regularFileMetadata = HttpUtils.readRegularFileMetadata(urlAddress, this.remoteConnectionBuilder, fileSystemRoot);
        return VFSFileAttributes.newFile(filePath, regularFileMetadata);
    }
}
