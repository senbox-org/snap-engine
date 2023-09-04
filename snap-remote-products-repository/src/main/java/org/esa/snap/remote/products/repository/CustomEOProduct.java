package org.esa.snap.remote.products.repository;

import org.esa.snap.remote.products.repository.listener.ProgressListener;
import ro.cs.tao.eodata.EOProduct;

public class CustomEOProduct extends EOProduct {

    private static ProgressListener progressListener;

    public void setProgressListener(ProgressListener progressListener){
        CustomEOProduct.progressListener = progressListener;
    }

    @Override
    public void setApproximateSize(long approximateSize) {
        super.setApproximateSize(approximateSize);

        if (approximateSize > 0) {
            CustomEOProduct.progressListener.notifyApproximateSize(approximateSize);
        }
    }

}
