package org.esa.snap.vfs.remote;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Queue;

/**
 * AbstractRemoteWalker for VFSWalker
 *
 * @author Jean Coravu
 * @author Adrian Drăghici
 */
public abstract class AbstractRemoteWalker implements VFSWalker {

    protected final IRemoteConnectionBuilder remoteConnectionBuilder;
    final static private Queue<String> dirsCache = new CircularFifoQueue<>(1000);

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
        if (dirsCache.contains(address)) {
            return VFSFileAttributes.newDir(path.toString());
        }
        try {
            // the address can represent a file
            return readFileAttributes(address, path.toString(), fileSystemRoot);
        } catch (IOException ioe) {
            if (!ioe.getMessage().contains("404") && !ioe.getMessage().contains("not a file")) {
                throw ioe;
            }
            walk(path);
            // the address represents a directory
            dirsCache.add(address);
            return VFSFileAttributes.newDir(path.toString());
        }
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
