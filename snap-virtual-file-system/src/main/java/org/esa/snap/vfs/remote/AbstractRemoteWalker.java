package org.esa.snap.vfs.remote;

import java.io.IOException;
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
        try {
            // the address can represent a file
            return readFileAttributes(address, path.toString(), fileSystemRoot);
        } catch (IOException ioe) {
            if (!ioe.getMessage().contains("404") && !ioe.getMessage().contains("not a file")) {
                throw ioe;
            }
            walk(path);
            // the address represents a directory
            return VFSFileAttributes.newDir(path.toString());
        }
    }

    private BasicFileAttributes readFileAttributes(String urlAddress, String filePath, String fileSystemRoot) throws IOException {
        RegularFileMetadata regularFileMetadata = HttpUtils.readRegularFileMetadata(urlAddress, this.remoteConnectionBuilder, fileSystemRoot);
        return VFSFileAttributes.newFile(filePath, regularFileMetadata);
    }
}
