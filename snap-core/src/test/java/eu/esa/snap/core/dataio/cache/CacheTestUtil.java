package eu.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

class CacheTestUtil {

    static ProductData createPreparedBuffer(int dataType, int numElems) {
        final ProductData productData = ProductData.createInstance(dataType, numElems);
        for (int i = 0; i < numElems; i++) {
            productData.setElemIntAt(i, i);
        }
        return productData;
    }
}
