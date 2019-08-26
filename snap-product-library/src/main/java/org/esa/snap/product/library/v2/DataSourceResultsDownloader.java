package org.esa.snap.product.library.v2;

import org.apache.http.auth.Credentials;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by jcoravu on 26/8/2019.
 */
public interface DataSourceResultsDownloader {

    public List<ProductLibraryItem> downloadProductList(Credentials credentials, String mission, Map<String, Object> parameterValues,
                                                        ProductsDownloaderListener downloaderListener, ThreadStatus thread)
                                                        throws java.lang.InterruptedException;

    public BufferedImage downloadProductQuickLookImage(Credentials credentials, String url, ThreadStatus thread)
                                                throws IOException, java.lang.InterruptedException;
}
