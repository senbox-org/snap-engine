package org.esa.snap.remote.products.repository.donwload;

import ro.cs.tao.datasource.ProductStatusListener;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.enums.ProductStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jcoravu on 7/2/2020.
 */
class DownloadProductStatusListener implements ProductStatusListener {

    private final List<String> downloadMessages;

    DownloadProductStatusListener() {
        this.downloadMessages = new ArrayList<>();
    }

    @Override
    public boolean downloadStarted(EOProduct eoProduct) {
        eoProduct.setProductStatus(ProductStatus.DOWNLOADING);
        return true;
    }

    @Override
    public void downloadCompleted(EOProduct eoProduct) {
        eoProduct.setProductStatus(ProductStatus.DOWNLOADED);
    }

    @Override
    public void downloadFailed(EOProduct eoProduct, String message) {
        eoProduct.setProductStatus(ProductStatus.FAILED);
        this.downloadMessages.add(message);
    }

    @Override
    public void downloadAborted(EOProduct eoProduct, String message) {
        this.downloadMessages.add(message);
    }

    @Override
    public void downloadIgnored(EOProduct eoProduct, String message) {
        this.downloadMessages.add(message);
    }

    public List<String> getDownloadMessages() {
        return this.downloadMessages;
    }
}
