package org.esa.snap.remote.products.repository;

import org.apache.http.auth.Credentials;
import org.esa.snap.remote.products.repository.listener.ProgressListener;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 26/8/2019.
 */
public interface ProductRepositoryDownloader {

    public String getRepositoryId();

    public Path download(RepositoryProduct product, Credentials credentials, Path targetFolderPath, ProgressListener progressListener) throws InterruptedException, IOException;

    public void cancel();
}
