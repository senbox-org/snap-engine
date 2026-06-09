package org.esa.snap.remote.products.repository.cdse;

import org.esa.snap.remote.products.repository.HTTPServerException;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

interface CdseHttpClient {

    CdseHttpResponse execute(CdseHttpRequest request) throws IOException;

    default void download(CdseHttpRequest request, Path targetFile, ProgressListener progressListener) throws IOException {
        CdseHttpResponse response = execute(request);
        if (!response.isSuccessful()) {
            throw new HTTPServerException(response.getStatusCode(), response.getBodyAsString());
        }
        Files.write(targetFile, response.getBody());
        if (progressListener != null) {
            progressListener.notifyProgress((short) 100);
        }
    }
}
