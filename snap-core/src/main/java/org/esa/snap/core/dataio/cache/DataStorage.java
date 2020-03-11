package org.esa.snap.core.dataio.cache;

import org.esa.snap.core.datamodel.ProductData;

interface DataStorage {

    void readRasterData(int offsetX, int offsetY, int width, int height, ProductData buffer);
}
