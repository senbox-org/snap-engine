package org.esa.snap.vfs.remote;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.attribute.BasicFileAttributes;

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
        String fileSystemRoot = path.getFileSystem().getRoot().getPath();
        URL directoryURL = getDirectoryURL(path);
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
        // the address does not represent a directory
        return readFileAttributes(address, path.toString(), fileSystemRoot);
    }

    protected URL getDirectoryURL(VFSPath path) throws IOException{
        final String address = path.buildURL().toString();
        final String fileSystemSeparator = path.getFileSystem().getSeparator();
        return new URL(address + (address.endsWith(fileSystemSeparator) ? "" : fileSystemSeparator));
    }

    private BasicFileAttributes readFileAttributes(String urlAddress, String filePath, String fileSystemRoot) throws IOException {
        RegularFileMetadata regularFileMetadata = HttpUtils.readRegularFileMetadata(urlAddress, this.remoteConnectionBuilder, fileSystemRoot);
        return VFSFileAttributes.newFile(filePath, regularFileMetadata);
    }
}
