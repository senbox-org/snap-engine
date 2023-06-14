package org.esa.snap.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class MG4LidarDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public MG4LidarDriverProductReaderPlugIn() {
        super(".view", "MG4Lidar", "MrSID Generation 4 / Lidar");
    }
}
