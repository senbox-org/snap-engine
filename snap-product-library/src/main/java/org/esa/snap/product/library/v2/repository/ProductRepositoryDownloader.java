package org.esa.snap.product.library.v2.repository;

import org.esa.snap.product.library.v2.RepositoryProduct;
import org.esa.snap.product.library.v2.ProgressListener;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 26/8/2019.
 */
public interface ProductRepositoryDownloader {

    public Path download(RepositoryProduct product, Path targetFolderPath, ProgressListener progressListener) throws IOException;

    public void cancel();
}
