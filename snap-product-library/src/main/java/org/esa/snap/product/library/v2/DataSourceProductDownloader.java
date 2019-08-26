package org.esa.snap.product.library.v2;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by jcoravu on 26/8/2019.
 */
public interface DataSourceProductDownloader {

    public Path download(ProductLibraryItem product, ProgressListener progressListener) throws IOException;

    public void cancel();
}
