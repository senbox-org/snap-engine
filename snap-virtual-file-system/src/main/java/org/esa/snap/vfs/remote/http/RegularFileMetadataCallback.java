package org.esa.snap.vfs.remote.http;

import org.esa.snap.vfs.remote.HttpUtils;
import org.esa.snap.vfs.remote.IRemoteConnectionBuilder;
import org.esa.snap.vfs.remote.RegularFileMetadata;

import java.io.IOException;

/**
 * Created by jcoravu on 17/5/2019.
 */
public class RegularFileMetadataCallback {

    private final String urlAddress;
    private final IRemoteConnectionBuilder remoteConnectionBuilder;
    private final String fileSystemRoot;

    RegularFileMetadataCallback(String urlAddress, IRemoteConnectionBuilder remoteConnectionBuilder, String fileSystemRoot) {
        this.urlAddress = urlAddress;
        this.remoteConnectionBuilder = remoteConnectionBuilder;
        this.fileSystemRoot = fileSystemRoot;
    }

    public RegularFileMetadata readFileMetadata() throws IOException {
        return HttpUtils.readRegularFileMetadata(this.urlAddress, this.remoteConnectionBuilder, this.fileSystemRoot);
    }
}
